package com.jobradar.backend.stats.service;

import com.jobradar.backend.global.cache.DistributedCacheLoader;
import com.jobradar.backend.global.config.CacheConfig;
import com.jobradar.backend.global.lock.RedisLockExecutor;
import com.jobradar.backend.global.time.BusinessTimeProvider;
import com.jobradar.backend.job.entity.Job;
import com.jobradar.backend.job.entity.TechStack;
import com.jobradar.backend.stats.dto.ExperienceStatResponse;
import com.jobradar.backend.stats.dto.LocationStatResponse;
import com.jobradar.backend.stats.dto.TechStackStatResponse;
import com.jobradar.backend.stats.dto.TodayStatResponse;
import com.jobradar.backend.stats.dto.TrendingJobRankRow;
import com.jobradar.backend.stats.dto.TrendingJobResponse;
import com.jobradar.backend.stats.repository.StatsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * StatsService 단위 테스트
 *
 * [@ExtendWith(MockitoExtension.class)]
 * - Spring 컨텍스트 없이 Mockito만으로 테스트 → 빠름
 * - @Mock: StatsRepository를 가짜 객체로 대체 → DB 연결 없이 테스트 가능
 * - @InjectMocks: StatsService에 @Mock 객체를 자동 주입
 *
 * [단위 테스트 vs 통합 테스트]
 * - 단위 테스트: 서비스 로직만 검증 (DB, Redis 연결 없음) → 빠름
 * - 통합 테스트: 실제 DB/Redis까지 포함 → 느리지만 실제 동작 검증
 * - 여기서는 비즈니스 로직(percentage 계산 등) 검증이 목적이므로 단위 테스트로 충분
 */
