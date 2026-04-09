package com.jobradar.backend.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;

/**
 * 오늘의 현황 응답 DTO
 * 예시: { "totalCount": 1284, "todayCount": 47, "urgentCount": 12, "juniorCount": 312 }
 *
 * Serializable: Redis 캐시(JDK 직렬화) 저장을 위해 필요
 */
@Getter
@AllArgsConstructor
public class TodayStatResponse implements Serializable {

    private final long totalCount;   // 전체 활성 공고 수
    private final long todayCount;   // 오늘 신규 등록 공고 수
    private final long urgentCount;  // 마감 D-7 이내 공고 수
    private final long juniorCount;  // 신입 공고 수
}
