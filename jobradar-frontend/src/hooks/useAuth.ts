import { useNavigate, useLocation } from "react-router-dom";
import useAuthStore from "../store/authStore";
import {
  login as loginApi,
  logout as logoutApi,
  signup as signupApi,
} from "../api/authApi";

const useAuth = () => {
  const { login, logout } = useAuthStore();
  const navigate = useNavigate();
  const location = useLocation();

  // 로그인
  const handleLogin = async (email: string, password: string) => {
    const res = await loginApi(email, password);
    const { accessToken, refreshToken } = res.data.data;
    localStorage.setItem("refreshToken", refreshToken);
    login(res.data.data, accessToken);
    // PrivateRoute에서 넘겨준 from 경로가 있으면 그 페이지로, 없으면 홈으로 이동
    const from = (location.state as { from?: string })?.from ?? "/";
    navigate(from, { replace: true });
  };

  // 회원가입
  const handleSignup = async (
    email: string,
    password: string,
    nickname: string,
  ) => {
    await signupApi(email, password, nickname);
    navigate("/login");
  };

  // 로그아웃
  const handleLogout = async () => {
    await logoutApi();
    logout();
    navigate("/login");
  };

  return { handleLogin, handleSignup, handleLogout };
};

export default useAuth;
