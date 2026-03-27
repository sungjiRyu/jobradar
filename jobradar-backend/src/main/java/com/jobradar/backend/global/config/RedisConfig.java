package com.jobradar.backend.global.config;

import org.springframework.context.annotation.Configuration;

/**
 * Redis 설정
 *
 * [사용하는 곳]
 * 1. JWT 리프레시 토큰 저장
 * 2. 채용공고 목록 캐싱
 *
 * [구현 예정]
 * - RedisTemplate Bean 설정
 * - 직렬화/역직렬화 설정 (JSON)
 */
@Configuration
public class RedisConfig {
    // JWT + Redis 인증 작업 시 구현 예정
}
