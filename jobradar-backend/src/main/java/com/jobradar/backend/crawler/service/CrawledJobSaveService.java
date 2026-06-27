package com.jobradar.backend.crawler.service;

import com.jobradar.backend.crawler.dto.CrawledJobDto;
import com.jobradar.backend.global.time.BusinessTimeProvider;
import com.jobradar.backend.job.entity.Job;
import com.jobradar.backend.job.entity.TechStack;
import com.jobradar.backend.job.repository.JobRepository;
import com.jobradar.backend.job.repository.TechStackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CrawledJobSaveService {

    private static final List<String> TECH_KEYWORDS = List.of(
            "Java", "Spring", "Python", "React", "Vue", "Node.js",
            "Docker", "AWS", "MySQL", "Redis", "Kotlin", "TypeScript", "Kubernetes"
    );

    private final JobRepository jobRepository;
    private final TechStackRepository techStackRepository;
    private final BusinessTimeProvider businessTimeProvider;

    public boolean save(CrawledJobDto dto) {
        if (jobRepository.existsBySourceUrl(dto.sourceUrl())) {
            return false;
        }

        LocalDate deadline = parseDeadline(dto.deadlineText());
        Job.DeadlineType deadlineType = resolveDeadlineType(dto.deadlineText());
        List<TechStack> techStacks = resolveTechStacks(dto.title());

        Job job = toEntity(dto, deadline, deadlineType, techStacks);
        jobRepository.save(job);
        return true;
    }

    private Job toEntity(CrawledJobDto dto,
                         LocalDate deadline,
                         Job.DeadlineType deadlineType,
                         List<TechStack> techStacks) {
        Job.DescriptionStatus descriptionStatus = dto.external() ? Job.DescriptionStatus.EXTERNAL : null;

        Job job = Job.builder()
                .title(dto.title())
                .company(dto.company())
                .location(dto.location() == null || dto.location().isBlank() ? "미기재" : dto.location())
                .experienceLevel(dto.experienceLevel())
                .employmentType(dto.employmentType())
                .deadline(deadline)
                .deadlineType(deadlineType)
                .sourceUrl(dto.sourceUrl())
                .sourceSite(dto.sourceSite())
                .jobType(dto.jobType())
                .listedAt(dto.listedAt())
                .descriptionStatus(descriptionStatus)
                .build();

        job.getTechStacks().addAll(techStacks);
        return job;
    }

    private LocalDate parseDeadline(String text) {
        if (text == null || text.isBlank() || text.contains("채용시") || text.contains("상시")) {
            return null;
        }
        if (text.contains("내일")) {
            return businessTimeProvider.today().plusDays(1);
        }
        if (text.contains("오늘")) {
            return businessTimeProvider.today();
        }

        Matcher matcher = Pattern.compile("(\\d{1,2})/(\\d{1,2})").matcher(text);
        if (matcher.find()) {
            int month = Integer.parseInt(matcher.group(1));
            int day = Integer.parseInt(matcher.group(2));
            LocalDate today = businessTimeProvider.today();
            LocalDate parsed = LocalDate.of(today.getYear(), month, day);
            if (parsed.isBefore(today)) {
                parsed = parsed.plusYears(1);
            }
            return parsed;
        }

        return null;
    }

    private Job.DeadlineType resolveDeadlineType(String text) {
        if (text == null || text.isBlank() || text.contains("내일") || text.contains("오늘")) {
            return Job.DeadlineType.UNKNOWN;
        }
        if (text.contains("채용시") || text.contains("상시")) {
            return Job.DeadlineType.ALWAYS;
        }
        Matcher matcher = Pattern.compile("(\\d{1,2})/(\\d{1,2})").matcher(text);
        if (matcher.find()) {
            return Job.DeadlineType.FIXED;
        }
        return Job.DeadlineType.UNKNOWN;
    }

    private List<TechStack> resolveTechStacks(String title) {
        List<TechStack> result = new ArrayList<>();

        for (String keyword : TECH_KEYWORDS) {
            if (title.toLowerCase().contains(keyword.toLowerCase())) {
                TechStack techStack = techStackRepository.findByName(keyword)
                        .orElseGet(() -> techStackRepository.save(
                                TechStack.builder().name(keyword).build()
                        ));
                result.add(techStack);
            }
        }

        return result;
    }
}
