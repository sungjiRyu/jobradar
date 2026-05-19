package com.jobradar.backend.stats.service;

import com.jobradar.backend.global.config.CacheConfig;
import com.jobradar.backend.job.entity.Job;
import com.jobradar.backend.stats.dto.ExperienceStatResponse;
import com.jobradar.backend.stats.dto.LocationStatResponse;
import com.jobradar.backend.stats.dto.TechStackStatResponse;
import com.jobradar.backend.stats.dto.TodayStatResponse;
import com.jobradar.backend.stats.repository.StatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 대시보드 통계 서비스
 *
 * [@Cacheable 동작 원리]
 * 1. 메서드 호출 시 Redis에 캐시 키가 존재하면 → DB 조회 없이 캐시 값 반환
 * 2. 캐시 미스(처음 호출 또는 TTL 만료)이면 → DB 조회 후 결과를 Redis에 저장
 * 3. 이후 TTL 내 동일 요청 → Redis에서 바로 반환
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 통계는 조회 전용이므로 readOnly로 성능 최적화
public class StatsService {

    private final StatsRepository statsRepository;

    /**
     * 기술스택별 공고 수 (상위 8개)
     *
     * @Cacheable: 캐시 이름 = "stats:tech-stacks", key = "all" (고정값)
     * TTL 10분 - CacheConfig에서 설정
     */
    @Cacheable(value = CacheConfig.CACHE_TECH_STACKS, key = "'all'")
    public List<TechStackStatResponse> getTechStackStats() {
        // PageRequest.of(0, 8): 첫 번째 페이지, 8개 제한 → SQL LIMIT 8 역할
        return statsRepository.findTechStackStats(Job.JobStatus.ACTIVE, LocalDate.now(), PageRequest.of(0, 8));
    }

    /**
     * 지역별 공고 수 + 비중(%)
     *
     * @Cacheable: TTL 10분
     * percentage는 전체 합산이 필요하므로 서비스에서 계산
     */
    @Cacheable(value = CacheConfig.CACHE_LOCATIONS, key = "'all'")
    public List<LocationStatResponse> getLocationStats() {
        List<LocationStatResponse> stats = statsRepository.findLocationStats(Job.JobStatus.ACTIVE, LocalDate.now());

        // 전체 공고 수 합산
        long total = stats.stream().mapToLong(LocationStatResponse::getCount).sum();

        // 각 지역의 비중(%) 계산 후 주입
        if (total > 0) {
            stats.forEach(s -> s.setPercentage(Math.round((double) s.getCount() / total * 100.0)));
        }

        return stats;
    }

    /**
     * 오늘의 현황 (전체/신규/마감임박/신입 공고 수)
     *
     * @Cacheable: TTL 1분 (신규 공고 반영을 위해 짧게 설정)
     */
    @Cacheable(value = CacheConfig.CACHE_TODAY, key = "'all'")
    public TodayStatResponse getTodayStats() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();           // 오늘 00:00:00
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay(); // 내일 00:00:00

        long totalCount  = statsRepository.countByStatus(Job.JobStatus.ACTIVE, today);
        long todayCount  = statsRepository.countToday(Job.JobStatus.ACTIVE, startOfDay, endOfDay, today);
        long urgentCount = statsRepository.countUrgent(Job.JobStatus.ACTIVE, today, today.plusDays(7));
        long juniorCount = statsRepository.countJunior(Job.JobStatus.ACTIVE, today);

        return new TodayStatResponse(totalCount, todayCount, urgentCount, juniorCount);
    }

    /**
     * 경력별 공고 수 + 비중(%)
     *
     * @Cacheable: TTL 10분
     */
    @Cacheable(value = CacheConfig.CACHE_EXPERIENCE, key = "'all'")
    public List<ExperienceStatResponse> getExperienceStats() {
        List<ExperienceStatResponse> stats = statsRepository.findExperienceStats(Job.JobStatus.ACTIVE, LocalDate.now());

        long total = stats.stream().mapToLong(ExperienceStatResponse::getCount).sum();

        if (total > 0) {
            stats.forEach(s -> s.setPercentage(Math.round((double) s.getCount() / total * 100.0)));
        }

        return stats;
    }
}
