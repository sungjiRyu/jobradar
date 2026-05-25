package com.jobradar.backend.job.service;

import com.jobradar.backend.crawler.JobkoreaCrawlerService;
import com.jobradar.backend.crawler.SaraminCrawlerService;
import com.jobradar.backend.global.config.AiSummaryService;
import com.jobradar.backend.job.dto.DescriptionResponse;
import com.jobradar.backend.job.entity.Job;
import com.jobradar.backend.job.repository.JobRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

    @Mock
    private JobRepository jobRepository;

    @Mock
    private SaraminCrawlerService saraminCrawlerService;

    @Mock
    private JobkoreaCrawlerService jobkoreaCrawlerService;

    @Mock
    private AiSummaryService aiSummaryService;

    @InjectMocks
    private JobService jobService;

    // ===== getDescription() 분기 테스트 =====

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

    // ===== Striped Locking 동시성 테스트 =====

    @Test
    @DisplayName("동시 요청 - Striped Lock으로 크롤러 1번만 호출됨")
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
}
