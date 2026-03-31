package com.jobradar.backend.job.dto;

import com.jobradar.backend.job.entity.Job;
import lombok.Getter;

import java.time.LocalDate;

/** 채용공고 목록 응답 DTO (요약 정보) */
@Getter
public class JobResponse {

    private final Long id;
    private final String company;
    private final String title;
    private final String location;
    private final String experienceLevel;
    private final String employmentType;
    private final String techStack;
    private final LocalDate deadline;
    private final String sourceSite;

    private JobResponse(Job job) {
        this.id = job.getId();
        this.company = job.getCompany();
        this.title = job.getTitle();
        this.location = job.getLocation();
        this.experienceLevel = job.getExperienceLevel();
        this.employmentType = job.getEmploymentType();
        this.techStack = job.getTechStack();
        this.deadline = job.getDeadline();
        this.sourceSite = job.getSourceSite();
    }

    public static JobResponse from(Job job) {
        return new JobResponse(job);
    }
}
