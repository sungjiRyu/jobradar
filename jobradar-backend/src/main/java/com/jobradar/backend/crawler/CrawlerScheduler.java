package com.jobradar.backend.crawler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 채용공고 수집 스케줄러
 *
 * [동작 방식]
 * - 매일 오전 9시에 자동 실행
 * - List<CrawlerService>로 모든 크롤러 구현체를 주입받아 순차 실행
 * - 새 크롤러 추가 시 CrawlerService 구현체만 만들면 자동으로 여기에도 포함됨
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CrawlerScheduler {

    /**
     * @RequiredArgsConstructor + final → 생성자 주입
     * List<CrawlerService>: CrawlerService 구현체 빈 전체를 리스트로 자동 주입
     */
    private final List<CrawlerService> crawlerServices;

    /**
     * @Scheduled(cron = "0 0 9 * * *")
     *
     * cron 표현식 구조: [초] [분] [시] [일] [월] [요일]
     *   0   0   3  *   *    *
     *   ↑   ↑   ↑  ↑   ↑    ↑
     *  0초 0분 3시 매일 매월 모든요일
     *
     * → 매일 새벽 3시 정각에 실행
     *
     * [주의] @EnableScheduling이 main 클래스에 있어야 동작함
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void runCrawling() {
        log.info("===== 채용공고 수집 스케줄러 시작 =====");

        for (CrawlerService crawler : crawlerServices) {
            try {
                log.info("[{}] 수집 시작", crawler.getSiteName());
                crawler.collect();
                log.info("[{}] 수집 완료", crawler.getSiteName());
            } catch (Exception e) {
                // 한 크롤러 실패가 다른 크롤러 실행을 막지 않도록 격리
                log.error("[{}] 수집 중 오류 발생 - 다음 크롤러로 진행", crawler.getSiteName(), e);
            }
        }

        log.info("===== 채용공고 수집 스케줄러 완료 =====");
    }
}
