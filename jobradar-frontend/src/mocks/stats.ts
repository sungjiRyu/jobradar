/**
 * stats mock 데이터 — 백엔드 없이 DashboardPage UI 테스트용
 * 실제 API 응답 data 필드와 동일한 구조로 작성
 * 테스트 완료 후 USE_MOCK = false 로 전환
 */

import type { TodayStats, TechStackStat, LocationStat, ExperienceStat } from "../api/statsApi";

// GET /api/stats/today 의 data 필드
export const mockTodayStats: TodayStats = {
  totalCount: 1284,
  todayCount: 37,
  urgentCount: 58,
  juniorCount: 312,
};

// GET /api/stats/tech-stacks 의 data 필드
// 상위 8개 이상 넣어도 DashboardPage에서 8개만 사용
export const mockTechStackStats: TechStackStat[] = [
  { name: "Java", count: 420 },
  { name: "Spring", count: 395 },
  { name: "Python", count: 280 },
  { name: "React", count: 265 },
  { name: "AWS", count: 210 },
  { name: "Docker", count: 175 },
  { name: "MySQL", count: 168 },
  { name: "Node.js", count: 132 },
  { name: "Kotlin", count: 98 },
  { name: "TypeScript", count: 87 },
];

// GET /api/stats/locations 의 data 필드
export const mockLocationStats: LocationStat[] = [
  { location: "서울", count: 742 },
  { location: "경기", count: 218 },
  { location: "판교", count: 157 },
  { location: "부산", count: 64 },
  { location: "대전", count: 48 },
  { location: "인천", count: 31 },
  { location: "기타", count: 24 },
];

// GET /api/stats/experience 의 data 필드
export const mockExperienceStats: ExperienceStat[] = [
  { experience: "신입", count: 312 },
  { experience: "경력 1~3년", count: 487 },
  { experience: "경력 3~5년", count: 318 },
  { experience: "경력 5년 이상", count: 167 },
];
