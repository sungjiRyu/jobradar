# JobRadar 프로젝트 컨텍스트

## 프로젝트 개요

취업준비생을 위한 채용정보 대시보드 서비스

## 프로젝트 목적

취업 준비용 포트폴리오

## 개발자 배경

- 30대 초반 백엔드 개발자 취업준비 중
- Java/Spring Boot 기초 CRUD 가능 수준
- Docker, Redis 처음 사용
- React 처음 사용

## 기술스택

- Backend: Spring Boot 4.0.5, Java 21, Spring Security, JPA
- Database: MySQL 8.0
- Cache: Redis 7.0
- Frontend: React (Vite + Tailwind CSS)
- Infra: Docker, docker-compose, AWS EC2/RDS/S3

## 폴더 구조

JobRadar/
├── jobradar-backend/ ← Spring Boot
├── jobradar-frontend/ ← React (미생성)
└── docker-compose.yml

## 패키지명

com.jobradar.backend

## Git 전략

- GitHub Flow (main + feature 브랜치)
- 커밋 컨벤션: feat/fix/chore/docs/refactor
- 이슈 번호를 커밋 메시지에 포함 예) feat: 회원가입 구현 (#5)

## 현재 진행 상황

- [x] Spring Boot 프로젝트 생성 완료
- [ ] React 프로젝트 초기 세팅 (다음 작업)
- [ ] docker-compose.yml + Dockerfile 작성
- [ ] ERD 설계 및 Entity 작성
- [ ] 회원가입/로그인 API
- [ ] JWT + Redis 인증
- [ ] 채용공고 크롤러
- [ ] 공고 목록/검색/필터 API
- [ ] 스크랩 API
- [ ] Redis 캐싱
- [ ] React 화면 구현
- [ ] AWS 배포

## 코드 작성 규칙

- 코드 작성 후 반드시 왜 이렇게 작성했는지 설명해줄 것
- 초보자도 이해할 수 있게 주석 포함
- 면접 질문 대비 설명 포함
- 해당 기술 왜 사용하는지 설명할 것

## UI 레퍼런스

- JobRadar UI 샘플 확정 (공고 목록, 대시보드, 마이페이지)
- Tailwind CSS 사용
