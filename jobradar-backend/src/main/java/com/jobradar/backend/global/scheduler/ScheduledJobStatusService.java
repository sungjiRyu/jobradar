package com.jobradar.backend.global.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ScheduledJobStatusService {

    private static final Duration STATUS_TTL = Duration.ofDays(7);

    private final RedisTemplate<String, String> redisTemplate;

    public void markRunning(ScheduledJobType jobType, String instanceId, LocalDateTime startedAt) {
        save(jobType.runningKey(), Map.of(
                "jobType", jobType.name(),
                "key", jobType.key(),
                "displayName", jobType.displayName(),
                "status", ScheduledJobRunStatus.RUNNING.name(),
                "instanceId", instanceId,
                "startedAt", startedAt.toString(),
                "finishedAt", "",
                "message", "started"
        ));
    }

    public void markSuccess(
            ScheduledJobType jobType,
            String instanceId,
            LocalDateTime startedAt,
            LocalDateTime finishedAt
    ) {
        redisTemplate.delete(jobType.runningKey());
        save(jobType.lastStatusKey(), Map.of(
                "jobType", jobType.name(),
                "key", jobType.key(),
                "displayName", jobType.displayName(),
                "status", ScheduledJobRunStatus.SUCCESS.name(),
                "instanceId", instanceId,
                "startedAt", startedAt.toString(),
                "finishedAt", finishedAt.toString(),
                "message", "completed"
        ));
    }

    public void markFailed(
            ScheduledJobType jobType,
            String instanceId,
            LocalDateTime startedAt,
            LocalDateTime finishedAt,
            String message
    ) {
        redisTemplate.delete(jobType.runningKey());
        save(jobType.lastStatusKey(), Map.of(
                "jobType", jobType.name(),
                "key", jobType.key(),
                "displayName", jobType.displayName(),
                "status", ScheduledJobRunStatus.FAILED.name(),
                "instanceId", instanceId,
                "startedAt", startedAt.toString(),
                "finishedAt", finishedAt.toString(),
                "message", normalizeMessage(message)
        ));
    }

    public void markSkipped(ScheduledJobType jobType, String instanceId, LocalDateTime skippedAt) {
        save(jobType.lastStatusKey(), Map.of(
                "jobType", jobType.name(),
                "key", jobType.key(),
                "displayName", jobType.displayName(),
                "status", ScheduledJobRunStatus.SKIPPED.name(),
                "instanceId", instanceId,
                "startedAt", skippedAt.toString(),
                "finishedAt", skippedAt.toString(),
                "message", "already running on another instance"
        ));
    }

    public List<ScheduledJobStatusResponse> findAll() {
        return Arrays.stream(ScheduledJobType.values())
                .map(this::find)
                .toList();
    }

    private ScheduledJobStatusResponse find(ScheduledJobType jobType) {
        Map<Object, Object> running = redisTemplate.opsForHash().entries(jobType.runningKey());
        Map<Object, Object> last = redisTemplate.opsForHash().entries(jobType.lastStatusKey());

        return new ScheduledJobStatusResponse(
                jobType.name(),
                jobType.key(),
                jobType.displayName(),
                parseStatus(value(running, "status")),
                blankToNull(value(running, "instanceId")),
                blankToNull(value(running, "startedAt")),
                blankToNull(value(running, "message")),
                parseStatus(value(last, "status")),
                blankToNull(value(last, "instanceId")),
                blankToNull(value(last, "startedAt")),
                blankToNull(value(last, "finishedAt")),
                blankToNull(value(last, "message"))
        );
    }

    private void save(String key, Map<String, String> values) {
        redisTemplate.opsForHash().putAll(key, values);
        redisTemplate.expire(key, STATUS_TTL);
    }

    private String value(Map<Object, Object> values, String key) {
        Object value = values.get(key);
        return value == null ? "" : value.toString();
    }

    private ScheduledJobRunStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return ScheduledJobRunStatus.valueOf(value);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String normalizeMessage(String message) {
        return message == null || message.isBlank() ? "failed" : message;
    }
}
