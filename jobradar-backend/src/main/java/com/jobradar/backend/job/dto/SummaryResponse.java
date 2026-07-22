package com.jobradar.backend.job.dto;

import lombok.Getter;

/**
 * AI 정리 응답 DTO
 *
 * summary  : AI가 생성한 JSON 문자열 (null이면 생성 불가)
 * imageOnly: true  → 이미지 공고 (텍스트 없음, 재시도해도 결과 없음)
 *            false → AI 요청 실패 (재시도하면 성공할 수 있음)
 * closed   : true  → 마감된 공고 (요약 생성 안 함, 비용 절감)
 * failureReason: AI 요약 실패 사유 (사용자 안내가 필요한 경우만 값 설정)
 * errorCode: AI 제공자가 반환한 실제 HTTP 에러 코드 (없으면 null)
 */
@Getter
public class SummaryResponse {

    public static final String AI_CAPACITY_LIMIT = "AI_CAPACITY_LIMIT";

    private final String summary;
    private final boolean imageOnly;
    private final boolean closed;
    private final boolean inProgress;
    private final String failureReason;
    private final Integer errorCode;

    private SummaryResponse(String summary, boolean imageOnly, boolean closed, boolean inProgress, String failureReason, Integer errorCode) {
        this.summary = summary;
        this.imageOnly = imageOnly;
        this.closed = closed;
        this.inProgress = inProgress;
        this.failureReason = failureReason;
        this.errorCode = errorCode;
    }

    public static SummaryResponse success(String summary) {
        return new SummaryResponse(summary, false, false, false, null, null);
    }

    public static SummaryResponse imageOnly() {
        return new SummaryResponse(null, true, false, false, null, null);
    }

    public static SummaryResponse aiFailed() {
        return aiFailed(null);
    }

    public static SummaryResponse aiFailed(Integer errorCode) {
        return new SummaryResponse(null, false, false, false, null, errorCode);
    }

    public static SummaryResponse aiCapacityLimited(int errorCode) {
        return new SummaryResponse(null, false, false, false, AI_CAPACITY_LIMIT, errorCode);
    }

    public static SummaryResponse closed() {
        return new SummaryResponse(null, false, true, false, null, null);
    }

    public static SummaryResponse inProgress() {
        return new SummaryResponse(null, false, false, true, null, null);
    }
}
