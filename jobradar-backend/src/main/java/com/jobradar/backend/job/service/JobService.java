package com.jobradar.backend.job.service;

import com.jobradar.backend.crawler.JobkoreaCrawlerService;
import com.jobradar.backend.crawler.SaraminCrawlerService;
import com.jobradar.backend.global.config.AiSummaryService;
import com.jobradar.backend.global.exception.CustomException;
import com.jobradar.backend.global.exception.ErrorCode;
import com.jobradar.backend.job.dto.DescriptionResponse;
import com.jobradar.backend.job.dto.JobDetailResponse;
import com.jobradar.backend.job.dto.JobResponse;
import com.jobradar.backend.job.dto.SummaryResponse;
import com.jobradar.backend.job.entity.Job;
import com.jobradar.backend.job.repository.JobRepository;
import com.jobradar.backend.job.repository.JobSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final SaraminCrawlerService saraminCrawlerService;
    private final JobkoreaCrawlerService jobkoreaCrawlerService;
    private final AiSummaryService aiSummaryService;

    /**
     * 동적 검색 — Specification을 조합해 WHERE 조건 구성
     * 각 파라미터가 null/빈 리스트면 해당 조건을 추가하지 않음
     */
    @Transactional(readOnly = true)
    public Page<JobResponse> search(String keyword,
                                    List<String> locations,
                                    List<String> experiences,
                                    List<String> techStacks,
                                    Pageable pageable) {
        // ACTIVE 조건은 항상 포함
        Specification<Job> spec = JobSpecification.isActive();

        if (keyword != null && !keyword.isBlank()) {
            spec = spec.and(JobSpecification.hasKeyword(keyword));
        }
        if (!CollectionUtils.isEmpty(locations)) {
            spec = spec.and(JobSpecification.locationContains(locations));
        }
        if (!CollectionUtils.isEmpty(experiences)) {
            spec = spec.and(JobSpecification.experienceContains(experiences));
        }
        if (!CollectionUtils.isEmpty(techStacks)) {
            // techStack JOIN 시 중복 row 발생 → distinct 필요
            spec = spec.and(JobSpecification.hasTechStack(techStacks));
        }

        return jobRepository.findAll(spec, pageable).map(JobResponse::from);
    }

    @Transactional
    public JobDetailResponse getDetail(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new CustomException(ErrorCode.JOB_NOT_FOUND));
        job.incrementViewCount();
        return JobDetailResponse.from(job);
    }

    @Transactional
    public DescriptionResponse getDescription(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new CustomException(ErrorCode.JOB_NOT_FOUND));

        if (job.getDescription() != null && !job.getDescription().isEmpty()) {
            return DescriptionResponse.success(job.getDescription());
        }

        DescriptionResponse result = switch (job.getSourceSite()) {
            case "사람인" -> saraminCrawlerService.fetchDescription(job.getSourceUrl());
            case "잡코리아" -> jobkoreaCrawlerService.fetchDescription(job.getSourceUrl());
            default -> DescriptionResponse.crawlFailed();
        };

        if ("SUCCESS".equals(result.getStatus())) {
            job.updateDescription(result.getDescription());
            log.info("[JobService] description 크롤링 완료: jobId={}", jobId);
        }

        return result;
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
