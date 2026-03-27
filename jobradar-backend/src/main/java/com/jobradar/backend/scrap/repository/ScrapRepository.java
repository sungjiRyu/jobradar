package com.jobradar.backend.scrap.repository;

import com.jobradar.backend.scrap.entity.Scrap;
import org.springframework.data.jpa.repository.JpaRepository;

/** 스크랩 레포지토리 */
public interface ScrapRepository extends JpaRepository<Scrap, Long> {
}
