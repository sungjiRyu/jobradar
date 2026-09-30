package com.jobradar.backend.crawler.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("batch")
@RequiredArgsConstructor
public class BatchJobRunner implements ApplicationRunner {

    private final CrawlerScheduler crawlerScheduler;

    @Override
    public void run(ApplicationArguments args) {
        var jobs = args.getOptionValues("batch.job");
        if (jobs == null || jobs.size() != 1) {
            throw new IllegalArgumentException("Exactly one --batch.job is required");
        }
        switch (jobs.getFirst()) {
            case "daily-crawling" -> crawlerScheduler.runCrawling();
            case "close-expired-jobs" -> crawlerScheduler.closeExpiredJobsScheduled();
            case "always-open-check" -> crawlerScheduler.runAlwaysOpenCheck();
            default -> throw new IllegalArgumentException("Unknown batch.job: " + jobs.getFirst());
        }
    }
}
