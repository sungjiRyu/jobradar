package com.jobradar.backend.job.service;

import com.jobradar.backend.crawler.JobkoreaCrawlerService;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final SaraminCrawlerService saraminCrawlerService;
    private final JobkoreaCrawlerService jobkoreaCrawlerService;
    private final AiSummaryService aiSummaryService;

    @Transactional(readOnly = true)
    public Page<JobResponse> search(String keyword, String location,
                                    String experienceLevel, String techStack,
                                    Pageable pageable) {
        return jobRepository.search(keyword, location, experienceLevel, techStack, pageable)
                .map(JobResponse::from);
    }

    @Transactional
    public JobDetailResponse getDetail(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new CustomException(ErrorCode.JOB_NOT_FOUND));
        job.incrementViewCount();
        return JobDetailResponse.from(job);
    }

    @Transactional
    public String getDescription(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new CustomException(ErrorCode.JOB_NOT_FOUND));

        boolean needsCrawl = job.getDescription() == null || job.getDescription().isEmpty();
        if (needsCrawl) {
            try {
                String desc = switch (job.getSourceSite()) {
                    case "사람인" -> saraminCrawlerService.fetchDescription(job.getSourceUrl());
                    case "잡코리아" -> jobkoreaCrawlerService.fetchDescription(job.getSourceUrl());
                    default -> null;
                };
                if (desc != null) {
                    job.updateDescription(desc);
                    log.info("[JobService] description 크롤링 완료: jobId={}", jobId);
                }
            } catch (Exception e) {
                log.warn("[JobService] description 크롤링 실패: jobId={}, error={}", jobId, e.getMessage());
            }
        }

        return job.getDescription();
    }

    @Transactional
    public SummaryResponse getSummary(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new CustomException(ErrorCode.JOB_NOT_FOUND));

        if (job.getSummary() != null) return SummaryResponse.success(job.getSummary());

        boolean hasDescription = job.getDescription() != null && !job.getDescription().isEmpty();
        if (!hasDescription) return SummaryResponse.imageOnly();

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

        return SummaryResponse.aiFailed();
    }
}