@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    private static final BusinessTimeProvider FIXED_TIME_PROVIDER = new BusinessTimeProvider(
            Clock.fixed(Instant.parse("2026-06-26T18:00:00Z"), ZoneOffset.UTC)
    );

    @Mock
    private StatsRepository statsRepository;

    private StatsService statsService;

    @BeforeEach
    void setUp() {
        DistributedCacheLoader cacheLoader = new DistributedCacheLoader(
                new ConcurrentMapCacheManager(
                        CacheConfig.CACHE_TECH_STACKS,
                        CacheConfig.CACHE_LOCATIONS,
                        CacheConfig.CACHE_TODAY,
                        CacheConfig.CACHE_EXPERIENCE
                ),
                new SynchronizedRedisLockExecutor()
        );
        statsService = new StatsService(statsRepository, cacheLoader, FIXED_TIME_PROVIDER);
    }

    private static class SynchronizedRedisLockExecutor extends RedisLockExecutor {

        SynchronizedRedisLockExecutor() {
            super(null);
        }

        @Override
        public synchronized <T> T executeWithLock(String key, Supplier<T> task) {
            return task.get();
        }
    }

    // ===== 기술스택 통계 테스트 =====

    @Test
    @DisplayName("기술스택 통계 정상 조회 - 상위 8개 반환")
    void getTechStackStats_정상조회() {
        // given: StatsRepository가 반환할 가짜 데이터 설정
        List<TechStackStatResponse> mockData = List.of(
                new TechStackStatResponse("Java", 924L),
                new TechStackStatResponse("Spring", 871L)
        );
        given(statsRepository.findTechStackStats(eq(Job.JobStatus.ACTIVE), any(LocalDate.class), any(PageRequest.class)))
                .willReturn(mockData);

        // when: 실제 서비스 메서드 호출
        List<TechStackStatResponse> result = statsService.getTechStackStats();

        // then: 결과 검증
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Java");
        assertThat(result.get(0).getCount()).isEqualTo(924L);
    }

    @Test
    @DisplayName("기술스택 통계 - 캐시 적중 시 Repository를 다시 호출하지 않음")
    void getTechStackStats_캐시적중_repository재호출없음() {
        // given
        List<TechStackStatResponse> mockData = List.of(
                new TechStackStatResponse("Java", 924L)
        );
        given(statsRepository.findTechStackStats(eq(Job.JobStatus.ACTIVE), any(LocalDate.class), any(PageRequest.class)))
                .willReturn(mockData);

        // when
        List<TechStackStatResponse> first = statsService.getTechStackStats();
        List<TechStackStatResponse> second = statsService.getTechStackStats();

        // then
        assertThat(first).hasSize(1);
        assertThat(second).hasSize(1);
        verify(statsRepository, times(1))
                .findTechStackStats(eq(Job.JobStatus.ACTIVE), any(LocalDate.class), any(PageRequest.class));
    }

    @Test
    @DisplayName("기술스택 통계 - 공고 없을 때 빈 리스트 반환")
    void getTechStackStats_빈데이터() {
        // given
        given(statsRepository.findTechStackStats(eq(Job.JobStatus.ACTIVE), any(LocalDate.class), any(PageRequest.class)))
                .willReturn(Collections.emptyList());

        // when
        List<TechStackStatResponse> result = statsService.getTechStackStats();

        // then: 빈 리스트여도 NPE 없이 정상 반환되는지 확인
        assertThat(result).isEmpty();
    }

    // ===== 지역별 통계 테스트 =====

    @Test
    @DisplayName("지역별 통계 - percentage가 올바르게 계산되는지 검증")
    void getLocationStats_percentage계산() {
        // given: 서울 600개, 경기 400개 → 전체 1000개
        List<LocationStatResponse> mockData = List.of(
                new LocationStatResponse("서울", 600L),
                new LocationStatResponse("경기", 400L)
        );
        given(statsRepository.findLocationStats(eq(Job.JobStatus.ACTIVE), any(LocalDate.class))).willReturn(mockData);

        // when
        List<LocationStatResponse> result = statsService.getLocationStats();

        // then: 서울 60%, 경기 40%
        assertThat(result.get(0).getPercentage()).isEqualTo(60.0);
        assertThat(result.get(1).getPercentage()).isEqualTo(40.0);
    }

    @Test
    @DisplayName("지역별 통계 - 공고 없을 때 빈 리스트 반환")
    void getLocationStats_빈데이터() {
        // given
        given(statsRepository.findLocationStats(eq(Job.JobStatus.ACTIVE), any(LocalDate.class))).willReturn(Collections.emptyList());

        // when
        List<LocationStatResponse> result = statsService.getLocationStats();

        // then: total=0 상황에서 percentage 계산 시 NPE 또는 0 나누기 오류가 없는지 확인
        assertThat(result).isEmpty();
    }

    // ===== 오늘의 현황 테스트 =====

    @Test
    @DisplayName("오늘의 현황 - 각 카운트가 올바르게 집계되는지 검증")
    void getTodayStats_정상조회() {
        // given
        given(statsRepository.countByStatus(eq(Job.JobStatus.ACTIVE), any(LocalDate.class))).willReturn(1284L);
        given(statsRepository.countToday(eq(Job.JobStatus.ACTIVE), any(LocalDate.class))).willReturn(47L);
        given(statsRepository.countUrgent(eq(Job.JobStatus.ACTIVE), any(LocalDate.class), any(LocalDate.class))).willReturn(12L);
        given(statsRepository.countJunior(eq(Job.JobStatus.ACTIVE), any(LocalDate.class))).willReturn(312L);

        // when
        TodayStatResponse result = statsService.getTodayStats();

        // then
        assertThat(result.getTotalCount()).isEqualTo(1284L);
        assertThat(result.getTodayCount()).isEqualTo(47L);
        assertThat(result.getUrgentCount()).isEqualTo(12L);
        assertThat(result.getJuniorCount()).isEqualTo(312L);
        verify(statsRepository).countToday(
                eq(Job.JobStatus.ACTIVE),
                eq(LocalDate.of(2026, 6, 27))
        );
    }

    // ===== 경력별 통계 테스트 =====

    @Test
    @DisplayName("경력별 통계 - percentage가 올바르게 계산되는지 검증")
    void getExperienceStats_percentage계산() {
        // given: 신입 300, 경력 1~3년 500 → 전체 800
        List<ExperienceStatResponse> mockData = List.of(
                new ExperienceStatResponse("신입", 300L),
                new ExperienceStatResponse("경력 1~3년", 500L)
        );
        given(statsRepository.findExperienceStats(eq(Job.JobStatus.ACTIVE), any(LocalDate.class))).willReturn(mockData);

        // when
        List<ExperienceStatResponse> result = statsService.getExperienceStats();

        // then: 신입 38%, 경력 1~3년 63% (Math.round 반올림)
        assertThat(result.get(0).getPercentage()).isEqualTo(38.0);
        assertThat(result.get(1).getPercentage()).isEqualTo(63.0);
    }

    @Test
    @DisplayName("경력별 통계 - 공고 없을 때 빈 리스트 반환")
    void getExperienceStats_빈데이터() {
        // given
        given(statsRepository.findExperienceStats(eq(Job.JobStatus.ACTIVE), any(LocalDate.class))).willReturn(Collections.emptyList());

        // when
        List<ExperienceStatResponse> result = statsService.getExperienceStats();

        // then
        assertThat(result).isEmpty();
    }

    // ===== 인기 공고 랭킹 테스트 =====

    @Test
    @DisplayName("인기 공고 랭킹 - Repository 순서대로 rank와 스크랩 수를 조립")
    void getTrendingJobs_정상조회() {
        // given
        List<TrendingJobRankRow> rankRows = List.of(
                new TrendingJobRankRow(2L, 20L),
                new TrendingJobRankRow(1L, 10L)
        );
        given(statsRepository.findTrendingJobRankRows(
                eq(Job.JobStatus.ACTIVE),
                eq(LocalDate.of(2026, 6, 27)),
                eq(PageRequest.of(0, 10))
        )).willReturn(rankRows);
        given(statsRepository.findJobsWithTechStacksByIds(List.of(2L, 1L)))
                .willReturn(List.of(
                        trendingJob(1L, "A회사", "백엔드 개발자", 100, "Java"),
                        trendingJob(2L, "B회사", "프론트엔드 개발자", 200, "React")
                ));

        // when
        List<TrendingJobResponse> result = statsService.getTrendingJobs();

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(2L);
        assertThat(result.get(0).getRank()).isEqualTo(1);
        assertThat(result.get(0).getScrapCount()).isEqualTo(20L);
        assertThat(result.get(0).getTechStacks()).containsExactly("React");
        assertThat(result.get(1).getId()).isEqualTo(1L);
        assertThat(result.get(1).getRank()).isEqualTo(2);
    }

    @Test
    @DisplayName("인기 공고 랭킹 - 공고 없을 때 빈 리스트 반환")
    void getTrendingJobs_빈데이터() {
        // given
        given(statsRepository.findTrendingJobRankRows(
                eq(Job.JobStatus.ACTIVE),
                any(LocalDate.class),
                eq(PageRequest.of(0, 10))
        )).willReturn(Collections.emptyList());

        // when
        List<TrendingJobResponse> result = statsService.getTrendingJobs();

        // then
        assertThat(result).isEmpty();
        verify(statsRepository, never()).findJobsWithTechStacksByIds(anyList());
    }

    private Job trendingJob(Long id, String company, String title, int viewCount, String techStack) {
        Job job = Job.builder()
                .company(company)
                .title(title)
                .location("서울")
                .experienceLevel("경력")
                .sourceUrl("https://example.com/jobs/" + id)
                .sourceSite("사람인")
                .deadline(LocalDate.of(2026, 7, 31))
                .build();
        ReflectionTestUtils.setField(job, "id", id);
        ReflectionTestUtils.setField(job, "viewCount", viewCount);
        job.getTechStacks().add(TechStack.builder().name(techStack).build());
        return job;
    }
}
