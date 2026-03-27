package com.jobradar.backend.scrap.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 스크랩 컨트롤러
 *
 * [예정 API]
 * POST   /api/scraps/{jobId}  - 스크랩 추가
 * DELETE /api/scraps/{jobId}  - 스크랩 취소
 * GET    /api/scraps          - 내 스크랩 목록 조회
 */
@RestController
@RequestMapping("/api/scraps")
public class ScrapController {
    // 스크랩 API 작업 시 구현 예정
}
