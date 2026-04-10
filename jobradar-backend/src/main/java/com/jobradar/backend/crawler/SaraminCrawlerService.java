package com.jobradar.backend.crawler;

import com.jobradar.backend.job.entity.Job;
import com.jobradar.backend.job.entity.TechStack;
import com.jobradar.backend.job.repository.JobRepository;
import com.jobradar.backend.job.repository.TechStackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 사람인 채용공고 크롤러
 *
 * [크롤링 흐름]
 * 1. Jsoup으로 사람인 개발자 공고 목록 페이지 HTML 요청
 * 2. CSS 선택자로 공고 정보 추출 (제목, 회사명, 지역, 경력, 마감일, URL)
 * 3. DB 중복 체크 → 이미 있는 공고면 스킵
 * 4. 공고 제목에서 기술스택 키워드 파싱
 * 5. 기술스택 없으면 DB에 새로 INSERT
 * 6. Job 엔티티 저장 (job_posts 테이블)
 * 7. 기술스택 연결 저장 (job_post_stacks 테이블)
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SaraminCrawlerService implements CrawlerService {

    private final JobRepository jobRepository;
    private final TechStackRepository techStackRepository;

    // 사람인 개발자 직군 공고 목록 URL (cat_kewd=2248: 개발·데이터)
    private static final String BASE_URL = "https://www.saramin.co.kr";
    private static final String LIST_URL = BASE_URL + "/zf_user/jobs/list/job-category?cat_kewd=2248&recruitPage=";

    // 파싱할 기술스택 키워드 목록
    // 공고 제목에 이 단어들이 포함되면 기술스택으로 인식
    static final List<String> TECH_KEYWORDS = List.of(
            "Java", "Spring", "Python", "React", "Vue", "Node.js",
            "Docker", "AWS", "MySQL", "Redis", "Kotlin", "TypeScript", "Kubernetes"
    );

    @Override
    public String getSiteName() {
        return "사람인";
    }

    /**
     * 사람인 개발자 공고 수집 (1~5페이지)
     *
     * - IOException: Jsoup HTTP 요청 실패 (네트워크 오류, 서버 응답 오류)
     * - InterruptedException: Thread.sleep 중 인터럽트 발생 시 스레드 상태 복원 후 루프 종료
     * - 각 페이지를 try-catch로 감싸서 한 페이지 실패가 전체를 중단시키지 않음
     */
    @Override
    public void collect() {
        log.info("[{}] 크롤링 시작", getSiteName());
        int savedCount = 0;

        for (int page = 1; page <= 5; page++) {
            try {
                int count = crawlPage(page);
                savedCount += count;
                log.info("[{}] {}페이지 완료 - 저장된 공고: {}건", getSiteName(), page, count);

                // 페이지 간 1초 대기: 서버 과부하 방지 (크롤링 에티켓)
                Thread.sleep(1000);

            } catch (IOException e) {
                // HTTP 요청 실패 → 해당 페이지만 스킵하고 다음 페이지 진행
                log.error("[{}] {}페이지 크롤링 실패 - 스킵", getSiteName(), page, e);
            } catch (InterruptedException e) {
                // Thread.sleep 중 인터럽트 → 스레드 상태 복원 후 루프 종료
                Thread.currentThread().interrupt();
                log.error("[{}] 크롤링 중 인터럽트 발생 - 중단", getSiteName(), e);
                break;
            }
        }

        log.info("[{}] 크롤링 완료 - 총 저장: {}건", getSiteName(), savedCount);
    }

    /**
     * 단일 페이지 크롤링
     *
     * @param page 페이지 번호 (1~5)
     * @return 저장된 공고 수
     * @throws IOException Jsoup 요청 실패 시
     */
    private int crawlPage(int page) throws IOException {
        // Jsoup으로 HTML 요청
        // userAgent: 브라우저처럼 보이게 해서 봇 차단 우회
        // timeout: 10초 안에 응답 없으면 IOException 발생
        Document doc = Jsoup.connect(LIST_URL + page)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .timeout(10_000)
                .get();

        // div.item_recruit: 공고 목록의 각 공고 항목 선택자
        Elements items = doc.select("div.item_recruit");
        int savedCount = 0;

        for (Element item : items) {
            try {
                boolean saved = processJob(item);
                if (saved) savedCount++;
            } catch (Exception e) {
                // 개별 공고 처리 실패 → 해당 공고만 스킵
                log.warn("[{}] 공고 저장 실패 - 스킵: {}", getSiteName(), e.getMessage());
            }
        }

        return savedCount;
    }

    /**
     * 단일 공고 항목 파싱 및 저장
     *
     * HTML 구조 (사람인):
     * <div class="item_recruit">
     *   <div class="job_tit"><a href="/zf_user/jobs/relay/view?...">공고 제목</a></div>
     *   <div class="corp_name"><a>회사명</a></div>
     *   <div class="job_condition">
     *     <span>서울 강남구</span>  ← location (첫 번째)
     *     <span>신입</span>         ← experience (두 번째)
     *   </div>
     *   <div class="job_date"><span class="date">~04/30(수)</span></div>
     * </div>
     *
     * @return true: 저장됨 / false: 중복으로 스킵
     */
    boolean processJob(Element item) {
        // 제목과 URL 추출
        Element titleEl = item.selectFirst(".job_tit a");
        if (titleEl == null) return false;

        String title = titleEl.text();
        String href = titleEl.attr("href");
        // href가 상대 경로이므로 절대 URL로 변환
        String sourceUrl = BASE_URL + href;

        // 중복 체크: 같은 URL이 이미 DB에 있으면 스킵
        // existsBySourceUrl은 SELECT COUNT(*) 쿼리로 빠르게 확인
        if (jobRepository.existsBySourceUrl(sourceUrl)) {
            return false;
        }

        // 회사명
        String company = "";
        Element companyEl = item.selectFirst(".corp_name a");
        if (companyEl != null) {
            company = companyEl.text();
        }

        // 근무 조건 (지역, 경력): span 순서로 추출
        Elements conditions = item.select(".job_condition span");
        String location = conditions.size() > 0 ? conditions.get(0).text().trim() : "미기재";
        String experience = conditions.size() > 1 ? conditions.get(1).text().trim() : "";

        // 마감일: "~04/30(수)", "채용시", "내일마감" 등 다양한 형식
        String deadlineText = item.select(".job_date .date").text();
        LocalDate deadline = parseDeadline(deadlineText);

        // 기술스택 키워드 파싱 (공고 제목 기반)
        List<TechStack> techStacks = resolveTechStacks(title);

        // Job 엔티티 생성
        Job job = Job.builder()
                .title(title)
                .company(company)
                .location(location)
                .experienceLevel(experience)
                .deadline(deadline)
                .sourceUrl(sourceUrl)
                .sourceSite(getSiteName())
                .build();

        // 기술스택 연결 (save 전에 추가해야 job_post_stacks에 함께 INSERT됨)
        job.getTechStacks().addAll(techStacks);

        jobRepository.save(job);
        return true;
    }

    /**
     * 마감일 텍스트 파싱 → LocalDate 변환
     *
     * 사람인 마감일 형식 예시:
     * - "~04/30(수)"  → LocalDate.of(2026, 4, 30)
     * - "채용시"      → null (상시 채용)
     * - "내일마감"    → LocalDate.now().plusDays(1)
     * - ""            → null
     *
     * [왜 DATE() 함수 대신 직접 파싱?]
     * LocalDate.parse()는 고정 포맷에만 작동 → 사람인의 다양한 형식을 정규식으로 직접 처리
     */
    LocalDate parseDeadline(String text) {
        if (text == null || text.isBlank() || text.contains("채용시") || text.contains("상시")) {
            return null; // 상시 채용 → deadline null로 저장
        }
        if (text.contains("내일")) {
            return LocalDate.now().plusDays(1);
        }
        if (text.contains("오늘")) {
            return LocalDate.now();
        }

        // "~04/30(수)" → month=04, day=30 추출
        Matcher matcher = Pattern.compile("(\\d{2})/(\\d{2})").matcher(text);
        if (matcher.find()) {
            int month = Integer.parseInt(matcher.group(1));
            int day   = Integer.parseInt(matcher.group(2));
            int year  = LocalDate.now().getYear();

            LocalDate parsed = LocalDate.of(year, month, day);
            // 이미 지난 날짜면 내년으로 처리 (예: 12월 공고를 1월에 크롤링)
            if (parsed.isBefore(LocalDate.now())) {
                parsed = parsed.plusYears(1);
            }
            return parsed;
        }

        return null;
    }

    /**
     * 공고 제목에서 기술스택 키워드 추출 → DB에서 조회 또는 신규 생성
     *
     * [흐름]
     * 1. TECH_KEYWORDS 중 title에 포함된 키워드 찾기
     * 2. DB에 해당 이름의 TechStack이 있으면 그대로 사용
     * 3. 없으면 새로 생성해서 tech_stacks 테이블에 INSERT
     *
     * [왜 findByName + orElseGet?]
     * - 이미 있는 기술스택을 중복 INSERT하지 않기 위해
     * - tech_stacks.name에 UNIQUE 제약이 있어서 중복 시 예외 발생
     */
    List<TechStack> resolveTechStacks(String title) {
        List<TechStack> result = new ArrayList<>();

        for (String keyword : TECH_KEYWORDS) {
            // 대소문자 무시 포함 여부 확인
            if (title.toLowerCase().contains(keyword.toLowerCase())) {
                // DB에 있으면 기존 엔티티 사용, 없으면 새로 생성
                TechStack techStack = techStackRepository.findByName(keyword)
                        .orElseGet(() -> techStackRepository.save(
                                TechStack.builder().name(keyword).build()
                        ));
                result.add(techStack);
            }
        }

        return result;
    }
}
