package com.jobradar.backend.job.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 기술스택 엔티티
 *
 * 기술스택 목록을 별도 테이블로 관리하는 이유:
 * - 공고 검색 시 기술스택 필터로 활용
 * - 프론트엔드 필터 드롭다운에 목록 제공
 * - 동일 기술명의 일관성 보장 (오타 방지)
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
