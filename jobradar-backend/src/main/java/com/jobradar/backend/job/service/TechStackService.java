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
     */
    @Transactional(readOnly = true)
    public List<String> getAll() {
        return techStackRepository.findAllByOrderByNameAsc()
                .stream()
                .map(ts -> ts.getName())
                .toList();
    }
}
