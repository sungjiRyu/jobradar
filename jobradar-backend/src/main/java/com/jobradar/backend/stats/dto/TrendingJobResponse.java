package com.jobradar.backend.stats.dto;

import com.jobradar.backend.job.entity.Job;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * 인기 공고 랭킹 응답 DTO
 */
@Getter
@NoArgsConstructor
public class TrendingJobResponse implements Serializable {

    private Long id;
    private int rank;
    private String company;
    private String title;
    private String location;
    private String experienceLevel;
    private List<String> techStacks;
    private LocalDate deadline;
    private String deadlineType;
    private String sourceSite;
    private int viewCount;
    private long scrapCount;

    private TrendingJobResponse(Job job, int rank, long scrapCount) {
        this.id = job.getId();
        this.rank = rank;
        this.company = job.getCompany();
        this.title = job.getTitle();
        this.location = job.getLocation();
        this.experienceLevel = job.getExperienceLevel();
        this.techStacks = job.getTechStacks().stream()
                .map(techStack -> techStack.getName())
                .toList();
        this.deadline = job.getDeadline();
        this.deadlineType = job.getDeadlineType() != null ? job.getDeadlineType().name() : null;
        this.sourceSite = job.getSourceSite();
        this.viewCount = job.getViewCount();
        this.scrapCount = scrapCount;
    }

    public static TrendingJobResponse from(Job job, int rank, long scrapCount) {
        return new TrendingJobResponse(job, rank, scrapCount);
    }
}
