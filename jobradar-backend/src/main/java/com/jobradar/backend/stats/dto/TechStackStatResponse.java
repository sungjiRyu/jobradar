package com.jobradar.backend.stats.dto;

import lombok.Getter;

import java.io.Serializable;

/**
 * 기술스택별 공고 수 응답 DTO
 * 예시: { "name": "Java", "count": 924 }
 *
 * Serializable: Redis 캐시(JDK 직렬화) 저장을 위해 필요
 */
@Getter
public class TechStackStatResponse implements Serializable {

    private final String name;  // 기술스택명
    private final long count;   // 해당 기술스택 공고 수

    // JPQL new 생성자 표현식: new TechStackStatResponse(ts.name, COUNT(j))
    public TechStackStatResponse(String name, long count) {
        this.name = name;
        this.count = count;
    }
}
