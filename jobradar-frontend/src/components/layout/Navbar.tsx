import { useState, useEffect, useMemo, useCallback } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { FiBell, FiMenu, FiX } from "react-icons/fi";
import useAuthStore from "../../store/authStore";
import useAuth from "../../hooks/useAuth";
import { getScraps } from "../../api/scrapApi";
import type { ScrapItem } from "../../api/scrapApi";
import { calcDday } from "../../utils/dateUtils";

const formatDday = (dday: number) => {
  if (dday === 0) return "D-day";
  return `D-${dday}`;
};

const Navbar = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { user } = useAuthStore();
  const { handleLogout } = useAuth();
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const [isNotificationOpen, setIsNotificationOpen] = useState(false);
  const [scraps, setScraps] = useState<ScrapItem[]>([]);

  // 현재 경로와 일치하면 활성 스타일 적용
  const isActive = (path: string) => location.pathname === path;

  // 라우트 변경 시 모바일 메뉴 자동 닫힘
  useEffect(() => {
    setIsMenuOpen(false);
    setIsNotificationOpen(false);
  }, [location.pathname]);

  const fetchScraps = useCallback(async () => {
    try {
      const res = await getScraps();
      setScraps(res.data.data ?? []);
    } catch {
      setScraps([]);
    }
  }, []);

  useEffect(() => {
    if (!user) {
      setScraps([]);
      setIsNotificationOpen(false);
      return;
    }

    fetchScraps();
  }, [fetchScraps, user]);

  const urgentScraps = useMemo(
    () =>
      scraps
        .map((scrap) => ({ ...scrap, dday: calcDday(scrap.deadline) }))
        .filter(
          (scrap) =>
            scrap.status === "PENDING" &&
            scrap.dday !== null &&
            scrap.dday >= 0 &&
            scrap.dday <= 3
        )
        .sort((a, b) => {
          if (a.dday !== b.dday) return (a.dday ?? 0) - (b.dday ?? 0);
          return (
            new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
          );
        }),
    [scraps]
  );

  const notificationCount = urgentScraps.length;
  const notificationBadge = notificationCount > 99 ? "99+" : notificationCount;

  const handleNotificationItemClick = (jobPostId: number) => {
    setIsNotificationOpen(false);
    navigate(`/jobs/${jobPostId}`);
  };

  const handleNotificationToggle = () => {
    if (!isNotificationOpen) fetchScraps();
    setIsNotificationOpen((prev) => !prev);
  };

  // 모바일 메뉴 항목 공통 스타일 헬퍼
  const mobileItemClass = (active: boolean) =>
    `w-full text-left px-4 py-3 text-sm transition-colors ${
      active
        ? "text-[#378ADD] font-medium bg-[#F0F7FF]"
        : "text-[#1A1A1A] hover:bg-[#F5F5F5]"
    }`;

  const notificationPanel = (
    <div className="w-full lg:w-80 bg-white border border-[#DDDDDD] rounded-lg shadow-lg overflow-hidden">
      <div className="px-4 py-3 border-b border-[#DDDDDD]">
        <p className="text-[14px] font-semibold text-[#1A1A1A]">
          마감 임박 알림
        </p>
        <p className="text-[12px] text-[#888780] mt-0.5">
          {notificationCount > 0
            ? `${notificationCount}개의 지원예정 공고가 마감 임박`
            : "지원예정 공고의 마감일을 확인합니다"}
        </p>
      </div>

      {notificationCount === 0 ? (
        <p className="px-4 py-6 text-center text-[13px] text-[#888780]">
          마감 임박 공고가 없습니다.
        </p>
      ) : (
        <ul className="max-h-80 overflow-y-auto">
          {urgentScraps.map((scrap) => (
            <li key={scrap.scrapId}>
              <button
                type="button"
                onClick={() => handleNotificationItemClick(scrap.jobPostId)}
                className="w-full text-left px-4 py-3 border-b border-[#EEEEEE] last:border-b-0 hover:bg-[#F0F7FF] transition-colors"
              >
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <p className="text-[12px] text-[#888780] truncate">
                      {scrap.company}
                    </p>
                    <p className="text-[13px] font-medium text-[#1A1A1A] truncate mt-0.5">
                      {scrap.title}
                    </p>
                  </div>
                  <span className="shrink-0 text-[11px] font-semibold text-[#A32D2D] bg-[#FCEBEB] px-2 py-0.5 rounded-full">
                    {formatDday(scrap.dday ?? 0)}
                  </span>
                </div>
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );

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
          채용 트렌드
        </button>
      </div>

      <div className="hidden lg:flex ml-auto items-center gap-3">
        {user ? (
          <>
            <div className="relative">
              <button
                type="button"
                onClick={handleNotificationToggle}
                className="relative p-2 text-[#888780] hover:text-[#1A1A1A] transition-colors"
                aria-label="마감 임박 알림"
                aria-expanded={isNotificationOpen}
              >
                <FiBell size={19} />
                {notificationCount > 0 && (
                  <span className="absolute -top-0.5 -right-0.5 min-w-4 h-4 px-1 rounded-full bg-[#A32D2D] text-white text-[10px] leading-4 text-center font-semibold">
                    {notificationBadge}
                  </span>
                )}
              </button>
              {isNotificationOpen && (
                <div className="absolute right-0 top-10 z-50">
                  {notificationPanel}
                </div>
              )}
            </div>
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
            채용 트렌드
          </button>
          {user ? (
            <>
              <button
                onClick={handleNotificationToggle}
                className={`${mobileItemClass(false)} flex items-center justify-between`}
                aria-expanded={isNotificationOpen}
              >
                <span>알림</span>
                {notificationCount > 0 && (
                  <span className="min-w-5 h-5 px-1 rounded-full bg-[#A32D2D] text-white text-[11px] leading-5 text-center font-semibold">
                    {notificationBadge}
                  </span>
                )}
              </button>
              {isNotificationOpen && (
                <div className="px-4 py-2">{notificationPanel}</div>
              )}
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
