package com.jobradar.backend.job.controller;

import com.jobradar.backend.global.common.ApiResponse;
import com.jobradar.backend.job.dto.DescriptionResponse;
import com.jobradar.backend.job.dto.JobDetailResponse;
import com.jobradar.backend.job.dto.JobResponse;
import com.jobradar.backend.job.dto.SummaryResponse;
import com.jobradar.backend.job.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    /**
     * GET /api/jobs
     * GET /api/jobs?keyword=카카오&location=서울&location=경기&experienceLevel=신입&techStack=Java&techStack=React
     *
     * location, experienceLevel, techStack 은 반복 파라미터로 복수 전달 가능
     * 예: ?location=서울&location=경기 → List.of("서울", "경기")
     */
    @GetMapping
    public ApiResponse<Page<JobResponse>> search(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "location", required = false) List<String> locations,
            @RequestParam(name = "experienceLevel", required = false) List<String> experiences,
            @RequestParam(name = "techStack", required = false) List<String> techStacks,
            @RequestParam(name = "jobType", required = false) List<String> jobTypes,
            @RequestParam(name = "todayOnly", required = false, defaultValue = "false") boolean todayOnly,
            @RequestParam(name = "urgentOnly", required = false, defaultValue = "false") boolean urgentOnly,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        // deadline 정렬 요청 시 NULL 공고를 맨 뒤로 배치
        // MySQL은 ASC에서 NULL을 가장 앞에 놓기 때문에 별도 처리 필요
        boolean isDeadlineSort = pageable.getSort().getOrderFor("deadline") != null;
        if (isDeadlineSort) {
            Sort sort = Sort.by(Sort.Order.asc("deadline").nullsLast());
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        }

        return ApiResponse.ok(jobService.search(keyword, locations, experiences, techStacks, jobTypes, todayOnly, urgentOnly, pageable));
    }

    /** GET /api/jobs/{id} — 공고 상세 조회 (DB 조회만, 즉시 응답) */
    @GetMapping("/{id}")
    public ApiResponse<JobDetailResponse> getDetail(@PathVariable("id") Long id) {
        return ApiResponse.ok(jobService.getDetail(id));
    }

    @GetMapping("/{id}/description")
    public ApiResponse<DescriptionResponse> getDescription(@PathVariable("id") Long id) {
        return ApiResponse.ok(jobService.getDescription(id));
    }

    @GetMapping("/{id}/summary")
    public ApiResponse<SummaryResponse> getSummary(@PathVariable("id") Long id) {
        return ApiResponse.ok(jobService.getSummary(id));
    }
}
