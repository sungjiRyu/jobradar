package com.jobradar.backend.job.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 채용공고 엔티티 */
@Entity
@Table(name = "jobs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String company; // 회사명

    @Column(nullable = false)
    private String title; // 공고 제목

    @Column(columnDefinition = "TEXT")
    private String description; // 공고 상세 내용

    @Column(nullable = false, length = 50)
    private String location; // 근무 지역 (예: 서울, 판교)

    @Column(length = 50)
    private String experienceLevel; // 경력 구분 (신입, 경력, 무관)

    @Column(length = 50)
    private String employmentType; // 고용 형태 (정규직, 계약직, 인턴)

    /**
     * 기술스택 태그 (쉼표 구분 문자열로 저장)
     * 예) "Java, Spring Boot, MySQL, Redis"
     */
    @Column(columnDefinition = "TEXT")
    private String techStack;

    private LocalDate deadline; // 지원 마감일 (null이면 상시채용)

    @Column(nullable = false)
    private String sourceUrl; // 원본 공고 URL

    @Column(nullable = false, length = 30)
    private String sourceSite; // 출처 사이트 (예: 사람인, 잡코리아, 원티드)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status; // 공고 상태

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = JobStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Builder
    public Job(String company, String title, String description, String location,
               String experienceLevel, String employmentType, String techStack,
               LocalDate deadline, String sourceUrl, String sourceSite) {
        this.company = company;
        this.title = title;
        this.description = description;
        this.location = location;
        this.experienceLevel = experienceLevel;
        this.employmentType = employmentType;
        this.techStack = techStack;
        this.deadline = deadline;
        this.sourceUrl = sourceUrl;
        this.sourceSite = sourceSite;
        this.status = JobStatus.ACTIVE;
    }

    // ===== 비즈니스 메서드 =====

    /** 공고 마감 처리 */
    public void close() {
        this.status = JobStatus.CLOSED;
    }

    // ===== 공고 상태 Enum =====
    public enum JobStatus {
        ACTIVE,  // 진행 중
        CLOSED   // 마감
    }
}
