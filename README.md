 # JobRadar 🎯

> 개발자 취업준비생을 위한 채용공고 통합 검색 및 분석 대시보드

채용공고 사이트마다 가입하고 필터링하는 번거로움을 해결하기 위해 만든 서비스입니다.
사람인과 잡코리아의 개발자 공고를 한 곳에서 검색하고, 채용 트렌드를 데이터로 분석합니다.

<br>

## 🔗 배포 URL

**🌐 https://jobradar.me**

테스트 계정: `test@test.com` / `test1234`

<br>

## 📸 주요 화면

| 공고 목록 | 채용 대시보드 |
|----------|--------------|
| ![공고목록](docs/images/joblist.png) | ![대시보드](docs/images/dashboard.png) |

| 마이페이지 | 로그인 |
|-----------|--------|
| ![마이페이지](docs/images/mypage.png) | ![로그인](docs/images/login.png) |

<br>

## 🛠 사용 기술

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

<br>

## 🎯 주요 기능

### 1. 채용공고 통합 검색
- 사람인 공식 API + 잡코리아 데이터 통합 (총 13,000건+)
- 직무 / 지역 / 경력 / 기술스택 복합 필터링
- 드롭다운 멀티필터 (각 필터 내부 검색 지원)

### 2. 자동 수집 크롤러
- 매일 오전 9시 자동 실행 (Spring `@Scheduled`)
- URL 기준 중복 방지 로직
- 공고 본문에서 기술스택 키워드 자동 추출

### 3. 채용 트렌드 대시보드
- 전체 / 신규 / 마감 임박 / 신입 공고 실시간 통계
- 기술스택 수요 순위 (Chart.js 막대 차트)
- 지역별 채용 비중 (Chart.js 도넛 차트)
- 경력별 공고 분포 분석

### 4. 스크랩 + 지원 현황 관리
- 관심 공고 스크랩
- 4단계 상태 관리: 지원예정 → 지원완료 → 서류검토 → 결과
- 상태별 필터링 및 통계

### 5. JWT 기반 인증
- AccessToken (15분) + RefreshToken (7일) 분리
- Redis에 RefreshToken 저장 (TTL 자동 만료)
- 토큰 자동 재발급 (Axios 인터셉터)

<br>

## 🏗 시스템 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│                        Client (React)                       │
│              CloudFront + S3 (정적 호스팅)                  │
└──────────────────────────┬──────────────────────────────────┘
                           │ HTTPS
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                   Spring Boot (EC2)                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │ Controller  │→ │   Service   │→ │     Repository      │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
│         │              │                      │             │
│         ▼              ▼                      ▼             │
│  ┌──────────┐   ┌──────────────┐    ┌─────────────────┐     │
│  │ Security │   │   @Cached    │    │     JPA         │     │
│  │   JWT    │   │  (Redis)     │    │   Hibernate     │     │
│  └──────────┘   └──────────────┘    └─────────────────┘     │
└──────────┬────────────┬────────────────────┬───────────────┘
           │            │                    │
           ▼            ▼                    ▼
    ┌──────────┐  ┌──────────┐        ┌──────────────┐
    │   사람인  │  │  Redis   │        │  MySQL (RDS) │
    │ 공식 API  │  │ (캐시)   │        │              │
    └──────────┘  └──────────┘        └──────────────┘
           ▲
           │ @Scheduled (매일 09:00)
    ┌──────────────────┐
    │ Crawler Service  │
    │  (Saramin API +  │
    │  Jobkorea Jsoup) │
    └──────────────────┘
