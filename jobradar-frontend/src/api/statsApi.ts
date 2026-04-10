/**
 * statsApi — 대시보드용 통계 API 함수
 * 각 차트에 필요한 집계 데이터를 백엔드에서 받아온다.
 * 반환 타입을 interface로 명시해서 타입 안전성을 보장한다.
 */

import api from "./axios";

// ─────────────────────────────────────────────
// 반환 타입 정의 (interface)
// ─────────────────────────────────────────────

/**
 * 오늘 신규 공고 / 마감 임박 / 전체 공고 수 등 요약 통계
 * GET /api/stats/today 응답 data 타입
 */
export interface TodayStats {
  totalCount: number;   // 전체 공고 수
  todayCount: number;   // 오늘 신규 공고 수
  urgentCount: number;  // D-7 이내 마감 임박 공고 수 (백엔드 필드명: urgentCount)
  juniorCount: number;  // 신입 공고 수 (백엔드 필드명: juniorCount)
}

/**
 * 기술스택별 공고 수
 * GET /api/stats/tech-stacks 응답 data 배열 요소 타입
 */
export interface TechStackStat {
  name: string;   // 기술스택 이름 (예: "Java", "React")
  count: number;  // 해당 기술스택을 요구하는 공고 수
}

/**
 * 지역별 공고 수
 * GET /api/stats/locations 응답 data 배열 요소 타입
 */
export interface LocationStat {
  location: string; // 지역명 (예: "서울", "경기")
  count: number;    // 해당 지역 공고 수
}

/**
 * 경력별 공고 수
 * GET /api/stats/experience 응답 data 배열 요소 타입
 */
export interface ExperienceStat {
  experience: string; // 경력 구분 (예: "신입", "경력 1~3년") (백엔드 필드명: experience)
  count: number;      // 해당 경력 공고 수
}

// ─────────────────────────────────────────────
// API 함수
// ─────────────────────────────────────────────

/**
 * 기술스택별 공고 수 집계
 * 막대 차트에 사용 (상위 8개만 표시)
 */
export const getStatsTechStacks = () => {
  return api.get<{ data: TechStackStat[] }>("/api/stats/tech-stacks");
};

/**
 * 지역별 공고 수 집계
 * 가로 막대(비중) 차트에 사용
 */
export const getStatsLocations = () => {
  return api.get<{ data: LocationStat[] }>("/api/stats/locations");
};

/**
 * 오늘 신규 공고 / 전체 / 마감 임박 / 신입 공고 수 요약
 * 상단 요약 카드 4개에 사용
 */
export const getStatsToday = () => {
  return api.get<{ data: TodayStats }>("/api/stats/today");
};

/**
 * 경력별 공고 수 집계
 * 도넛 또는 막대 차트에 사용
 */
export const getStatsExperience = () => {
  return api.get<{ data: ExperienceStat[] }>("/api/stats/experience");
};
