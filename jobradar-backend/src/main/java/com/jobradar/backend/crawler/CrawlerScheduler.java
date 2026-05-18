package com.jobradar.backend.crawler;

import com.jobradar.backend.job.service.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 채용공고 수집 스케줄러
 *
 * [동작 방식]
 * - 매일 새벽 3시에 자동 실행 (KST 기준, EC2 JVM 타임존 Asia/Seoul 설정 필요)
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
    private final JobService jobService;

    /**
     * cron 표현식: 매일 새벽 3시 정각 실행 (JVM 타임존 Asia/Seoul 기준)
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

        // 모든 크롤러 완료 후, 마감일 지난 ACTIVE 공고를 일괄 CLOSED 처리
        // 크롤러로 새로 수집된 공고에도 마감일 지난 경우가 있을 수 있으므로 마지막에 실행
        try {
            jobService.closeExpiredJobs();
        } catch (Exception e) {
            log.error("마감 공고 정리 중 오류 발생", e);
        }

        log.info("===== 채용공고 수집 스케줄러 완료 =====");
    }
}
