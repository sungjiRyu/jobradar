package com.jobradar.backend.job.repository;

import com.jobradar.backend.job.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;

/** 채용공고 레포지토리 */
public interface JobRepository extends JpaRepository<Job, Long> {
}
