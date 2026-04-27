package com.jobradar.backend.job.dto;

import lombok.Getter;

/**
 * AI 정리 응답 DTO
 *
 * summary  : AI가 생성한 JSON 문자열 (null이면 생성 불가)
 * imageOnly: true  → 이미지 공고 (텍스트 없음, 재시도해도 결과 없음)
 *            false → AI 요청 실패 (재시도하면 성공할 수 있음)
 */
@Getter
public class SummaryResponse {

    private final String summary;
    private final boolean imageOnly;

    private SummaryResponse(String summary, boolean imageOnly) {
        this.summary = summary;
        this.imageOnly = imageOnly;
    }

    public static SummaryResponse success(String summary) {
        return new SummaryResponse(summary, false);
    }

    public static SummaryResponse imageOnly() {
        return new SummaryResponse(null, true);
    }

    public static SummaryResponse aiFailed() {
        return new SummaryResponse(null, false);
    }
}
