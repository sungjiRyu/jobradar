package com.jobradar.backend.job.service;

import com.jobradar.backend.crawler.SaraminCrawlerService;
import com.jobradar.backend.global.config.AiSummaryService;
import com.jobradar.backend.global.exception.CustomException;
import com.jobradar.backend.global.exception.ErrorCode;
import com.jobradar.backend.job.dto.JobDetailResponse;
import com.jobradar.backend.job.dto.JobResponse;
import com.jobradar.backend.job.dto.SummaryResponse;
import com.jobradar.backend.job.entity.Job;
import com.jobradar.backend.job.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 채용공고 서비스 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;

    // 사람인 상세 크롤링 (description lazy loading)
    private final SaraminCrawlerService saraminCrawlerService;

    // Gemini AI 요약 (summary lazy loading)
    private final AiSummaryService aiSummaryService;

    /** 공고 목록 조회 및 검색 (키워드/지역/경력/기술스택 복합 필터, 페이지네이션) */
    @Transactional(readOnly = true)
    public Page<JobResponse> search(String keyword, String location,
                                    String experienceLevel, String techStack,
                                    Pageable pageable) {
        return jobRepository.search(keyword, location, experienceLevel, techStack, pageable)
                .map(JobResponse::from);
    }

    /**
     * 공고 상세 조회 — DB 조회만 수행 (즉시 응답)
     * 크롤링/AI는 GET /api/jobs/{id}/summary 에서 별도 처리
     */
    @Transactional
    public JobDetailResponse getDetail(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new CustomException(ErrorCode.JOB_NOT_FOUND));
        job.incrementViewCount();
        return JobDetailResponse.from(job);
    }

    /**
     * AI 정리 조회 — 최초 요청 시 크롤링 + Gemini API 실행 후 SummaryResponse 반환
     * 두 번째 요청부터는 DB 캐시 값 바로 반환
     *
     * 반환 케이스:
     * - success   : AI 정리 완료
     * - imageOnly : 텍스트 없는 이미지 공고 (재시도해도 결과 없음)
     * - failed  : Gemini 호출 실패 또는 null 반환 (재시도 가능)
     */
    @Transactional
    public SummaryResponse getSummary(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new CustomException(ErrorCode.JOB_NOT_FOUND));

        // 이미 생성된 summary가 있으면 바로 반환
        if (job.getSummary() != null) return SummaryResponse.success(job.getSummary());

        // 1) description이 없거나 이전 크롤링이 실패한 경우 재시도
        boolean needsCrawl = "사람인".equals(job.getSourceSite())
                && (job.getDescription() == null || job.getDescription().isEmpty());
        if (needsCrawl) {
            try {
                String desc = saraminCrawlerService.fetchDescription(job.getSourceUrl());
                job.updateDescription(desc != null ? desc : "");
                log.info("[JobService] description 크롤링 완료: jobId={}", jobId);
            } catch (Exception e) {
                log.warn("[JobService] description 크롤링 실패: jobId={}, error={}", jobId, e.getMessage());
            }
        }

        // description이 없으면 이미지 공고 (텍스트 추출 불가)
        boolean hasDescription = job.getDescription() != null && !job.getDescription().isEmpty();
        if (!hasDescription) return SummaryResponse.imageOnly();

        // 2) Gemini AI 요약 생성
        try {
            String summary = aiSummaryService.summarize(job.getDescription());
            if (summary != null) {
                job.updateSummary(summary);
                log.info("[JobService] AI 요약 완료: jobId={}", jobId);
                return SummaryResponse.success(summary);
            }
        } catch (Exception e) {
            log.warn("[JobService] AI 요약 실패: jobId={}, error={}", jobId, e.getMessage());
        }

        // description은 있지만 AI 호출 실패 (429, 네트워크 등)
        return SummaryResponse.aiFailed();
    }
}
