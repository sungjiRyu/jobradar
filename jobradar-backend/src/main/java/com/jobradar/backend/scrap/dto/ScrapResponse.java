package com.jobradar.backend.scrap.dto;

import com.jobradar.backend.scrap.entity.Scrap;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 스크랩 목록 응답 DTO
 *
 * Entity를 직접 반환하지 않고 DTO로 변환하는 이유:
 * 1. 불필요한 필드 노출 방지 (보안)
 * 2. 순환 참조 방지 (User ↔ Scrap ↔ Job)
 * 3. API 스펙 변경 시 Entity 영향 없음
 */
@Getter
@Builder
public class ScrapResponse {

    private Long scrapId;           // 스크랩 ID (삭제/상태변경에 사용)
    private Long jobPostId;         // 채용공고 ID (상세 페이지 이동에 사용)
    private String title;           // 공고 제목
    private String company;         // 회사명
    private LocalDate deadline;     // 마감일 (null이면 상시채용)
    private String status;          // 스크랩 상태 (PENDING, APPLIED, REVIEWING, REJECTED)
    private LocalDateTime createdAt; // 스크랩한 시간

    /**
     * Entity → DTO 변환 정적 팩토리 메서드
     * Controller가 아닌 DTO 내부에서 변환 로직을 관리하여 응집도를 높임
     */
    public static ScrapResponse from(Scrap scrap) {
        return ScrapResponse.builder()
                .scrapId(scrap.getId())
                .jobPostId(scrap.getJob().getId())
                .title(scrap.getJob().getTitle())
                .company(scrap.getJob().getCompany())
                .deadline(scrap.getJob().getDeadline())
                .status(scrap.getStatus().name())
                .createdAt(scrap.getCreatedAt())
                .build();
    }
}
