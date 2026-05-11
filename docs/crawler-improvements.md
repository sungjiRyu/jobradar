# 크롤러 로직 개선 계획

## 1. 배경 및 목적

현재 "등록일순" 정렬은 `created_at`(크롤러가 DB에 저장한 시각) 기준이라,  
같은 배치에서 수집된 공고들은 모두 `created_at`이 거의 동일해 의미 있는 정렬이 불가능하다.

→ 각 사이트에서 직접 날짜를 파싱해 `listed_at` 컬럼에 저장한다.

---

## 2. 필드 정의

| 항목 | 내용 |
|------|------|
| 필드명 | `listedAt` (DB 컬럼: `listed_at`) |
| 타입 | `LocalDate` |
| 의미 | 원본 사이트(사람인/잡코리아)에서 목록에 표시된 날짜 (등록일 또는 수정일) |
| null 여부 | nullable (기존 데이터, 파싱 실패 시 null) |
| 정렬 방식 | `listedAt DESC NULLS LAST, createdAt DESC` |

> 사람인은 수정 시 등록일 자리에 수정일이 표시된다.  
> 따라서 listedAt은 "이 공고가 목록에서 마지막으로 활성화된 날짜"를 의미한다.

---

## 3. 사람인 파싱

### HTML 구조
```html
<span class="job_day">등록일 26/05/11</span>
<span class="job_day">수정일 26/05/11</span>
```

### selector
```java
Element listedAtEl = item.selectFirst("span.job_day");
String listedAtText = (listedAtEl != null) ? listedAtEl.text().trim() : "";
```

### 파싱 로직
- 형식: `(등록일|수정일) YY/MM/DD`
- 연도: 2자리 → `2000 + YY`

```java
LocalDate parseListedAt(String text) {
    if (text == null || text.isBlank()) return null;
    // "등록일 26/05/11" 또는 "수정일 26/05/11"
    Matcher m = Pattern.compile("(\\d{2})/(\\d{2})/(\\d{2})").matcher(text);
    if (m.find()) {
        int year  = 2000 + Integer.parseInt(m.group(1));
        int month = Integer.parseInt(m.group(2));
        int day   = Integer.parseInt(m.group(3));
        return LocalDate.of(year, month, day);
    }
    return null;
}
```

---

## 4. 잡코리아 파싱

### HTML 구조
```html
<!-- _GI_List/ POST 엔드포인트 응답 HTML -->
<td class="odd">
  <span class="time dotum">3시간 전 등록</span>   ← 등록 시점 (상대 시간)
  <span class="date dotum">~06/10(수)</span>       ← 마감일
</td>
```

> 브라우저에서 보이는 `text-gray700 text-typo-c1-13` 클래스와 다르다.  
> 크롤러는 `POST /Recruit/Home/_GI_List/` 응답(서버사이드 HTML 조각)을 파싱하므로  
> 전통 CSS 클래스명(`td.odd span.time`)을 사용한다.

### selector
```java
Element listedAtEl = item.selectFirst("td.odd span.time");
String listedAtText = (listedAtEl != null) ? listedAtEl.text().trim() : "";
```

### 파싱 로직
- 형식: `N시간 전 등록` 또는 `N일 전 등록`
- `LocalDateTime.now().minusHours(N).toLocalDate()` 으로 역산

```java
LocalDate parseListedAt(String text) {
    if (text == null || text.isBlank()) return null;

    // "N시간 전" → 현재 시각에서 N시간 역산
    Matcher hourMatcher = Pattern.compile("(\\d+)시간 전").matcher(text);
    if (hourMatcher.find()) {
        long hours = Long.parseLong(hourMatcher.group(1));
        return LocalDateTime.now().minusHours(hours).toLocalDate();
    }

    // "N일 전" → 오늘에서 N일 역산
    Matcher dayMatcher = Pattern.compile("(\\d+)일 전").matcher(text);
    if (dayMatcher.find()) {
        long days = Long.parseLong(dayMatcher.group(1));
        return LocalDate.now().minusDays(days);
    }

    // "방금" 등 → 오늘
    return LocalDate.now();
}
```

