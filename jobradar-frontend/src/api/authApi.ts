import api from "./axios";

// 회원가입: POST /api/users/signup
export const signup = (email: string, password: string, nickname: string) => {
  return api.post("/api/users/signup", { email, password, nickname });
};

// 로그인: POST /api/auth/login
export const login = (email: string, password: string) => {
  return api.post("/api/auth/login", { email, password });
};

// 로그아웃: POST /api/auth/logout
export const logout = () => {
  return api.post("/api/auth/logout");
};

// 비밀번호 확인: POST /api/auth/verify-password
export const verifyPassword = (password: string) => {
  return api.post("/api/auth/verify-password", { password });
};
