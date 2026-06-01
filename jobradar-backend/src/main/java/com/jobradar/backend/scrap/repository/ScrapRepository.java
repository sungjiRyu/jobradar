package com.jobradar.backend.scrap.repository;

import com.jobradar.backend.scrap.entity.Scrap;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 스크랩 레포지토리
 *
 * Spring Data JPA가 인터페이스를 보고 구현체를 자동 생성한다.
 * 메서드 이름 규칙(findBy..., existsBy...)을 따르면 JPQL을 직접 작성하지 않아도 된다.
 */
public interface ScrapRepository extends JpaRepository<Scrap, Long> {

    /**
     * 중복 스크랩 체크
     * 같은 사용자가 같은 공고를 이미 스크랩했는지 확인
     * → 메서드명 컨벤션: existsBy + 필드명 조합 → Spring Data JPA가 자동으로 SQL 생성
     */
    boolean existsByUserEmailAndJobId(String email, Long jobId);

    /**
     * 사용자 이메일로 전체 스크랩 목록 조회
     */
    @EntityGraph(attributePaths = {"job"})
    List<Scrap> findByUserEmailOrderByCreatedAtDesc(String email);

    /**
     * 사용자 이메일 + 상태별 스크랩 목록 필터 조회
     * 프론트엔드에서 "지원예정", "지원완료" 등 탭별 필터링에 사용
     */
    @EntityGraph(attributePaths = {"job"})
    List<Scrap> findByUserEmailAndStatusOrderByCreatedAtDesc(String email, Scrap.ScrapStatus status);
}
