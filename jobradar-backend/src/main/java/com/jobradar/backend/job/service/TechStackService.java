package com.jobradar.backend.job.service;

import com.jobradar.backend.job.repository.TechStackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 기술스택 서비스 */
@Service
@RequiredArgsConstructor
public class TechStackService {

    private final TechStackRepository techStackRepository;

    /**
     * 전체 기술스택 이름 목록 조회 (A-Z 정렬)
     *
     * 엔티티 전체 대신 이름(String)만 반환하는 이유:
     * 프론트엔드 필터 드롭다운에는 id가 필요 없고 이름만 필요하기 때문
     */
    @Transactional(readOnly = true)
    public List<String> getAll() {
        return techStackRepository.findAllByOrderByNameAsc()
                .stream()
                .map(ts -> ts.getName())
                .toList();
    }
}
