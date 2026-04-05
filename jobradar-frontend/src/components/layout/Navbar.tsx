import { useNavigate } from "react-router-dom";
import useAuthStore from "../../store/authStore";
import useAuth from "../../hooks/useAuth";

const Navbar = () => {
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const { handleLogout } = useAuth();

  return (
    <nav className="w-full h-14 bg-white border-b border-[#DDDDDD] flex items-center px-6">
      {/* 로고 */}
      <span
        className="text-[#378ADD] font-bold text-lg cursor-pointer"
        onClick={() => navigate("/")}
      >
        JobRadar
      </span>

      {/* 우측 영역 */}
      <div className="ml-auto flex items-center gap-3">
        {user ? (
          <>
            <span className="text-sm text-[#1A1A1A]">{user.nickname}</span>
            <button
              onClick={handleLogout}
              className="text-sm text-[#888780] hover:text-[#1A1A1A]"
            >
              로그아웃
            </button>
          </>
        ) : (
          <button
            onClick={() => navigate("/login")}
            className="text-sm bg-[#378ADD] text-white px-4 py-1.5 rounded-md hover:opacity-90"
          >
            로그인
          </button>
        )}
      </div>
    </nav>
  );
};

export default Navbar;
