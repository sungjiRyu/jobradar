package com.jobradar.backend.crawler;

import com.jobradar.backend.job.dto.DescriptionResponse;
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
class JobkoreaCrawlerServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private TechStackRepository techStackRepository;

    @InjectMocks
    private JobkoreaCrawlerService jobkoreaCrawlerService;

    // ===== 중복 공고 스킵 테스트 =====

    @Test
    @DisplayName("중복 공고 스킵 - 이미 DB에 있는 URL이면 save() 호출 안 됨")
    void saveJob_중복URL_스킵() {
        // given
        given(jobRepository.existsBySourceUrl(anyString())).willReturn(true);

        // when
        boolean saved = jobkoreaCrawlerService.saveJob(
                "Java 백엔드 개발자", "테스트회사", "서울", "신입",
                "~05/10(일)", "https://www.jobkorea.co.kr/Recruit/GI_Read/12345",
                "백엔드", null, "정규직",
                DescriptionResponse.success("Java 백엔드 채용 본문")
        );

        // then
        assertThat(saved).isFalse();
        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    @DisplayName("신규 공고 저장 - DB에 없는 URL이면 save() 호출됨")
    void saveJob_신규공고_저장() {
        // given
        given(jobRepository.existsBySourceUrl(anyString())).willReturn(false);
        given(techStackRepository.findByName("React")).willReturn(Optional.of(TechStack.builder().name("React").build()));
        given(jobRepository.save(any(Job.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        boolean saved = jobkoreaCrawlerService.saveJob(
                "React 프론트엔드 개발자", "새회사", "서울 강남구", "경력 2년",
                "~05/31(토)", "https://www.jobkorea.co.kr/Recruit/GI_Read/99999",
                "프론트엔드", null, "정규직",
                DescriptionResponse.success("React 프론트엔드 채용 본문")
        );

        // then
        assertThat(saved).isTrue();
        verify(jobRepository).save(any(Job.class));
    }

    // ===== 기술스택 파싱 테스트 =====

    @Test
    @DisplayName("기술스택 파싱 - 'Node.js, TypeScript 사용' 텍스트에서 키워드 추출")
    void resolveTechStacks_키워드파싱() {
        // given
        String title = "Node.js TypeScript 백엔드 개발자 채용";

        given(techStackRepository.findByName("Node.js")).willReturn(Optional.empty());
        given(techStackRepository.findByName("TypeScript")).willReturn(Optional.empty());
        given(techStackRepository.save(any(TechStack.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        List<TechStack> result = jobkoreaCrawlerService.resolveTechStacks(title);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(TechStack::getName)
                .containsExactlyInAnyOrder("Node.js", "TypeScript");
    }

    @Test
    @DisplayName("기술스택 파싱 - 키워드 없는 공고는 빈 리스트 반환")
    void resolveTechStacks_키워드없음() {
        // when
        List<TechStack> result = jobkoreaCrawlerService.resolveTechStacks("영업팀 경력직 채용");

        // then
        assertThat(result).isEmpty();
    }

    // ===== 마감일 파싱 테스트 =====

    @Test
    @DisplayName("마감일 파싱 - '~05/10(일)' 잡코리아 형식")
    void parseDeadline_날짜형식() {
        LocalDate result = jobkoreaCrawlerService.parseDeadline("~05/10(일)");

        assertThat(result).isNotNull();
        assertThat(result.getMonthValue()).isEqualTo(5);
        assertThat(result.getDayOfMonth()).isEqualTo(10);
    }

    @Test
    @DisplayName("마감일 파싱 - '채용시' → null")
    void parseDeadline_채용시() {
        assertThat(jobkoreaCrawlerService.parseDeadline("채용시")).isNull();
    }

    @Test
    @DisplayName("마감일 파싱 - 빈 문자열 → null")
    void parseDeadline_빈문자열() {
        assertThat(jobkoreaCrawlerService.parseDeadline("")).isNull();
    }

    @Test
    @DisplayName("마감일 파싱 - '내일마감' → 내일 날짜")
    void parseDeadline_내일마감() {
        assertThat(jobkoreaCrawlerService.parseDeadline("내일마감"))
                .isEqualTo(LocalDate.now().plusDays(1));
    }
}
