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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 잡코리아 채용공고 크롤러
 *
 * [크롤링 흐름]
 * 1. Jsoup으로 잡코리아 개발자 공고 목록 페이지 HTML 요청
 * 2. CSS 선택자로 공고 정보 추출 (제목, 회사명, 지역, 경력, 마감일, URL)
 * 3. DB 중복 체크 → 이미 있는 공고면 스킵
 * 4. 공고 제목에서 기술스택 키워드 파싱
 * 5. 기술스택 없으면 DB에 새로 INSERT
 * 6. Job 엔티티 저장 (job_posts 테이블)
 * 7. 기술스택 연결 저장 (job_post_stacks 테이블)
 *
 * [잡코리아 HTML 구조]
 * tr.devloopArea                              ← 공고 항목
 *   td.tplCo > a.link                         ← 회사명
 *   td.tplTit > strong > a[href]              ← 공고 제목 + URL
 *   td.tplTit > p.etc > span.cell[0]          ← 경력 (예: "신입·경력")
 *   td.tplTit > p.etc > span.cell[2]          ← 지역 (예: "서울 강남구")
 *   td.odd > span.date                        ← 마감일 (예: "~05/10(일)")
 *
 * [직무 필터 코드]
 * 백엔드개발자: 1000229 / 프론트엔드개발자: 1000230 / 웹개발자: 1000231
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobkoreaCrawlerService implements CrawlerService {

    private final JobRepository jobRepository;
    private final TechStackRepository techStackRepository;

    static final String BASE_URL = "https://www.jobkorea.co.kr";
    // 백엔드(1000229), 프론트엔드(1000230), 웹개발(1000231) 직무 필터
    static final String LIST_URL = BASE_URL + "/recruit/joblist?menucode=duty"
            + "&duty=1000229%2C1000230%2C1000231&Page_No=";

    // 최대 페이지 수 (안전장치): 빈 페이지 감지 시 자동 중단되므로 실제로는 마지막 페이지에서 멈춤
    static final int MAX_PAGES = 200;

    // 공고 제목에서 파싱할 기술스택 키워드 목록
    static final List<String> TECH_KEYWORDS = List.of(
            "Java", "Spring", "Python", "React", "Vue", "Node.js",
            "Docker", "AWS", "MySQL", "Redis", "Kotlin", "TypeScript", "Kubernetes"
    );

    @Override
    public String getSiteName() {
        return "잡코리아";
    }

    /**
     * 잡코리아 공고 수집 (빈 페이지가 나올 때까지 자동 반복)
     *
     * 잡코리아는 SSR(서버사이드 렌더링)이라 Jsoup으로 직접 HTML 파싱 가능.
     * 필터 선택 시 브라우저 URL은 안 바뀌지만, 서버는 파라미터를 받아 처리함.
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

        // - maxBodySize(0): 응답 크기 제한 해제 (기본값 2MB로 잘릴 수 있음)
        // - userAgent: 브라우저처럼 보이게 설정 (미설정 시 차단될 수 있음)
        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
                        + "AppleWebKit/537.36 (KHTML, like Gecko) "
                        + "Chrome/124.0.0.0 Safari/537.36")
                .header("Accept-Language", "ko-KR,ko;q=0.9")
                .maxBodySize(0)
                .timeout(10_000)
                .get();

        // tr.devloopArea: 잡코리아 공고 목록의 각 공고 항목 (table row)
        Elements items = doc.select("tr.devloopArea");

        if (items.isEmpty()) {
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
     * @return true: 저장됨 / false: 중복으로 스킵
     */
    private boolean parseAndSave(Element item) {
        // 공고 제목과 URL 추출
        Element titleEl = item.selectFirst("td.tplTit strong a");
        if (titleEl == null) return false;

        String title = titleEl.text().trim();
        // href 예시: /Recruit/GI_Read/48998248?rPageCode=PL&...
        // 쿼리 파라미터 제거하여 고정 URL 사용 (중복 저장 방지)
        String sourceUrl = BASE_URL + titleEl.attr("href").replaceAll("\\?.*", "");

        // 회사명
        Element corpEl = item.selectFirst("td.tplCo a.link");
        String company = (corpEl != null) ? corpEl.text().trim() : "미기재";

        // p.etc 안의 span.cell 목록: [경력, 학력, 지역, 고용형태, 연봉]
        Elements cells = item.select("td.tplTit p.etc span.cell");
        String experience = cells.size() > 0 ? cells.get(0).text().trim() : "";
        String location   = cells.size() > 2 ? cells.get(2).text().trim() : "";

        // 마감일 (예: "~05/10(일)") — 사람인과 동일한 형식
        Element dateEl = item.selectFirst("td.odd span.date");
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
     * 잡코리아 형식: "~05/10(일)", "채용시", ""
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

        Matcher matcher = Pattern.compile("(\\d{2})/(\\d{2})").matcher(text);
        if (matcher.find()) {
            int month = Integer.parseInt(matcher.group(1));
            int day   = Integer.parseInt(matcher.group(2));
            LocalDate parsed = LocalDate.of(LocalDate.now().getYear(), month, day);
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
