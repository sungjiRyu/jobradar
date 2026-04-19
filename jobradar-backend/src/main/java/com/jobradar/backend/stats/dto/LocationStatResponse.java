package com.jobradar.backend.stats.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * 지역별 공고 수 응답 DTO
 * 예시: { "location": "서울", "count": 612, "percentage": 48 }
 *
 * @NoArgsConstructor: JSON 역직렬화 시 Jackson이 기본 생성자로 객체를 먼저 생성하므로 필요
 */
@Getter
@NoArgsConstructor
public class LocationStatResponse implements Serializable {

    private String location;
    private long count;
    @Setter
    private double percentage;

    // JPQL new 생성자 표현식: new LocationStatResponse(j.location, COUNT(j))
    public LocationStatResponse(String location, long count) {
        this.location = location;
        this.count = count;
    }
}
