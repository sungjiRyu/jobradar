package com.jobradar.backend.global.scheduler;

import com.jobradar.backend.global.lock.LockAcquisitionException;
import com.jobradar.backend.global.lock.RedisLockExecutor;
import com.jobradar.backend.global.time.BusinessTimeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledJobExecutor {

    private static final long LOCK_WAIT_SECONDS = 1;

    private final RedisLockExecutor redisLockExecutor;
    private final ScheduledJobStatusService statusService;
    private final BusinessTimeProvider businessTimeProvider;

    @Value("${APP_INSTANCE_ID:${HOSTNAME:local}}")
    private String instanceId;

    public void execute(ScheduledJobType jobType, Runnable task) {
        LocalDateTime startedAt = businessTimeProvider.now();

        try {
            redisLockExecutor.executeWithLock(
                    jobType.lockKey(),
                    LOCK_WAIT_SECONDS,
                    jobType.maxExpectedDuration().toSeconds(),
                    TimeUnit.SECONDS,
                    () -> {
                        runWithStatus(jobType, task, startedAt);
                        return null;
                    }
            );
        } catch (LockAcquisitionException e) {
            log.info("[Scheduler] {} 작업이 이미 다른 인스턴스에서 실행 중입니다. instanceId={}",
                    jobType.displayName(), instanceId);
            statusService.markSkipped(jobType, instanceId, businessTimeProvider.now());
        } catch (ScheduledJobTaskException e) {
            throw e.asRuntimeException();
        }
    }

    private void runWithStatus(ScheduledJobType jobType, Runnable task, LocalDateTime startedAt) {
        statusService.markRunning(jobType, instanceId, startedAt);
        log.info("[Scheduler] {} 시작 - instanceId={}", jobType.displayName(), instanceId);

        try {
            task.run();
            statusService.markSuccess(
                    jobType,
                    instanceId,
                    startedAt,
                    businessTimeProvider.now()
            );
            log.info("[Scheduler] {} 완료 - instanceId={}", jobType.displayName(), instanceId);
        } catch (Exception e) {
            statusService.markFailed(
                    jobType,
                    instanceId,
                    startedAt,
                    businessTimeProvider.now(),
                    e.getMessage()
            );
            log.error("[Scheduler] {} 실패 - instanceId={}", jobType.displayName(), instanceId, e);
            throw new ScheduledJobTaskException(e);
        }
    }

    private static class ScheduledJobTaskException extends RuntimeException {

        ScheduledJobTaskException(Exception cause) {
            super(cause);
        }

        RuntimeException asRuntimeException() {
            Throwable cause = getCause();
            if (cause instanceof RuntimeException runtimeException) {
                return runtimeException;
            }
            return new IllegalStateException(cause);
        }
    }
}
