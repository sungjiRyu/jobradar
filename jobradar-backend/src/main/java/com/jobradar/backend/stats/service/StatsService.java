package com.jobradar.backend.stats.service;

import com.jobradar.backend.global.cache.DistributedCacheLoader;
import com.jobradar.backend.global.config.CacheConfig;
import com.jobradar.backend.job.entity.Job;
import com.jobradar.backend.stats.dto.ExperienceStatResponse;
import com.jobradar.backend.stats.dto.LocationStatResponse;
import com.jobradar.backend.stats.dto.TechStackStatResponse;
import com.jobradar.backend.stats.dto.TodayStatResponse;
import com.jobradar.backend.stats.repository.StatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 대시보드 통계 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 통계는 조회 전용이므로 readOnly로 성능 최적화
public class StatsService {

    private static final String CACHE_KEY_ALL = "all";

    private final StatsRepository statsRepository;
    private final DistributedCacheLoader cacheLoader;

    /**
     * 기술스택별 공고 수 (상위 8개)
     *
     * Redisson 분산락 기반 cache-aside 로딩
     */
    public List<TechStackStatResponse> getTechStackStats() {
        return cacheLoader.getOrLoad(CacheConfig.CACHE_TECH_STACKS, CACHE_KEY_ALL, this::loadTechStackStats);
    }

    private List<TechStackStatResponse> loadTechStackStats() {
        // PageRequest.of(0, 8): 첫 번째 페이지, 8개 제한 → SQL LIMIT 8 역할
        return statsRepository.findTechStackStats(Job.JobStatus.ACTIVE, LocalDate.now(), PageRequest.of(0, 8));
    }

    /**
     * 지역별 공고 수 + 비중(%)
     *
     * Redisson 분산락 기반 cache-aside 로딩
     * percentage는 전체 합산이 필요하므로 서비스에서 계산
     */
    public List<LocationStatResponse> getLocationStats() {
        return cacheLoader.getOrLoad(CacheConfig.CACHE_LOCATIONS, CACHE_KEY_ALL, this::loadLocationStats);
    }

    private List<LocationStatResponse> loadLocationStats() {
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
     * Redisson 분산락 기반 cache-aside 로딩
     */
    public TodayStatResponse getTodayStats() {
        return cacheLoader.getOrLoad(CacheConfig.CACHE_TODAY, CACHE_KEY_ALL, this::loadTodayStats);
    }

    private TodayStatResponse loadTodayStats() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();           // 오늘 00:00:00
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay(); // 내일 00:00:00
        log.info("[StatsToday] query range: today={}, startOfDay={}, endOfDay={}", today, startOfDay, endOfDay);

        long totalCount  = statsRepository.countByStatus(Job.JobStatus.ACTIVE, today);
        long todayCount  = statsRepository.countToday(Job.JobStatus.ACTIVE, startOfDay, endOfDay, today);
        long urgentCount = statsRepository.countUrgent(Job.JobStatus.ACTIVE, today, today.plusDays(7));
        long juniorCount = statsRepository.countJunior(Job.JobStatus.ACTIVE, today);
        log.info("[StatsToday] query result: totalCount={}, todayCount={}, urgentCount={}, juniorCount={}",
                totalCount, todayCount, urgentCount, juniorCount);

        return new TodayStatResponse(totalCount, todayCount, urgentCount, juniorCount);
    }

    /**
     * 경력별 공고 수 + 비중(%)
     *
     * Redisson 분산락 기반 cache-aside 로딩
     */
    public List<ExperienceStatResponse> getExperienceStats() {
        return cacheLoader.getOrLoad(CacheConfig.CACHE_EXPERIENCE, CACHE_KEY_ALL, this::loadExperienceStats);
    }

    private List<ExperienceStatResponse> loadExperienceStats() {
        List<ExperienceStatResponse> stats = statsRepository.findExperienceStats(Job.JobStatus.ACTIVE, LocalDate.now());

        long total = stats.stream().mapToLong(ExperienceStatResponse::getCount).sum();

        if (total > 0) {
            stats.forEach(s -> s.setPercentage(Math.round((double) s.getCount() / total * 100.0)));
        }

        return stats;
    }
}
