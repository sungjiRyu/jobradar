package com.jobradar.backend.job.service;

import com.jobradar.backend.crawler.SaraminCrawlerService;
import com.jobradar.backend.global.config.AiSummaryService;
import com.jobradar.backend.global.exception.CustomException;
import com.jobradar.backend.global.exception.ErrorCode;
import com.jobradar.backend.job.dto.JobDetailResponse;
import com.jobradar.backend.job.dto.JobResponse;
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
     * 공고 상세 조회 — 최초 조회 시 상세 내용 크롤링 + AI 요약 lazy loading
     *
     * [흐름]
     * 1. DB에서 공고 조회
     * 2. description이 없으면 사람인 view-detail 크롤링 → DB 저장
     * 3. summary가 없으면 Gemini API로 요약 생성 → DB 저장
     * 4. 두 번째 조회부터는 DB 값 바로 반환 (추가 HTTP 요청 없음)
     *
     * [왜 @Transactional?]
     * JPA 더티 체킹을 활용: description/summary를 setter로 바꾸면
     * 트랜잭션 종료 시 자동으로 UPDATE 쿼리가 실행됨
     */
    @Transactional
    public JobDetailResponse getDetail(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new CustomException(ErrorCode.JOB_NOT_FOUND));

        job.incrementViewCount();

        // 1) 사람인 공고이고 description이 없으면 크롤링
        // desc가 null(이미지 공고 등)이면 ""로 저장해 다음 조회 때 재시도하지 않음
        if (job.getDescription() == null && "사람인".equals(job.getSourceSite())) {
            try {
                String desc = saraminCrawlerService.fetchDescription(job.getSourceUrl());
                job.updateDescription(desc != null ? desc : "");
                log.info("[JobService] description 크롤링 완료: jobId={}", jobId);
            } catch (Exception e) {
                log.warn("[JobService] description 크롤링 실패: jobId={}, error={}", jobId, e.getMessage());
            }
        }

        // 2) description은 있는데 summary가 없으면 AI 요약 생성
        if (job.getSummary() == null && job.getDescription() != null) {
            try {
                String summary = aiSummaryService.summarize(job.getDescription());
                if (summary != null) {
                    job.updateSummary(summary);
                    log.info("[JobService] AI 요약 완료: jobId={}", jobId);
                }
            } catch (Exception e) {
                log.warn("[JobService] AI 요약 실패: jobId={}, error={}", jobId, e.getMessage());
            }
        }

        return JobDetailResponse.from(job);
    }
}
