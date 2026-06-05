/**
 * jobApi — 채용공고 관련 API 함수
 * axios 인스턴스를 사용해서 백엔드와 통신
 */

import api from "./axios";

// 공고 목록 조회용 쿼리 파라미터 타입
export interface JobSearchParams {
  keyword?: string;
  jobType?: string | string[];
  sourceSite?: string | string[];
  location?: string | string[];
  experienceLevel?: string | string[];
  techStack?: string | string[];
  todayOnly?: boolean;
  urgentOnly?: boolean;
  page?: number;
  size?: number;
  sort?: string;
}

// 공고 목록 조회 + 검색 + 필터: GET /api/jobs
export const getJobs = (params?: JobSearchParams) => {
  return api.get("/api/jobs", { params });
};

// 공고 상세 조회: GET /api/jobs/{id}
export const getJobById = (id: number) => {
  return api.get(`/api/jobs/${id}`);
};

export const getJobDescription = (id: number) => {
  return api.get(`/api/jobs/${id}/description`);
};

export const getJobSummary = (id: number) => {
  return api.get(`/api/jobs/${id}/summary`);
};

// 기술스택 목록 조회: GET /api/tech-stacks
export const getTechStacks = () => {
  return api.get("/api/tech-stacks");
};
