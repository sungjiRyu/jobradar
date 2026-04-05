package com.jobradar.backend.job.repository;

import com.jobradar.backend.job.entity.TechStack;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** 기술스택 레포지토리 */
public interface TechStackRepository extends JpaRepository<TechStack, Long> {

    List<TechStack> findAllByOrderByNameAsc();
}
