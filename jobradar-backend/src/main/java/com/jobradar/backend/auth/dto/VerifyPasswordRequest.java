package com.jobradar.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

/** 비밀번호 확인 요청 DTO */
@Getter
public class VerifyPasswordRequest {

    @NotBlank(message = "비밀번호를 입력해주세요.")
    private String password;
}
