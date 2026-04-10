package com.jobradar.backend.crawler;

import com.jobradar.backend.job.entity.Job;
import com.jobradar.backend.job.entity.TechStack;
import com.jobradar.backend.job.repository.JobRepository;
import com.jobradar.backend.job.repository.TechStackRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
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

/**
 * SaraminCrawlerService 단위 테스트
 *
 * [@ExtendWith(MockitoExtension.class)]
 * - Spring 컨텍스트 없이 Mockito만으로 테스트 → 실제 DB/HTTP 연결 없음
 * - @Mock: 가짜 객체로 대체 (실제 DB 쿼리 없이 원하는 값 반환)
 * - @InjectMocks: 테스트 대상 객체에 @Mock 주입
 *
 * [테스트 전략]
 * - collect()는 실제 HTTP 요청이 필요해서 단위 테스트 불가
 * - 비즈니스 로직 메서드(processJob, parseTechStacks, parseDeadline)를 직접 테스트
 * - processJob, resolveTechStacks, parseDeadline이 package-private이므로 같은 패키지에서 호출 가능
 */
@ExtendWith(MockitoExtension.class)
class SaraminCrawlerServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private TechStackRepository techStackRepository;

    @InjectMocks
    private SaraminCrawlerService saraminCrawlerService;

    // ===== 중복 공고 스킵 테스트 =====

    /**
     * [테스트 목적]
     * 이미 DB에 있는 URL의 공고가 들어오면 jobRepository.save()를 호출하지 않아야 함
     * → 같은 공고가 중복으로 저장되는 것을 방지
     *
     * [왜 필요한가?]
     * 매일 크롤링이 실행되는데 이미 수집한 공고를 또 저장하면 DB에 중복 데이터가 쌓임
     * existsBySourceUrl()로 체크해서 있으면 스킵하는 로직이 실제로 동작하는지 검증
     */
    @Test
    @DisplayName("중복 공고 스킵 - 이미 DB에 있는 URL이면 save() 호출 안 됨")
    void processJob_중복URL_스킵() {
        // given: HTML 공고 항목 생성
        String html = """
                <div class="item_recruit">
                  <div class="job_tit">
                    <a href="/zf_user/jobs/relay/view?rec_idx=12345">Java 백엔드 개발자</a>
                  </div>
                  <div class="corp_name"><a>테스트 회사</a></div>
                  <div class="job_condition">
                    <span>서울</span><span>신입</span>
                  </div>
                  <div class="job_date"><span class="date">~04/30(수)</span></div>
                </div>
                """;
        Element item = Jsoup.parse(html).selectFirst("div.item_recruit");

        // given: 해당 URL이 이미 DB에 존재
        given(jobRepository.existsBySourceUrl(anyString())).willReturn(true);

        // when: 크롤링 처리
        boolean saved = saraminCrawlerService.processJob(item);

        // then: save()가 호출되지 않아야 함 (중복이므로 스킵)
        assertThat(saved).isFalse();
        verify(jobRepository, never()).save(any(Job.class));
    }

    /**
     * [테스트 목적]
     * DB에 없는 새로운 공고는 정상적으로 저장되는지 확인
     */
    @Test
    @DisplayName("신규 공고 저장 - DB에 없는 URL이면 save() 호출됨")
    void processJob_신규공고_저장() {
        // given: HTML 공고 항목
        String html = """
                <div class="item_recruit">
                  <div class="job_tit">
                    <a href="/zf_user/jobs/relay/view?rec_idx=99999">Spring Boot 개발자</a>
                  </div>
                  <div class="corp_name"><a>새로운 회사</a></div>
                  <div class="job_condition">
                    <span>서울</span><span>경력 3년</span>
                  </div>
                  <div class="job_date"><span class="date">~05/31(금)</span></div>
                </div>
                """;
        Element item = Jsoup.parse(html).selectFirst("div.item_recruit");

        // given: DB에 해당 URL 없음
        given(jobRepository.existsBySourceUrl(anyString())).willReturn(false);
        // given: 기술스택 "Spring"이 DB에 존재
        TechStack springStack = TechStack.builder().name("Spring").build();
        given(techStackRepository.findByName("Spring")).willReturn(Optional.of(springStack));
        // given: save() 호출 시 저장된 Job 반환
        given(jobRepository.save(any(Job.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        boolean saved = saraminCrawlerService.processJob(item);

        // then: 저장됨
        assertThat(saved).isTrue();
        verify(jobRepository).save(any(Job.class));
    }

    // ===== 기술스택 파싱 테스트 =====

    /**
     * [테스트 목적]
     * 공고 제목에서 TECH_KEYWORDS에 해당하는 키워드를 올바르게 추출하는지 검증
     *
     * [왜 필요한가?]
     * 기술스택 파싱은 대시보드의 "기술스택별 공고 수" 통계에 직접 영향
     * 파싱 로직이 잘못되면 통계 데이터가 부정확해짐
     */
    @Test
    @DisplayName("기술스택 파싱 - 'Java, Spring Boot 사용' 텍스트에서 Java, Spring 추출")
    void resolveTechStacks_키워드파싱() {
        // given: Java와 Spring이 포함된 공고 제목
        String title = "Java, Spring Boot 사용 백엔드 개발자 채용";

        // given: DB에 해당 기술스택 없음 → 새로 생성
        // findByName은 이름별로 스텁, 나머지 키워드는 기본값(empty) 반환
        given(techStackRepository.findByName("Java")).willReturn(Optional.empty());
        given(techStackRepository.findByName("Spring")).willReturn(Optional.empty());
        // 다른 키워드들은 Mockito 기본값 Optional.empty() 반환

        // save()는 전달받은 TechStack을 그대로 반환 (ID 없이도 동작하도록)
        given(techStackRepository.save(any(TechStack.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        List<TechStack> result = saraminCrawlerService.resolveTechStacks(title);

        // then: Java, Spring 두 개가 추출되어야 함
        assertThat(result).hasSize(2);
        assertThat(result).extracting(TechStack::getName)
                .containsExactlyInAnyOrder("Java", "Spring");
    }

    @Test
    @DisplayName("기술스택 파싱 - 키워드 없는 공고는 빈 리스트 반환")
    void resolveTechStacks_키워드없음() {
        // given: 기술스택 키워드가 없는 제목
        String title = "총무팀 경력직 채용";

        // when
        List<TechStack> result = saraminCrawlerService.resolveTechStacks(title);

        // then: 빈 리스트
        assertThat(result).isEmpty();
    }

    // ===== 마감일 파싱 테스트 =====

    /**
     * [테스트 목적]
     * 사람인의 다양한 마감일 형식을 올바르게 LocalDate로 변환하는지 검증
     *
     * [왜 필요한가?]
     * 마감일이 잘못 파싱되면 "마감 임박 공고 수" 통계가 부정확해짐
     */
    @Test
    @DisplayName("마감일 파싱 - '~04/30(수)' 형식 파싱")
    void parseDeadline_날짜형식() {
        // when
        LocalDate result = saraminCrawlerService.parseDeadline("~04/30(수)");

        // then: 4월 30일로 파싱됨
        assertThat(result).isNotNull();
        assertThat(result.getMonthValue()).isEqualTo(4);
        assertThat(result.getDayOfMonth()).isEqualTo(30);
    }

    @Test
    @DisplayName("마감일 파싱 - '채용시' → null 반환 (상시 채용)")
    void parseDeadline_채용시() {
        // when
        LocalDate result = saraminCrawlerService.parseDeadline("채용시");

        // then: 상시채용은 null
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("마감일 파싱 - 빈 문자열 → null 반환")
    void parseDeadline_빈문자열() {
        // when
        LocalDate result = saraminCrawlerService.parseDeadline("");

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("마감일 파싱 - '내일마감' → 내일 날짜 반환")
    void parseDeadline_내일마감() {
        // when
        LocalDate result = saraminCrawlerService.parseDeadline("내일마감");

        // then: 오늘 +1일
        assertThat(result).isEqualTo(LocalDate.now().plusDays(1));
    }

}
