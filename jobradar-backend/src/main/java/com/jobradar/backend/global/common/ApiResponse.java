package com.jobradar.backend.global.common;

import lombok.Getter;

/**
 * 공통 API 응답 형식
 *
 * 모든 API 응답을 이 형식으로 통일합니다.
 * 예시:
 * {
 *   "success": true,
 *   "message": "요청이 성공했습니다.",
 *   "data": { ... }
 * }
 */
@Getter
public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;

    private ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    /** 성공 응답 (데이터 포함) */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "요청이 성공했습니다.", data);
    }

    /** 성공 응답 (커스텀 메시지 + 데이터) */
    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    /** 성공 응답 (데이터 없음) */
    public static <T> ApiResponse<T> ok(String message) {
        return new ApiResponse<>(true, message, null);
    }

    /** 실패 응답 */
    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(false, message, null);
    }
}
