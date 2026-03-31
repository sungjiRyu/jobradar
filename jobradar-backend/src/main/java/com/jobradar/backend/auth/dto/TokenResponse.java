package com.jobradar.backend.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 로그인/토큰 재발급 응답 DTO */
@Getter
@AllArgsConstructor
public class TokenResponse {

    private String accessToken;
    private String refreshToken;
}
