package com.jobradar.backend.global.scheduler;

import com.jobradar.backend.global.lock.LockAcquisitionException;
import com.jobradar.backend.global.lock.RedisLockExecutor;
import com.jobradar.backend.global.time.BusinessTimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ScheduledJobExecutorTest {

    @Mock
    private RedisLockExecutor redisLockExecutor;

    @Mock
    private ScheduledJobStatusService statusService;

    private ScheduledJobExecutor scheduledJobExecutor;

    @BeforeEach
    void setUp() {
        scheduledJobExecutor = new ScheduledJobExecutor(
                redisLockExecutor,
                statusService,
                new BusinessTimeProvider(Clock.fixed(
                        Instant.parse("2026-06-29T18:00:00Z"),
                        ZoneOffset.UTC
                ))
        );
        ReflectionTestUtils.setField(scheduledJobExecutor, "instanceId", "i-test");
    }

    @Test
    @DisplayName("락 획득 성공 시 RUNNING 이후 SUCCESS를 기록한다")
    void execute_success_recordsRunningAndSuccess() {
        doAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(4);
            return supplier.get();
        }).when(redisLockExecutor).executeWithLock(
                eq(ScheduledJobType.DAILY_CRAWLING.lockKey()),
                anyLong(),
                eq(ScheduledJobType.DAILY_CRAWLING.maxExpectedDuration().toSeconds()),
                eq(TimeUnit.SECONDS),
                any()
        );

        Runnable task = mock(Runnable.class);

        scheduledJobExecutor.execute(ScheduledJobType.DAILY_CRAWLING, task);

        verify(task).run();
        verify(statusService).markRunning(
                eq(ScheduledJobType.DAILY_CRAWLING),
                eq("i-test"),
                any()
        );
        verify(statusService).markSuccess(
                eq(ScheduledJobType.DAILY_CRAWLING),
                eq("i-test"),
                any(),
                any()
        );
        verify(statusService, never()).markFailed(any(), any(), any(), any(), any());
        verify(statusService, never()).markSkipped(any(), any(), any());
    }

    @Test
    @DisplayName("작업 실패 시 FAILED를 기록하고 예외를 다시 던진다")
    void execute_taskFailure_recordsFailedAndRethrows() {
        doAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(4);
            return supplier.get();
        }).when(redisLockExecutor).executeWithLock(
                eq(ScheduledJobType.CLOSE_EXPIRED_JOBS.lockKey()),
                anyLong(),
                eq(ScheduledJobType.CLOSE_EXPIRED_JOBS.maxExpectedDuration().toSeconds()),
                eq(TimeUnit.SECONDS),
                any()
        );

        RuntimeException failure = new RuntimeException("boom");

        assertThatThrownBy(() ->
                scheduledJobExecutor.execute(ScheduledJobType.CLOSE_EXPIRED_JOBS, () -> {
                    throw failure;
                }))
                .isSameAs(failure);

        verify(statusService).markRunning(
                eq(ScheduledJobType.CLOSE_EXPIRED_JOBS),
                eq("i-test"),
                any()
        );
        verify(statusService).markFailed(
                eq(ScheduledJobType.CLOSE_EXPIRED_JOBS),
                eq("i-test"),
                any(),
                any(),
                eq("boom")
        );
        verify(statusService, never()).markSuccess(any(), any(), any(), any());
    }

    @Test
    @DisplayName("락 획득 실패 시 작업을 실행하지 않고 SKIPPED를 기록한다")
    void execute_lockBusy_recordsSkipped() {
        doThrow(new LockAcquisitionException("busy"))
                .when(redisLockExecutor).executeWithLock(
                        eq(ScheduledJobType.ALWAYS_OPEN_CHECK.lockKey()),
                        anyLong(),
                        eq(ScheduledJobType.ALWAYS_OPEN_CHECK.maxExpectedDuration().toSeconds()),
                        eq(TimeUnit.SECONDS),
                        any()
                );
        Runnable task = mock(Runnable.class);

        scheduledJobExecutor.execute(ScheduledJobType.ALWAYS_OPEN_CHECK, task);

        verify(task, never()).run();
        verify(statusService).markSkipped(
                eq(ScheduledJobType.ALWAYS_OPEN_CHECK),
                eq("i-test"),
                any()
        );
        verify(statusService, never()).markRunning(any(), any(), any());
    }
}
