package com.jobradar.backend.job.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 채용공고 엔티티 */
@Entity
@Table(name = "job_posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String company;       // 회사명

    @Column(nullable = false)
    private String title;         // 공고 제목

    @Column(columnDefinition = "TEXT")
    private String description;   // 공고 상세 내용 (크롤링 시 함께 수집)

    // description 수집 결과 상태 (크롤링 시 eager fetch 결과를 기록)
    // null  - 아직 fetch 안 됨 (기존 데이터, 백필 대상)
    // SUCCESS - 정상 수집 완료
    // IMAGE   - 이미지 공고로 텍스트 없음
    // FAILED  - 외부 사이트 fetch 실패 (재시도 안 함)
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DescriptionStatus descriptionStatus;

    @Column(columnDefinition = "TEXT")
    private String summary;       // AI 요약 (Gemini API, 최초 조회 시 생성)

    @Column(nullable = false, length = 50)
    private String location;      // 근무 지역 (예: 서울, 판교)

    @Column(length = 50)
    private String experienceLevel; // 경력 구분 (신입, 경력, 무관)

    @Column(length = 50)
    private String employmentType;  // 고용 형태 (정규직, 계약직, 인턴)

    private LocalDate deadline;   // 지원 마감일 (null이면 상시채용)

    @Column(nullable = false)
    private String sourceUrl;     // 원본 공고 URL

    @Column(nullable = false, length = 30)
    private String sourceSite;    // 출처 사이트 (사람인, 잡코리아, 원티드 등)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status;     // 공고 상태 (ACTIVE: 진행 중, CLOSED: 마감)

    @Column(name = "view_count", columnDefinition = "INT DEFAULT 0")
    private int viewCount;        // 조회수

    // 기술스택 다대다 관계 (job_post_stacks 중간 테이블로 연결)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "job_post_stacks",
            joinColumns = @JoinColumn(name = "job_post_id"),
            inverseJoinColumns = @JoinColumn(name = "tech_stack_id")
    )
    private List<TechStack> techStacks = new ArrayList<>();

    // 직무 분류 (백엔드, 프론트엔드, 풀스택, 모바일, 데이터, AI/ML, DevOps)
    // 크롤러가 카테고리 코드 기반으로 수집 시 설정됨
    @Column(length = 30)
    private String jobType;

    // 공고 게시일 (사이트에 등록/수정된 날짜)
    // 등록일순 정렬에 사용. null이면 createdAt(크롤링 시각) 기준으로 대체됨
    private LocalDate listedAt;

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
               String experienceLevel, String employmentType,
               LocalDate deadline, String sourceUrl, String sourceSite,
               String jobType, LocalDate listedAt,
               DescriptionStatus descriptionStatus) {
        this.company = company;
        this.title = title;
        this.description = description;
        this.location = location;
        this.experienceLevel = experienceLevel;
        this.employmentType = employmentType;
        this.deadline = deadline;
        this.sourceUrl = sourceUrl;
        this.sourceSite = sourceSite;
        this.jobType = jobType;
        this.listedAt = listedAt;
        this.descriptionStatus = descriptionStatus;
        this.status = JobStatus.ACTIVE;
        this.viewCount = 0;
    }

    // ===== 비즈니스 메서드 =====

    /** 조회수 1 증가 — JPA 더티 체킹으로 트랜잭션 종료 시 자동 UPDATE */
    public void incrementViewCount() {
        this.viewCount++;
    }

    /** 공고 마감 처리 */
    public void close() {
        this.status = JobStatus.CLOSED;
    }

    /** 상세 내용 + 수집 상태 저장 — 백필 또는 lazy fetch 시 사용 */
    public void updateDescription(String description, DescriptionStatus descriptionStatus) {
        this.description = description;
        this.descriptionStatus = descriptionStatus;
    }

    /** AI 요약 저장 — 최초 상세 조회 시 Gemini API 결과를 저장 */
    public void updateSummary(String summary) {
        this.summary = summary;
    }

    // ===== 공고 상태 Enum =====
    public enum JobStatus {
        ACTIVE,  // 진행 중
        CLOSED   // 마감
    }

    // ===== description 수집 결과 상태 Enum =====
    // 크롤러가 description을 eager fetch한 결과를 기록
    // null은 "아직 fetch 안 됨"(기존 데이터)을 의미하므로 enum 값으로 두지 않음
    public enum DescriptionStatus {
        SUCCESS,  // 텍스트 정상 수집
        IMAGE,    // 이미지 공고 (텍스트 없음)
        FAILED    // 외부 사이트 fetch 실패 (재시도 안 함)
    }
}
