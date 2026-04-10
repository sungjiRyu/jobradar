package com.jobradar.backend.crawler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 크롤링 수동 실행 컨트롤러 (개발/테스트 전용)
 *
 * [주의] 배포 전 반드시 삭제하거나 관리자 권한으로 제한할 것
 * - 현재: SecurityConfig에서 /api/admin/** 는 로그인한 사용자만 접근 가능
 * - 운영 환경: ADMIN 롤을 가진 계정만 호출 가능하도록 hasRole("ADMIN") 추가 권장
 *
 * [왜 만들었는가?]
 * - 스케줄러는 매일 9시에만 실행됨 → 개발 중 즉시 테스트 불가
 * - 이 API로 언제든 수동 실행 가능
 * - Postman 또는 Swagger에서 POST /api/admin/crawl 호출
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class CrawlerController {

    private final List<CrawlerService> crawlerServices;

    /**
     * POST /api/admin/crawl
     * 모든 크롤러를 즉시 실행
     *
     * [인증 필요]
     * SecurityConfig에서 /api/admin/** → authenticated() 설정
     * Authorization: Bearer {accessToken} 헤더 필요
     */
    @PostMapping("/crawl")
    public ResponseEntity<String> crawlNow() {
        log.info("수동 크롤링 요청 수신");

        for (CrawlerService crawler : crawlerServices) {
            try {
                crawler.collect();
            } catch (Exception e) {
                log.error("[{}] 수동 크롤링 실패", crawler.getSiteName(), e);
                return ResponseEntity.internalServerError()
                        .body("[" + crawler.getSiteName() + "] 크롤링 실패: " + e.getMessage());
            }
        }

        return ResponseEntity.ok("크롤링 완료");
    }
}
