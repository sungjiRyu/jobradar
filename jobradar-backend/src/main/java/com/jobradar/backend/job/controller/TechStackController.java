package com.jobradar.backend.job.controller;

import com.jobradar.backend.global.common.ApiResponse;
import com.jobradar.backend.job.service.TechStackService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 기술스택 컨트롤러
 *
 * 용도: 프론트엔드 공고 검색 필터의 "기술스택" 드롭다운 목록 제공
 * 인증 없이 누구나 조회 가능 (SecurityConfig에서 permitAll 처리)
 */
@RestController
@RequestMapping("/api/tech-stacks")
@RequiredArgsConstructor
public class TechStackController {

    private final TechStackService techStackService;

    /**
     * GET /api/tech-stacks
     *
     * 전체 기술스택 목록 반환 (A-Z 정렬)
     *
     * 응답 예시:
     * {
     *   "success": true,
     *   "message": "요청이 성공했습니다.",
     *   "data": ["AWS", "Docker", "Java", "MySQL", "Python", "React", "Redis", "Spring"]
     * }
     */
    @GetMapping
    public ApiResponse<List<String>> getAll() {
        return ApiResponse.ok(techStackService.getAll());
    }
}
