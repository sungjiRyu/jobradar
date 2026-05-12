package com.jobradar.backend.job.service;

import com.jobradar.backend.crawler.JobkoreaCrawlerService;
import com.jobradar.backend.crawler.SaraminCrawlerService;
import com.jobradar.backend.global.config.AiSummaryService;
import com.jobradar.backend.global.exception.CustomException;
import com.jobradar.backend.global.exception.ErrorCode;
import com.jobradar.backend.job.dto.DescriptionResponse;
import com.jobradar.backend.job.dto.JobDetailResponse;
import com.jobradar.backend.job.dto.JobResponse;
import com.jobradar.backend.job.dto.SummaryResponse;
import com.jobradar.backend.job.entity.Job;
import com.jobradar.backend.job.repository.JobRepository;
import com.jobradar.backend.job.repository.JobSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final SaraminCrawlerService saraminCrawlerService;
    private final JobkoreaCrawlerService jobkoreaCrawlerService;
    private final AiSummaryService aiSummaryService;

    /**
     * 동적 검색 — Specification을 조합해 WHERE 조건 구성
     * 각 파라미터가 null/빈 리스트면 해당 조건을 추가하지 않음
     */
    @Transactional(readOnly = true)
    public Page<JobResponse> search(String keyword,
                                    List<String> locations,
                                    List<String> experiences,
                                    List<String> techStacks,
                                    List<String> jobTypes,
                                    boolean todayOnly,
                                    boolean urgentOnly,
                                    Pageable pageable) {
        // ACTIVE 조건은 항상 포함
        Specification<Job> spec = JobSpecification.isActive();

        if (keyword != null && !keyword.isBlank()) {
            spec = spec.and(JobSpecification.hasKeyword(keyword));
        }
        if (!CollectionUtils.isEmpty(locations)) {
            spec = spec.and(JobSpecification.locationContains(locations));
        }
        if (!CollectionUtils.isEmpty(experiences)) {
            spec = spec.and(JobSpecification.experienceContains(experiences));
        }
        if (!CollectionUtils.isEmpty(techStacks)) {
            spec = spec.and(JobSpecification.hasTechStack(techStacks));
        }
        if (!CollectionUtils.isEmpty(jobTypes)) {
            spec = spec.and(JobSpecification.hasJobType(jobTypes));
        }
        if (todayOnly) {
            spec = spec.and(JobSpecification.isCreatedToday());
        }
        if (urgentOnly) {
            spec = spec.and(JobSpecification.isUrgent());
        }

        return jobRepository.findAll(spec, pageable).map(JobResponse::from);
    }

    @Transactional
    public JobDetailResponse getDetail(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new CustomException(ErrorCode.JOB_NOT_FOUND));
        job.incrementViewCount();
        return JobDetailResponse.from(job);
    }

    @Transactional
    public DescriptionResponse getDescription(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new CustomException(ErrorCode.JOB_NOT_FOUND));

        // 크롤러가 eager fetch한 결과 상태에 따라 즉시 응답
        // status가 null인 경우(기존 데이터)에만 lazy fetch로 폴백
        Job.DescriptionStatus status = job.getDescriptionStatus();
        if (status != null) {
            return switch (status) {
                case SUCCESS  -> DescriptionResponse.success(job.getDescription());
                case IMAGE    -> DescriptionResponse.image();
                case FAILED   -> DescriptionResponse.crawlFailed();
                case EXTERNAL -> DescriptionResponse.external();
            };
        }

        // 기존 데이터(descriptionStatus = null) → lazy fetch 후 결과 저장
        DescriptionResponse result = fetchDescriptionBySourceSite(job);
        Job.DescriptionStatus newStatus = mapStatus(result.getStatus());
        job.updateDescription(result.getDescription(), newStatus);
        log.info("[JobService] lazy fetch 완료: jobId={}, status={}", jobId, newStatus);

        return result;
    }

    /**
     * sourceSite에 맞는 크롤러로 description fetch.
     * 빈 데이터 폴백, 백필 API에서도 동일하게 사용
     */
    private DescriptionResponse fetchDescriptionBySourceSite(Job job) {
        return switch (job.getSourceSite()) {
            case "사람인" -> saraminCrawlerService.fetchDescription(job.getSourceUrl());
            case "잡코리아" -> jobkoreaCrawlerService.fetchDescription(job.getSourceUrl());
            default -> DescriptionResponse.crawlFailed();
        };
    }

    /** DescriptionResponse.status(문자열) → Job.DescriptionStatus enum 변환 */
    private Job.DescriptionStatus mapStatus(String responseStatus) {
        return switch (responseStatus) {
            case "SUCCESS"  -> Job.DescriptionStatus.SUCCESS;
            case "IMAGE"    -> Job.DescriptionStatus.IMAGE;
            case "EXTERNAL" -> Job.DescriptionStatus.EXTERNAL;
            default         -> Job.DescriptionStatus.FAILED; // CRAWL_FAILED 포함
        };
    }

    /**
     * 마감일 지난 ACTIVE 공고를 CLOSED로 일괄 업데이트
     * 크롤링 직후 호출 (스케줄러 및 수동 크롤 API에서 사용)
     *
     * @return 영향 받은 행 수
     */
    @Transactional
    public int closeExpiredJobs() {
        int closed = jobRepository.closeExpiredJobs();
        log.info("[JobService] 마감일 지난 공고 {}건 CLOSED 처리", closed);
        return closed;
    }

    /**
     * 기존에 description이 아직 fetch되지 않은 공고들을 일괄 백필
     *
     * - description_status가 null인 공고만 대상 (이미 처리된 공고는 스킵)
     * - sourceSite에 맞는 크롤러로 fetchDescription 호출
     * - 각 호출 사이 1초 sleep으로 외부 사이트 부하 분산
     * - @Async로 비동기 실행 → HTTP 요청은 즉시 반환, 실제 처리는 백그라운드
     *
     * 트랜잭션: 각 공고를 별도 트랜잭션이 아닌 단일 트랜잭션으로 감싸면 너무 길어지므로
     * 메서드 단위가 아닌 공고 단위로 분리 — 아래 fetchAndSaveDescription 메서드에 @Transactional 부여
     */
    @Async
    public void backfillDescriptions() {
        List<Job> targets = jobRepository.findByDescriptionStatusIsNull();
        int total = targets.size();
        log.info("[backfill] 시작 - 대상 공고 {}건", total);

        int success = 0;
        int image = 0;
        int failed = 0;

        for (int i = 0; i < total; i++) {
            Job job = targets.get(i);
            try {
                Job.DescriptionStatus status = fetchAndSaveDescription(job.getId());
                switch (status) {
                    case SUCCESS -> success++;
                    case IMAGE   -> image++;
                    case FAILED  -> failed++;
                }
            } catch (Exception e) {
                log.error("[backfill] 공고 처리 실패: jobId={}, error={}", job.getId(), e.getMessage());
                failed++;
            }

            // 진행 상황 로그 (100건 단위)
            if ((i + 1) % 100 == 0 || i == total - 1) {
                log.info("[backfill] {}/{} 처리됨 (성공: {}, 이미지: {}, 실패: {})",
                        i + 1, total, success, image, failed);
            }

            // 외부 사이트 부하 분산
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("[backfill] 인터럽트 발생 → 작업 중단");
                break;
            }
        }

        log.info("[backfill] 완료 - 총 {}건 처리 (성공: {}, 이미지: {}, 실패: {})",
                total, success, image, failed);
    }

    /**
     * 단일 공고의 description fetch + 저장 (백필 내부 호출용)
     * 트랜잭션 단위를 공고 1건으로 짧게 유지
     */
    @Transactional
    public Job.DescriptionStatus fetchAndSaveDescription(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new CustomException(ErrorCode.JOB_NOT_FOUND));

        DescriptionResponse result = fetchDescriptionBySourceSite(job);
        Job.DescriptionStatus status = mapStatus(result.getStatus());
        job.updateDescription(result.getDescription(), status);
        return status;
    }

    @Transactional
    public SummaryResponse getSummary(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new CustomException(ErrorCode.JOB_NOT_FOUND));

        if (job.getSummary() != null) return SummaryResponse.success(job.getSummary());

        // descriptionStatus 기반 분기
        // null(기존 데이터)이면 lazy fetch 후 status 결정
        Job.DescriptionStatus status = job.getDescriptionStatus();
        if (status == null) {
            DescriptionResponse descResp = fetchDescriptionBySourceSite(job);
            status = mapStatus(descResp.getStatus());
            job.updateDescription(descResp.getDescription(), status);
            log.info("[JobService] lazy fetch 완료 (summary용): jobId={}, status={}", jobId, status);
        }

        if (status == Job.DescriptionStatus.IMAGE)    return SummaryResponse.imageOnly();
        if (status == Job.DescriptionStatus.FAILED)   return SummaryResponse.aiFailed();
        if (status == Job.DescriptionStatus.EXTERNAL) return SummaryResponse.aiFailed();

        // SUCCESS → AI 요약
        try {
            String summary = aiSummaryService.summarize(job.getDescription());
            if (summary != null) {
                job.updateSummary(summary);
                log.info("[JobService] AI 요약 완료: jobId={}", jobId);
                return SummaryResponse.success(summary);
            }
        } catch (Exception e) {
            log.warn("[JobService] AI 요약 실패: jobId={}, error={}", jobId, e.getMessage());
        }

        return SummaryResponse.aiFailed();
    }
}
