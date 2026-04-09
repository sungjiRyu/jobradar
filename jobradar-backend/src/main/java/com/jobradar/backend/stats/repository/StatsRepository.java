package com.jobradar.backend.stats.repository;

import com.jobradar.backend.job.entity.Job;
import com.jobradar.backend.stats.dto.ExperienceStatResponse;
import com.jobradar.backend.stats.dto.LocationStatResponse;
import com.jobradar.backend.stats.dto.TechStackStatResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 대시보드 통계 조회 Repository
 *
 * [왜 별도 Repository인가?]
 * - 통계 쿼리는 JobRepository의 CRUD 목적과 다름
 * - 도메인 책임 분리: 집계/통계는 StatsRepository, CRUD는 JobRepository
 * - 같은 job_posts 테이블을 조회하지만 JPA는 동일 엔티티에 여러 Repository를 허용
 */
public interface StatsRepository extends JpaRepository<Job, Long> {

    /**
     * 전체 활성 공고 수
     * Spring Data JPA가 메서드명을 분석해서 자동으로 SQL 생성:
     * SELECT COUNT(*) FROM job_posts WHERE status = ?
     */
    long countByStatus(Job.JobStatus status);

    /**
     * 기술스택별 공고 수 집계 (상위 N개)
     *
     * [JPQL 설명]
     * - JOIN j.techStacks ts: Job 엔티티의 techStacks 컬렉션과 조인 (job_post_stacks 테이블 경유)
     * - new TechStackStatResponse(...): 조회 결과를 바로 DTO로 매핑 (생성자 표현식)
     * - Pageable로 상위 8개 제한 (SQL LIMIT 역할)
     */
    @Query("SELECT new com.jobradar.backend.stats.dto.TechStackStatResponse(ts.name, COUNT(j)) " +
           "FROM Job j JOIN j.techStacks ts " +
           "WHERE j.status = :status " +
           "GROUP BY ts.name " +
           "ORDER BY COUNT(j) DESC")
    List<TechStackStatResponse> findTechStackStats(@Param("status") Job.JobStatus status, Pageable pageable);

    /**
     * 지역별 공고 수 집계
     *
     * [JPQL 설명]
     * - j.location 기준 GROUP BY
     * - percentage는 전체 합산이 필요해서 JPQL 대신 서비스 레이어에서 계산
     */
    @Query("SELECT new com.jobradar.backend.stats.dto.LocationStatResponse(j.location, COUNT(j)) " +
           "FROM Job j " +
           "WHERE j.status = :status " +
           "GROUP BY j.location " +
           "ORDER BY COUNT(j) DESC")
    List<LocationStatResponse> findLocationStats(@Param("status") Job.JobStatus status);

    /**
     * 오늘 신규 등록 공고 수
     *
     * [JPQL 설명]
     * - DATE() 함수 대신 범위 비교(>= startOfDay AND < endOfDay)를 쓰는 이유:
     *   함수로 감싸면 인덱스를 못 타지만, 범위 비교는 createdAt 인덱스를 활용 가능
     */
    @Query("SELECT COUNT(j) FROM Job j " +
           "WHERE j.status = :status " +
           "AND j.createdAt >= :startOfDay " +
           "AND j.createdAt < :endOfDay")
    long countToday(@Param("status") Job.JobStatus status,
                    @Param("startOfDay") LocalDateTime startOfDay,
                    @Param("endOfDay") LocalDateTime endOfDay);

    /**
     * 마감 임박 공고 수 (오늘 ~ D+7)
     *
     * [JPQL 설명]
     * - BETWEEN :today AND :limit: 오늘부터 7일 이내 마감 공고
     * - deadline IS NOT NULL: 상시채용(null) 제외
     */
    @Query("SELECT COUNT(j) FROM Job j " +
           "WHERE j.status = :status " +
           "AND j.deadline IS NOT NULL " +
           "AND j.deadline BETWEEN :today AND :limit")
    long countUrgent(@Param("status") Job.JobStatus status,
                     @Param("today") LocalDate today,
                     @Param("limit") LocalDate limit);

    /**
     * 신입 공고 수
     */
    @Query("SELECT COUNT(j) FROM Job j " +
           "WHERE j.status = :status " +
           "AND j.experienceLevel = '신입'")
    long countJunior(@Param("status") Job.JobStatus status);

    /**
     * 경력별 공고 수 집계
     *
     * [JPQL 설명]
     * - experienceLevel IS NOT NULL: 경력 정보 없는 공고 제외
     * - percentage는 전체 합산 후 서비스에서 계산
     */
    @Query("SELECT new com.jobradar.backend.stats.dto.ExperienceStatResponse(j.experienceLevel, COUNT(j)) " +
           "FROM Job j " +
           "WHERE j.status = :status " +
           "AND j.experienceLevel IS NOT NULL " +
           "GROUP BY j.experienceLevel " +
           "ORDER BY COUNT(j) DESC")
    List<ExperienceStatResponse> findExperienceStats(@Param("status") Job.JobStatus status);
}
