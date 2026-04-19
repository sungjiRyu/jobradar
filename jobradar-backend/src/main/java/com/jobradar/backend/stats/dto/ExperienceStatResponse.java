package com.jobradar.backend.stats.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * 경력별 공고 비중 응답 DTO
 * 예시: { "experience": "신입", "count": 312, "percentage": 24 }
 *
 * @NoArgsConstructor: JSON 역직렬화 시 Jackson이 기본 생성자로 객체를 먼저 생성하므로 필요
 */
@Getter
@NoArgsConstructor
public class ExperienceStatResponse implements Serializable {

    private String experience;
    private long count;
    @Setter
    private double percentage;

    // JPQL new 생성자 표현식: new ExperienceStatResponse(j.experienceLevel, COUNT(j))
    public ExperienceStatResponse(String experience, long count) {
        this.experience = experience;
        this.count = count;
    }
}