```

<br>

## 💡 기술적 의사결정

### 1. 왜 JWT + Redis 조합인가?

**문제**
세션 방식은 서버 확장 시 세션 동기화 비용이 발생합니다.
순수 JWT는 토큰 탈취 시 즉시 무효화가 불가능합니다.

**해결**
AccessToken은 짧게 유지하고 RefreshToken을 Redis에 저장하는 하이브리드 방식 채택.

**이점**
- Redis TTL로 만료 자동 처리 → 별도 스케줄러 불필요
- 로그아웃 시 Redis에서 즉시 삭제 → 보안성 확보
- Stateless API 유지 → 서버 확장성 확보

<br>

### 2. 왜 크롤링과 공식 API를 혼합했는가?

**문제**
사람인은 공식 API를 제공하지만, 잡코리아는 개인 개발자에게 API를 제공하지 않습니다.
잡코리아 데이터를 제외하면 서비스의 핵심 가치인 "통합 검색"이 약해집니다.

**검토한 대안**
1. 사람인 API만 사용 → 데이터 부족
2. 두 사이트 모두 Selenium → EC2 t2.micro 메모리 한계
3. 사람인 API + 잡코리아 Jsoup ← 선택

**선택 이유**
공식 API가 있는 곳은 API를 우선 사용 (안정성, 법적 안전성).
공식 API가 없는 곳은 robots.txt 준수 + 요청 간격 1초 + User-Agent 설정으로 윤리적 크롤링.

<br>

### 3. 왜 도메인형 패키지 구조를 채택했는가?

**비교**

**계층형 (Layered)**
```
src/
├── controller/  (모든 Controller)
├── service/     (모든 Service)
└── repository/  (모든 Repository)
```

**도메인형 (Domain) ← 채택**
```
src/
├── user/        (User 관련 전체)
├── job/         (Job 관련 전체)
└── scrap/       (Scrap 관련 전체)
```

**선택 이유**
- 도메인 단위로 응집도가 높아짐
- 기능 추가/수정 시 한 패키지만 보면 됨
- 마이크로서비스로 분리할 때 유리

<br>

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

### 5. 왜 TypeScript를 선택했는가?

**선택 이유**
- 컴파일 단계에서 타입 오류 발견 → 런타임 에러 감소
- Props 타입 명시 → 컴포넌트 사용 방법이 명확
- IDE 자동완성 → 개발 생산성 향상
- 백엔드 DTO와 프론트엔드 인터페이스 일치 → 협업 시 명확

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

**배운 점**
SSR/CSR의 차이와 크롤링 가능 여부 판단 방법을 익혔습니다.
Selenium 대신 Jsoup을 선택한 것은 EC2 t2.micro 메모리 제약 때문이었는데,
브라우저를 띄우지 않는 가벼운 방식의 트레이드오프를 이해하게 되었습니다.

<br>

### 2. JWT subject에 userId 노출 문제

**문제**
초기 구현 시 JWT의 subject로 DB의 `userId` (PK)를 사용했습니다.
JWT는 디코딩만 하면 누구나 내용을 볼 수 있어서 DB PK가 외부에 노출됩니다.

**원인**
JWT는 암호화가 아닌 서명(Signature)만 검증합니다.
Base64로 인코딩되어 있어 누구나 디코딩 가능합니다.

**해결**
- JWT subject를 `userId` → `email`로 변경
- 모든 Controller에서 `@AuthenticationPrincipal String email` 사용
- Redis 키도 `refresh:{userId}` → `refresh:{email}`로 변경

**배운 점**
JWT의 보안 원리(서명 vs 암호화)를 정확히 이해하게 되었습니다.

<br>

### 3. Hibernate 7과 MySQL Dialect 호환성

**문제**
Spring Boot 4.0 업그레이드 후 빌드 실패.
`MySQL8Dialect` 클래스를 찾을 수 없다는 에러가 발생했습니다.

**원인**
Hibernate 7부터 `MySQL8Dialect`가 제거되고 `MySQLDialect`로 통합되었습니다.

**해결**
`application.yml`에서 dialect 설정을 변경하고,
Spring Boot가 자동으로 적절한 dialect를 선택하도록 변경.

```yaml
# Before
spring.jpa.database-platform: org.hibernate.dialect.MySQL8Dialect

# After (자동 선택)
# 명시하지 않음
```

**배운 점**
프레임워크 메이저 업그레이드 시 deprecated API 확인의 중요성.

<br>

### 4. MySQL 테이블명 변경 시 FK 제약

**문제**
`Job` 엔티티의 테이블명을 `jobs`에서 `job_posts`로 변경했는데
JPA `ddl-auto: update` 설정으로 `jobs` 테이블이 삭제되지 않고 남아있었습니다.
DROP TABLE 시도 시 `scraps.job_id` FK 제약으로 실패.

**해결**
1. `scraps` 테이블의 구 `job_id` 컬럼 삭제 (`ALTER TABLE scraps DROP COLUMN job_id`)
2. 구 `jobs` 테이블 DROP

**배운 점**
`ddl-auto: update`는 안전하지만 잔여 스키마 정리는 수동으로 해야 합니다.
프로덕션에서는 `validate` + Flyway 같은 마이그레이션 도구 사용이 필요함을 이해했습니다.

<br>

## 🗂 프로젝트 구조

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

<br>

## 🗄 ERD

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

<br>

---
*이 프로젝트는 포트폴리오 목적으로 제작되었습니다. 크롤링은 robots.txt를 준수하며, 비상업적 학습용으로만 사용됩니다.*
