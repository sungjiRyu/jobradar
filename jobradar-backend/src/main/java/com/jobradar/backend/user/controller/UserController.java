package com.jobradar.backend.user.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회원 컨트롤러
 *
 * [예정 API]
 * POST   /api/users/signup  - 회원가입
 * GET    /api/users/me      - 내 정보 조회
 * PUT    /api/users/me      - 내 정보 수정
 * DELETE /api/users/me      - 회원 탈퇴
 */
@RestController
@RequestMapping("/api/users")
public class UserController {
    // 회원가입/로그인 API 작업 시 구현 예정
}
