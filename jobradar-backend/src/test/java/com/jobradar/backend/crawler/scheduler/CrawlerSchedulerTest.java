package com.jobradar.backend.crawler.scheduler;

import com.jobradar.backend.crawler.service.AlwaysOpenCheckService;
import com.jobradar.backend.crawler.service.CrawlerService;
import com.jobradar.backend.global.scheduler.ScheduledJobExecutor;
import com.jobradar.backend.global.scheduler.ScheduledJobType;
import com.jobradar.backend.job.service.JobService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CrawlerSchedulerTest {

    private final CrawlerService crawlerService = mock(CrawlerService.class);
    private final JobService jobService = mock(JobService.class);
    private final AlwaysOpenCheckService alwaysOpenCheckService = mock(AlwaysOpenCheckService.class);
    private final ScheduledJobExecutor scheduledJobExecutor = mock(ScheduledJobExecutor.class);

    private final CrawlerScheduler crawlerScheduler = new CrawlerScheduler(
            List.of(crawlerService),
            jobService,
            alwaysOpenCheckService,
            scheduledJobExecutor
    );

    @Test
    @DisplayName("예약 크롤링은 DAILY_CRAWLING 락으로 실행된다")
    void runCrawling_usesDailyCrawlingLock() {
        crawlerScheduler.runCrawling();

        verify(scheduledJobExecutor).execute(eq(ScheduledJobType.DAILY_CRAWLING), any(Runnable.class));
    }

    @Test
    @DisplayName("수동 크롤링도 예약 크롤링과 같은 DAILY_CRAWLING 락을 공유한다")
    void runCrawlAsync_usesDailyCrawlingLock() {
        crawlerScheduler.runCrawlAsync();

        verify(scheduledJobExecutor).execute(eq(ScheduledJobType.DAILY_CRAWLING), any(Runnable.class));
    }

    @Test
    @DisplayName("상시채용 검사는 ALWAYS_OPEN_CHECK 락으로 실행된다")
    void runAlwaysOpenCheck_usesAlwaysOpenCheckLock() {
        crawlerScheduler.runAlwaysOpenCheck();

        verify(scheduledJobExecutor).execute(eq(ScheduledJobType.ALWAYS_OPEN_CHECK), any(Runnable.class));
    }

    @Test
    @DisplayName("마감 공고 정리는 CLOSE_EXPIRED_JOBS 락으로 실행된다")
    void closeExpiredJobsScheduled_usesCloseExpiredJobsLock() {
        crawlerScheduler.closeExpiredJobsScheduled();

        verify(scheduledJobExecutor).execute(eq(ScheduledJobType.CLOSE_EXPIRED_JOBS), any(Runnable.class));
    }
}
