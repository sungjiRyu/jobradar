package com.jobradar.backend.crawler.scheduler;

import com.jobradar.backend.global.config.CacheConfig;
import com.jobradar.backend.crawler.service.AlwaysOpenCheckService;
import com.jobradar.backend.crawler.service.CrawlerService;
import com.jobradar.backend.global.scheduler.ScheduledJobExecutor;
import com.jobradar.backend.global.scheduler.ScheduledJobType;
import com.jobradar.backend.job.service.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Async;
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
    private final AlwaysOpenCheckService alwaysOpenCheckService;
    private final ScheduledJobExecutor scheduledJobExecutor;

    /**
     * CrawlerController에서 수동 트리거 시 호출
     * @Async로 별도 스레드에서 실행 → HTTP 요청은 즉시 202 반환
     */
    @Async
    public void runCrawlAsync() {
        runCrawling();
    }

    /**
     * 상시채용 공고 유효성 검사 — 매주 월요일 새벽 3시 실행
     * 일별 크롤링(runCrawling)과 같은 시각에 예약되어 기본 스케줄러에서는 순차 실행됨
     * cron: 초 분 시 일 월 요일 (1=일요일, 2=월요일, ... 7=토요일)
     */
    @Scheduled(cron = "0 0 3 * * 2", zone = "Asia/Seoul")
    public void runAlwaysOpenCheck() {
        scheduledJobExecutor.execute(ScheduledJobType.ALWAYS_OPEN_CHECK, this::checkAlwaysOpenJobs);
    }

    private void checkAlwaysOpenJobs() {
        log.info("===== 상시채용 유효성 검사 시작 =====");
        alwaysOpenCheckService.checkAll();
        log.info("===== 상시채용 유효성 검사 완료 =====");
    }

    /**
     * cron 표현식: 매일 새벽 3시 정각 실행 (JVM 타임존 Asia/Seoul 기준)
     * [주의] @EnableScheduling이 main 클래스에 있어야 동작함
     */
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    @CacheEvict(cacheNames = {
        CacheConfig.CACHE_TECH_STACKS,
        CacheConfig.CACHE_LOCATIONS,
        CacheConfig.CACHE_EXPERIENCE,
        CacheConfig.CACHE_TODAY,
        CacheConfig.CACHE_TRENDING_JOBS
    }, allEntries = true)
    public void runCrawling() {
        scheduledJobExecutor.execute(ScheduledJobType.DAILY_CRAWLING, this::collectAll);
    }

    private void collectAll() {
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

    /**
     * 마감일 지난 ACTIVE 공고 일괄 CLOSED 처리 — 매일 자정(00:00) 실행
     *
     * 데이터 정합성 위해 날짜가 바뀌는 시점에 즉시 처리.
     * 크롤링(03:00)과 분리하여, 사용자가 새 날짜 진입 직후부터
     * 만료 공고를 보지 않도록 보장한다.
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    @CacheEvict(cacheNames = {
        CacheConfig.CACHE_TECH_STACKS,
        CacheConfig.CACHE_LOCATIONS,
        CacheConfig.CACHE_EXPERIENCE,
        CacheConfig.CACHE_TODAY,
        CacheConfig.CACHE_TRENDING_JOBS
    }, allEntries = true)
    public void closeExpiredJobsScheduled() {
        scheduledJobExecutor.execute(ScheduledJobType.CLOSE_EXPIRED_JOBS, this::closeExpiredJobs);
    }

    private void closeExpiredJobs() {
        log.info("===== 마감 공고 정리 스케줄러 시작 =====");
        try {
            jobService.closeExpiredJobs();
        } catch (Exception e) {
            log.error("마감 공고 정리 중 오류 발생", e);
        }
        log.info("===== 마감 공고 정리 스케줄러 완료 =====");
    }
}
