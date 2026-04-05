import { Routes, Route, Navigate } from "react-router-dom";
import useAuthStore from "../store/authStore";
import JobListPage from "../pages/JobListPage";
import JobDetailPage from "../pages/JobDetailPage";
import LoginPage from "../pages/LoginPage";
import SignupPage from "../pages/SignupPage";
import DashboardPage from "../pages/DashboardPage";
import MyPage from "../pages/MyPage";

// 로그인한 사용자만 접근 가능한 라우트
// 비로그인 상태면 /login으로 리다이렉트
const PrivateRoute = ({ children }: { children: React.ReactNode }) => {
  const { user } = useAuthStore();
  return user ? children : <Navigate to="/login" replace />;
};

const AppRouter = () => {
  return (
    <Routes>
      <Route path="/" element={<JobListPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/signup" element={<SignupPage />} />
      <Route path="/jobs/:id" element={<JobDetailPage />} />
      <Route path="/dashboard" element={<DashboardPage />} />
      <Route
        path="/my"
        element={
          <PrivateRoute>
            <MyPage />
          </PrivateRoute>
        }
      />
    </Routes>
  );
};

export default AppRouter;
