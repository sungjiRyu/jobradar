package com.jobradar.backend.crawler;

import com.jobradar.backend.job.entity.Job;
import com.jobradar.backend.job.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

/**
 * 상시채용(ALWAYS) 공고 유효성 검사 서비스
 *
 * [동작 방식]
 * - DeadlineType.ALWAYS + ACTIVE 공고를 대상으로 sourceUrl에 HTTP GET 요청
 * - 마감 여부 판단:
 *   1. HTTP 404 / 접속 실패 (IOException)    → CLOSED
 *   2. 페이지 내 "마감" 텍스트 감지           → CLOSED
 *   3. 정상 응답 + 마감 텍스트 없음           → ACTIVE 유지
 * - 공고 간 1초 sleep으로 외부 사이트 부하 분산
 * - @Async로 비동기 실행 → HTTP 요청은 즉시 반환
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlwaysOpenCheckService {

    private final JobRepository jobRepository;

    // 사이트별 마감 감지 텍스트 — 페이지 내 포함 여부로 마감 판단
    private static final List<String> CLOSED_INDICATORS = List.of(
            "마감된 공고", "채용이 마감", "접수가 마감", "마감되었습니다", "종료된 공고"
    );

    @Async
    public void checkAll() {
        List<Job> targets = jobRepository.findByDeadlineTypeAndStatus(
                Job.DeadlineType.ALWAYS, Job.JobStatus.ACTIVE);
        int total = targets.size();
        log.info("[AlwaysOpenCheck] 시작 - 대상 공고 {}건", total);

        int closed = 0;
        int failed = 0;

        for (int i = 0; i < total; i++) {
            Job job = targets.get(i);
            try {
                if (!isActive(job.getSourceUrl(), job.getSourceSite())) {
                    closeJob(job.getId());
                    closed++;
                    log.info("[AlwaysOpenCheck] 마감 처리: jobId={}, url={}", job.getId(), job.getSourceUrl());
                }
            } catch (Exception e) {
                log.error("[AlwaysOpenCheck] 처리 실패: jobId={}, error={}", job.getId(), e.getMessage());
                failed++;
            }

            if ((i + 1) % 50 == 0 || i == total - 1) {
                log.info("[AlwaysOpenCheck] {}/{} 처리됨 (마감: {}, 오류: {})",
                        i + 1, total, closed, failed);
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("[AlwaysOpenCheck] 인터럽트 발생 → 작업 중단");
                break;
            }
        }

        log.info("[AlwaysOpenCheck] 완료 - 총 {}건 (마감 처리: {}, 오류: {})", total, closed, failed);
    }

    /**
     * sourceUrl에 HTTP GET 요청해 공고 활성 여부 판단
     * - IOException (404 포함): 페이지 없음 → 마감으로 판단
     * - 200 응답 + CLOSED_INDICATORS 포함: 마감으로 판단
     * - 200 응답 + 정상: 활성 유지
     */
    boolean isActive(String sourceUrl, String sourceSite) {
        try {
            Document doc = Jsoup.connect(sourceUrl)
                    .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
                            + "AppleWebKit/537.36 (KHTML, like Gecko) "
                            + "Chrome/124.0.0.0 Safari/537.36")
                    .header("Accept-Language", "ko-KR,ko;q=0.9")
                    .timeout(10_000)
                    .get();

            String bodyText = doc.body().text();
            for (String indicator : CLOSED_INDICATORS) {
                if (bodyText.contains(indicator)) {
                    log.debug("[AlwaysOpenCheck] 마감 텍스트 감지 ({}): {}", indicator, sourceUrl);
                    return false;
                }
            }
            return true;

        } catch (HttpStatusException e) {
            // 404 등 HTTP 에러 → 공고 페이지 없음
            log.debug("[AlwaysOpenCheck] HTTP {} → 마감 판단: {}", e.getStatusCode(), sourceUrl);
            return false;
        } catch (IOException e) {
            // 접속 실패 → 보수적으로 활성 유지 (일시적 네트워크 오류일 수 있음)
            log.warn("[AlwaysOpenCheck] 접속 실패 (활성 유지): url={}, error={}", sourceUrl, e.getMessage());
            return true;
        }
    }

    /** 단일 공고 CLOSED 처리 — 트랜잭션 단위를 공고 1건으로 유지 */
    @Transactional
    public void closeJob(Long jobId) {
        jobRepository.findById(jobId).ifPresent(Job::close);
    }
}
