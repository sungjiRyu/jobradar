package com.jobradar.backend.job.dto;

import lombok.Getter;

/**
 * 공고 상세 내용 크롤링 결과 DTO
 *
 * status:
 *   SUCCESS      - 텍스트 추출 성공
 *   IMAGE        - 이미지 공고 (텍스트 없음, img 태그 감지)
 *   CRAWL_FAILED - 크롤링 실패 (네트워크 오류, 파싱 실패 등)
 */
@Getter
public class DescriptionResponse {

    private final String status;
    private final String description;

    private DescriptionResponse(String status, String description) {
        this.status = status;
        this.description = description;
    }

    public static DescriptionResponse success(String description) {
        return new DescriptionResponse("SUCCESS", description);
    }

    public static DescriptionResponse image() {
        return new DescriptionResponse("IMAGE", null);
    }

    public static DescriptionResponse crawlFailed() {
        return new DescriptionResponse("CRAWL_FAILED", null);
    }
}
