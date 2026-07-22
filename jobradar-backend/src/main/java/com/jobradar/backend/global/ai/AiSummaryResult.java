package com.jobradar.backend.global.ai;

public record AiSummaryResult(String summary) {

    public static AiSummaryResult success(String summary) {
        return new AiSummaryResult(summary);
    }

    public static AiSummaryResult failed() {
        return new AiSummaryResult(null);
    }
}
