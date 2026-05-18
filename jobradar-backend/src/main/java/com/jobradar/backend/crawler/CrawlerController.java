package com.jobradar.backend.crawler;

import com.jobradar.backend.job.service.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 크롤링 수동 실행 컨트롤러 (개발/테스트 전용)
 *
 * [주의] 배포 전 반드시 삭제하거나 관리자 권한으로 제한할 것
 * - 현재: SecurityConfig에서 /api/admin/** 는 로그인한 사용자만 접근 가능
 * - 운영 환경: ADMIN 롤을 가진 계정만 호출 가능하도록 hasRole("ADMIN") 추가 권장
 *
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class CrawlerController {

    private final CrawlerScheduler crawlerScheduler;
    private final JobService jobService;

    /**
     * POST /api/admin/crawl
     * 크롤링을 백그라운드에서 비동기 실행 → 즉시 202 Accepted 반환
     *
     * - 크롤링 1 사이클은 수 분~수십 분 소요되므로 @Async로 분리
     * - 진행 상황은 서버 로그에서 확인 (===== 채용공고 수집 스케줄러 시작 ===== 등)
     * [인증 필요]
     * SecurityConfig에서 /api/admin/** → authenticated() 설정
     * Authorization: Bearer {accessToken} 헤더 필요
     */
    @PostMapping("/crawl")
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
     */
    @PostMapping("/backfill-descriptions")
    public ResponseEntity<String> backfillDescriptions() {
        log.info("description 백필 요청 수신");
        jobService.backfillDescriptions();
        return ResponseEntity.accepted()
                .body("백필 작업이 백그라운드에서 시작되었습니다. 진행 상황은 서버 로그를 확인하세요.");
    }
}
