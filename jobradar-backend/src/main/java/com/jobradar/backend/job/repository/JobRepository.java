package com.jobradar.backend.job.repository;

import com.jobradar.backend.job.entity.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 채용공고 레포지토리
 * JpaSpecificationExecutor: 동적 검색 조건(Specification)을 지원하기 위해 추가
 */
public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {

    /**
     * 중복 공고 체크 - sourceUrl 기준
     * Spring Data JPA가 메서드명을 분석해 자동으로 SQL 생성:
     * SELECT COUNT(*) > 0 FROM job_posts WHERE source_url = ?
     * 크롤러에서 이미 수집한 공고를 다시 저장하지 않기 위해 사용
     */
    boolean existsBySourceUrl(String sourceUrl);

    /** 복합 조건 검색 쿼리 (키워드/지역/경력/기술스택) */
    @Query(value = "SELECT DISTINCT j FROM Job j " +
                   "LEFT JOIN j.techStacks ts " +
                   "WHERE j.status = com.jobradar.backend.job.entity.Job.JobStatus.ACTIVE " +
                   "AND (:keyword IS NULL OR j.company LIKE %:keyword% OR j.title LIKE %:keyword%) " +
                   "AND (:location IS NULL OR j.location = :location) " +
                   "AND (:experienceLevel IS NULL OR j.experienceLevel = :experienceLevel) " +
                   "AND (:techStack IS NULL OR ts.name = :techStack)",
           countQuery = "SELECT COUNT(DISTINCT j) FROM Job j " +
                   "LEFT JOIN j.techStacks ts " +
                   "WHERE j.status = com.jobradar.backend.job.entity.Job.JobStatus.ACTIVE " +
                   "AND (:keyword IS NULL OR j.company LIKE %:keyword% OR j.title LIKE %:keyword%) " +
                   "AND (:location IS NULL OR j.location = :location) " +
                   "AND (:experienceLevel IS NULL OR j.experienceLevel = :experienceLevel) " +
                   "AND (:techStack IS NULL OR ts.name = :techStack)")
    Page<Job> search(@Param("keyword") String keyword,
                     @Param("location") String location,
                     @Param("experienceLevel") String experienceLevel,
                     @Param("techStack") String techStack,
                     Pageable pageable);

    /**
     * description_status가 null인 공고 조회 (백필 대상)
     * 기존 데이터 중 아직 description fetch 안 된 공고만 선별
     */
    List<Job> findByDescriptionStatusIsNull();

    /**
     * 마감일 지난 ACTIVE 공고를 일괄 CLOSED 처리
     *
     * - deadline < CURRENT_DATE: 마감일이 오늘보다 이전 (오늘 마감은 제외)
     * - deadline IS NULL (상시 채용/채용시까지)인 공고는 자동으로 제외
     *   ※ JPQL/SQL에서 NULL과의 비교 결과는 UNKNOWN → WHERE 조건에서 false 처리
     * - @Modifying: SELECT가 아닌 UPDATE 쿼리임을 알림
     *
     * @return 영향 받은 행 수
     */
    @Modifying
    @Query("UPDATE Job j " +
           "SET j.status = com.jobradar.backend.job.entity.Job.JobStatus.CLOSED, " +
           "    j.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE j.deadline < CURRENT_DATE " +
           "  AND j.status = com.jobradar.backend.job.entity.Job.JobStatus.ACTIVE")
    int closeExpiredJobs();
}
