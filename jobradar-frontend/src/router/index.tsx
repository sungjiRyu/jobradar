/**
 * AppRouter — 앱 전체 라우팅 설정
 * PrivateRoute로 감싼 경로는 로그인(accessToken)이 있어야 접근 가능하다.
 */

import { Routes, Route } from "react-router-dom";
import PrivateRoute from "../components/common/PrivateRoute";
import JobListPage from "../pages/JobListPage";
import JobDetailPage from "../pages/JobDetailPage";
import LoginPage from "../pages/LoginPage";
import SignupPage from "../pages/SignupPage";
import DashboardPage from "../pages/DashboardPage";
import MyPage from "../pages/MyPage";
import VerifyPasswordPage from "../pages/VerifyPasswordPage";
import EditProfilePage from "../pages/EditProfilePage";

const AppRouter = () => {
  return (
    <Routes>
      {/* 공개 라우트 */}
      <Route path="/" element={<JobListPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/signup" element={<SignupPage />} />
      <Route path="/jobs/:id" element={<JobDetailPage />} />
      <Route path="/dashboard" element={<DashboardPage />} />

      {/* 보호된 라우트 — accessToken 없으면 /login으로 리다이렉트 */}
      <Route
        path="/my"
        element={
          <PrivateRoute>
            <MyPage />
          </PrivateRoute>
        }
      />
      <Route
        path="/my/verify"
        element={
          <PrivateRoute>
            <VerifyPasswordPage />
          </PrivateRoute>
        }
      />
      <Route
        path="/my/edit"
        element={
          <PrivateRoute>
            <EditProfilePage />
          </PrivateRoute>
        }
      />
    </Routes>
  );
};

export default AppRouter;
