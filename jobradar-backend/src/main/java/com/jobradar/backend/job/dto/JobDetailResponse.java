package com.jobradar.backend.job.dto;

import com.jobradar.backend.job.entity.Job;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 채용공고 상세 응답 DTO (전체 정보 — description, sourceUrl 등 포함) */
@Getter
public class JobDetailResponse {

    private final Long id;
    private final String company;
    private final String title;
    private final String description;
    private final String summary;       // AI 요약 (null이면 미생성)
    private final String location;
    private final String experienceLevel;
    private final String employmentType;
    private final List<String> techStacks;  // 기술스택 이름 목록
    private final LocalDate deadline;
    private final String sourceUrl;
    private final String sourceSite;
    private final String status;
    private final int viewCount;
    private final LocalDateTime createdAt;

    private JobDetailResponse(Job job) {
        this.id = job.getId();
        this.company = job.getCompany();
        this.title = job.getTitle();
        this.description = job.getDescription();
        this.summary = job.getSummary();
        this.location = job.getLocation();
        this.experienceLevel = job.getExperienceLevel();
        this.employmentType = job.getEmploymentType();
        this.techStacks = job.getTechStacks().stream()
                .map(ts -> ts.getName())
                .toList();
        this.deadline = job.getDeadline();
        this.sourceUrl = job.getSourceUrl();
        this.sourceSite = job.getSourceSite();
        this.status = job.getStatus().name();
        this.viewCount = job.getViewCount();
        this.createdAt = job.getCreatedAt();
    }

    public static JobDetailResponse from(Job job) {
        return new JobDetailResponse(job);
    }
}
