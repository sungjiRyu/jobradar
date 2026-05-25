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

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final SaraminCrawlerService saraminCrawlerService;
    private final JobkoreaCrawlerService jobkoreaCrawlerService;
    private final AiSummaryService aiSummaryService;

    // Striped Locking: 256개 락을 미리 생성해두고 jobId를 해시로 매핑
    // 같은 공고에 동시 요청이 오면 같은 락에 걸려 직렬화됨
    // 서로 다른 공고는 대부분 다른 락 → 불필요한 경합 없음
    private static final int STRIPE_COUNT = 256;
    private static final ReentrantLock[] STRIPE_LOCKS = new ReentrantLock[STRIPE_COUNT];

    static {
        for (int i = 0; i < STRIPE_COUNT; i++) {
            STRIPE_LOCKS[i] = new ReentrantLock();
        }
    }

    private ReentrantLock getStripeLock(Long jobId) {
        // jobId를 256개 중 하나의 락으로 매핑 (비트 AND: STRIPE_COUNT가 2의 거듭제곱일 때 균등 분산)
        return STRIPE_LOCKS[(int)(jobId & (STRIPE_COUNT - 1))];
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

    @Transactional
    public DescriptionResponse getDescription(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new CustomException(ErrorCode.JOB_NOT_FOUND));

        // 1단계: 이미 수집된 경우 락 없이 즉시 반환 (대부분의 요청은 여기서 끝남)
        Job.DescriptionStatus status = job.getDescriptionStatus();
        if (status != null) {
            return switch (status) {
                case SUCCESS  -> DescriptionResponse.success(job.getDescription());
                case IMAGE    -> DescriptionResponse.image();
                case EXTERNAL -> DescriptionResponse.external();
            };
        }

        // 마감된 공고 + description 미수집 → 크롤링 안 함 (비용 절감)
        if (isClosed(job)) {
            return DescriptionResponse.closed();
        }

        // 2단계: status = null → 스트라이프 락으로 직렬화
        // 같은 jobId를 가진 동시 요청들이 같은 락에 걸려 순서대로 처리됨
        ReentrantLock lock = getStripeLock(jobId);
        lock.lock();
        try {
            // 3단계: 락 획득 후 재확인
            // 기다리는 동안 앞선 요청이 이미 크롤링·저장을 완료했을 수 있음
            job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new CustomException(ErrorCode.JOB_NOT_FOUND));
            status = job.getDescriptionStatus();
            if (status != null) {
                return switch (status) {
                    case SUCCESS  -> DescriptionResponse.success(job.getDescription());
                    case IMAGE    -> DescriptionResponse.image();
                    case EXTERNAL -> DescriptionResponse.external();
                };
            }

            // 4단계: 실제 크롤링 실행 (전체 동시 요청 중 딱 1번만 실행됨)
            // CRAWL_FAILED는 DB에 저장하지 않음 → null 유지 → 다음 방문 시 재시도
            DescriptionResponse result = fetchDescriptionBySourceSite(job);
            if (!"CRAWL_FAILED".equals(result.getStatus())) {
                job.updateDescription(result.getDescription(), mapStatus(result.getStatus()));
            }
            log.info("[JobService] lazy fetch 완료: jobId={}, status={}", jobId, result.getStatus());
            return result;

        } finally {
            lock.unlock();
        }
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

    @Transactional
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

        // descriptionStatus 기반 분기
        // null(기존 데이터)이면 lazy fetch 후 status 결정
        Job.DescriptionStatus status = job.getDescriptionStatus();
        if (status == null) {
            DescriptionResponse descResp = fetchDescriptionBySourceSite(job);

            if (!"CRAWL_FAILED".equals(descResp.getStatus())) {
                status = mapStatus(descResp.getStatus());
                job.updateDescription(descResp.getDescription(), status);
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
                log.info("[JobService] AI 요약 완료: jobId={}", jobId);
                return SummaryResponse.success(summary);
            }
        } catch (Exception e) {
            log.warn("[JobService] AI 요약 실패: jobId={}, error={}", jobId, e.getMessage());
        }

        return SummaryResponse.aiFailed();
    }
}
