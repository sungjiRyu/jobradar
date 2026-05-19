import { useState, useEffect } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { FiMenu, FiX } from "react-icons/fi";
import useAuthStore from "../../store/authStore";
import useAuth from "../../hooks/useAuth";

const Navbar = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { user } = useAuthStore();
  const { handleLogout } = useAuth();
  const [isMenuOpen, setIsMenuOpen] = useState(false);

  // 현재 경로와 일치하면 활성 스타일 적용
  const isActive = (path: string) => location.pathname === path;

  // 라우트 변경 시 모바일 메뉴 자동 닫힘
  useEffect(() => {
    setIsMenuOpen(false);
  }, [location.pathname]);

  // 모바일 메뉴 항목 공통 스타일 헬퍼
  const mobileItemClass = (active: boolean) =>
    `w-full text-left px-4 py-3 text-sm transition-colors ${
      active
        ? "text-[#378ADD] font-medium bg-[#F0F7FF]"
        : "text-[#1A1A1A] hover:bg-[#F5F5F5]"
    }`;

  return (
    <nav className="w-full h-14 bg-white border-b border-[#DDDDDD] flex items-center px-4 sm:px-6 relative">
      {/* 로고 */}
      <div
        className="flex items-center gap-2 cursor-pointer"
        onClick={() => navigate("/")}
      >
        <img src="/favicon.svg" alt="JobRadar" className="w-7 h-7" />
        <span className="text-[#378ADD] font-bold text-lg">JobRadar</span>
      </div>

      {/* ===== 데스크톱(lg+): 가운데 네비 + 우측 버튼 ===== */}
      <div className="hidden lg:flex ml-8 items-center gap-1">
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

      <div className="hidden lg:flex ml-auto items-center gap-3">
        {user ? (
          <>
            <button
              onClick={() => navigate("/my")}
              className={`text-sm transition-colors ${
                isActive("/my")
                  ? "text-[#378ADD] font-medium"
                  : "text-[#888780] hover:text-[#1A1A1A]"
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

      {/* ===== 모바일(lg 미만): 햄버거 버튼 ===== */}
      <button
        className="lg:hidden ml-auto text-[#1A1A1A] p-2 -mr-2"
        onClick={() => setIsMenuOpen((prev) => !prev)}
        aria-label={isMenuOpen ? "메뉴 닫기" : "메뉴 열기"}
      >
        {isMenuOpen ? <FiX size={22} /> : <FiMenu size={22} />}
      </button>

      {/* ===== 모바일 드롭다운 패널 ===== */}
      {isMenuOpen && (
        <div className="lg:hidden absolute top-14 left-0 right-0 bg-white border-b border-[#DDDDDD] shadow-md z-50 flex flex-col py-2">
          <button
            onClick={() => navigate("/")}
            className={mobileItemClass(isActive("/"))}
          >
            공고
          </button>
          <button
            onClick={() => navigate("/dashboard")}
            className={mobileItemClass(isActive("/dashboard"))}
          >
            대시보드
          </button>
          {user ? (
            <>
              <button
                onClick={() => navigate("/my")}
                className={mobileItemClass(isActive("/my"))}
              >
                마이페이지
              </button>
              <button
                onClick={handleLogout}
                className={mobileItemClass(false)}
              >
                로그아웃
              </button>
            </>
          ) : (
            <>
              <button
                onClick={() => navigate("/login")}
                className={mobileItemClass(isActive("/login"))}
              >
                로그인
              </button>
              <button
                onClick={() => navigate("/signup")}
                className={mobileItemClass(isActive("/signup"))}
              >
                회원가입
              </button>
            </>
          )}
        </div>
      )}
    </nav>
  );
};

export default Navbar;
