package com.jobradar.backend.job.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 채용공고 컨트롤러
 *
 * [예정 API]
 * GET /api/jobs           - 공고 목록 조회 (페이지네이션)
 * GET /api/jobs/search    - 공고 검색 (키워드, 지역, 경력 필터)
 * GET /api/jobs/{id}      - 공고 상세 조회
 */
@RestController
@RequestMapping("/api/jobs")
public class JobController {
    // 공고 목록/검색/필터 API 작업 시 구현 예정
}
