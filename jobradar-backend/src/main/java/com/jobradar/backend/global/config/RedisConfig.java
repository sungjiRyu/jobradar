package com.jobradar.backend.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 설정
 *
 * [사용하는 곳]
 * 1. JWT 리프레시 토큰 저장 → key: "refresh:{email}", value: refreshToken
 * 2. 통계·공고 목록 캐싱 (@Cacheable)
 */
@Configuration
public class RedisConfig {

    @Primary
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 키와 값을 모두 String으로 직렬화 (사람이 읽을 수 있는 형태로 Redis에 저장)
        StringRedisSerializer serializer = new StringRedisSerializer();
        template.setKeySerializer(serializer);
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(serializer);
        template.setHashValueSerializer(serializer);

        return template;
    }

}
