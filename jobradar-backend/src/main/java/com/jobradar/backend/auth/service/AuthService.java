package com.jobradar.backend.auth.service;

import org.springframework.stereotype.Service;

/**
 * 인증 서비스
 *
 * [구현 예정]
 * - 로그인 (JWT 액세스 토큰 + 리프레시 토큰 발급)
 * - 토큰 재발급 (리프레시 토큰으로 액세스 토큰 갱신)
 * - 로그아웃 (Redis에서 리프레시 토큰 삭제)
 *
 * [JWT + Redis 구조]
 * - 액세스 토큰: 짧은 유효기간(30분), stateless
 * - 리프레시 토큰: 긴 유효기간(7일), Redis에 저장
 * - 로그아웃 시 Redis에서 리프레시 토큰 삭제 → 재발급 불가
 */
@Service
public class AuthService {
    // JWT + Redis 인증 작업 시 구현 예정
}
