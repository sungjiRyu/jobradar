import { useNavigate, useLocation } from "react-router-dom";
import useAuthStore from "../../store/authStore";
import useAuth from "../../hooks/useAuth";

const Navbar = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { user } = useAuthStore();
  const { handleLogout } = useAuth();

  // 현재 경로와 일치하면 활성 스타일 적용
  const isActive = (path: string) => location.pathname === path;

  return (
    <nav className="w-full h-14 bg-white border-b border-[#DDDDDD] flex items-center px-6">
      {/* 로고 */}
      <span
        className="text-[#378ADD] font-bold text-lg cursor-pointer"
        onClick={() => navigate("/")}
      >
        JobRadar
      </span>

      {/* 가운데 네비게이션 링크 */}
      <div className="ml-8 flex items-center gap-1">
        <button
          onClick={() => navigate("/")}
          className={`px-3 py-1.5 rounded-md text-sm transition-colors ${
            isActive("/")
              ? "text-[#378ADD] font-medium"
              : "text-[#888780] hover:text-[#1A1A1A]"
          }`}
        >
          공고
        </button>
        <button
          onClick={() => navigate("/dashboard")}
          className={`px-3 py-1.5 rounded-md text-sm transition-colors ${
            isActive("/dashboard")
              ? "text-[#378ADD] font-medium"
              : "text-[#888780] hover:text-[#1A1A1A]"
          }`}
        >
          대시보드
        </button>
      </div>

      {/* 우측 영역 */}
      <div className="ml-auto flex items-center gap-3">
        {user ? (
          <>
            <button
              onClick={() => navigate("/my")}
              className={`text-sm transition-colors ${
                isActive("/my") ? "text-[#378ADD] font-medium" : "text-[#888780] hover:text-[#1A1A1A]"
              }`}
            >
              마이페이지
            </button>
            <button
              onClick={handleLogout}
              className="text-sm text-[#888780] hover:text-[#1A1A1A]"
            >
              로그아웃
            </button>
          </>
        ) : (
          <>
            <button
              onClick={() => navigate("/signup")}
              className="text-sm text-[#378ADD] px-4 py-1.5 rounded-md border border-[#378ADD] hover:opacity-90"
            >
              회원가입
            </button>
            <button
              onClick={() => navigate("/login")}
              className="text-sm bg-[#378ADD] text-white px-4 py-1.5 rounded-md hover:opacity-90"
            >
              로그인
            </button>
          </>
        )}
      </div>
    </nav>
  );
};

export default Navbar;
