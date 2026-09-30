package com.jobradar.backend.crawler.scheduler;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class BatchJobRunnerTest {
    private final CrawlerScheduler scheduler = mock(CrawlerScheduler.class);
    private final BatchJobRunner runner = new BatchJobRunner(scheduler);

    @Test
    void routesEachJobWithoutRunningOthers() {
        runner.run(new DefaultApplicationArguments("--batch.job=daily-crawling"));
        verify(scheduler).runCrawling();
        verifyNoMoreInteractions(scheduler);
        clearInvocations(scheduler);
        runner.run(new DefaultApplicationArguments("--batch.job=close-expired-jobs"));
        verify(scheduler).closeExpiredJobsScheduled();
        verifyNoMoreInteractions(scheduler);
        clearInvocations(scheduler);
        runner.run(new DefaultApplicationArguments("--batch.job=always-open-check"));
        verify(scheduler).runAlwaysOpenCheck();
        verifyNoMoreInteractions(scheduler);
    }

    @Test
    void rejectsMissingDuplicateAndUnknownJobs() {
        for (String[] args : new String[][]{ {}, {"--batch.job=wrong"},
                {"--batch.job=daily-crawling", "--batch.job=always-open-check"} }) {
            assertThatThrownBy(() -> runner.run(new DefaultApplicationArguments(args)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        verifyNoInteractions(scheduler);
    }

    @Test
    void propagatesJobFailureToApplicationStartup() {
        doThrow(new IllegalStateException("failed")).when(scheduler).runCrawling();
        assertThatThrownBy(() -> runner.run(new DefaultApplicationArguments("--batch.job=daily-crawling")))
                .hasMessage("failed");
    }
}
