/**
 * Mock 데이터 — 백엔드 없이 프론트엔드 UI 테스트용
 * 실제 API 응답 형식과 동일한 구조
 * 테스트 완료 후 삭제 예정
 */

import type { Job } from "../components/job/JobCard";

// 공고 목록 mock 데이터
export const mockJobs: Job[] = [
  {
    id: 1,
    company: "카카오",
    title: "[카카오] 서버 개발자 (백엔드)",
    location: "판교",
    experienceLevel: "경력",
    employmentType: "정규직",
    techStacks: ["Java", "Spring", "MySQL", "Redis"],
    deadline: "2026-05-31",
    sourceSite: "원티드",
    viewCount: 42,
  },
  {
    id: 2,
    company: "네이버",
    title: "[네이버] 프론트엔드 개발자",
    location: "서울",
    experienceLevel: "신입",
    employmentType: "정규직",
    techStacks: ["React", "TypeScript", "Next.js"],
    deadline: "2026-04-09", // D-3 이내 → 빨간색 테스트
    sourceSite: "사람인",
    viewCount: 128,
  },
  {
    id: 3,
    company: "라인",
    title: "[LINE] 풀스택 엔지니어",
    location: "서울",
    experienceLevel: "경력",
    employmentType: "정규직",
    techStacks: ["Java", "Spring", "React", "AWS"],
    deadline: "2026-04-07", // D-1 → 빨간색 테스트
    sourceSite: "원티드",
    viewCount: 87,
  },
  {
    id: 4,
    company: "토스",
    title: "[토스] 백엔드 개발자 (신입)",
    location: "서울",
    experienceLevel: "신입",
    employmentType: "정규직",
    techStacks: ["Kotlin", "Spring", "MySQL"],
    deadline: "2026-06-15",
    sourceSite: "원티드",
    viewCount: 256,
  },
  {
    id: 5,
    company: "쿠팡",
    title: "[쿠팡] DevOps 엔지니어",
    location: "서울",
    experienceLevel: "경력",
    employmentType: "정규직",
    techStacks: ["AWS", "Docker", "Kubernetes", "Python"],
    deadline: "2026-05-20",
    sourceSite: "잡코리아",
    viewCount: 63,
  },
];

// 공고 상세 mock 데이터
export const mockJobDetail = {
  id: 1,
  company: "카카오",
  title: "[카카오] 서버 개발자 (백엔드)",
  description:
    "카카오 서비스의 백엔드 시스템을 개발합니다.\n\n주요 업무:\n- Java/Spring 기반 API 서버 개발\n- 대용량 트래픽 처리 시스템 설계\n- MySQL, Redis를 활용한 데이터 관리\n\n자격 요건:\n- Java/Spring 경력 3년 이상\n- RDBMS 설계 및 최적화 경험\n- RESTful API 설계 경험",
  location: "판교",
  experienceLevel: "경력",
  employmentType: "정규직",
  techStacks: ["Java", "Spring", "MySQL", "Redis"],
  deadline: "2026-05-31",
  sourceUrl: "https://careers.kakao.com/jobs/1",
  sourceSite: "원티드",
  status: "ACTIVE",
  viewCount: 42,
  createdAt: "2026-04-06T08:17:17.123",
};

// 기술스택 목록 mock 데이터
export const mockTechStacks = [
  "AWS",
  "Docker",
  "Java",
  "MySQL",
  "Python",
  "React",
  "Redis",
  "Spring",
];
