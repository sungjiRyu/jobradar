package com.jobradar.backend.crawler;

import com.jobradar.backend.job.dto.DescriptionResponse;
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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 사람인 채용공고 크롤러
 *
 * [크롤링 흐름]
 * 1. Jsoup으로 사람인 개발자 공고 목록 페이지 HTML 요청 (User-Agent 설정 필수)
 * 2. CSS 선택자로 공고 정보 추출 (제목, 회사명, 지역, 경력, 마감일, URL)
 * 3. DB 중복 체크 → 이미 있는 공고면 스킵
 * 4. 공고 제목에서 기술스택 키워드 파싱
 * 5. 기술스택 없으면 DB에 새로 INSERT
 * 6. Job 엔티티 저장 (job_posts 테이블)
 * 7. 기술스택 연결 저장 (job_post_stacks 테이블)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SaraminCrawlerService implements CrawlerService {

    private final JobRepository jobRepository;
    private final TechStackRepository techStackRepository;

    static final String BASE_URL = "https://www.saramin.co.kr";
    // 웹개발(87), 백엔드/서버개발(84) 카테고리, 대기업~중견기업 포함
    static final String LIST_URL = BASE_URL + "/zf_user/search?cat_kewd=87%2C84"
            + "&company_cd=0%2C1%2C2%2C3%2C4%2C5%2C6%2C7%2C9%2C10"
            + "&search_optional_item=y&search_done=y&panel_count=y&preview=y&recruitPage=";

    // 최대 페이지 수 (안전장치): 빈 페이지 감지 시 자동 중단되므로 실제로는 마지막 페이지에서 멈춤
    // 비정상적으로 무한 루프가 도는 상황을 막기 위한 상한값
    static final int MAX_PAGES = 200;

    // 공고 제목에서 파싱할 기술스택 키워드 목록
    static final List<String> TECH_KEYWORDS = List.of(
            "Java", "Spring", "Python", "React", "Vue", "Node.js",
            "Docker", "AWS", "MySQL", "Redis", "Kotlin", "TypeScript", "Kubernetes"
    );

    @Override
    public String getSiteName() {
        return "사람인";
    }

    /**
     * 사람인 공고 수집 (빈 페이지가 나올 때까지 자동 반복)
     *
     * 사람인은 SSR(서버사이드 렌더링)이라 Jsoup으로 직접 HTML 파싱 가능.
     * 단, 기본 User-Agent("Java/21")는 차단되므로 브라우저 User-Agent 필수.
     *
     * [종료 조건]
     * - crawlPage()가 -1 반환 → 해당 페이지에 공고가 없음 = 마지막 페이지 도달
     * - MAX_PAGES 초과 → 무한 루프 방지용 안전장치
     * - IOException → 네트워크 오류
     */
    @Override
    public void collect() {
        log.info("[{}] 크롤링 시작 (마지막 페이지까지 자동 수집)", getSiteName());
        int totalSaved = 0;

        for (int page = 1; page <= MAX_PAGES; page++) {
            try {
                int saved = crawlPage(page);

                // -1: 빈 페이지 = 마지막 페이지 도달 → 수집 종료
                if (saved == -1) {
                    log.info("[{}] {}페이지에서 공고 없음 → 수집 완료", getSiteName(), page);
                    break;
                }

                totalSaved += saved;
                log.info("[{}] {}페이지 완료 - {}개 저장", getSiteName(), page, saved);

                // 서버 부하 방지: 페이지 간 1초 대기
                Thread.sleep(1000);
            } catch (IOException e) {
                log.error("[{}] {}페이지 요청 실패: {}", getSiteName(), page, e.getMessage());
                break;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        log.info("[{}] 크롤링 완료 - 총 {}개 저장", getSiteName(), totalSaved);
    }

    /**
     * 특정 페이지에서 공고 목록 파싱 후 저장
     *
     * @return 저장된 공고 수 (공고가 없는 마지막 페이지면 -1)
     */
    private int crawlPage(int page) throws IOException {
        String url = LIST_URL + page;

        // Jsoup으로 HTML 요청
        // - userAgent: 브라우저처럼 보이게 설정 (미설정 시 사람인이 차단)
        // - timeout: 10초 (네트워크 지연 대비)
        // - maxBodySize(0): 응답 크기 제한 해제 (기본값 2MB, 사람인 HTML이 2.4MB라 공고가 잘림)
        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
                        + "AppleWebKit/537.36 (KHTML, like Gecko) "
                        + "Chrome/124.0.0.0 Safari/537.36")
                .header("Accept-Language", "ko-KR,ko;q=0.9")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .maxBodySize(0)
                .timeout(10_000)
                .get();

        // div.item_recruit: 사람인 공고 목록의 각 공고 항목
        Elements items = doc.select("div.item_recruit");

        if (items.isEmpty()) {
            // -1을 반환해 collect()에서 루프를 종료하도록 신호
            log.info("[{}] {}페이지 공고 없음 → 마지막 페이지 도달", getSiteName(), page);
            return -1;
        }

        int savedCount = 0;
        for (Element item : items) {
            if (parseAndSave(item)) {
                savedCount++;
            }
        }
        return savedCount;
    }

    /**
     * 공고 HTML 항목에서 데이터 추출 후 저장
     *
     * [사람인 HTML 구조]
     * div.item_recruit
     *   h2.job_tit > a[href]           ← 공고 제목 + URL
     *     span                          ← 제목 텍스트
     *   div.job_date > span.date        ← 마감일 (예: "~ 04/30(목)")
     *   div.job_condition
     *     span:first-child              ← 지역 (예: "서울 강남구")
     *     span:nth-child(2)             ← 경력 (예: "신입·경력")
     *   div.area_corp > strong.corp_name > a  ← 회사명
     *
     * @return true: 저장됨 / false: 중복으로 스킵
     */
    private boolean parseAndSave(Element item) {
        // 공고 제목과 URL 추출
        Element titleEl = item.selectFirst("h2.job_tit a");
        if (titleEl == null) return false;

        String title = titleEl.text().trim();
        String href = titleEl.attr("href"); // 예: /zf_user/jobs/relay/view?...&rec_idx=12345
        String sourceUrl = BASE_URL + href;

        // 회사명 (없으면 "미기재")
        Element corpEl = item.selectFirst("strong.corp_name a");
        String company = (corpEl != null) ? corpEl.text().trim() : "미기재";

        // 지역 (job_condition 첫 번째 span)
        Element locationEl = item.selectFirst("div.job_condition span");
        String location = (locationEl != null) ? locationEl.text().trim() : "";

        // 경력 (job_condition 두 번째 span)
        Elements condSpans = item.select("div.job_condition span");
        String experience = (condSpans.size() >= 2) ? condSpans.get(1).text().trim() : "";

        // 마감일 (예: "~ 04/30(목)")
        Element dateEl = item.selectFirst("div.job_date span.date");
        String deadlineText = (dateEl != null) ? dateEl.text().trim() : "";

        return saveJob(title, company, location, experience, deadlineText, sourceUrl);
    }

    /**
     * 공고 저장 (중복 체크 후 DB INSERT)
     *
     * @return true: 저장됨 / false: 중복으로 스킵
     */
    boolean saveJob(String title, String company, String location,
                    String experience, String deadlineText, String sourceUrl) {
        // sourceUrl이 이미 DB에 존재하면 중복 공고 → 스킵
        if (jobRepository.existsBySourceUrl(sourceUrl)) {
            return false;
        }

        LocalDate deadline = parseDeadline(deadlineText);
        List<TechStack> techStacks = resolveTechStacks(title);

        Job job = Job.builder()
                .title(title)
                .company(company)
                .location(location.isBlank() ? "미기재" : location)
                .experienceLevel(experience)
                .deadline(deadline)
                .sourceUrl(sourceUrl)
                .sourceSite(getSiteName())
                .build();

        job.getTechStacks().addAll(techStacks);
        jobRepository.save(job);
        return true;
    }

    /**
     * 마감일 텍스트 → LocalDate 변환
     * 사람인 형식: "~04/30(수)", "채용시", "내일마감", ""
     */
    LocalDate parseDeadline(String text) {
        if (text == null || text.isBlank() || text.contains("채용시") || text.contains("상시")) {
            return null;
        }
        if (text.contains("내일")) {
            return LocalDate.now().plusDays(1);
        }
        if (text.contains("오늘")) {
            return LocalDate.now();
        }

        // "~ 04/30(목)" 패턴에서 MM/dd 추출
        Matcher matcher = Pattern.compile("(\\d{2})/(\\d{2})").matcher(text);
        if (matcher.find()) {
            int month = Integer.parseInt(matcher.group(1));
            int day   = Integer.parseInt(matcher.group(2));
            LocalDate parsed = LocalDate.of(LocalDate.now().getYear(), month, day);
            // 이미 지난 날짜면 내년으로 처리
            if (parsed.isBefore(LocalDate.now())) {
                parsed = parsed.plusYears(1);
            }
            return parsed;
        }

        return null;
    }

    /**
     * 공고 제목에서 기술스택 키워드 추출 후 DB 조회 또는 신규 생성
     */
    List<TechStack> resolveTechStacks(String title) {
        List<TechStack> result = new ArrayList<>();

        for (String keyword : TECH_KEYWORDS) {
            if (title.toLowerCase().contains(keyword.toLowerCase())) {
                // DB에 이미 있으면 조회, 없으면 새로 저장
                TechStack techStack = techStackRepository.findByName(keyword)
                        .orElseGet(() -> techStackRepository.save(
                                TechStack.builder().name(keyword).build()
                        ));
                result.add(techStack);
            }
        }

        return result;
    }

    /**
     * 사람인 공고 상세 내용 크롤링
     * sourceUrl에서 rec_idx 추출 → view-detail GET 요청 → div.user_content 텍스트 반환
     *
     * @param sourceUrl 저장된 공고 URL (rec_idx 포함)
     * @return 공고 상세 텍스트 (파싱 실패 시 null)
     */
    public DescriptionResponse fetchDescription(String sourceUrl) {
        Matcher m = Pattern.compile("[?&]rec_idx=(\\d+)").matcher(sourceUrl);
        if (!m.find()) {
            log.warn("[사람인] rec_idx 파싱 실패: {}", sourceUrl);
            return DescriptionResponse.crawlFailed();
        }
        String recIdx = m.group(1);
        String detailUrl = BASE_URL + "/zf_user/jobs/relay/view-detail?rec_idx=" + recIdx + "&rec_seq=0";

        try {
            Document doc = Jsoup.connect(detailUrl)
                    .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
                            + "AppleWebKit/537.36 (KHTML, like Gecko) "
                            + "Chrome/124.0.0.0 Safari/537.36")
                    .header("Accept-Language", "ko-KR,ko;q=0.9")
                    .timeout(10_000)
                    .get();

            Element content = doc.selectFirst("div.user_content");
            if (content == null) {
                log.warn("[사람인] user_content 없음: rec_idx={}", recIdx);
                return DescriptionResponse.crawlFailed();
            }
            String text = content.text().trim();
            if (text.isEmpty()) {
                boolean hasImage = !content.select("img").isEmpty();
                return hasImage ? DescriptionResponse.image() : DescriptionResponse.crawlFailed();
            }
            return DescriptionResponse.success(text);

        } catch (IOException e) {
            log.error("[사람인] 상세 크롤링 실패: rec_idx={}, error={}", recIdx, e.getMessage());
            return DescriptionResponse.crawlFailed();
        }
    }
}
