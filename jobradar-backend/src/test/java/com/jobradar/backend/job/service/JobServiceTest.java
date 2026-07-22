package com.jobradar.backend.job.service;

import com.jobradar.backend.crawler.service.source.JobkoreaCrawlerService;
import com.jobradar.backend.crawler.service.source.SaraminCrawlerService;
import com.jobradar.backend.global.ai.AiSummaryResult;
import com.jobradar.backend.global.ai.AiSummaryService;
import com.jobradar.backend.global.lock.LockAcquisitionException;
import com.jobradar.backend.global.lock.RedisLockExecutor;
import com.jobradar.backend.global.time.BusinessTimeProvider;
import com.jobradar.backend.job.dto.DescriptionResponse;
import com.jobradar.backend.job.dto.SummaryResponse;
import com.jobradar.backend.job.entity.Job;
import com.jobradar.backend.job.repository.JobRepository;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * JobService 단위 테스트
 *
 * [테스트 대상: getDescription()]
 * - descriptionStatus 값에 따라 즉시 반환 vs 크롤러 호출로 분기되는 핵심 로직
 * - 이 분기가 잘못되면 불필요한 크롤링이 발생하거나 (IP 차단 위험),
 *   이미 수집된 내용을 다시 fetch하는 낭비가 생김
 *
 * [AiSummaryService @Mock 필요 이유]
 * - JobService 생성자에 AiSummaryService가 포함돼 있어 @InjectMocks가 주입을 시도함
 * - getDescription()에서는 실제로 사용하지 않지만 Mock 선언 없으면 주입 실패
 */
