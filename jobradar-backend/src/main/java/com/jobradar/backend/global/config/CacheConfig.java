package com.jobradar.backend.global.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis 캐시 설정
 *
 * [TTL 전략]
 * 기본 캐시는 24시간, 사용자 반응 기반 랭킹 캐시는 짧은 TTL로 설정
 * 크롤링시 혹은 데이터 변경시 @CacheEvict 을 통해 refresh 한다
 *
 * [직렬화 방식]
 * - Spring Data Redis 4.0부터 Jackson 기반 직렬화기가 deprecated
 * - 기본 JDK 직렬화(Serializable) 사용 → DTO에 Serializable 구현 필요
 */
@Configuration
@EnableCaching // @Cacheable, @CacheEvict 등 캐시 어노테이션 활성화
public class CacheConfig {

    // 캐시 이름
    public static final String CACHE_TECH_STACKS = "stats:tech-stacks";
    public static final String CACHE_LOCATIONS   = "stats:locations";
    public static final String CACHE_TODAY       = "stats:today";
    public static final String CACHE_EXPERIENCE  = "stats:experience";
    public static final String CACHE_TRENDING_JOBS = "stats:trending-jobs";

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {

        // 기본 캐시 설정: JSON 직렬화, null 캐싱 비허용, TTL 24시간
        // GenericJackson2JsonRedisSerializer: 클래스 로더 정보 없이 JSON으로 저장
        // → DevTools 재시작 후에도 ClassCastException 없이 정상 역직렬화 가능
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(24))
                .disableCachingNullValues()
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer()
                        )
                );

        // 캐시별 TTL 개별 설정
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put(CACHE_TECH_STACKS, defaultConfig.entryTtl(Duration.ofHours(24)));
        cacheConfigs.put(CACHE_LOCATIONS,   defaultConfig.entryTtl(Duration.ofHours(24)));
        cacheConfigs.put(CACHE_EXPERIENCE,  defaultConfig.entryTtl(Duration.ofHours(24)));
        cacheConfigs.put(CACHE_TODAY,       defaultConfig.entryTtl(Duration.ofHours(24)));
        cacheConfigs.put(CACHE_TRENDING_JOBS, defaultConfig.entryTtl(Duration.ofMinutes(1)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }
}
