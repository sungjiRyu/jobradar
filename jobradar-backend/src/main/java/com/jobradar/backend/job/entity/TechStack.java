package com.jobradar.backend.job.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 기술스택 엔티티
 *
 */
@Entity
@Table(name = "tech_stacks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TechStack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * unique = true: 동일한 기술스택 이름 중복 저장 방지
     * length = 50: 기술명은 짧으므로 VARCHAR(50) 충분
     */
    @Column(nullable = false, unique = true, length = 50)
    private String name;  // 기술명 (예: Java, Spring Boot, React)

    @Builder
    public TechStack(String name) {
        this.name = name;
    }
}
