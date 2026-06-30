package com.jobradar.backend.global.scheduler;

import java.time.Duration;

public enum ScheduledJobType {
    CLOSE_EXPIRED_JOBS(
            "close-expired-jobs",
            "마감 공고 정리",
            Duration.ofMinutes(10)
    ),
    DAILY_CRAWLING(
            "daily-crawling",
            "채용공고 수집",
            Duration.ofHours(2)
    ),
    ALWAYS_OPEN_CHECK(
            "always-open-check",
            "상시채용 유효성 검사",
            Duration.ofHours(3)
    );

    private static final String LOCK_PREFIX = "scheduler:";
    private static final String RUNNING_PREFIX = "jobradar:scheduler:running:";
    private static final String LAST_STATUS_PREFIX = "jobradar:scheduler:last:";

    private final String key;
    private final String displayName;
    private final Duration maxExpectedDuration;

    ScheduledJobType(String key, String displayName, Duration maxExpectedDuration) {
        this.key = key;
        this.displayName = displayName;
        this.maxExpectedDuration = maxExpectedDuration;
    }

    public String key() {
        return key;
    }

    public String displayName() {
        return displayName;
    }

    public String lockKey() {
        return LOCK_PREFIX + key;
    }

    public String runningKey() {
        return RUNNING_PREFIX + key;
    }

    public String lastStatusKey() {
        return LAST_STATUS_PREFIX + key;
    }

    public Duration maxExpectedDuration() {
        return maxExpectedDuration;
    }
}
