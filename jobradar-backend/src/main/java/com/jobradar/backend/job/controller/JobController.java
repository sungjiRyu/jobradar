package com.jobradar.backend.job.controller;

import com.jobradar.backend.global.common.ApiResponse;
import com.jobradar.backend.job.dto.JobDetailResponse;
import com.jobradar.backend.job.dto.JobResponse;
import com.jobradar.backend.job.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

/** 채용공고 컨트롤러 */
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    /**
     * GET /api/jobs
     * GET /api/jobs?keyword=카카오&location=서울&experienceLevel=신입&techStack=Java&page=0&size=10
     *
     * 공고 목록 조회 및 검색 (모든 파라미터 선택사항)
     */
    @GetMapping
    public ApiResponse<Page<JobResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String experienceLevel,
            @RequestParam(required = false) String techStack,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ApiResponse.ok(jobService.search(keyword, location, experienceLevel, techStack, pageable));
    }

    /** GET /api/jobs/{id} — 공고 상세 조회 (조회 시 viewCount 자동 증가) */
    @GetMapping("/{id}")
    public ApiResponse<JobDetailResponse> getDetail(@PathVariable Long id) {
        return ApiResponse.ok(jobService.getDetail(id));
    }
}
