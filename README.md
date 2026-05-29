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
1. [로그인 방식(Session vs JWT)](#1-로그인-방식-결정session-vs-jwt)
2. [데이터 수집 방식(웹크롤링 VS 공식 API)](#2-데이터-수집-방식웹크롤링-vs-공식-api)
3. [인덱스를 통한 쿼리 성능 개선](#3-인덱스를-통한-쿼리-성능-개선)
4. [DB부하 감소를 위한 캐싱 적용](#4-DB부하-감소를-위한-캐싱-적용)
5. [N+1 문제 해결](#)
<br>


## 💡 의사결정 과정

### 1. 로그인 방식 결정(Session vs JWT)

#### 1-1. 세션과 JWT 비교
* 세션 방식은 Stateful한 방식으로 서버가 사용자 정보를 직접 저장합니다. 따라서 사용자가 늘어날수록 서버의 자원 부하가 증가한다는 단점이 있습니다. 또한, 추후 Scale-out 시 여러 서버 간 세션을 공유하기 위해 Redis 등을 활용한 세션 클러스터링 구축이 필요하므로, 아키텍처의 확장성 측면에서 제약이 생깁니다.
* 반면에, JWT 방식은 Stateless한 방식으로 서버가 사용자의 정보를 저장하지 않기 때문에 확장성 측면에서 장점이 있습니다. 다만, 토큰이 탈취된다면 만료되기 전까지 서버측에서 무효화 할 방법이 없다는 단점이 있습니다. 

#### 1-2. JWT 채택과 이유
아래와 같은 사항을 고려해서 JWT를 채택했습니다.
* 추후 확장성과 Ec2 서버 부하 감소
* 크로스도메인 환경을 고려했을때 JWT가 구현에 용이
* RefreshToken과 Redis를 사용해서 보안 취약점을 보완
---
<br>

### 2. 데이터 수집 방식(웹크롤링 VS 공식 API)

#### 2-1. 문제
성능과 안정성을 고려하여 공식 API를 사용하려 했으나, 잡코리아는 개인 대상 API를 미지원하며, 사람인은 발급 신청이 승인되지 않아 부득이하게 웹 크롤링 방식을 채택하였습니다

#### 2-2. 웹 크롤링 방식(jsoup vs Playwright)**
* 정적 웹페이지 크롤링(jsoup) 방식은 서버에서 정적인 HTML받아서 데이터를 파싱합니다. 단순 HTML문서를 받아서 파싱하기 때문에 속도가 빠르고 리소스 소모가 적습니다.
* 동적 웹페이지 크롤링(Playwright)은 실제로 브라우저를 구동하여 JavaScript 렌더링이 완료된 후의 데이터를 수집합니다. 화면 클릭, 스크롤 등 사용자 상호작용이 필요한 동적 웹사이트 수집이 가능하지만 리소스 소모가 크고 상대적으로 속도가 느립니다.

#### 2-3. jsoup 채택과 이유**
* 네트워크 탭을 분석한 결과, 사람인은 SSR(서버 사이드 렌더링) 방식으로 HTML을 응답하고 있었으며, 잡코리아는 SSR과 AJAX Partial Update 패턴을 혼용하여 사용 중인 것을 확인했습니다.
* 두 사이트 모두 요청 시 HTML을 반환하고 있었기 때문에, 오버헤드가 큰 동적 크롤링 대신 설정이 직관적이고 가벼운 jsoup을 선택하여 수집 속도와 효율을 높였습니다.

<small>*※ 대상 웹사이트의 `robots.txt`를 준수하였으며, 서버 부하를 최소화하기 위해 요청 간격에 최소 1초 이상의 대기 시간을 두었습니다.*</small>

---
<br>

### 3. 인덱스를 통한 쿼리 성능 개선
공고 데이터가 늘어날 수록 집계 쿼리에서 병목이 발생할 것이라고 생각했습니다. `EXPLAIN`을 통해 실행계획을 분석하고 병목지점을 파악 및 개선했습니다.

#### 3-1. 문제
* `/api/stats/today` (전체, 신규, 마감임박, 신입 공고 집계 처리) 에서 `Full Table Scan`이 발생하고 있었습니다.

#### 3-2. 인덱스 추가
* `Full Table Scan`을 제거하기 위해 필터링 카디널리티가 높은 `status`을 선행으로, 범위 조건`deadline`을 후행으로 묶은 복합 인덱스를 생성했습니다.

#### 3-3. 성능 테스트
* 부하 테스트를 통해 인덱스 추가 후 성능이 얼마나 향상되었는지 측정했습니다.
* 테스트 도구는 JavaScript 기반으로 시나리오 코드의 가독성이 좋고, 적은 리소스로도 대규모 동시 접속 부하를 안정적으로 발생시킬 수 있는 `K6`를 사용했습니다.
* 테스트 진행과정

  a. 대용량 더미데이터 구축
     * 유의미한 인덱스 성능을 측정하기 위해 `Procedure`를 작성하여 10만 건의 더미데이터를 `Bulk Insert`했습니다.
  
  b. 테스트 시나리오 설정
     * 실제 트래픽이 몰리는 상황을 가정하여 총 100명의 가상 유저(VUs)가 통계 API를 집중적으로 호출하도록 시나리오를 구성했습니다.
       * Ramp-up: 20초 동안 0명에서 100명으로 점진적 증가
       * Peak: 30초 동안 100명 유지 (최대 부하 발생 구간)
       * Ramp-down: 10초 동안 0명으로 감소
     * 테스트 스크립트:
       ```javascript
       export const options = {
         stages: [
           { duration: "20s", target: 100 }, // 20초 동안 가상 유저(VU)를 0명에서 100명까지 점진적 증가
           { duration: "30s", target: 100 }, // 30초 동안 가상 유저 100명 유지 (피크 트래픽)
           { duration: "10s", target: 0 }, // 10초 동안 가상 유저를 100명으로 감소시키며 마무리
         ],
         thresholds: {
           http_req_failed: ["rate<0.01"], // 에러율이 1% 미만이어야 테스트 통과
           http_req_duration: ["p(95)<500"], // 전체 요청의 95%는 0.5초(500ms) 이내에 응답해야 함
         },
       };
       ```

  c. 테스트 수행
     * 인덱스 생성 전 쿼리로 테스트를 수행합니다.

  d. 인덱스 생성 후 테스트 수행
     * 인덱스 생성 후 동일한 조건으로 테스트를 수행합니다.

  e. 결과 비교
     * 인덱스 생성 전후의 측정 결과를 비교하여 유의미한 성능 향상이 있었는지 평가합니다.
 
 #### 3-4. 결과
* 복합 인덱스 적용 결과, 4개 쿼리 모두 탐색 범위가 절반가량 단축(type: range)되었습니다.
* `/api/stats/today` 의 평균 응답시간이 36% 단축되었습니다.
* 대표 집계 쿼리 (전체 공고 수)
  ```SQL
  SELECT COUNT(id)
    FROM job_posts
   WHERE status = 'ACTIVE'
     AND (deadline IS NULL OR deadline >= CURDATE());
  ```
  
 * 실행 계획(EXPLAIN) 비교
   * 인덱스 추가 전
     <img width="1163" height="47" alt="image" src="https://github.com/user-attachments/assets/6522848d-aec5-4619-b105-e0e1ff7b6ee5" />
   * 인덱스 추가 후
     <img width="1252" height="48" alt="image" src="https://github.com/user-attachments/assets/f9835f17-9e4d-44bd-9086-f82654d19c99" />
     
* 성능 향상 지표 (/api/stats/today)
   | 측정 지표 | 개선 전 (Full Scan) | 개선 후 (Covering Index) | 성능 개선 효과 |
   | :--- | :--- | :--- | :--- |
   | P(95) 응답 시간 | 4,579.76 ms | 3,991.85 ms | 약 13% 단축 |
   | 평균 응답 (Avg) | 3,395.02 ms | 2,171.53 ms |  약 36% 단축 |
   | 초당 처리량 (RPS)| 17.55 req/s | 24.05 req/s |  약 37% 증가 |

---
<br>

### 4. DB부하 감소를 위한 캐싱 적용

통계 집계 연산으로 인한 DB 부하를 해결하기 위해 캐시 도입을 결정했습니다. 본 서비스는 매일 1회 스케줄러 기반의 크롤링으로 데이터가 갱신되기 때문에, 하루 단위로 데이터의 상태가 고정되는 특징이 있습니다. 이를 활용하면 캐시 불일치 문제에 대한 부담 없이 높은 캐시 히트율을 달성할 수 있어 캐싱 도입에 최적화된 조건이라 판단했습니다.

#### 4-1. 캐시 저장소 선택(Memcached vs Redis)
  * Memcached: 구조가 단순하고 멀티스레드를 지원하여 응답속도가 빠릅니다. 하지만 데이터 영속성을 지원하지 않아 서버가 재시작 되면 모든 데이터가 사라집니다.
  * redis: 다양한 데이터 구조와 데이터 영속성(`RDB snapshot`, `AOF` 등)을 지원합니다.

#### 4-2. Redis 선택과 이유
* 확장성과 안정성 측면에서 Memcached 보다 Redis가 더 효율적이라고 판단했습니다. Spring boot 환경에서 어노테이션으로 간단히 구현이 가능하고 참고 레퍼런스가 많다는 점도 매력적이었습니다.

#### 4-2. 데이터 정합성 유지 (TTL 및 갱신)
캐시 데이터와 원본 DB 간의 불일치(Stale Data)를 최소화하고, 동시에 서버 부하를 방어하기 위해 다음과 같은 전략을 취했습니다.

* **TTL(Time-To-Live) 설정:** 하루에 한번 데이터가 업데이트되는 서비스 측성상 모든 통계 캐시 의 만료 시간(TTL)은 24시간으로 설정했습니다.
* **Cache Invalidation (Eviction):** 크롤링이 완료되고 데이터가 업데이트 된 후 `@CacheEvict` 를 사용해서 Flush 했습니다.

#### 4-3. Cache Stampede 방어
크롤링 직후 캐시가 비워졌을 때 트래픽이 몰리면 다수의 요청이 DB에 접근하고 캐시에 데이터를 적재하는 `Cache Stampede` 현상이 발생할 수 있습니다.
아래와 같은 방법으로 ` Cache Stampede`를 방어했습니다.

* **



#### 4-3. 최종 성능 개선 결과 (캐싱 결합)

캐시 계층 도입 후 동일한 부하 테스트(100 VUs)를 수행한 결과, DB로 향하던 집계 연산 부하가 완벽히 격리되어 응답 속도가 비약적으로 상승했습니다.

| 측정 지표 | 개선 전 (Full Scan) | 1차 개선 (Covering Index) | 2차 최종 개선 (Redis Cache) |
| :--- | :--- | :--- | :--- |
| **접근 계층** | Database (Disk) | Database (Memory/Index) | **Redis (In-Memory)** |
| **P(95) 응답 시간** | 4,579.76 ms | 3,991.85 ms | 🚀 **[측정값] ms** (획기적 단축) |
| **최대 지연 (Max)** | 8,272.05 ms | 5,815.47 ms | 📉 **[측정값] ms** |
| **초당 처리량(RPS)**| 17.55 req/s | 24.05 req/s | ⚡ **[측정값] req/s** |

---
<br>

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
*이 프로젝트는 포트폴리오 목적으로 제작되었습니다. 크롤링은 robots.txt를 준수하며, 비상업적 학습용으로만 사용됩니다.*
