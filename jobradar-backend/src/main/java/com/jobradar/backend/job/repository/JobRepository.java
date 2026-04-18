package com.jobradar.backend.job.repository;

import com.jobradar.backend.job.entity.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 채용공고 레포지토리 */
public interface JobRepository extends JpaRepository<Job, Long> {

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
}
