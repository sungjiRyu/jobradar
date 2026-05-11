package com.jobradar.backend.crawler;

import com.jobradar.backend.job.entity.Job;
import com.jobradar.backend.job.entity.TechStack;
import com.jobradar.backend.job.repository.JobRepository;
import com.jobradar.backend.job.repository.TechStackRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SaraminCrawlerServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private TechStackRepository techStackRepository;

    @InjectMocks
    private SaraminCrawlerService saraminCrawlerService;

    // ===== 중복 공고 스킵 테스트 =====

    @Test
    @DisplayName("중복 공고 스킵 - 이미 DB에 있는 URL이면 save() 호출 안 됨")
    void saveJob_중복URL_스킵() {
        // given: 해당 URL이 이미 DB에 존재
        given(jobRepository.existsBySourceUrl(anyString())).willReturn(true);

        // when
        boolean saved = saraminCrawlerService.saveJob(
                "Java 백엔드 개발자", "테스트회사", "서울", "신입",
                "~04/30(수)", "https://www.saramin.co.kr/zf_user/jobs/relay/view?rec_idx=12345",
                "백엔드", null, "정규직"
        );

        // then: 중복이므로 save() 미호출
        assertThat(saved).isFalse();
        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    @DisplayName("신규 공고 저장 - DB에 없는 URL이면 save() 호출됨")
    void saveJob_신규공고_저장() {
        // given
        given(jobRepository.existsBySourceUrl(anyString())).willReturn(false);
        given(techStackRepository.findByName("Spring")).willReturn(Optional.of(TechStack.builder().name("Spring").build()));
        given(jobRepository.save(any(Job.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        boolean saved = saraminCrawlerService.saveJob(
                "Spring Boot 백엔드 개발자", "새회사", "서울", "경력 3년",
                "~05/31(금)", "https://www.saramin.co.kr/zf_user/jobs/relay/view?rec_idx=99999",
                "백엔드", null, "정규직"
        );

        // then
        assertThat(saved).isTrue();
        verify(jobRepository).save(any(Job.class));
    }

    // ===== 기술스택 파싱 테스트 =====

    @Test
    @DisplayName("기술스택 파싱 - 'Java, Spring Boot 사용' 텍스트에서 Java, Spring 추출")
    void resolveTechStacks_키워드파싱() {
        // given
        String title = "Java, Spring Boot 사용 백엔드 개발자 채용";

        given(techStackRepository.findByName("Java")).willReturn(Optional.empty());
        given(techStackRepository.findByName("Spring")).willReturn(Optional.empty());
        given(techStackRepository.save(any(TechStack.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        List<TechStack> result = saraminCrawlerService.resolveTechStacks(title);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(TechStack::getName)
                .containsExactlyInAnyOrder("Java", "Spring");
    }

    @Test
    @DisplayName("기술스택 파싱 - 키워드 없는 공고는 빈 리스트 반환")
    void resolveTechStacks_키워드없음() {
        // when
        List<TechStack> result = saraminCrawlerService.resolveTechStacks("총무팀 경력직 채용");

        // then
        assertThat(result).isEmpty();
    }

    // ===== 마감일 파싱 테스트 =====

    @Test
    @DisplayName("마감일 파싱 - '~04/30(수)' 형식")
    void parseDeadline_날짜형식() {
        LocalDate result = saraminCrawlerService.parseDeadline("~04/30(수)");

        assertThat(result).isNotNull();
        assertThat(result.getMonthValue()).isEqualTo(4);
        assertThat(result.getDayOfMonth()).isEqualTo(30);
    }

    @Test
    @DisplayName("마감일 파싱 - '채용시' → null (상시 채용)")
    void parseDeadline_채용시() {
        assertThat(saraminCrawlerService.parseDeadline("채용시")).isNull();
    }

    @Test
    @DisplayName("마감일 파싱 - 빈 문자열 → null")
    void parseDeadline_빈문자열() {
        assertThat(saraminCrawlerService.parseDeadline("")).isNull();
    }

    @Test
    @DisplayName("마감일 파싱 - '내일마감' → 내일 날짜")
    void parseDeadline_내일마감() {
        assertThat(saraminCrawlerService.parseDeadline("내일마감"))
                .isEqualTo(LocalDate.now().plusDays(1));
    }
}
