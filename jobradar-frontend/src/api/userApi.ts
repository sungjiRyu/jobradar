/**
 * userApi — 사용자 정보 관련 API 함수
 * /api/users 엔드포인트와 통신
 */

import api from "./axios";

// 내 정보 조회: GET /api/users/me
export const getMe = () => {
  return api.get("/api/users/me");
};

// 닉네임 수정: PUT /api/users/me
export const updateNickname = (nickname: string) => {
  return api.put("/api/users/me", { nickname });
};

// 비밀번호 변경: PATCH /api/users/me/password
export const updatePassword = (newPassword: string, newPasswordConfirm: string) => {
  return api.patch("/api/users/me/password", { newPassword, newPasswordConfirm });
};

// 회원 탈퇴: DELETE /api/users/me
export const withdrawUser = () => {
  return api.delete("/api/users/me");
};
