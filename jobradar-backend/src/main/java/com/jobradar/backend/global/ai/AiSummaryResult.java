package com.jobradar.backend.global.ai;

public record AiSummaryResult(String summary, boolean capacityLimited, Integer errorCode) {

    public static AiSummaryResult success(String summary) {
        return new AiSummaryResult(summary, false, null);
    }

    public static AiSummaryResult failed() {
        return new AiSummaryResult(null, false, null);
    }

    public static AiSummaryResult failed(int errorCode) {
        return new AiSummaryResult(null, false, errorCode);
    }

    public static AiSummaryResult capacityLimit(int errorCode) {
        return new AiSummaryResult(null, true, errorCode);
    }
}
