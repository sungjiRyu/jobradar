package com.jobradar.backend.job.dto;

import com.jobradar.backend.job.entity.Job;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 채용공고 상세 응답 DTO (전체 정보) */
@Getter
public class JobDetailResponse {

    private final Long id;
    private final String company;
    private final String title;
    private final String description;
    private final String location;
    private final String experienceLevel;
    private final String employmentType;
    private final String techStack;
    private final LocalDate deadline;
    private final String sourceUrl;
    private final String sourceSite;
    private final String status;
    private final LocalDateTime createdAt;

    private JobDetailResponse(Job job) {
        this.id = job.getId();
        this.company = job.getCompany();
        this.title = job.getTitle();
        this.description = job.getDescription();
        this.location = job.getLocation();
        this.experienceLevel = job.getExperienceLevel();
        this.employmentType = job.getEmploymentType();
        this.techStack = job.getTechStack();
        this.deadline = job.getDeadline();
        this.sourceUrl = job.getSourceUrl();
        this.sourceSite = job.getSourceSite();
        this.status = job.getStatus().name();
        this.createdAt = job.getCreatedAt();
    }

    public static JobDetailResponse from(Job job) {
        return new JobDetailResponse(job);
    }
}
