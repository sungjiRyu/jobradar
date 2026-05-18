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
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 잡코리아 채용공고 크롤러
 *
 * [크롤링 흐름]
 * 1. JOB_TYPE_DUTIES 직무 목록을 순회하며 직무별로 공고 수집
 * 2. Jsoup POST /Recruit/Home/_GI_List/ (AJAX 엔드포인트)으로 공고 목록 HTML 요청
 *    - duty 파라미터로 직무 지정 (잡코리아 직종 코드)
 *    - 브라우저 JavaScript가 사용하는 POST 방식이므로 GET 방식 불가
 * 3. CSS 선택자로 공고 정보 추출 (제목, 회사명, 지역, 경력, 마감일, 게시일, URL)
 * 4. DB 중복 체크 → 이미 있는 공고면 스킵
 * 5. 한 페이지가 모두 중복이면 조기 종료
 *
 * [잡코리아 HTML 구조]
 * tr.devloopArea
 *   td.tplCo > a.link                         ← 회사명
 *   td.tplTit > strong > a[href]              ← 공고 제목 + URL
 *   td.tplTit > p.etc > span.cell[0]          ← 경력 (예: "신입·경력")
 *   td.tplTit > p.etc > span.cell[2]          ← 지역 (예: "서울 강남구")
 *   td.odd > span.date                        ← 마감일 (예: "~05/10(일)")
 *   td.odd > span.time                        ← 게시일 (예: "3시간 전 등록", "1일 전 등록")
 *
 * [직무 코드 (duty)]
 * 1000229: 백엔드 / 1000230: 프론트엔드 / 1000231: 웹개발(풀스택)
 * 1000232: 모바일 / 1000236,1000237,1000418: 데이터
 * 1000242,1000417: AI/ML
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobkoreaCrawlerService implements CrawlerService {

    private final JobRepository jobRepository;
    private final TechStackRepository techStackRepository;

    static final String BASE_URL = "https://www.jobkorea.co.kr";

    // 잡코리아 공고 목록 AJAX 엔드포인트
    // GET /recruit/joblist 는 SSR로 1페이지만 반환하며 Page 파라미터를 무시함
    // 브라우저 페이지 이동 시 JavaScript가 이 POST URL을 호출하므로 직접 POST해야 함
    static final String POST_URL = BASE_URL + "/Recruit/Home/_GI_List/";

    // 최대 페이지 수 (안전장치)
    static final int MAX_PAGES = 200;

    // 누적 중복 공고가 이 값을 초과하면 수집 종료
    static final int MAX_DUPLICATE_COUNT = 20;

    // 직무명 → 잡코리아 duty 코드 매핑
    // 복수 코드(예: 데이터)는 쉼표로 구분
    // 참고: 잡코리아는 DevOps 카테고리를 별도로 제공하지 않으므로 지원 직무에서 제외
    static final Map<String, String> JOB_TYPE_DUTIES = Map.ofEntries(
            Map.entry("백엔드", "1000229"),
            Map.entry("프론트엔드", "1000230"),
            Map.entry("풀스택", "1000231"),
            Map.entry("모바일", "1000232"),
            Map.entry("데이터", "1000236,1000237,1000418"),
            Map.entry("AI/ML", "1000242,1000417")
    );

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
     * 직무별로 공고를 순회하며 전체 수집
     */
    @Override
    public void collect() {
        long startMs = System.currentTimeMillis();
        log.info("[{}] 크롤링 시작 (직무별 카테고리 수집)", getSiteName());
        int totalSaved = 0;

        for (Map.Entry<String, String> entry : JOB_TYPE_DUTIES.entrySet()) {
            totalSaved += collectByJobType(entry.getKey(), entry.getValue());
        }

        // 소요시간 출력 — 잡코리아는 description fetch가 2-step(main + S3)이라 더 무거움
        long elapsedSec = (System.currentTimeMillis() - startMs) / 1000;
        log.info("[{}] 크롤링 완료 - 총 {}개 저장 (소요시간: {}분 {}초)",
                getSiteName(), totalSaved, elapsedSec / 60, elapsedSec % 60);
    }

    /**
     * 특정 직무의 공고를 마지막 페이지까지 수집
     *
     * [종료 조건]
     * - crawlPage()가 -1 반환 → 공고 없음 = 마지막 페이지 도달
     * - crawlPage()가 0 반환 → 해당 페이지 공고가 모두 DB에 이미 존재 → 조기 종료
     * - MAX_PAGES 초과 → 무한 루프 방지
     *
     * @param jobType  직무명 (예: "백엔드")
     * @param duty     잡코리아 duty 파라미터 값 (예: "1000229")
     */
    private int collectByJobType(String jobType, String duty) {
        log.info("[{}] {} 직무 수집 시작 (duty={})", getSiteName(), jobType, duty);
        int totalSaved = 0;
        int totalDuplicates = 0;

        for (int page = 1; page <= MAX_PAGES; page++) {
            try {
                int[] result = crawlPage(page, duty, jobType);
                int saved = result[0];
                int duplicates = result[1];

                if (saved == -1) {
                    log.info("[{}] {} {}페이지에서 공고 없음 → 수집 완료", getSiteName(), jobType, page);
                    break;
                }

                totalDuplicates += duplicates;
                if (totalDuplicates > MAX_DUPLICATE_COUNT) {
                    log.info("[{}] {} 누적 중복 {}개 초과 → 수집 종료", getSiteName(), jobType, totalDuplicates);
                    break;
                }

                totalSaved += saved;
                log.info("[{}] {} {}페이지 완료 - {}개 저장 (누적 중복 {}개)",
                        getSiteName(), jobType, page, saved, totalDuplicates);

                // 서버 부하 방지: 페이지 간 2~4초 랜덤 대기 (규칙적 패턴 회피)
                Thread.sleep(ThreadLocalRandom.current().nextLong(2000, 4000));
            } catch (IOException e) {
                log.error("[{}] {} {}페이지 요청 실패: {}", getSiteName(), jobType, page, e.getMessage());
                break;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        return totalSaved;
    }

    /**
     * 특정 직무의 특정 페이지에서 공고 목록 파싱 후 저장
     *
     * GET 방식은 항상 1페이지만 반환하므로, AJAX 엔드포인트(POST)를 직접 호출함.
     * - POST_URL: /Recruit/Home/_GI_List/
     * - Body: menucode=duty&duty=...&Page=N
     * - X-Requested-With: XMLHttpRequest → AJAX 요청임을 서버에 알림
     *
     * @param page     페이지 번호
     * @param duty     잡코리아 duty 파라미터 값
     * @param jobType  직무명 (Job.jobType 필드에 저장)
     * @return int[] { 저장 수, 중복 수 } — 저장 수가 -1이면 마지막 페이지
     */
    private int[] crawlPage(int page, String duty, String jobType) throws IOException {
        Document doc = Jsoup.connect(POST_URL)
                .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
                        + "AppleWebKit/537.36 (KHTML, like Gecko) "
                        + "Chrome/124.0.0.0 Safari/537.36")
                .header("Accept-Language", "ko-KR,ko;q=0.9")
                .header("X-Requested-With", "XMLHttpRequest")
                .data("menucode", "duty")
                .data("duty", duty)
                .data("Page", String.valueOf(page))
                .maxBodySize(0)
                .timeout(10_000)
                .post();

        // div.tplList.tplJobList 안의 tr.devloopArea: 일반공고만 선택 (강조공고 li.devloopArea 제외)
        Elements items = doc.select("div.tplList.tplJobList tr.devloopArea");

        if (items.isEmpty()) {
            log.info("[{}] {}페이지 공고 없음 → 마지막 페이지 도달", getSiteName(), page);
            return new int[]{-1, 0};
        }

        int savedCount = 0;
        for (Element item : items) {
            if (parseAndSave(item, jobType)) {
                savedCount++;
            }
        }
        return new int[]{savedCount, items.size() - savedCount};
    }

    /**
     * 공고 HTML 항목에서 데이터 추출 후 저장
     *
     * description fetch는 크롤링 시점에 하지 않음 (lazy fetch 방식).
     * 사용자가 공고 상세 페이지에 처음 진입할 때 fetchDescription()을 호출해 저장.
     * 외부 공고(알바몬·고용24)는 예외로 크롤링 시점에 EXTERNAL 상태를 확정.
     * → 일반 공고 descriptionStatus = null, 외부 공고 = EXTERNAL로 저장됨
     *
     * @param item    tr.devloopArea 엘리먼트
     * @param jobType 직무명 (Job 엔티티에 저장)
     * @return true: 저장됨 / false: 중복으로 스킵
     */
    private boolean parseAndSave(Element item, String jobType) {
        Element titleEl = item.selectFirst("td.tplTit strong a");
        if (titleEl == null) return false;

        String title = titleEl.text().trim();
        // href 예시: /Recruit/GI_Read/48998248?rPageCode=PL&...
        // 알바몬 등 외부 광고는 절대 URL, /Ext는 고용24 임베드 공고 → EXTERNAL 상태로 저장
        String href = titleEl.attr("href").replaceAll("\\?.*", "");
        boolean isExternal = !href.startsWith("/") || href.endsWith("/Ext");
        String sourceUrl = href.startsWith("/") ? BASE_URL + href : href;

        // 중복 체크 먼저 — 이미 있으면 스킵
        if (jobRepository.existsBySourceUrl(sourceUrl)) {
            return false;
        }

        Element corpEl = item.selectFirst("td.tplCo a.link");
        String company = (corpEl != null) ? corpEl.text().trim() : "미기재";

        // p.etc 안의 span.cell 목록: [경력, 학력, 지역, 고용형태, 연봉]
        Elements cells = item.select("td.tplTit p.etc span.cell");
        String experience = cells.size() > 0 ? cells.get(0).text().trim() : "";
        String location   = cells.size() > 2 ? cells.get(2).text().trim() : "";

        Element dateEl = item.selectFirst("td.odd span.date");
        String deadlineText = (dateEl != null) ? dateEl.text().trim() : "";

        // p.etc span.cell 순서: [경력, 학력, 지역, 고용형태, 연봉, 직급]
        // 경력/학력/지역은 모든 공고의 필수 항목이므로 고용형태는 항상 index 3에 위치
        String employmentType = (cells.size() > 3) ? cells.get(3).text().trim() : null;

        // 게시일 파싱: "3시간 전 등록", "1일 전 등록" 등 상대적 표현
        LocalDate listedAt = parseListedAt(item);

        return saveJob(title, company, location, experience, deadlineText, sourceUrl,
                jobType, listedAt, employmentType, isExternal);
    }

    /**
     * 공고 저장 (중복 체크 후 DB INSERT)
     *
     * @return true: 저장됨 / false: 중복으로 스킵
     */
    boolean saveJob(String title, String company, String location,
                    String experience, String deadlineText, String sourceUrl,
                    String jobType, LocalDate listedAt, String employmentType,
                    boolean isExternal) {
        // 중복 체크는 parseAndSave에서 이미 수행했으나, 단위 테스트가 saveJob을 직접 호출하므로 안전망
        if (jobRepository.existsBySourceUrl(sourceUrl)) {
            return false;
        }

        LocalDate deadline = parseDeadline(deadlineText);
        Job.DeadlineType deadlineType = resolveDeadlineType(deadlineText);
        List<TechStack> techStacks = resolveTechStacks(title);

        // 외부 공고(알바몬/고용24)는 EXTERNAL 상태로 저장, 일반 공고는 null (lazy fetch 대상)
        Job.DescriptionStatus descriptionStatus = isExternal ? Job.DescriptionStatus.EXTERNAL : null;

        Job job = Job.builder()
                .title(title)
                .company(company)
                .location(location.isBlank() ? "미기재" : location)
                .experienceLevel(experience)
                .employmentType(employmentType)
                .deadline(deadline)
                .deadlineType(deadlineType)
                .sourceUrl(sourceUrl)
                .sourceSite(getSiteName())
                .jobType(jobType)
                .listedAt(listedAt)
                .descriptionStatus(descriptionStatus)
                .build();

        job.getTechStacks().addAll(techStacks);
        jobRepository.save(job);
        return true;
    }

    /**
     * 게시일 파싱
     * 잡코리아 형식: "3시간 전 등록", "1일 전 등록", "방금 전 등록"
     * 상대적 시간 표현이므로 오늘 또는 N일 전으로 변환
     */
    LocalDate parseListedAt(Element item) {
        Element timeEl = item.selectFirst("td.odd span.time");
        if (timeEl == null) return null;

        String text = timeEl.text().trim();

        // "N시간 전" → 오늘 날짜 (같은 날 등록)
        Matcher hourMatcher = Pattern.compile("(\\d+)시간 전").matcher(text);
        if (hourMatcher.find()) {
            return LocalDate.now();
        }

        // "N일 전" → N일 전 날짜
        Matcher dayMatcher = Pattern.compile("(\\d+)일 전").matcher(text);
        if (dayMatcher.find()) {
            return LocalDate.now().minusDays(Integer.parseInt(dayMatcher.group(1)));
        }

        // "방금 전" 또는 "오늘" → 오늘 날짜
        if (text.contains("방금") || text.contains("오늘")) {
            return LocalDate.now();
        }

        return null;
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

        // (\d{1,2}): 한 자리(~5/3) 및 두 자리(~05/03) 모두 매칭
        Matcher matcher = Pattern.compile("(\\d{1,2})/(\\d{1,2})").matcher(text);
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
     * 마감일 텍스트 → DeadlineType 변환
     * parseDeadline()과 동일한 기준으로 판단
     */
    Job.DeadlineType resolveDeadlineType(String text) {
        if (text == null || text.isBlank() || text.contains("내일") || text.contains("오늘")) {
            return Job.DeadlineType.UNKNOWN;
        }
        if (text.contains("채용시") || text.contains("상시")) {
            return Job.DeadlineType.ALWAYS;
        }
        Matcher matcher = Pattern.compile("(\\d{1,2})/(\\d{1,2})").matcher(text);
        if (matcher.find()) {
            return Job.DeadlineType.FIXED;
        }
        return Job.DeadlineType.UNKNOWN;
    }

    /**
     * 잡코리아 공고 상세 내용 크롤링
     *
     * 잡코리아는 Next.js RSC 앱으로, 실제 공고 HTML이 AWS S3 presigned URL로 제공됨.
     * 1단계: 상세 페이지 HTML에서 RSC payload 안의 S3 URL 추출
     * 2단계: S3 HTML 파일 직접 GET → body 텍스트 반환
     */
    public DescriptionResponse fetchDescription(String sourceUrl) {
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
                return DescriptionResponse.crawlFailed();
            }
            String s3Url = m.group(1).replace("\\u0026", "&");

            Document descDoc = Jsoup.connect(s3Url)
                    .userAgent("Mozilla/5.0")
                    .timeout(10_000)
                    .get();

            String text = descDoc.body().text().trim();
            if (text.isEmpty()) {
                boolean hasImage = !descDoc.body().select("img").isEmpty();
                return hasImage ? DescriptionResponse.image() : DescriptionResponse.crawlFailed();
            }
            return DescriptionResponse.success(text);

        } catch (IOException e) {
            log.error("[잡코리아] 상세 크롤링 실패: url={}, error={}", sourceUrl, e.getMessage());
            return DescriptionResponse.crawlFailed();
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
