package com.jobradar.backend.crawler.service;

import com.jobradar.backend.crawler.dto.CrawledJobDto;
import com.jobradar.backend.job.entity.Job;
import com.jobradar.backend.job.entity.TechStack;
import com.jobradar.backend.job.repository.JobRepository;
import com.jobradar.backend.job.repository.TechStackRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CrawledJobSaveServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private TechStackRepository techStackRepository;

    @InjectMocks
    private CrawledJobSaveService crawledJobSaveService;

    @Test
    @DisplayName("중복 공고 스킵 - 이미 DB에 있는 URL이면 save() 호출 안 됨")
    void save_중복URL_스킵() {
        given(jobRepository.existsBySourceUrl(anyString())).willReturn(true);

        boolean saved = crawledJobSaveService.save(defaultDto("백엔드 개발자", "~05/10(일)", false));

        assertThat(saved).isFalse();
        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    @DisplayName("신규 공고 저장 - DB에 없는 URL이면 Job 저장")
    void save_신규공고_저장() {
        given(jobRepository.existsBySourceUrl(anyString())).willReturn(false);

        boolean saved = crawledJobSaveService.save(defaultDto("백엔드 개발자", "~05/10(일)", false));

        assertThat(saved).isTrue();
        Job job = captureSavedJob();
        assertThat(job.getTitle()).isEqualTo("백엔드 개발자");
        assertThat(job.getDeadline()).isNotNull();
        assertThat(job.getDeadline().getMonthValue()).isEqualTo(5);
        assertThat(job.getDeadline().getDayOfMonth()).isEqualTo(10);
        assertThat(job.getDeadlineType()).isEqualTo(Job.DeadlineType.FIXED);
        assertThat(job.getDescriptionStatus()).isNull();
    }

    @Test
    @DisplayName("기술스택 파싱 - 제목에서 Java, Spring 추출")
    void save_기술스택_키워드파싱() {
        given(jobRepository.existsBySourceUrl(anyString())).willReturn(false);
        given(techStackRepository.findByName("Java")).willReturn(Optional.empty());
        given(techStackRepository.findByName("Spring")).willReturn(Optional.empty());
        given(techStackRepository.save(any(TechStack.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        crawledJobSaveService.save(defaultDto("Java Spring Boot 백엔드 개발자", "~05/10(일)", false));

        Job job = captureSavedJob();
        assertThat(job.getTechStacks()).extracting(TechStack::getName)
                .containsExactlyInAnyOrder("Java", "Spring");
    }

    @Test
    @DisplayName("기술스택 파싱 - 키워드 없는 공고는 빈 리스트")
    void save_기술스택_키워드없음() {
        given(jobRepository.existsBySourceUrl(anyString())).willReturn(false);

        crawledJobSaveService.save(defaultDto("총무팀 경력직 채용", "~05/10(일)", false));

        Job job = captureSavedJob();
        assertThat(job.getTechStacks()).isEmpty();
    }

    @Test
    @DisplayName("마감일 파싱 - 채용시는 상시채용")
    void save_마감일_채용시() {
        given(jobRepository.existsBySourceUrl(anyString())).willReturn(false);

        crawledJobSaveService.save(defaultDto("백엔드 개발자", "채용시", false));

        Job job = captureSavedJob();
        assertThat(job.getDeadline()).isNull();
        assertThat(job.getDeadlineType()).isEqualTo(Job.DeadlineType.ALWAYS);
    }

    @Test
    @DisplayName("마감일 파싱 - 빈 문자열은 UNKNOWN")
    void save_마감일_빈문자열() {
        given(jobRepository.existsBySourceUrl(anyString())).willReturn(false);

        crawledJobSaveService.save(defaultDto("백엔드 개발자", "", false));

        Job job = captureSavedJob();
        assertThat(job.getDeadline()).isNull();
        assertThat(job.getDeadlineType()).isEqualTo(Job.DeadlineType.UNKNOWN);
    }

    @Test
    @DisplayName("마감일 파싱 - 내일마감은 내일 날짜")
    void save_마감일_내일마감() {
        given(jobRepository.existsBySourceUrl(anyString())).willReturn(false);

        crawledJobSaveService.save(defaultDto("백엔드 개발자", "내일마감", false));

        Job job = captureSavedJob();
        assertThat(job.getDeadline()).isEqualTo(LocalDate.now().plusDays(1));
        assertThat(job.getDeadlineType()).isEqualTo(Job.DeadlineType.UNKNOWN);
    }

    @Test
    @DisplayName("마감일 파싱 - 오늘마감은 오늘 날짜")
    void save_마감일_오늘마감() {
        given(jobRepository.existsBySourceUrl(anyString())).willReturn(false);

        crawledJobSaveService.save(defaultDto("백엔드 개발자", "오늘마감", false));

        Job job = captureSavedJob();
        assertThat(job.getDeadline()).isEqualTo(LocalDate.now());
        assertThat(job.getDeadlineType()).isEqualTo(Job.DeadlineType.UNKNOWN);
    }

    @Test
    @DisplayName("외부 공고 저장 - external이면 descriptionStatus가 EXTERNAL")
    void save_외부공고_EXTERNAL() {
        given(jobRepository.existsBySourceUrl(anyString())).willReturn(false);

        crawledJobSaveService.save(defaultDto("백엔드 개발자", "~05/10(일)", true));

        Job job = captureSavedJob();
        assertThat(job.getDescriptionStatus()).isEqualTo(Job.DescriptionStatus.EXTERNAL);
    }

    @Test
    @DisplayName("지역 정규화 - 빈 location은 미기재로 저장")
    void save_지역_빈값은미기재() {
        given(jobRepository.existsBySourceUrl(anyString())).willReturn(false);

        CrawledJobDto dto = new CrawledJobDto(
                "백엔드 개발자",
                "테스트회사",
                "",
                "신입",
                "정규직",
                "~05/10(일)",
                "https://example.com/jobs/1",
                "테스트",
                "백엔드",
                null,
                false
        );
        crawledJobSaveService.save(dto);

        Job job = captureSavedJob();
        assertThat(job.getLocation()).isEqualTo("미기재");
    }

    private Job captureSavedJob() {
        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(captor.capture());
        return captor.getValue();
    }

    private CrawledJobDto defaultDto(String title, String deadlineText, boolean external) {
        return new CrawledJobDto(
                title,
                "테스트회사",
                "서울",
                "신입",
                "정규직",
                deadlineText,
                "https://example.com/jobs/" + title.hashCode() + deadlineText.hashCode() + external,
                "테스트",
                "백엔드",
                null,
                external
        );
    }
}
