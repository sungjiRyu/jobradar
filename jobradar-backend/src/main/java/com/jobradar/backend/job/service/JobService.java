package com.jobradar.backend.job.service;

import com.jobradar.backend.crawler.service.source.JobkoreaCrawlerService;
import com.jobradar.backend.crawler.service.source.SaraminCrawlerService;
import com.jobradar.backend.global.ai.AiSummaryService;
import com.jobradar.backend.global.exception.CustomException;
import com.jobradar.backend.global.exception.ErrorCode;
import com.jobradar.backend.global.lock.LockAcquisitionException;
import com.jobradar.backend.global.lock.RedisLockExecutor;
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

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final SaraminCrawlerService saraminCrawlerService;
    private final JobkoreaCrawlerService jobkoreaCrawlerService;
    private final AiSummaryService aiSummaryService;
    private final RedisLockExecutor redisLockExecutor;

    // getDescription()과 getSummary()가 같은 공고 기준으로 락을 공유
    private String jobLockKey(Long jobId) {
        return "jobradar:lock:job:" + jobId;
    }

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
        // ACTIVE 상태 + 마감일 미경과 조건은 항상 포함
        Specification<Job> spec = JobSpecification.isActive()
                .and(JobSpecification.deadlineNotPassed());

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

    public DescriptionResponse getDescription(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new CustomException(ErrorCode.JOB_NOT_FOUND));

        // 1단계: 이미 수집된 경우 락 없이 즉시 반환 (대부분의 요청은 여기서 끝남)
        Job.DescriptionStatus status = job.getDescriptionStatus();
        if (status != null) {
            return descriptionResponseFrom(job);
        }

        // 마감된 공고 + description 미수집 → 크롤링 안 함 (비용 절감)
        if (isClosed(job)) {
            return DescriptionResponse.closed();
        }

        try {
            // 락 획득 시도
            return redisLockExecutor.executeWithLock(jobLockKey(jobId), () -> {
                // 대기시간동안 데이터가 저장되었을 수 있기 때문에 DB 재조회 
                Job lockedJob = jobRepository.findById(jobId)
                        .orElseThrow(() -> new CustomException(ErrorCode.JOB_NOT_FOUND));

                Job.DescriptionStatus lockedStatus = lockedJob.getDescriptionStatus();
                if (lockedStatus != null) {
                    return descriptionResponseFrom(lockedJob);
                }
                if (isClosed(lockedJob)) {
                    return DescriptionResponse.closed();
                }

                // 실제 크롤링 실행 (같은 jobId에 대해서는 전체 동시 요청 중 1번만 실행)
                DescriptionResponse result = fetchDescriptionBySourceSite(lockedJob);
                if (!"CRAWL_FAILED".equals(result.getStatus())) {
                    lockedJob.updateDescription(result.getDescription(), mapStatus(result.getStatus()));
                    jobRepository.save(lockedJob);
                }
                log.info("[JobService] lazy fetch 완료: jobId={}, status={}", jobId, result.getStatus());
                return result;
            });
        } catch (LockAcquisitionException e) {
            log.warn("[JobService] description lock 획득 실패: jobId={}, error={}", jobId, e.getMessage());
            job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new CustomException(ErrorCode.JOB_NOT_FOUND));
            return descriptionResponseAfterLockContention(job);
        }
    }

    private DescriptionResponse descriptionResponseAfterLockContention(Job job) {
        if (job.getDescriptionStatus() != null) {
            return descriptionResponseFrom(job);
        }
        if (isClosed(job)) {
            return DescriptionResponse.closed();
        }
        return DescriptionResponse.inProgress();
    }

    private DescriptionResponse descriptionResponseFrom(Job job) {
        return switch (job.getDescriptionStatus()) {
            case SUCCESS  -> DescriptionResponse.success(job.getDescription());
            case IMAGE    -> DescriptionResponse.image();
            case EXTERNAL -> DescriptionResponse.external();
        };
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

    /**
     * 마감된 공고인지 판단
     * - status가 CLOSED이거나
     * - deadline이 명시되어 있고 오늘보다 이전이면 마감으로 간주
     */
    private boolean isClosed(Job job) {
        return job.getStatus() == Job.JobStatus.CLOSED
                || (job.getDeadline() != null && job.getDeadline().isBefore(LocalDate.now()));
    }

    /** DescriptionResponse.status(문자열) → Job.DescriptionStatus enum 변환 (CRAWL_FAILED는 호출 전에 걸러야 함) */
    private Job.DescriptionStatus mapStatus(String responseStatus) {
        return switch (responseStatus) {
            case "SUCCESS"  -> Job.DescriptionStatus.SUCCESS;
            case "IMAGE"    -> Job.DescriptionStatus.IMAGE;
            case "EXTERNAL" -> Job.DescriptionStatus.EXTERNAL;
            default -> throw new IllegalStateException("mapStatus: unexpected status=" + responseStatus);
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
                if (status == null) {
                    failed++;
                } else switch (status) {
                    case SUCCESS  -> success++;
                    case IMAGE    -> image++;
                    default       -> {}
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
    /** 백필 내부 호출용 — CRAWL_FAILED면 DB 저장 없이 null 반환 */
    @Transactional
    public Job.DescriptionStatus fetchAndSaveDescription(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new CustomException(ErrorCode.JOB_NOT_FOUND));

        DescriptionResponse result = fetchDescriptionBySourceSite(job);
        if ("CRAWL_FAILED".equals(result.getStatus())) {
            return null; // DB 저장 안 함 → null 유지 → 재시도 가능
        }
        Job.DescriptionStatus status = mapStatus(result.getStatus());
        job.updateDescription(result.getDescription(), status);
        return status;
    }

    public SummaryResponse getSummary(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new CustomException(ErrorCode.JOB_NOT_FOUND));

        if (job.getSummary() != null) {
            return SummaryResponse.success(job.getSummary());
        }

        // 마감된 공고 + 요약 미생성 → AI 호출 안 함 (비용 절감)
        // 이미 생성된 요약은 위에서 반환됨
        if (isClosed(job)) {
            return SummaryResponse.closed();
        }

        Job.DescriptionStatus status = job.getDescriptionStatus();
        if (status == Job.DescriptionStatus.IMAGE) return SummaryResponse.imageOnly();
        if (status == Job.DescriptionStatus.EXTERNAL) return SummaryResponse.aiFailed();

        try {
            return redisLockExecutor.executeWithLock(jobLockKey(jobId), () -> generateSummaryUnderLock(jobId));
        } catch (LockAcquisitionException e) {
            log.warn("[JobService] summary lock 획득 실패: jobId={}, error={}", jobId, e.getMessage());
            job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new CustomException(ErrorCode.JOB_NOT_FOUND));
            return summaryResponseAfterLockContention(job);
        }
    }

    private SummaryResponse summaryResponseAfterLockContention(Job job) {
        if (job.getSummary() != null) {
            return SummaryResponse.success(job.getSummary());
        }
        if (isClosed(job)) {
            return SummaryResponse.closed();
        }

        Job.DescriptionStatus status = job.getDescriptionStatus();
        if (status == Job.DescriptionStatus.IMAGE) return SummaryResponse.imageOnly();
        if (status == Job.DescriptionStatus.EXTERNAL) return SummaryResponse.aiFailed();

        return SummaryResponse.inProgress();
    }

    private SummaryResponse generateSummaryUnderLock(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new CustomException(ErrorCode.JOB_NOT_FOUND));

        if (job.getSummary() != null) {
            return SummaryResponse.success(job.getSummary());
        }
        if (isClosed(job)) {
            return SummaryResponse.closed();
        }

        // descriptionStatus 기반 분기
        // null(기존 데이터)이면 lazy fetch 후 status 결정
        Job.DescriptionStatus status = job.getDescriptionStatus();
        if (status == null) {
            DescriptionResponse descResp = fetchDescriptionBySourceSite(job);

            if (!"CRAWL_FAILED".equals(descResp.getStatus())) {
                status = mapStatus(descResp.getStatus());
                job.updateDescription(descResp.getDescription(), status);
                jobRepository.save(job);
            }
            log.info("[JobService] lazy fetch 완료 (summary용): jobId={}, status={}", jobId, descResp.getStatus());
            if (status == null) {
                return SummaryResponse.aiFailed(); // CRAWL_FAILED
            }
        }

        if (status == Job.DescriptionStatus.IMAGE) return SummaryResponse.imageOnly();
        if (status == Job.DescriptionStatus.EXTERNAL) return SummaryResponse.aiFailed();

        // SUCCESS → AI 요약
        try {
            String summary = aiSummaryService.summarize(job.getDescription());
            if (summary != null) {
                job.updateSummary(summary);
                jobRepository.save(job);
                log.info("[JobService] AI 요약 완료: jobId={}", jobId);
                return SummaryResponse.success(summary);
            }
        } catch (Exception e) {
            log.warn("[JobService] AI 요약 실패: jobId={}, error={}", jobId, e.getMessage());
        }

        return SummaryResponse.aiFailed();
    }
}
