/**
 * scrapApi — 스크랩(관심 공고) 관련 API 함수
 * /api/scraps 엔드포인트와 통신
 */

import api from "./axios";

// 스크랩 상태 타입
export type ScrapStatus = "PENDING" | "APPLIED" | "REVIEWING" | "REJECTED";

// 스크랩 목록 조회: GET /api/scraps
export const getScraps = () => {
  return api.get("/api/scraps");
};

// 스크랩 상태 변경: PATCH /api/scraps/{id}
export const updateScrapStatus = (id: number, status: ScrapStatus) => {
  return api.patch(`/api/scraps/${id}`, { status });
};

// 스크랩 삭제: DELETE /api/scraps/{id}
export const deleteScrap = (id: number) => {
  return api.delete(`/api/scraps/${id}`);
};
