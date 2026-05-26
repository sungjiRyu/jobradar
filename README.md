 # JobRadar 🎯

> 개발자 취업준비생을 위한 채용공고 통합 검색 및 분석 대시보드

사람인, 잡코리아를 따로 방문해 공고를 검색하는 번거로움을 느끼고 
두 플랫폼의 공고를 한 곳에서 검색하고, 채용 트렌드를 데이터로 분석할 수 있는 서비스를 기획·개발 했습니다.

**🌐 https://jobradar.me**

테스트 계정: `test@jobradar.com` / `test1234`

<br>

<details>
 <summary> 📋 Version History</summary>
 
### v1.0.1 (2026-05-24)
- favicon 디자인 변경 및 브라우저/기기별 대응 추가
  - SVG, ICO, PNG 멀티포맷 적용
  - iOS 홈 화면, Android PWA 아이콘 지원
  - site.webmanifest 설정 (theme_color: #378ADD)

### v1.0.0 (2026-05-23)
- 첫번째 릴리즈
</details>

<details>
 <summary>🛠 사용 기술</summary>
 
### Backend
![Java](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.5-6DB33F?logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-7-6DB33F?logo=springsecurity&logoColor=white)
![JPA](https://img.shields.io/badge/Spring_Data_JPA-6DB33F?logo=spring&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-0.12.6-000000?logo=jsonwebtokens&logoColor=white)
![Jsoup](https://img.shields.io/badge/Jsoup-1.17.2-3776AB)

### Frontend
![React](https://img.shields.io/badge/React-19-61DAFB?logo=react&logoColor=white)
![TypeScript](https://img.shields.io/badge/TypeScript-3178C6?logo=typescript&logoColor=white)
![Vite](https://img.shields.io/badge/Vite-646CFF?logo=vite&logoColor=white)
![TailwindCSS](https://img.shields.io/badge/Tailwind_CSS-3-06B6D4?logo=tailwindcss&logoColor=white)
![Zustand](https://img.shields.io/badge/Zustand-433E38)
![Chart.js](https://img.shields.io/badge/Chart.js-FF6384?logo=chartdotjs&logoColor=white)

### Infrastructure
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7.0-DC382D?logo=redis&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?logo=docker&logoColor=white)
![AWS EC2](https://img.shields.io/badge/AWS_EC2-FF9900?logo=amazonec2&logoColor=white)
![AWS RDS](https://img.shields.io/badge/AWS_RDS-527FFF?logo=amazonrds&logoColor=white)
![AWS S3](https://img.shields.io/badge/AWS_S3-569A31?logo=amazons3&logoColor=white)
![CloudFront](https://img.shields.io/badge/CloudFront-FF9900?logo=amazoncloudfront&logoColor=white)
[![Review with CodeRabbit](https://img.shields.io/badge/Review_with-CodeRabbit-brightgreen?style=flat&logo=coderabbit)](https://coderabbit.ai)

</details>

<details>
 <summary>📸 주요 기능</summary>

| 통합검색 | AI공고 요약 | 채용 트렌드 | 스크랩 |
| :---: | :---: | :---: | :---: |
| <img width="300" src="https://github.com/user-attachments/assets/56b38724-b014-4fab-8309-3e879f46b2a4" /> | <img width="300" src="https://github.com/user-attachments/assets/8c1617c8-14e9-4f9b-b1be-2449cc9e3f15" /> | <img width="300" src="https://github.com/user-attachments/assets/4ab302b9-7ec6-412b-964f-5c9c9500145f" /> | <img width="300" src="https://github.com/user-attachments/assets/d10f85ae-4015-4aee-a210-4a5e3a5f4181" /> |



### 1. 채용공고 통합 검색
- 사람인·잡코리아 개발 직군 공고를 한 곳에서 검색 (총 40,000건+, 매일 업데이트)
- 직무 / 지역 / 경력 / 기술스택 복합 필터링
- 키워드 검색 + 드롭다운 멀티필터

### 2. AI 채용공고 요약
- 긴 공고 본문을 핵심만 추려 한눈에 파악
- 주요 업무, 자격 요건, 우대 사항을 카테고리별로 분류

### 3. 채용 트렌드 대시보드
- 전체 / 신규 / 마감 임박 / 신입 공고 실시간 통계
- 기술스택 수요 순위 (상위 8개 기술스택)
- 지역별 채용 비중 (상위 10개 지역)
- 경력별 공고 분포 (신입 / 1\~3년 / 3\~5년 / 5년+)

### 4. 스크랩 + 지원 현황 관리
- 관심 공고 스크랩
- 4단계 지원 상태 관리: 지원예정 → 지원완료 → 서류검토 → 결과
- 상태별 필터링으로 지원 현황 한눈에 파악

</details>

<details>
<summary>🗂 프로젝트 구조</summary>

### Backend
```
jobradar-backend/
├── src/main/java/com/jobradar/backend/
│   ├── auth/              # 인증 (로그인, 로그아웃, 토큰 재발급)
│   │   ├── controller/
│   │   ├── service/
│   │   └── dto/
│   ├── user/              # 회원
│   ├── job/               # 채용공고
│   ├── scrap/             # 스크랩
│   ├── stats/             # 대시보드 통계
│   ├── crawler/           # 크롤러 (Saramin API + Jobkorea Jsoup)
│   └── global/            # 공통 (Security, Redis, Exception)
└── src/test/              # 단위 테스트
```

### Frontend
```
jobradar-frontend/
├── src/
│   ├── pages/             # 페이지 (JobList, Dashboard, MyPage 등)
│   ├── components/        # 재사용 컴포넌트
│   │   ├── common/        # PrivateRoute 등
│   │   ├── job/           # JobCard, JobFilter, SearchBar
│   │   └── layout/        # Navbar, Sidebar
│   ├── api/               # API 함수 (authApi, jobApi, scrapApi 등)
│   ├── hooks/             # 커스텀 훅 (useAuth)
│   ├── store/             # Zustand (authStore)
│   └── router/            # 라우터 설정
```

</details>

<details>
 <summary>🗄 ERD</summary>

```
┌──────────────┐         ┌─────────────────┐
│    users     │         │   job_posts     │
├──────────────┤         ├─────────────────┤
│ id (PK)      │         │ id (PK)         │
│ email (UK)   │         │ title           │
│ password     │         │ company         │
│ nickname     │         │ location        │
│ role         │         │ experience      │
└──────┬───────┘         │ deadline        │
       │ 1:N             │ url             │
       │                 │ view_count      │
       ▼                 │ created_at      │
┌──────────────┐         └─────────┬───────┘
│    scraps    │                   │ 1:N
├──────────────┤                   ▼
│ id (PK)      │         ┌─────────────────┐
│ user_id (FK) │←────────│ job_post_stacks │
│ job_post_id  │         ├─────────────────┤
│ status (EN)  │         │ id (PK)         │
│ created_at   │         │ job_post_id(FK) │
└──────────────┘         │ tech_stack_id   │
                         └────────┬────────┘
                                  │ N:1
                                  ▼
                         ┌─────────────────┐
                         │  tech_stacks    │
                         ├─────────────────┤
                         │ id (PK)         │
                         │ name (UK)       │
                         └─────────────────┘
```
</details>

<details>
 <summary>🏗 아키텍처</summary>

[![Architecture](/jobRadar_arch.svg)](/jobRadar_arch.svg)

</details>

<br>

## 📌 Description
1. [로그인 방식 결정(Session vs JWT)](#1-로그인-방식-결정session-vs-jwt)
2. [데이터 수집 방식(웹크롤링 VS 공식 API)](#2-데이터-수집-방식웹크롤링-vs-공식-api)
3. [인덱스를 통한 쿼리 성능 개선](#3-인덱스를-통한-쿼리-성능-개선)
<br>
<!-- 
<details>
 <summary>기술적 의사결정</summary>
</details>

<details>
 <summary>트러블 슈팅</summary>
</details>
--!>


<br>



## 💡 의사결정 과정

### 1. 로그인 방식 결정(Session vs JWT)

#### 세션과 JWT 비교
* 세션 방식은 Stateful한 방식으로 서버가 사용자 정보를 직접 저장합니다. 따라서 사용자가 늘어날수록 서버의 자원 부하가 증가한다는 단점이 있습니다. 또한, 추후 Scale-out 시 여러 서버 간 세션을 공유하기 위해 Redis 등을 활용한 세션 클러스터링 구축이 필요하므로, 아키텍처의 확장성 측면에서 제약이 생깁니다.
* 반면에, JWT 방식은 Stateless한 방식으로 서버가 사용자의 정보를 저장하지 않기 때문에 확장성 측면에서 장점이 있습니다. 다만, 토큰이 탈취된다면 만료되기 전까지 서버측에서 무효화 할 방법이 없다는 단점이 있습니다. 

#### JWT 채택과 이유
아래와 같은 사항을 고려해서 JWT를 채택했습니다.
* 추후 확장성과 Ec2 서버 부하 감소
* 크로스도메인 환경을 고려했을때 JWT가 구현에 용이
* RefreshToken과 Redis를 사용해서 보안 취약점을 보완

<br>

### 2. 데이터 수집 방식(웹크롤링 VS 공식 API)

#### 문제
성능과 안정성을 고려하여 공식 API를 사용하려 했으나, 잡코리아는 개인 대상 API를 미지원하며, 사람인은 발급 신청이 승인되지 않아 부득이하게 웹 크롤링 방식을 채택하였습니다

**웹 크롤링 방식(jsoup vs Playwright)**
* 정적 웹페이지 크롤링(jsoup) 방식은 서버에서 정적인 HTML받아서 데이터를 파싱합니다. 단순 HTML문서를 받아서 파싱하기 때문에 속도가 빠르고 리소스 소모가 적습니다.
* 동적 웹페이지 크롤링(Playwright)은 실제로 브라우저를 구동하여 JavaScript 렌더링이 완료된 후의 데이터를 수집합니다. 화면 클릭, 스크롤 등 사용자 상호작용이 필요한 동적 웹사이트 수집이 가능하지만 리소스 소모가 크고 상대적으로 속도가 느립니다.


**jsoup 채택과 이유**
* 네트워크 탭을 분석한 결과, 사람인은 SSR(서버 사이드 렌더링) 방식으로 HTML을 응답하고 있었으며, 잡코리아는 SSR과 AJAX Partial Update 패턴을 혼용하여 사용 중인 것을 확인했습니다.
* 두 사이트 모두 요청 시 HTML을 반환하고 있었기 때문에, 오버헤드가 큰 동적 크롤링 대신 설정이 직관적이고 가벼운 jsoup을 선택하여 수집 속도와 효율을 높였습니다.

<small>*※ 대상 웹사이트의 `robots.txt`를 준수하였으며, 서버 부하를 최소화하기 위해 요청 간격에 최소 1초 이상의 대기 시간을 두었습니다.*</small>

<br>

### 3. 쿼리 성능 개선
**문제**
채용트렌드를 분석하는 페이지에서 현재까지 


---

### 4. 왜 통계 API에 Redis 캐싱을 적용했는가?

**문제**
대시보드의 기술스택/지역/경력 통계는 GROUP BY + COUNT의 무거운 집계 쿼리입니다.
사용자가 대시보드를 열 때마다 13,000건의 데이터를 매번 집계하면 DB 부하가 큽니다.

**해결**
`@Cacheable`로 결과를 Redis에 캐싱.

**TTL 전략**
- 통계 데이터 (자주 안 바뀜) → 10분
- 오늘의 현황 (빠른 반영 필요) → 1분

**성능 측정**
| API | 캐싱 미적용 | 캐싱 적용 |
|-----|-----------|----------|
| `/api/stats/tech-stacks` | 234ms | 8ms |
| `/api/stats/locations` | 198ms | 6ms |
| `/api/stats/experience` | 187ms | 7ms |

<br>

## 🚨 트러블슈팅

### 1. 잡코리아 크롤링 실패

**문제**
잡코리아 검색 페이지가 Jsoup 크롤링 시 빈 결과를 반환했습니다.

**원인**
잡코리아 검색 페이지는 Next.js 기반 SPA로,
JavaScript가 실행되어야 데이터가 렌더링됩니다.
Jsoup은 정적 HTML만 파싱하므로 데이터를 가져올 수 없었습니다.

**해결**
- `view-source:` 로 실제 응답 HTML 확인
- 동적 렌더링 사용하지 않는 정적 페이지(`/recruit/joblist`) 탐색
- 해당 페이지에 대해서만 Jsoup 크롤링 적용

---

### 2. N+1 문제 → JOIN FETCH로 쿼리 최적화

**문제**
스크랩 목록 조회 시 `show-sql` 로그에서 SQL이 N+1번 출력됨을 발견했습니다.
스크랩 100개를 조회하면 Job SELECT 쿼리가 100번 추가 발생했습니다.

**원인**
`Scrap.job` 필드가 `FetchType.LAZY`로 설정되어 있어
루프에서 `scrap.getJob()` 접근 시마다 SELECT 쿼리가 발생했습니다.

**해결**
```java
@Query("SELECT s FROM Scrap s JOIN FETCH s.job WHERE s.user.email = :email")
List<Scrap> findAllByUserEmailWithJob(@Param("email") String email);
```
쿼리 N+1번 → 1번으로 감소했습니다.

---

### 2. 레이스 컨디션 — 동시 요청 시 중복 크롤링·AI 호출

**문제**

Lazy Fetch 전략으로 공고 상세 내용을 첫 조회 시점에 fetch합니다.
같은 공고를 동시에 여러 사용자가 처음 열면 모두 `descriptionStatus = null`을 읽고 각자 외부 크롤링 요청 + AI API 호출을 보냅니다.
동일 URL에 N번 중복 요청 → **IP 차단 위험 + AI 비용 낭비**.

**원인**

DB에 아직 아무것도 저장되지 않은 상태에서 여러 스레드가 동시 접근 → 모두 "null → 크롤링 필요"로 판단 → 각자 크롤러 실행.

**해결 — Striped Locking (Check → Lock → Check 패턴)**

256개 `ReentrantLock`을 미리 생성해두고 `jobId`를 해시로 매핑합니다.
같은 공고 요청들은 같은 락 스트라이프에서 직렬화되고, 락 획득 후 재확인(Double-Checked)으로 앞선 요청이 이미 저장했으면 즉시 반환합니다.

```java
// 1단계: 이미 수집됐으면 락 없이 즉시 반환
if (status != null) return DescriptionResponse.success(job.getDescription());

// 2단계: 스트라이프 락으로 직렬화 (같은 jobId → 같은 락)
ReentrantLock lock = getStripeLock(jobId);
lock.lock();
try {
    // 3단계: 락 획득 후 재확인 — 대기 중 앞선 요청이 이미 저장했을 수 있음
    job = jobRepository.findById(jobId).orElseThrow(...);
    if (job.getDescriptionStatus() != null) return ...;

    // 4단계: 전체 동시 요청 중 딱 1번만 실행
    return fetchDescriptionBySourceSite(job);
} finally {
    lock.unlock();
}
```

---

### 4. 상시채용 공고 마감 처리 — DeadlineType 컬럼 설계

**문제**
`deadline = NULL`인 상시채용 공고는 사이트에서 내려가도
마감일 기반 스케줄러가 감지하지 못해 삭제된 공고가 계속 노출됐습니다.

**원인**
기존 설계가 마감일(날짜) 유무만으로 공고 상태를 판단했습니다.
NULL은 처리할 수 없는 구조였습니다.

**해결**
`DeadlineType` 컬럼을 추가(`FIXED` / `ALWAYS_OPEN` / `UNKNOWN`)해
공고 유형을 명시적으로 구분했습니다.
`AlwaysOpenCheckService`를 별도 구현해 상시채용 공고의 원본 URL 접근 여부를
주기적으로 확인하고 404 응답 시 CLOSED 처리했습니다.

**배운 점**
NULL로 상태를 표현하는 것의 한계를 직접 경험했습니다.
상태값은 Enum으로 명시적으로 관리하는 것이 유지보수에 유리함을 이해했습니다.

---

### 5. 운영 환경 한정 버그 — EC2 JVM 타임존

**문제**
로컬에서는 정상이지만 운영 서버에서 오늘 신규 공고가 항상 0건으로 표시됐습니다.

**원인**
EC2 JVM 기본 타임존이 UTC로 설정되어 있어
`LocalDate.now()`가 한국 시각보다 9시간 뒤를 반환했습니다.
새벽 3시 크롤링 후 "오늘 등록된 공고" 조건이 전날 날짜로 비교됐습니다.

**해결**
systemd 서비스 파일에 JVM 옵션을 추가했습니다.
```
-Duser.timezone=Asia/Seoul
```

**배운 점**
로컬과 운영 환경의 타임존 차이가 버그를 유발할 수 있음을 이해했습니다.
날짜/시간 관련 로직은 항상 타임존을 명시적으로 지정하는 습관을 갖게 됐습니다.

---

### 6. 잡코리아 크롤러 — 2페이지부터 동일 데이터 반복

**문제**
페이지를 넘겨도 1페이지와 동일한 공고만 계속 수집됐습니다.

**원인**
잡코리아 목록 페이지가 SPA 구조라 GET 요청은 항상 1페이지만 반환했습니다.
브라우저 네트워크 탭 분석으로 실제 목록 데이터는
`POST /Recruit/Home/_GI_List/`로 요청됨을 확인했습니다.

**해결**
GET 방식에서 POST + Page 파라미터 방식으로 변경했습니다.

```java
// GET → POST + Page 파라미터 방식으로 변경
POST /Recruit/Home/_GI_List/
Body: { Page: 2, PageCount: 20, ... }
```

**배운 점**
SPA에서 실제 데이터 요청 방식은 브라우저 네트워크 탭으로 분석해야 함을 이해했습니다.
개발자 도구의 Network 탭이 크롤러 설계에 핵심 도구임을 경험했습니다.





---
*이 프로젝트는 포트폴리오 목적으로 제작되었습니다. 크롤링은 robots.txt를 준수하며, 비상업적 학습용으로만 사용됩니다.*
