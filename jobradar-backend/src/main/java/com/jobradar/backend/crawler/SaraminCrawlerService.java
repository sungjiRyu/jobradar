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
 * 사람인 채용공고 크롤러
 *
 * [크롤링 흐름]
 * 1. JOB_TYPE_CODES 직무 목록을 순회하며 직무별로 공고 수집
 * 2. Jsoup으로 사람인 공고 목록 페이지 HTML 요청 (cat_kewd 파라미터로 직무 지정)
 * 3. CSS 선택자로 공고 정보 추출 (제목, 회사명, 지역, 경력, 마감일, 게시일, URL)
 * 4. DB 중복 체크 → 이미 있는 공고면 스킵
 * 5. 한 페이지가 모두 중복이면 조기 종료 (이후 페이지도 마찬가지로 판단)
 * 6. 공고 제목에서 기술스택 키워드 파싱 후 저장
 *
 * [사람인 HTML 구조]
 * div.item_recruit
 *   h2.job_tit > a[href]           ← 공고 제목 + URL
 *   strong.corp_name > a           ← 회사명
 *   div.job_condition > span       ← 지역(0번), 경력(1번)
 *   div.job_date > span.date       ← 마감일 (예: "~ 05/10(목)")
 *   span.job_day                   ← 게시일 (예: "수정일 26/05/11")
 *
 * [직무 카테고리 코드 (cat_kewd)]
 * 84: 백엔드/서버 / 92: 프론트엔드 / 87: 풀스택 / 86: 모바일
 * 82,83,2248: 데이터엔지니어·분석가·사이언티스트
 * 181: AI(인공지능) / 146: DevOps
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SaraminCrawlerService implements CrawlerService {

    private final JobRepository jobRepository;
    private final TechStackRepository techStackRepository;

    static final String BASE_URL = "https://www.saramin.co.kr";

    // 사람인 공고 목록 URL 템플릿 (%s: cat_kewd 코드, %d: 페이지 번호)
    // String.format(LIST_URL_TEMPLATE, catKewd, page) 형태로 사용
    // %%2C는 String.format 처리 후 %2C (URL 인코딩된 쉼표)로 남음
    static final String LIST_URL_TEMPLATE = BASE_URL + "/zf_user/search?cat_kewd=%s"
            + "&company_cd=0%%2C1%%2C2%%2C3%%2C4%%2C5%%2C6%%2C7%%2C9%%2C10"
            + "&search_optional_item=y&search_done=y&panel_count=y&preview=y&recruitPage=%d";

    // 최대 페이지 수 (안전장치): 빈 페이지 또는 중복 감지 시 자동 중단
    static final int MAX_PAGES = 200;

    // 누적 중복 공고가 이 값을 초과하면 수집 종료 (광고 슬롯 등으로 신규가 섞여도 안전하게 처리)
    static final int MAX_DUPLICATE_COUNT = 20;

    // N페이지 연속으로 모두 중복이면 수집 종료 (광고 슬롯 등으로 중간에 신규가 끼는 경우 대비)
    static final int MAX_DUPLICATE_PAGES = 3;

    // 직무명 → 사람인 cat_kewd 매핑
    // 복수 코드(예: 데이터)는 URL 인코딩된 쉼표(%2C)로 구분
    static final Map<String, String> JOB_TYPE_CODES = Map.ofEntries(
            Map.entry("백엔드", "84"),
            Map.entry("프론트엔드", "92"),
            Map.entry("풀스택", "87"),
            Map.entry("모바일", "86"),
            Map.entry("데이터", "82%2C83%2C2248"),
            Map.entry("AI/ML", "181"),
            Map.entry("DevOps", "146")
    );

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
     * 직무별로 공고를 순회하며 전체 수집
     * JOB_TYPE_CODES에 정의된 직무 순서대로 각각 크롤링
     */
    @Override
    public void collect() {
        long startMs = System.currentTimeMillis();
        log.info("[{}] 크롤링 시작 (직무별 카테고리 수집)", getSiteName());
        int totalSaved = 0;

        for (Map.Entry<String, String> entry : JOB_TYPE_CODES.entrySet()) {
            totalSaved += collectByJobType(entry.getKey(), entry.getValue());
        }

        // 소요시간 출력 — eager description fetch까지 포함하므로 추적 중요
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
     *   (이미 수집된 데이터뿐이므로 이후 페이지도 마찬가지로 판단하여 중단)
     * - MAX_PAGES 초과 → 무한 루프 방지
     *
     * @param jobType  직무명 (예: "백엔드")
     * @param catKewd  사람인 cat_kewd 파라미터 값 (예: "84" 또는 "82%2C83%2C2248")
     */
    private int collectByJobType(String jobType, String catKewd) {
        log.info("[{}] {} 직무 수집 시작 (cat_kewd={})", getSiteName(), jobType, catKewd);
        int totalSaved = 0;
        int totalDuplicates = 0;

        for (int page = 1; page <= MAX_PAGES; page++) {
            try {
                int[] result = crawlPage(page, catKewd, jobType);
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
     * 특정 직무의 특정 페이지 공고 파싱 후 저장
     *
     * @param page     페이지 번호 (1부터 시작)
     * @param catKewd  사람인 cat_kewd 파라미터 값
     * @param jobType  직무명 (저장 시 Job.jobType 필드에 기록)
     * @return int[] { 저장 수, 중복 수 } — 저장 수가 -1이면 마지막 페이지
     */
    private int[] crawlPage(int page, String catKewd, String jobType) throws IOException {
        String url = String.format(LIST_URL_TEMPLATE, catKewd, page);

        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
                        + "AppleWebKit/537.36 (KHTML, like Gecko) "
                        + "Chrome/124.0.0.0 Safari/537.36")
                .header("Accept-Language", "ko-KR,ko;q=0.9")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .maxBodySize(0)
                .timeout(10_000)
                .get();

        Elements items = doc.select("div.item_recruit");

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
     * → descriptionStatus = null로 저장됨
     *
     * @param item    div.item_recruit 엘리먼트
     * @param jobType 직무명 (Job 엔티티에 저장)
     * @return true: 저장됨 / false: 중복으로 스킵
     */
    private boolean parseAndSave(Element item, String jobType) {
        Element titleEl = item.selectFirst("h2.job_tit a");
        if (titleEl == null) return false;

        String title = titleEl.text().trim();

        // rec_idx만 추출해 정규화 — 추적/세션 파라미터 제거로 existsBySourceUrl 중복 체크 신뢰성 확보
        // href 예: /zf_user/jobs/relay/view?view_type=search&rec_idx=53603997&...
        String href = titleEl.attr("href");
        Matcher recIdxMatcher = Pattern.compile("[?&]rec_idx=(\\d+)").matcher(href);
        String sourceUrl = recIdxMatcher.find()
                ? BASE_URL + "/zf_user/jobs/relay/view?rec_idx=" + recIdxMatcher.group(1)
                : BASE_URL + href;

        // 중복 체크를 먼저 — 이미 있으면 description fetch 자체를 스킵해 부하 감소
        if (jobRepository.existsBySourceUrl(sourceUrl)) {
            return false;
        }

        Element corpEl = item.selectFirst("strong.corp_name a");
        String company = (corpEl != null) ? corpEl.text().trim() : "미기재";

        Elements condSpans = item.select("div.job_condition span");
        String location   = (condSpans.size() >= 1) ? condSpans.get(0).text().trim() : "";
        String experience = (condSpans.size() >= 2) ? condSpans.get(1).text().trim() : "";

        Element dateEl = item.selectFirst("div.job_date span.date");
        String deadlineText = (dateEl != null) ? dateEl.text().trim() : "";

        // div.job_condition span 순서: [지역, 경력, 학력, 고용형태]
        // 지역/경력/학력은 모든 공고의 필수 항목이므로 고용형태는 항상 index 3에 위치
        String employmentType = (condSpans.size() >= 4) ? condSpans.get(3).text().trim() : null;

        LocalDate listedAt = parseListedAt(item);

        return saveJob(title, company, location, experience, deadlineText, sourceUrl,
                jobType, listedAt, employmentType);
    }

    /**
     * 공고 저장 (중복 체크 후 DB INSERT)
     *
     * @return true: 저장됨 / false: 중복으로 스킵
     */
    boolean saveJob(String title, String company, String location,
                    String experience, String deadlineText, String sourceUrl,
                    String jobType, LocalDate listedAt, String employmentType) {
        // 중복 체크는 parseAndSave에서 이미 수행했으나, 단위 테스트가 saveJob을 직접 호출하므로 안전망으로 한 번 더 검사
        if (jobRepository.existsBySourceUrl(sourceUrl)) {
            return false;
        }

        LocalDate deadline = parseDeadline(deadlineText);
        Job.DeadlineType deadlineType = resolveDeadlineType(deadlineText);
        List<TechStack> techStacks = resolveTechStacks(title);

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
                .build();

        job.getTechStacks().addAll(techStacks);
        jobRepository.save(job);
        return true;
    }

    /**
     * 게시일 파싱
     * 사람인 형식: "수정일 26/05/11" 또는 "등록일 26/05/11" (YY/MM/DD)
     */
    LocalDate parseListedAt(Element item) {
        Element dayEl = item.selectFirst("span.job_day");
        if (dayEl == null) return null;

        // "수정일 26/05/11" → 그룹1=26(년), 그룹2=05(월), 그룹3=11(일)
        Matcher m = Pattern.compile("(\\d{2})/(\\d{2})/(\\d{2})").matcher(dayEl.text());
        if (m.find()) {
            int year  = Integer.parseInt(m.group(1));
            int month = Integer.parseInt(m.group(2));
            int day   = Integer.parseInt(m.group(3));
            return LocalDate.of(2000 + year, month, day);
        }
        return null;
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
     * 사람인 공고 상세 내용 크롤링
     * sourceUrl에서 rec_idx 추출 → view-detail GET 요청 → div.user_content 텍스트 반환
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
