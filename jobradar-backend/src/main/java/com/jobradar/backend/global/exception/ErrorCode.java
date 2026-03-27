package com.jobradar.backend.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** 에러 코드 정의 */
@Getter
public enum ErrorCode {

    // 공통
    INVALID_INPUT_VALUE(HttpStatus., "입력값이 올바르지 않습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),
    BAD_REQUEST
    // 회원
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 올바르지 않습니다."),

    // 인증
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),

    // 채용공고
    JOB_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 채용공고입니다."),

    // 스크랩
    SCRAP_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 스크랩한 공고입니다."),
    SCRAP_NOT_FOUND(HttpStatus.NOT_FOUND, "스크랩 정보를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
