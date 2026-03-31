package com.jobradar.backend.job.service;

import com.jobradar.backend.global.exception.CustomException;
import com.jobradar.backend.global.exception.ErrorCode;
import com.jobradar.backend.job.dto.JobDetailResponse;
import com.jobradar.backend.job.dto.JobResponse;
import com.jobradar.backend.job.entity.Job;
import com.jobradar.backend.job.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;

    /** 공고 목록 조회 및 검색 (키워드/지역/경력 필터, 페이지네이션) */
    @Transactional(readOnly = true)
    public Page<JobResponse> search(String keyword, String location, String experienceLevel, Pageable pageable) {
        return jobRepository.search(keyword, location, experienceLevel, pageable)
                .map(JobResponse::from);
    }

    /** 공고 상세 조회 */
    @Transactional(readOnly = true)
    public JobDetailResponse getDetail(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new CustomException(ErrorCode.JOB_NOT_FOUND));
        return JobDetailResponse.from(job);
    }
}
