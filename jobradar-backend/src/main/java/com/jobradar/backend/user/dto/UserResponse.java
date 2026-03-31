package com.jobradar.backend.user.dto;

import com.jobradar.backend.user.entity.User;
import lombok.Getter;

/** 회원 정보 응답 DTO */
@Getter
public class UserResponse {

    private final Long id;
    private final String email;
    private final String nickname;

    private UserResponse(Long id, String email, String nickname) {
        this.id = id;
        this.email = email;
        this.nickname = nickname;
    }

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getNickname());
    }
}
