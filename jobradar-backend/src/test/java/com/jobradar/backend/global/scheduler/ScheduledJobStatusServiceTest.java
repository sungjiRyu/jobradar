package com.jobradar.backend.global.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduledJobStatusServiceTest {

    private RedisTemplate<String, String> redisTemplate;
    private HashOperations<String, Object, Object> hashOperations;
    private ScheduledJobStatusService statusService;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        hashOperations = mock(HashOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        statusService = new ScheduledJobStatusService(redisTemplate);
    }

    @Test
    @DisplayName("RUNNING 상태는 running 키에만 기록한다")
    void markRunning_savesRunningKeyOnly() {
        statusService.markRunning(
                ScheduledJobType.DAILY_CRAWLING,
                "i-blue",
                LocalDateTime.parse("2026-06-30T03:00:00")
        );

        verify(hashOperations).putAll(
                eq(ScheduledJobType.DAILY_CRAWLING.runningKey()),
                anyMap()
        );
        verify(redisTemplate).expire(
                ScheduledJobType.DAILY_CRAWLING.runningKey(),
                Duration.ofDays(7)
        );
        verify(hashOperations, never()).putAll(
                eq(ScheduledJobType.DAILY_CRAWLING.lastStatusKey()),
                anyMap()
        );
    }

    @Test
    @DisplayName("SUCCESS 상태는 running 키를 삭제하고 last 키에 기록한다")
    void markSuccess_deletesRunningAndSavesLastStatus() {
        statusService.markSuccess(
                ScheduledJobType.DAILY_CRAWLING,
                "i-blue",
                LocalDateTime.parse("2026-06-30T03:00:00"),
                LocalDateTime.parse("2026-06-30T03:20:00")
        );

        verify(redisTemplate).delete(ScheduledJobType.DAILY_CRAWLING.runningKey());
        verify(hashOperations).putAll(
                eq(ScheduledJobType.DAILY_CRAWLING.lastStatusKey()),
                anyMap()
        );
    }

    @Test
    @DisplayName("SKIPPED 상태는 running 키를 덮지 않고 last 키에만 기록한다")
    void markSkipped_doesNotOverwriteRunning() {
        statusService.markSkipped(
                ScheduledJobType.DAILY_CRAWLING,
                "i-green",
                LocalDateTime.parse("2026-06-30T03:05:00")
        );

        verify(hashOperations).putAll(
                eq(ScheduledJobType.DAILY_CRAWLING.lastStatusKey()),
                anyMap()
        );
        verify(hashOperations, never()).putAll(
                eq(ScheduledJobType.DAILY_CRAWLING.runningKey()),
                anyMap()
        );
        verify(redisTemplate, never()).delete(ScheduledJobType.DAILY_CRAWLING.runningKey());
    }

    @Test
    @DisplayName("상태 조회는 running과 last를 분리해서 반환한다")
    void findAll_returnsRunningAndLastSeparately() {
        when(hashOperations.entries(ScheduledJobType.DAILY_CRAWLING.runningKey()))
                .thenReturn(Map.of(
                        "status", "RUNNING",
                        "instanceId", "i-blue",
                        "startedAt", "2026-06-30T03:00:00",
                        "message", "started"
                ));
        when(hashOperations.entries(ScheduledJobType.DAILY_CRAWLING.lastStatusKey()))
                .thenReturn(Map.of(
                        "status", "SKIPPED",
                        "instanceId", "i-green",
                        "startedAt", "2026-06-30T03:05:00",
                        "finishedAt", "2026-06-30T03:05:00",
                        "message", "already running on another instance"
                ));

        ScheduledJobStatusResponse response = statusService.findAll().stream()
                .filter(status -> status.jobType().equals(ScheduledJobType.DAILY_CRAWLING.name()))
                .findFirst()
                .orElseThrow();

        assertThat(response.runningStatus()).isEqualTo(ScheduledJobRunStatus.RUNNING);
        assertThat(response.runningInstanceId()).isEqualTo("i-blue");
        assertThat(response.lastStatus()).isEqualTo(ScheduledJobRunStatus.SKIPPED);
        assertThat(response.lastInstanceId()).isEqualTo("i-green");
    }
}
