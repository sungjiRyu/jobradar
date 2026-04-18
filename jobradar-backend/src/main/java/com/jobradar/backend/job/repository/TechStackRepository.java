package com.jobradar.backend.job.repository;

import com.jobradar.backend.job.entity.TechStack;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import java.util.Optional;

/** 기술스택 레포지토리 */
public interface TechStackRepository extends JpaRepository<TechStack, Long> {

    List<TechStack> findAllByOrderByNameAsc();

    /**
     * 이름으로 기술스택 조회
     * 크롤러에서 키워드 파싱 후 DB에 이미 존재하는지 확인할 때 사용
     * 없으면 Optional.empty() 반환 → orElseGet()으로 새로 생성
     */
    Optional<TechStack> findByName(String name);
}
