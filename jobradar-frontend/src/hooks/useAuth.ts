import { useNavigate } from "react-router-dom";
import useAuthStore from "../store/authStore";
import {
  login as loginApi,
  logout as logoutApi,
  signup as signupApi,
} from "../api/authApi";

const useAuth = () => {
  const { login, logout } = useAuthStore();
  const navigate = useNavigate();

  // 로그인
  const handleLogin = async (email: string, password: string) => {
    const res = await loginApi(email, password);
    const { accessToken, refreshToken } = res.data.data;
    localStorage.setItem("refreshToken", refreshToken);
    login(res.data.data, accessToken);
    navigate("/");
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
