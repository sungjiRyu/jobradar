package com.jobradar.backend.stats.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 지역별 공고 수 응답 DTO
 * 예시: { "location": "서울", "count": 612, "percentage": 48 }
 *
 * Serializable: Redis 캐시(JDK 직렬화) 저장을 위해 필요
 */
@Getter
public class LocationStatResponse implements Serializable {

    private final String location;  // 지역명
    private final long count;       // 해당 지역 공고 수
    @Setter
    private double percentage;      // 전체 대비 비중 (%) - 서비스에서 계산 후 주입

    // JPQL new 생성자 표현식: new LocationStatResponse(j.location, COUNT(j))
    public LocationStatResponse(String location, long count) {
        this.location = location;
        this.count = count;
    }
}