@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    private static final BusinessTimeProvider FIXED_TIME_PROVIDER = new BusinessTimeProvider(
            Clock.fixed(Instant.parse("2026-06-26T18:00:00Z"), ZoneOffset.UTC)
    );

    @Mock
    private JobRepository jobRepository;

    @Mock
    private SaraminCrawlerService saraminCrawlerService;

    @Mock
    private JobkoreaCrawlerService jobkoreaCrawlerService;

    @Mock
    private AiSummaryService aiSummaryService;

    private JobService jobService;

    @BeforeEach
    void setUp() {
        jobService = new JobService(
                jobRepository,
                saraminCrawlerService,
                jobkoreaCrawlerService,
                aiSummaryService,
                new SynchronizedRedisLockExecutor(),
                FIXED_TIME_PROVIDER
        );
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

    private static class ThrowingRedisLockExecutor extends RedisLockExecutor {

        ThrowingRedisLockExecutor() {
            super(null);
        }

        @Override
        public <T> T executeWithLock(String key, Supplier<T> task) {
            throw new LockAcquisitionException("lock busy");
        }
    }

    // ===== getDescription() 분기 테스트 =====

    @Test
    @DisplayName("공고 검색 - 오늘 등록/마감임박 필터는 KST 기준 경계값으로 Specification을 생성")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void search_todayOnlyAndUrgent_비즈니스날짜경계조건() {
        given(jobRepository.findAll(any(Specification.class), any(PageRequest.class))).willReturn(Page.empty());

        jobService.search(null, null, null, null, null, null, true, true, PageRequest.of(0, 10));

        ArgumentCaptor<Specification<Job>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(jobRepository).findAll(captor.capture(), any(PageRequest.class));

        Root<Job> root = (Root<Job>) org.mockito.Mockito.mock(Root.class);
        CriteriaQuery<?> query = org.mockito.Mockito.mock(CriteriaQuery.class);
        CriteriaBuilder cb = org.mockito.Mockito.mock(CriteriaBuilder.class);
        Path statusPath = org.mockito.Mockito.mock(Path.class);
        Path deadlinePath = org.mockito.Mockito.mock(Path.class);
        Path createdAtPath = org.mockito.Mockito.mock(Path.class);
        Expression<LocalDate> createdDateExpression = org.mockito.Mockito.mock(Expression.class);
        Predicate predicate = org.mockito.Mockito.mock(Predicate.class);

        given(root.get("status")).willReturn(statusPath);
        given(root.get("deadline")).willReturn(deadlinePath);
        given(root.get("createdAt")).willReturn(createdAtPath);
        given(cb.function("DATE", LocalDate.class, createdAtPath)).willReturn(createdDateExpression);
        org.mockito.Mockito.doReturn(predicate).when(cb).equal(statusPath, Job.JobStatus.ACTIVE);
        org.mockito.Mockito.doReturn(predicate).when(cb).equal(createdDateExpression, LocalDate.of(2026, 6, 27));
        org.mockito.Mockito.doReturn(predicate).when(cb).isNull(deadlinePath);
        org.mockito.Mockito.doReturn(predicate).when(cb).greaterThanOrEqualTo(deadlinePath, LocalDate.of(2026, 6, 27));
        org.mockito.Mockito.doReturn(predicate).when(cb)
                .between(deadlinePath, LocalDate.of(2026, 6, 27), LocalDate.of(2026, 7, 4));
        org.mockito.Mockito.doReturn(predicate).when(cb).or(any(Predicate.class), any(Predicate.class));
        org.mockito.Mockito.doReturn(predicate).when(cb).and(any(Predicate.class), any(Predicate.class));

        captor.getValue().toPredicate(root, query, cb);

        verify(cb).function("DATE", LocalDate.class, createdAtPath);
        verify(cb).equal(createdDateExpression, LocalDate.of(2026, 6, 27));
        verify(cb).between(deadlinePath, LocalDate.of(2026, 6, 27), LocalDate.of(2026, 7, 4));
    }

    @Test
    @DisplayName("상세 내용 조회 - descriptionStatus = SUCCESS → 크롤러 호출 없이 즉시 반환")
    void getDescription_SUCCESS_즉시반환() {
        // given: 이미 수집된 공고 (descriptionStatus = SUCCESS)
        Job job = Job.builder()
                .company("테스트회사")
                .title("Java 백엔드 개발자")
                .location("서울")
                .sourceUrl("https://www.saramin.co.kr/test")
                .sourceSite("사람인")
                .description("상세 내용입니다.")
                .descriptionStatus(Job.DescriptionStatus.SUCCESS)
                .build();

        given(jobRepository.findById(1L)).willReturn(Optional.of(job));

        // when
        DescriptionResponse response = jobService.getDescription(1L);

        // then: 크롤러 호출 없이 캐시된 내용 반환
        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getDescription()).isEqualTo("상세 내용입니다.");
        verify(saraminCrawlerService, never()).fetchDescription(job.getSourceUrl());
    }

    @Test
    @DisplayName("상세 내용 조회 - 마감일이 오늘이면 아직 마감으로 보지 않음")
    void getDescription_마감일오늘_크롤링진행() {
        Job job = Job.builder()
                .company("테스트회사")
                .title("오늘 마감 공고")
                .location("서울")
                .sourceUrl("https://www.saramin.co.kr/today-deadline")
                .sourceSite("사람인")
                .deadline(LocalDate.of(2026, 6, 27))
                .build();

        given(jobRepository.findById(1L)).willReturn(Optional.of(job));
        given(saraminCrawlerService.fetchDescription(job.getSourceUrl()))
                .willReturn(DescriptionResponse.success("상세 내용"));

        DescriptionResponse response = jobService.getDescription(1L);

        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        verify(saraminCrawlerService).fetchDescription(job.getSourceUrl());
    }

    @Test
    @DisplayName("상세 내용 조회 - 마감일이 어제면 CLOSED 응답")
    void getDescription_마감일어제_CLOSED반환() {
        Job job = Job.builder()
                .company("테스트회사")
                .title("어제 마감 공고")
                .location("서울")
                .sourceUrl("https://www.saramin.co.kr/yesterday-deadline")
                .sourceSite("사람인")
                .deadline(LocalDate.of(2026, 6, 26))
                .build();

        given(jobRepository.findById(1L)).willReturn(Optional.of(job));

        DescriptionResponse response = jobService.getDescription(1L);

        assertThat(response.getStatus()).isEqualTo("CLOSED");
        verify(saraminCrawlerService, never()).fetchDescription(any());
    }

    @Test
    @DisplayName("상세 내용 조회 - descriptionStatus = IMAGE → 크롤러 호출 없이 이미지 응답 반환")
    void getDescription_IMAGE_즉시반환() {
        // given: 이미지 공고로 텍스트 수집 불가 상태
        Job job = Job.builder()
                .company("테스트회사")
                .title("프론트엔드 개발자")
                .location("서울")
                .sourceUrl("https://www.saramin.co.kr/test2")
                .sourceSite("사람인")
                .descriptionStatus(Job.DescriptionStatus.IMAGE)
                .build();

        given(jobRepository.findById(2L)).willReturn(Optional.of(job));

        // when
        DescriptionResponse response = jobService.getDescription(2L);

        // then
        assertThat(response.getStatus()).isEqualTo("IMAGE");
        assertThat(response.getDescription()).isNull();
        verify(saraminCrawlerService, never()).fetchDescription(job.getSourceUrl());
    }

    @Test
    @DisplayName("상세 내용 조회 - descriptionStatus = null → 크롤러 호출 후 결과 반환")
    void getDescription_NULL_크롤러호출() {
        // given: 아직 한 번도 fetch하지 않은 공고 (descriptionStatus = null)
        Job job = Job.builder()
                .company("테스트회사")
                .title("DevOps 엔지니어")
                .location("판교")
                .sourceUrl("https://www.saramin.co.kr/test3")
                .sourceSite("사람인")
                // descriptionStatus 미설정 → null (fetch 대상)
                .build();

        given(jobRepository.findById(3L)).willReturn(Optional.of(job));
        given(saraminCrawlerService.fetchDescription("https://www.saramin.co.kr/test3"))
                .willReturn(DescriptionResponse.success("크롤링된 상세 내용"));

        // when
        DescriptionResponse response = jobService.getDescription(3L);

        // then: 크롤러가 호출되고 결과 반환
        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getDescription()).isEqualTo("크롤링된 상세 내용");
        verify(saraminCrawlerService).fetchDescription("https://www.saramin.co.kr/test3");
    }

    // ===== Redisson 분산락 동시성 테스트 =====

    @Test
    @DisplayName("동시 요청 - Redisson Lock 경계로 크롤러 1번만 호출됨")
    void getDescription_동시요청_크롤러1번만호출() throws InterruptedException {
        // given: 미수집 공고 (descriptionStatus = null)
        Job job = Job.builder()
                .company("테스트회사")
                .title("동시성 테스트용 공고")
                .location("서울")
                .sourceUrl("https://www.saramin.co.kr/concurrent")
                .sourceSite("사람인")
                .build();

        int threadCount = 5;

        // readyLatch: 5개 스레드가 모두 준비될 때까지 메인 스레드가 대기
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        // startLatch: 카운트다운 1번으로 5개 스레드 동시 출발 신호
        CountDownLatch startLatch = new CountDownLatch(1);
        // doneLatch: 5개 스레드가 모두 완료될 때까지 메인 스레드가 대기
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        given(jobRepository.findById(99L)).willReturn(Optional.of(job));

        // 크롤러가 50ms 걸리는 척 → 이 시간 동안 나머지 스레드들이 락 대기 상태로 진입
        // 이 sleep 없으면 Thread 1이 너무 빨리 끝나서 2~5가 lock 경쟁 없이 step 1에서 이미 SUCCESS를 봄
        given(saraminCrawlerService.fetchDescription(any())).willAnswer(invocation -> {
            Thread.sleep(50);
            return DescriptionResponse.success("크롤링 결과");
        });

        // when: 5개 스레드 동시 출발
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    readyLatch.countDown();  // 준비 완료 신호
                    startLatch.await();      // 출발 신호 대기
                    jobService.getDescription(99L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();   // 완료 신호
                }
            }).start();
        }

        readyLatch.await();          // 5개 스레드 모두 준비될 때까지 대기
        startLatch.countDown();      // 동시 출발
        doneLatch.await(5, TimeUnit.SECONDS);  // 최대 5초 내 완료 대기

        // then: 크롤러는 딱 1번만 호출됨
        // 락이 없었다면 5개 스레드 모두 status=null을 읽고 각자 크롤링 → times(5) 실패
        verify(saraminCrawlerService, times(1)).fetchDescription(any());
    }

    @Test
    @DisplayName("description 락 획득 실패 - 아직 처리 중이면 CRAWL_FAILED 대신 IN_PROGRESS 반환")
    void getDescription_락획득실패_진행중반환() {
        jobService = new JobService(
                jobRepository,
                saraminCrawlerService,
                jobkoreaCrawlerService,
                aiSummaryService,
                new ThrowingRedisLockExecutor(),
                FIXED_TIME_PROVIDER
        );

        Job job = Job.builder()
                .company("테스트회사")
                .title("락 경합 테스트")
                .location("서울")
                .sourceUrl("https://www.saramin.co.kr/lock-busy")
                .sourceSite("사람인")
                .build();

        given(jobRepository.findById(101L)).willReturn(Optional.of(job));

        DescriptionResponse response = jobService.getDescription(101L);

        assertThat(response.getStatus()).isEqualTo("IN_PROGRESS");
        verify(saraminCrawlerService, never()).fetchDescription(any());
    }

    @Test
    @DisplayName("description 락 획득 실패 - 재조회 상태가 IMAGE면 IMAGE 반환")
    void getDescription_락획득실패_IMAGE반환() {
        jobService = new JobService(
                jobRepository,
                saraminCrawlerService,
                jobkoreaCrawlerService,
                aiSummaryService,
                new ThrowingRedisLockExecutor(),
                FIXED_TIME_PROVIDER
        );

        Job job = Job.builder()
                .company("테스트회사")
                .title("이미지 공고")
                .location("서울")
                .sourceUrl("https://www.saramin.co.kr/image")
                .sourceSite("사람인")
                .build();
        Job initiallyLoadedJob = Job.builder()
                .company("테스트회사")
                .title("이미지 공고")
                .location("서울")
                .sourceUrl("https://www.saramin.co.kr/image")
                .sourceSite("사람인")
                .build();
        job.updateDescription(null, Job.DescriptionStatus.IMAGE);

        given(jobRepository.findById(102L))
                .willReturn(Optional.of(initiallyLoadedJob))
                .willReturn(Optional.of(job));

        DescriptionResponse response = jobService.getDescription(102L);

        assertThat(response.getStatus()).isEqualTo("IMAGE");
        verify(saraminCrawlerService, never()).fetchDescription(any());
    }

    @Test
    @DisplayName("동시 요청 - Redisson Lock 경계로 AI 요약 1번만 호출됨")
    void getSummary_동시요청_AI요약1번만호출() throws InterruptedException {
        // given: description은 이미 수집됐지만 summary는 아직 없는 공고
        Job job = Job.builder()
                .company("테스트회사")
                .title("AI 요약 동시성 테스트용 공고")
                .location("서울")
                .sourceUrl("https://www.saramin.co.kr/summary-concurrent")
                .sourceSite("사람인")
                .description("AI 요약 테스트를 위해 충분히 긴 상세 내용입니다. 주요업무와 자격요건이 포함된 텍스트라고 가정합니다.")
                .descriptionStatus(Job.DescriptionStatus.SUCCESS)
                .build();

        int threadCount = 5;

        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        given(jobRepository.findById(100L)).willReturn(Optional.of(job));
        given(aiSummaryService.summarize(any())).willAnswer(invocation -> {
            Thread.sleep(50);
            return AiSummaryResult.success("{\"header\":{\"summary\":\"요약 결과\"}}");
        });

        // when: 5개 스레드 동시 출발
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();
                    SummaryResponse response = jobService.getSummary(100L);
                    assertThat(response.getSummary()).isNotNull();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        readyLatch.await();
        startLatch.countDown();
        doneLatch.await(5, TimeUnit.SECONDS);

        // then: 같은 공고 요약 요청이 겹쳐도 AI 호출은 1번만 수행됨
        verify(aiSummaryService, times(1)).summarize(any());
    }

    @Test
    @DisplayName("summary 락 획득 실패 - 아직 처리 중이면 aiFailed 대신 inProgress 반환")
    void getSummary_락획득실패_진행중반환() {
        jobService = new JobService(
                jobRepository,
                saraminCrawlerService,
                jobkoreaCrawlerService,
                aiSummaryService,
                new ThrowingRedisLockExecutor(),
                FIXED_TIME_PROVIDER
        );

        Job job = Job.builder()
                .company("테스트회사")
                .title("요약 락 경합 테스트")
                .location("서울")
                .sourceUrl("https://www.saramin.co.kr/summary-lock-busy")
                .sourceSite("사람인")
                .description("요약 대상 상세 내용")
                .descriptionStatus(Job.DescriptionStatus.SUCCESS)
                .build();

        given(jobRepository.findById(103L)).willReturn(Optional.of(job));

        SummaryResponse response = jobService.getSummary(103L);

        assertThat(response.isInProgress()).isTrue();
        assertThat(response.getSummary()).isNull();
        verify(aiSummaryService, never()).summarize(any());
    }

    @Test
    @DisplayName("summary 락 획득 실패 - 재조회 상태가 IMAGE면 imageOnly 반환")
    void getSummary_락획득실패_IMAGE반환() {
        jobService = new JobService(
                jobRepository,
                saraminCrawlerService,
                jobkoreaCrawlerService,
                aiSummaryService,
                new ThrowingRedisLockExecutor(),
                FIXED_TIME_PROVIDER
        );

        Job initiallyLoadedJob = Job.builder()
                .company("테스트회사")
                .title("이미지 요약")
                .location("서울")
                .sourceUrl("https://www.saramin.co.kr/summary-image")
                .sourceSite("사람인")
                .build();
        Job imageJob = Job.builder()
                .company("테스트회사")
                .title("이미지 요약")
                .location("서울")
                .sourceUrl("https://www.saramin.co.kr/summary-image")
                .sourceSite("사람인")
                .descriptionStatus(Job.DescriptionStatus.IMAGE)
                .build();

        given(jobRepository.findById(104L))
                .willReturn(Optional.of(initiallyLoadedJob))
                .willReturn(Optional.of(imageJob));

        SummaryResponse response = jobService.getSummary(104L);

        assertThat(response.isImageOnly()).isTrue();
        assertThat(response.isInProgress()).isFalse();
        verify(aiSummaryService, never()).summarize(any());
    }
}
