package com.jobradar.backend.stats.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 기술스택별 공고 수 응답 DTO
 * 예시: { "name": "Java", "count": 924 }
 *
 * @NoArgsConstructor: JSON 역직렬화 시 Jackson이 기본 생성자로 객체를 먼저 생성하므로 필요
 */
@Getter
@NoArgsConstructor
public class TechStackStatResponse implements Serializable {

    private String name;  // 기술스택명
    private long count;   // 해당 기술스택 공고 수

    // JPQL new 생성자 표현식: new TechStackStatResponse(ts.name, COUNT(j))
    public TechStackStatResponse(String name, long count) {
        this.name = name;
        this.count = count;
    }
}
