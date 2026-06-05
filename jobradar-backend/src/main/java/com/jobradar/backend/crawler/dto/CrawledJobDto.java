package com.jobradar.backend.crawler.dto;

import java.time.LocalDate;

public record CrawledJobDto(
        String title,
        String company,
        String location,
        String experienceLevel,
        String employmentType,
        String deadlineText,
        String sourceUrl,
        String sourceSite,
        String jobType,
        LocalDate listedAt,
        boolean external
) {
}
