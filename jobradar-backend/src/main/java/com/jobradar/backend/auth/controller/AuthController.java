package com.jobradar.backend.auth.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 컨트롤러
 *
 * [예정 API]
 * POST /api/auth/login    - 로그인 (JWT 발급)
 * POST /api/auth/refresh  - 액세스 토큰 재발급
 * POST /api/auth/logout   - 로그아웃
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    // JWT + Redis 인증 작업 시 구현 예정
}
