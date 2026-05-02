package com.jobradar.backend.scrap.controller;

import com.jobradar.backend.global.common.ApiResponse;
import com.jobradar.backend.scrap.dto.ScrapRequest;
import com.jobradar.backend.scrap.dto.ScrapResponse;
import com.jobradar.backend.scrap.dto.StatusUpdateRequest;
import com.jobradar.backend.scrap.entity.Scrap;
import com.jobradar.backend.scrap.service.ScrapService;

import java.util.List;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 스크랩 컨트롤러
 *
 * 모든 API는 로그인 필수 (SecurityConfig에서 /api/scraps/** 는 authenticated)
 * @AuthenticationPrincipal: JwtFilter가 SecurityContext에 저장한 email을 주입받음
 */
@RestController
@RequestMapping("/api/scraps")
@RequiredArgsConstructor
public class ScrapController {

    private final ScrapService scrapService;

    /**
     * POST /api/scraps — 스크랩 추가
     *
     * @Valid: ScrapRequest의 @NotNull 유효성 검사를 실행
     * @RequestBody: HTTP Body의 JSON을 ScrapRequest 객체로 변환
     * @AuthenticationPrincipal: JWT에서 추출한 로그인 사용자 email
     */
    @PostMapping
    public ApiResponse<ScrapResponse> addScrap(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody ScrapRequest request) {

        return ApiResponse.ok("스크랩이 추가되었습니다.", scrapService.addScrap(email, request.getJobPostId()));
    }

    /**
     * DELETE /api/scraps/{id} — 스크랩 삭제
     *
     * @PathVariable: URL 경로의 {id} 값을 파라미터로 바인딩
     * 본인의 스크랩만 삭제 가능 (타인 스크랩 시 403 Forbidden)
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteScrap(
            @AuthenticationPrincipal String email,
            @PathVariable("id") Long id) {

        scrapService.deleteScrap(email, id);
        return ApiResponse.ok("스크랩이 삭제되었습니다.");
    }

    /**
     * GET /api/scraps — 내 스크랩 목록 조회
     * GET /api/scraps?status=PENDING — 상태별 필터 조회
     *
     * @RequestParam(required = false): 선택 파라미터 — 없으면 전체 조회, 있으면 해당 상태만 필터링
     */
    @GetMapping
    public ApiResponse<List<ScrapResponse>> getMyScrapList(
            @AuthenticationPrincipal String email,
            @RequestParam(name = "status", required = false) Scrap.ScrapStatus status) {

        return ApiResponse.ok(scrapService.getMyScrapList(email, status));
    }

    /**
     * PATCH /api/scraps/{id} — 스크랩 상태 변경
     *
     * @PatchMapping: 리소스의 일부(상태값)만 수정할 때 사용 (PUT은 전체 교체)
     * 본인의 스크랩만 상태 변경 가능 (타인 스크랩 시 403 Forbidden)
     */
    @PatchMapping("/{id}")
    public ApiResponse<ScrapResponse> updateStatus(
            @AuthenticationPrincipal String email,
            @PathVariable("id") Long id,
            @Valid @RequestBody StatusUpdateRequest request) {

        return ApiResponse.ok("스크랩 상태가 변경되었습니다.", scrapService.updateStatus(email, id, request.getStatus()));
    }
}
