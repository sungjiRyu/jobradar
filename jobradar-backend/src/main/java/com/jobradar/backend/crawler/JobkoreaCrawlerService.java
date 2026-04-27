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

    // 잡코리아 공고 목록 AJAX 엔드포인트
    // - GET /recruit/joblist 는 SSR로 1페이지만 반환하며 Page 파라미터를 무시함
    // - 브라우저에서 페이지 이동 시 JavaScript가 이 POST URL을 호출하여 HTML 조각을 받아옴
    // - Jsoup은 JavaScript를 실행할 수 없으므로 이 POST 엔드포인트를 직접 호출해야 페이지 이동 가능
    static final String POST_URL = BASE_URL + "/Recruit/Home/_GI_List/";

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
     * [페이지 이동 방식]
     * - GET /recruit/joblist 는 SSR로 1페이지만 반환, Page 파라미터를 무시함
     * - 페이지 이동은 POST /Recruit/Home/_GI_List/ + Page=N 으로만 가능
     *
     * [종료 조건]
     * - crawlPage()가 -1 반환 → 해당 페이지에 공고가 없음 = 마지막 페이지 도달
     * - crawlPage()가 0 반환 → 해당 페이지의 공고가 모두 DB에 이미 존재 = 중복 페이지
     *   (이미 수집된 데이터만 있으므로 이후 페이지도 마찬가지일 가능성이 높아 중단)
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

                // 0: 해당 페이지 공고가 모두 DB에 이미 존재 → 더 이상 수집할 신규 공고 없음
                if (saved == 0) {
                    log.info("[{}] {}페이지 모두 중복 → 수집 종료", getSiteName(), page);
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
     * GET 방식은 항상 1페이지만 반환하므로, AJAX 엔드포인트(POST)를 직접 호출함.
     * - POST_URL: /Recruit/Home/_GI_List/
     * - Body: menucode=duty&duty=...&Page=N
     * - X-Requested-With: XMLHttpRequest → AJAX 요청임을 서버에 알림 (없으면 차단될 수 있음)
     *
     * @return 저장된 공고 수 (공고가 없는 마지막 페이지면 -1)
     */
    private int crawlPage(int page) throws IOException {
        // Jsoup.connect().post() 대신 data() + post() 조합 사용
        // - .data(key, value): application/x-www-form-urlencoded 형식으로 body 전송
        // - Jsoup은 POST body를 key=value 쌍으로만 설정 가능하므로 requestBody() 대신 data() 사용
        Document doc = Jsoup.connect(POST_URL)
                .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
                        + "AppleWebKit/537.36 (KHTML, like Gecko) "
                        + "Chrome/124.0.0.0 Safari/537.36")
                .header("Accept-Language", "ko-KR,ko;q=0.9")
                // AJAX 요청임을 서버에 알리는 헤더 (없으면 일반 페이지 HTML을 반환할 수 있음)
                .header("X-Requested-With", "XMLHttpRequest")
                .data("menucode", "duty")
                .data("duty", "1000229,1000230,1000231")
                .data("Page", String.valueOf(page))
                .maxBodySize(0)
                .timeout(10_000)
                .post();

        // div.tplList.tplJobList 안의 tr.devloopArea: 일반공고만 선택 (강조공고 li.devloopArea 제외)
        Elements items = doc.select("div.tplList.tplJobList tr.devloopArea");

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
     * 잡코리아 공고 상세 내용 크롤링
     *
     * 잡코리아는 Next.js RSC 앱으로, 실제 공고 HTML이 AWS S3 presigned URL로 제공됨.
     * 1단계: 상세 페이지 HTML에서 RSC payload 안의 S3 URL 추출
     * 2단계: S3 HTML 파일 직접 GET → body 텍스트 반환
     *
     * @param sourceUrl 저장된 공고 URL (https://www.jobkorea.co.kr/Recruit/GI_Read/{id})
     * @return 공고 상세 텍스트 (파싱 실패 시 null)
     */
    public String fetchDescription(String sourceUrl) {
        try {
            Document mainDoc = Jsoup.connect(sourceUrl)
                    .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
                            + "AppleWebKit/537.36 (KHTML, like Gecko) "
                            + "Chrome/124.0.0.0 Safari/537.36")
                    .header("Accept-Language", "ko-KR,ko;q=0.9")
                    .maxBodySize(0)
                    .timeout(10_000)
                    .get();

            Matcher m = Pattern.compile("(https://job-hub-files[^\"]+_DESCRIPTION\\.html[^\"]+)")
                    .matcher(mainDoc.html());
            if (!m.find()) {
                log.warn("[잡코리아] DESCRIPTION S3 URL 없음: {}", sourceUrl);
                return null;
            }
            String s3Url = m.group(1).replace("\\u0026", "&");

            Document descDoc = Jsoup.connect(s3Url)
                    .userAgent("Mozilla/5.0")
                    .timeout(10_000)
                    .get();

            String text = descDoc.body().text().trim();
            return text.isEmpty() ? null : text;

        } catch (IOException e) {
            log.error("[잡코리아] 상세 크롤링 실패: url={}, error={}", sourceUrl, e.getMessage());
            return null;
        }
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
