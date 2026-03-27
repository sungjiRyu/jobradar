import { BrowserRouter, Routes, Route } from "react-router-dom";
import JobListPage from "../pages/JobListPage";
import JobDetailPage from "../pages/JobDetailPage";
import LoginPage from "../pages/LoginPage";
import SignupPage from "../pages/SignupPage";
import DashboardPage from "../pages/DashboardPage";
import MyPage from "../pages/MyPage";

const AppRouter = () => {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<JobListPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignupPage />} />
        <Route path="/jobs:id" element={<JobDetailPage />} />
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/my" element={<MyPage />} />
      </Routes>
    </BrowserRouter>
  );
};

export default AppRouter;
