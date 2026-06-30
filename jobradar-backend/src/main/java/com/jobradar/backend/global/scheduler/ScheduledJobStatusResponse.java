package com.jobradar.backend.global.scheduler;

public record ScheduledJobStatusResponse(
        String jobType,
        String key,
        String displayName,
        ScheduledJobRunStatus runningStatus,
        String runningInstanceId,
        String runningStartedAt,
        String runningMessage,
        ScheduledJobRunStatus lastStatus,
        String lastInstanceId,
        String lastStartedAt,
        String lastFinishedAt,
        String lastMessage
) {
}
