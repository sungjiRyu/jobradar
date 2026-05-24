# JobRadar

> 개발자 취업준비생을 위한 채용공고 수집 · 관리 서비스

[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.5-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19-61DAFB?logo=react&logoColor=black)](https://react.dev/)
[![Redis](https://img.shields.io/badge/Redis-7.0-DC382D?logo=redis&logoColor=white)](https://redis.io/)
[![AWS](https://img.shields.io/badge/AWS-EC2%2FRDS%2FS3-FF9900?logo=amazonaws&logoColor=white)](https://aws.amazon.com/)

**배포 URL**: https://[배포도메인]

---

## 서비스 소개

사람인, 잡코리아의 채용공고를 매일 자동으로 수집해 한 곳에서 검색·필터링할 수 있는 대시보드 서비스입니다.
공고 상세 내용은 AI(Gemini)가 자동 요약하고, 관심 공고를 스크랩해 지원 현황을 관리할 수 있습니다.

---

## 주요 기능

| 기능 | 설명 |
|------|------|
| 채용공고 자동 수집 | 사람인·잡코리아 크롤링, 매일 새벽 3시 자동 실행 |
| 검색 · 필터링 | 키워드, 직무, 지역, 경력, 기술스택 복합 필터 |
| AI 공고 요약 | Gemini API로 상세 내용 자동 요약 (첫 조회 시 생성 후 저장) |
| 대시보드 통계 | 기술스택·지역·경력별 공고 현황 차트 |
| 스크랩 관리 | 관심 공고 저장 + 지원예정/완료/서류검토/탈락 상태 관리 |
| 회원 인증 | JWT + Redis 기반 (AccessToken 15분 / RefreshToken 7일) |

---

## 기술 스택

| 분류 | 기술 |
|------|------|
| Backend | Java 21, Spring Boot 4.0.5, Spring Security 7, Spring Data JPA |
| Database | MySQL 8.0, Redis 7.0 |
| Frontend | React 19 (TypeScript), Tailwind CSS, Zustand, Chart.js, Vite |
| Infra | AWS EC2 · RDS · S3 · CloudFront, Docker, docker-compose |
| Crawling | Jsoup 1.17.2 |
| AI | Google Gemini 2.5 Flash |
| API Docs | Springdoc OpenAPI (Swagger) |

---

## 시스템 아키텍처

```
[사용자 브라우저]
      │
      ▼
[CloudFront + S3]        ← React 정적 빌드 배포
      │ API 요청 (HTTPS)
      ▼
[EC2 — Spring Boot :8080]
      ├── Redis 7.0       ← JWT 토큰 저장 / 통계 캐싱
      └── RDS MySQL 8.0   ← 공고 / 유저 / 스크랩 데이터
            ↑
[CrawlerScheduler — 매일 새벽 03:00 KST]
      ├── SaraminCrawlerService   (사람인)
      └── JobkoreaCrawlerService  (잡코리아)
```

---

## 핵심 구현

### 1. Lazy Fetch 전략 — 크롤링 부하 절감

크롤링 시점에는 공고 목록(제목·회사·마감일)만 수집하고, 상세 내용(description)은 사용자가 처음 조회할 때 fetch합니다.
요청 수를 절반으로 줄여 IP 차단 위험을 낮추고, 크롤링 시간을 단축했습니다.

```java
// descriptionStatus가 null → 아직 fetch 안 된 상태 → 지금 가져옴
if (job.getDescriptionStatus() == null) {
    fetchAndSaveDescription(job);
}
```

### 2. Redis 멀티레이어 캐싱 — TTL 세분화

조회 빈도와 데이터 변경 주기에 따라 TTL을 다르게 설정했습니다.

| 캐시 대상 | TTL | 이유 |
|----------|-----|------|
| 기술스택·지역·경력 통계 | 10분 | 크롤링 주기(1일) 대비 충분 |
| 오늘의 현황 | 1분 | 신규 공고 빠른 반영 필요 |
| RefreshToken | 7일 | 토큰 만료 주기와 TTL 일치 |

### 3. 확장 가능한 크롤러 설계 — OCP 준수

`CrawlerService` 인터페이스를 구현하기만 하면 `CrawlerScheduler`가 자동으로 감지해 실행합니다.
새로운 사이트를 추가할 때 기존 코드를 전혀 수정하지 않아도 됩니다.

```java
// List<CrawlerService>로 모든 구현체 자동 주입
// 새 크롤러 = @Component 클래스 1개 추가로 끝
@Component
public class NewSiteCrawlerService implements CrawlerService { ... }
```

### 4. JWT + Redis 이중 보안

- AccessToken 15분 단기 유효 → 탈취 시 피해 최소화
- RefreshToken Redis 저장 → 로그아웃 시 서버에서 즉시 무효화
- 토큰 재발급 시 Redis 저장값과 비교 → 탈취된 토큰 차단

---

## 로컬 실행 방법

**사전 요구사항**: Java 21, Docker, Node.js

```bash
# 1. 저장소 클론
git clone https://github.com/sungjiRyu/jobradar.git
cd jobradar

# 2. 환경변수 설정
cp .env.example .env
# .env 파일에서 DB 비밀번호, JWT Secret, Gemini API Key 설정

# 3. MySQL + Redis 컨테이너 실행
docker-compose up -d

# 4. 백엔드 실행
cd jobradar-backend
./gradlew bootRun

# 5. 프론트엔드 실행 (새 터미널)
cd jobradar-frontend
npm install
npm run dev
```

프론트엔드: `http://localhost:5173`
백엔드 API: `http://localhost:8080`
Swagger UI: `http://localhost:8080/swagger-ui.html`

---

## 프로젝트 구조

```
jobradar/
├── jobradar-backend/
│   └── src/main/java/com/jobradar/backend/
│       ├── auth/        # JWT 인증 (로그인·로그아웃·토큰 재발급)
│       ├── user/        # 회원 관리 (가입·수정·탈퇴)
│       ├── job/         # 채용공고 조회·검색·필터
│       ├── scrap/       # 스크랩 CRUD + 상태 관리
│       ├── stats/       # 대시보드 통계 (Redis 캐싱)
│       ├── crawler/     # 크롤링 스케줄러 + 사이트별 구현
│       └── global/      # 공통 설정·예외·보안 필터
├── jobradar-frontend/
│   └── src/
│       ├── pages/       # 페이지 컴포넌트
│       ├── components/  # 공통 UI 컴포넌트
│       ├── api/         # Axios 인스턴스 + API 함수
│       ├── hooks/       # 커스텀 훅
│       ├── store/       # Zustand 전역 상태
│       └── utils/       # 유틸 함수
└── docker-compose.yml
```
