package com.jobradar.backend.job.repository;

import com.jobradar.backend.job.entity.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 채용공고 레포지토리 */
public interface JobRepository extends JpaRepository<Job, Long> {

    // 키워드(회사명 or 제목), 지역, 경력 조건으로 ACTIVE 공고 검색
    @Query("SELECT j FROM Job j WHERE j.status = 'ACTIVE'" +
            " AND (:keyword IS NULL OR j.company LIKE %:keyword% OR j.title LIKE %:keyword%)" +
            " AND (:location IS NULL OR j.location = :location)" +
            " AND (:experienceLevel IS NULL OR j.experienceLevel = :experienceLevel)")
    Page<Job> search(@Param("keyword") String keyword,
                     @Param("location") String location,
                     @Param("experienceLevel") String experienceLevel,
                     Pageable pageable);
}