### 정확도 한계
- `"N시간 전"`은 정수로 내림(floor)된 값
- 자정 직전(23:50~00:10)에 등록된 공고는 하루 오차 가능
- 크롤링 시간으로 해결 불가 — 근본적으로 상대 시간 포맷의 한계

---

## 5. 수정할 파일

### 백엔드

| 파일 | 변경 내용 |
|------|-----------|
| `Job.java` | `listedAt` 필드 추가, Builder에 포함 |
| `SaraminCrawlerService.java` | `parseListedAt()` 추가, `saveJob()` 파라미터 추가 |
| `JobkoreaCrawlerService.java` | `parseListedAt()` 추가, `saveJob()` 파라미터 추가 |
| `JobController.java` | `listedAt` 정렬 시 `nullsLast` + 2차 정렬 `createdAt DESC` 적용 |

### 프론트엔드

| 파일 | 변경 내용 |
|------|-----------|
| `SearchFilter.tsx` | `SORT_OPTIONS`에서 "등록일순" 값을 `listedAt,DESC`로 변경 |

---

## 6. 정렬 처리 (JobController)

`listedAt` 정렬 요청 시:
1. `listedAt DESC NULLS LAST` — null 공고(기존 데이터)는 맨 뒤
2. 2차 정렬 `createdAt DESC` — 같은 날짜 공고끼리는 DB 삽입 순서로 정렬

```java
if (pageable.getSort().getOrderFor("listedAt") != null) {
    Sort sort = Sort.by(
        Sort.Order.desc("listedAt").nullsLast(),
        Sort.Order.desc("createdAt")
    );
    pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
}
```

---

## 7. 기존 데이터 처리

기존 `job_posts` 레코드는 `listed_at = NULL`로 유지된다.  
`NULLS LAST` 정렬로 인해 새로 수집된 공고가 상단, 기존 데이터가 하단에 위치한다.  
재크롤링 없이 점진적으로 데이터가 채워지는 구조.

---

## 8. 공고 상세내용 수집 시점 변경 (선택 작업)

### 현재 방식 (lazy)
사용자가 공고 상세 페이지를 **처음 열 때** `fetchDescription()`을 호출해 크롤링한다.
- 장점: 크롤링 속도 빠름
- 단점: 첫 조회 시 사용자가 로딩 대기

### 변경 방식 (eager — 수집 시 함께)
크롤러가 신규 공고를 저장할 때 `fetchDescription()`을 함께 호출한다.
- 장점: 상세 페이지 첫 조회 딜레이 없음
- 단점: 크롤링 시간 증가

### 추가 요청 수

| 사이트 | 신규 공고 1건당 추가 요청 |
|--------|--------------------------|
| 사람인 | 1회 (`view-detail` 엔드포인트) |
| 잡코리아 | 2회 (상세 페이지 → S3 URL 추출) |

### 크롤링 시간 영향 (미측정)

> **현재 크롤러 실행 시간을 먼저 측정해야 정확한 배수를 알 수 있다.**

측정 방법 — `CrawlerScheduler`에 임시 로그 추가:
```java
long start = System.currentTimeMillis();
crawler.collect();
long elapsed = (System.currentTimeMillis() - start) / 1000;
log.info("[{}] 수집 시간: {}초", crawler.getSiteName(), elapsed);
```

이론적 추정 (일 신규 공고 100건 기준):
```
사람인:  100건 × ~1초 = +100초
잡코리아: 100건 × ~2초 = +200초
합계: 약 +5분 추가
```

초기 전체 수집(1700+ 공고) 기준으로는 수십 분 이상 추가된다.

### 코드 변경량

두 크롤러의 `saveJob()` 메서드에 각각 ~5줄 추가:

```java
// saveJob() 내부, jobRepository.save(job) 직후
DescriptionResponse desc = fetchDescription(sourceUrl);
if (desc.getStatus() == DescriptionResponse.Status.SUCCESS) {
    job.updateDescription(desc.getContent());
}
```

수정 파일: `SaraminCrawlerService.java`, `JobkoreaCrawlerService.java` 각 1곳씩
