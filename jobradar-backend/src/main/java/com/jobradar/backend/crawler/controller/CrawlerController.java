package com.jobradar.backend.crawler.controller;

import com.jobradar.backend.crawler.scheduler.CrawlerScheduler;
import com.jobradar.backend.global.scheduler.ScheduledJobStatusResponse;
import com.jobradar.backend.global.scheduler.ScheduledJobStatusService;
import com.jobradar.backend.job.service.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 크롤링 수동 실행 컨트롤러 (관리 기능)
 *
 * [권한]
 * SecurityConfig에서 /api/admin/** → hasRole("ADMIN")로 1차 제한 +
 * 각 메서드에 @PreAuthorize("hasRole('ADMIN')")로 2차 제한 (defense in depth)
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class CrawlerController {

    private final CrawlerScheduler crawlerScheduler;
    private final JobService jobService;
    private final ScheduledJobStatusService scheduledJobStatusService;

    /**
     * POST /api/admin/crawl
     * 크롤링을 백그라운드에서 비동기 실행 → 즉시 202 Accepted 반환
     *
     * - 크롤링 1 사이클은 수 분~수십 분 소요되므로 @Async로 분리
     * - 진행 상황은 서버 로그에서 확인 (===== 채용공고 수집 스케줄러 시작 ===== 등)
     * - ADMIN 권한 필수
     */
    @PostMapping("/crawl")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> crawlNow() {
        log.info("수동 크롤링 요청 수신 - 비동기 실행 시작");
        crawlerScheduler.runCrawlAsync();
        return ResponseEntity.accepted()
                .body("크롤링이 백그라운드에서 시작되었습니다. 진행 상황은 서버 로그를 확인하세요.");
    }

    /**
     * POST /api/admin/backfill-descriptions
     * description_status가 null인 기존 공고들을 일괄 fetch
     *
     * - @Async로 백그라운드 실행 → 즉시 202 Accepted 반환
     * - 진행 상황은 서버 로그에서 확인 ([backfill] N/M 처리됨)
     * - 외부 사이트 부하를 줄이기 위해 공고당 1초 sleep
     * - ADMIN 권한 필수
     */
    @PostMapping("/backfill-descriptions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> backfillDescriptions() {
        log.info("description 백필 요청 수신");
        jobService.backfillDescriptions();
        return ResponseEntity.accepted()
                .body("백필 작업이 백그라운드에서 시작되었습니다. 진행 상황은 서버 로그를 확인하세요.");
    }

    /**
     * GET /api/admin/scheduler/status
     * 예약 작업의 최근 실행 상태를 조회한다.
     */
    @GetMapping("/scheduler/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ScheduledJobStatusResponse>> schedulerStatus() {
        return ResponseEntity.ok(scheduledJobStatusService.findAll());
    }
}
