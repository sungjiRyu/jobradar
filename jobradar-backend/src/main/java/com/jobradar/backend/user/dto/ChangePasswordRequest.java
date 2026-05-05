package com.jobradar.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

/** 비밀번호 변경 요청 DTO */
@Getter
public class ChangePasswordRequest {

    @NotBlank(message = "새 비밀번호를 입력해주세요.")
    @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
    private String newPassword;
}
