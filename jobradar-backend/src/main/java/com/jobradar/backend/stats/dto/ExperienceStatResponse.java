package com.jobradar.backend.stats.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 경력별 공고 비중 응답 DTO
 * 예시: { "experience": "신입", "count": 312, "percentage": 24 }
 *
 * Serializable: Redis 캐시(JDK 직렬화) 저장을 위해 필요
 */
@Getter
public class ExperienceStatResponse implements Serializable {

    private final String experience; // 경력 구분명 (신입, 경력 1~3년 등)
    private final long count;        // 해당 경력 공고 수
    @Setter
    private double percentage;       // 전체 대비 비중 (%) - 서비스에서 계산 후 주입

    // JPQL new 생성자 표현식: new ExperienceStatResponse(j.experienceLevel, COUNT(j))
    public ExperienceStatResponse(String experience, long count) {
        this.experience = experience;
        this.count = count;
    }
}
