package com.jobradar.backend.stats.dto;

import lombok.Getter;

/**
 * 인기 공고 랭킹 정렬용 중간 조회 결과
 */
@Getter
public class TrendingJobRankRow {

    private final Long jobId;
    private final long scrapCount;

    public TrendingJobRankRow(Long jobId, long scrapCount) {
        this.jobId = jobId;
        this.scrapCount = scrapCount;
    }
}
