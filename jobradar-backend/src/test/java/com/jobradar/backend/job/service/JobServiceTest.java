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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
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
}
