package com.jobradar.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

/** 토큰 재발급 요청 DTO */
@Getter
public class RefreshRequest {

    @NotBlank(message = "리프레시 토큰을 입력해주세요.")
    private String refreshToken;
}
