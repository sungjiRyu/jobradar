package com.jobradar.backend.scrap.dto;

import com.jobradar.backend.scrap.entity.Scrap;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 스크랩 상태 변경 요청 DTO
 *
 * PATCH /api/scraps/{id} 에서 사용
 * 변경할 상태값(ScrapStatus)을 전달받는다.
 */
@Getter
@NoArgsConstructor
public class StatusUpdateRequest {

    @NotNull(message = "변경할 상태값은 필수입니다.")
    private Scrap.ScrapStatus status;    
}
