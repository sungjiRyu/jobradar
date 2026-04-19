package com.jobradar.backend.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 오늘의 현황 응답 DTO
 * 예시: { "totalCount": 1284, "todayCount": 47, "urgentCount": 12, "juniorCount": 312 }
 *
 * @NoArgsConstructor: JSON 역직렬화 시 Jackson이 기본 생성자로 객체를 먼저 생성하므로 필요
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TodayStatResponse implements Serializable {

    private long totalCount;   // 전체 활성 공고 수
    private long todayCount;   // 오늘 신규 등록 공고 수
    private long urgentCount;  // 마감 D-7 이내 공고 수
    private long juniorCount;  // 신입 공고 수
}
