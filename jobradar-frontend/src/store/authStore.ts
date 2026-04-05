import { create } from "zustand";

// 유저 정보 타입 (auth-api.md 응답 기준)
interface User {
  email: string;
  nickname: string;
}

// 스토어 전체 타입
interface AuthStore {
  user: User | null;
  login: (user: User, accessToken: string) => void;
  logout: () => void;
}

const useAuthStore = create<AuthStore>((set) => ({
  // 로그인한 유저 정보(null이면 비로그인 상태)
  user: null,

  // 로그인 - 유저 정보 저장 + 토큰 localStorage에 저장
  login: (user: User, accessToken: string) => {
    localStorage.setItem("accessToken", accessToken);
    set({ user });
  },

  // 로그아웃 - 유저 정보 초기화 + 토큰 삭제
  logout: () => {
    localStorage.removeItem("accessToken");
    set({ user: null });
  },
}));

export default useAuthStore;
