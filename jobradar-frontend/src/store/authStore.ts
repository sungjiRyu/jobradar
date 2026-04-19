import { create } from "zustand";

interface User {
  email: string;
  nickname: string;
}

interface AuthStore {
  user: User | null;
  login: (user: User, accessToken: string) => void;
  logout: () => void;
}

// 새로고침 후에도 로그인 상태를 유지하기 위해 localStorage에서 user 정보를 복원
const savedUser = localStorage.getItem("user");
const initialUser: User | null = savedUser ? JSON.parse(savedUser) : null;

const useAuthStore = create<AuthStore>((set) => ({
  user: initialUser,

  login: (user: User, accessToken: string) => {
    localStorage.setItem("accessToken", accessToken);
    // user 정보도 localStorage에 저장해야 새로고침 후 복원 가능
    localStorage.setItem("user", JSON.stringify(user));
    set({ user });
  },

  logout: () => {
    localStorage.removeItem("accessToken");
    localStorage.removeItem("refreshToken");
    localStorage.removeItem("user");
    set({ user: null });
  },
}));

export default useAuthStore;
