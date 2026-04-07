package com.jobradar.backend.scrap.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 스크랩 추가 요청 DTO
 *
 * 클라이언트가 스크랩할 채용공고 ID를 전달한다.
 * @NotNull: jobPostId가 반드시 존재해야 함 (null이면 400 Bad Request)
 */
@Getter
@NoArgsConstructor
public class ScrapRequest {

    @NotNull(message = "채용공고 ID는 필수입니다.")
    private Long jobPostId;
}
