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

const clearStoredAuth = () => {
  localStorage.removeItem("accessToken");
  localStorage.removeItem("refreshToken");
  localStorage.removeItem("user");
};

// 새로고침 후에도 로그인 상태를 유지하되, 토큰과 사용자 정보가 모두 있을 때만 복원
const getInitialUser = (): User | null => {
  const accessToken = localStorage.getItem("accessToken");
  const savedUser = localStorage.getItem("user");

  if (!accessToken || !savedUser) {
    clearStoredAuth();
    return null;
  }

  try {
    const user = JSON.parse(savedUser) as User;
    if (!user.email || !user.nickname) {
      clearStoredAuth();
      return null;
    }
    return user;
  } catch {
    clearStoredAuth();
    return null;
  }
};

const useAuthStore = create<AuthStore>((set) => ({
  user: getInitialUser(),

  login: (user: User, accessToken: string) => {
    localStorage.setItem("accessToken", accessToken);
    localStorage.setItem("user", JSON.stringify(user));
    set({ user });
  },

  logout: () => {
    clearStoredAuth();
    set({ user: null });
  },
}));

export default useAuthStore;
