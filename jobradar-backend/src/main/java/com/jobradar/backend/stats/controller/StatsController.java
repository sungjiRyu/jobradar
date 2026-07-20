package com.jobradar.backend.stats.controller;

import com.jobradar.backend.global.common.ApiResponse;
import com.jobradar.backend.stats.dto.ExperienceStatResponse;
import com.jobradar.backend.stats.dto.LocationStatResponse;
import com.jobradar.backend.stats.dto.TechStackStatResponse;
import com.jobradar.backend.stats.dto.TodayStatResponse;
import com.jobradar.backend.stats.dto.TrendingJobResponse;
import com.jobradar.backend.stats.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 대시보드 통계 API 컨트롤러
 * 모든 엔드포인트는 비로그인 접근 허용 (SecurityConfig에서 /api/stats/** permitAll 설정)
 */
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    /** GET /api/stats/tech-stacks - 기술스택별 공고 수 (상위 8개) */
    @GetMapping("/tech-stacks")
    public ApiResponse<List<TechStackStatResponse>> getTechStackStats() {
        return ApiResponse.ok(statsService.getTechStackStats());
    }

    /** GET /api/stats/locations - 지역별 공고 수 + 비중 */
    @GetMapping("/locations")
    public ApiResponse<List<LocationStatResponse>> getLocationStats() {
        return ApiResponse.ok(statsService.getLocationStats());
    }

    /** GET /api/stats/today - 오늘의 현황 */
    @GetMapping("/today")
    public ApiResponse<TodayStatResponse> getTodayStats() {
        return ApiResponse.ok(statsService.getTodayStats());
    }

    /** GET /api/stats/experience - 경력별 공고 비중 */
    @GetMapping("/experience")
    public ApiResponse<List<ExperienceStatResponse>> getExperienceStats() {
        return ApiResponse.ok(statsService.getExperienceStats());
    }

    /** GET /api/stats/trending-jobs - 인기 공고 랭킹 Top 10 */
    @GetMapping("/trending-jobs")
    public ApiResponse<List<TrendingJobResponse>> getTrendingJobs() {
        return ApiResponse.ok(statsService.getTrendingJobs());
    }
}
