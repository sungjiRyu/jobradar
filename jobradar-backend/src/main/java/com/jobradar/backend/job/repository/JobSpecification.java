package com.jobradar.backend.job.repository;

import com.jobradar.backend.job.entity.Job;
import com.jobradar.backend.job.entity.TechStack;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

/**
 * JobSpecification — Job 엔티티 동적 검색 조건 모음
 *
 * Specification 패턴: JPA Criteria API를 람다로 감싼 것.
 * 각 메서드가 독립적인 WHERE 조건 하나를 반환하고,
 * 서비스에서 and() / or() 로 조합한다.
 *
 * 기존 @Query JPQL 대비 장점:
 *   - null 조건을 직접 if 문으로 제어 (JPQL의 :param IS NULL 불필요)
 *   - 다중값 LIKE / IN 등 동적 조건을 유연하게 작성 가능
 */
public class JobSpecification {

    /** ACTIVE 상태 공고만 조회 */
    public static Specification<Job> isActive() {
        return (root, query, cb) ->
                cb.equal(root.get("status"), Job.JobStatus.ACTIVE);
    }

    /**
     * 제목 또는 회사명에 keyword 포함 (대소문자 무시)
     * LIKE '%keyword%'
     */
    public static Specification<Job> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            String pattern = "%" + keyword + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), pattern.toLowerCase()),
                    cb.like(cb.lower(root.get("company")), pattern.toLowerCase())
            );
        };
    }

    /**
     * 지역 부분 일치 (LIKE), OR 조건으로 복수 지원
     * "서울" → location LIKE '%서울%' → "서울 강남구" 등 모두 매칭
     */
    public static Specification<Job> locationContains(List<String> locations) {
        return (root, query, cb) -> {
            // 각 지역에 대해 LIKE 조건 생성 후 OR 로 결합
            List<Predicate> predicates = locations.stream()
                    .map(loc -> cb.like(root.get("location"), "%" + loc + "%"))
                    .toList();
            return cb.or(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * 경력 부분 일치 (LIKE), OR 조건으로 복수 지원
     * "신입" → experienceLevel LIKE '%신입%' → "신입", "신입·경력" 모두 매칭
     * "경력1년" → experienceLevel LIKE '%경력1년%' → "경력1년↑" 매칭
     */
    public static Specification<Job> experienceContains(List<String> experiences) {
        return (root, query, cb) -> {
            List<Predicate> predicates = experiences.stream()
                    .map(exp -> cb.like(root.get("experienceLevel"), "%" + exp + "%"))
                    .toList();
            return cb.or(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * 기술스택 이름 IN 절 (다중값 완전 일치)
     * distinct() 는 서비스에서 처리
     */
    public static Specification<Job> hasTechStack(List<String> techStacks) {
        return (root, query, cb) -> {
            // techStacks 컬렉션 조인
            Join<Job, TechStack> join = root.join("techStacks", JoinType.INNER);
            return join.get("name").in(techStacks);
        };
    }
}
