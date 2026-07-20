package com.jobradar.backend.global.config;

import com.jobradar.backend.job.entity.Job;
import com.jobradar.backend.job.entity.TechStack;
import com.jobradar.backend.stats.dto.TrendingJobResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CacheConfigTest {

    @Test
    @DisplayName("Redis 캐시 직렬화 - LocalDate를 포함한 인기 공고 응답을 처리")
    void redisValueSerializer_trendingJobResponseWithLocalDate() {
        // given
        Job job = Job.builder()
                .company("A회사")
                .title("백엔드 개발자")
                .location("서울")
                .experienceLevel("경력")
                .sourceUrl("https://example.com/jobs/1")
                .sourceSite("사람인")
                .deadline(LocalDate.of(2026, 7, 31))
                .build();
        ReflectionTestUtils.setField(job, "id", 1L);
        ReflectionTestUtils.setField(job, "viewCount", 100);
        job.getTechStacks().add(TechStack.builder().name("Java").build());

        List<TrendingJobResponse> responses = new ArrayList<>();
        responses.add(TrendingJobResponse.from(job, 1, 10L));

        // when
        byte[] serialized = CacheConfig.redisValueSerializer().serialize(responses);
        Object deserialized = CacheConfig.redisValueSerializer().deserialize(serialized);

        // then
        assertThat(serialized).isNotEmpty();
        assertThat(deserialized).isInstanceOf(List.class);
    }
}
