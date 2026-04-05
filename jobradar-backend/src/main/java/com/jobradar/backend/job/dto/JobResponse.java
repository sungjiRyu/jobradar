package com.jobradar.backend.job.dto;

import com.jobradar.backend.job.entity.Job;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

/** 채용공고 목록 응답 DTO (요약 정보) */
@Getter
public class JobResponse {

    private final Long id;
    private final String company;
    private final String title;
    private final String location;
    private final String experienceLevel;
    private final String employmentType;
    private final List<String> techStacks;  // 기술스택 이름 목록
    private final LocalDate deadline;
    private final String sourceSite;
    private final int viewCount;

    private JobResponse(Job job) {
        this.id = job.getId();
        this.company = job.getCompany();
        this.title = job.getTitle();
        this.location = job.getLocation();
        this.experienceLevel = job.getExperienceLevel();
        this.employmentType = job.getEmploymentType();
        // TechStack 엔티티 목록에서 이름만 추출
        this.techStacks = job.getTechStacks().stream()
                .map(ts -> ts.getName())
                .toList();
        this.deadline = job.getDeadline();
        this.sourceSite = job.getSourceSite();
        this.viewCount = job.getViewCount();
    }

    public static JobResponse from(Job job) {
        return new JobResponse(job);
    }
}
