package com.jobradar.backend.scrap.entity;

import com.jobradar.backend.job.entity.Job;
import com.jobradar.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 스크랩 엔티티 (회원 - 채용공고 연결 테이블) */
@Entity
@Table(
    name = "scraps",
    uniqueConstraints = {
        // 같은 회원이 같은 공고를 중복 스크랩하지 못하도록 유니크 제약 조건
        @UniqueConstraint(columnNames = {"user_id", "job_id"})
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Scrap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * @ManyToOne: 다대일 관계 (스크랩 N : 회원 1)
     * @JoinColumn: FK 컬럼명 지정
     * fetch = FetchType.LAZY: 지연 로딩
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Builder
    public Scrap(User user, Job job) {
        this.user = user;
        this.job = job;
    }
}
