import { useState } from "react";
import { Link } from "react-router-dom";
import useAuth from "../hooks/useAuth";

/**
 * LoginPage — 로그인 페이지 컴포넌트
 * 이메일/비밀번호 입력, 유효성 검사, API 연동까지 담당
 */
const LoginPage = () => {
  // 이메일 입력값 상태 — onChange로 입력할 때마다 갱신
  const [email, setEmail] = useState("");

  // 비밀번호 입력값 상태
  const [password, setPassword] = useState("");

  // 비밀번호 표시/숨김 상태 (false = 숨김, true = 표시)
  const [showPassword, setShowPassword] = useState(false);

  // API 요청 실패 시 표시할 에러 메시지 상태
  const [error, setError] = useState("");

  // useAuth 훅에서 로그인 처리 함수 가져옴
  const { handleLogin } = useAuth();

  // 이메일 유효성 검사: @ 포함 여부만 확인 (간단한 검사)
  const isEmailValid = email.includes("@");

  // 폼 전체 유효성: 이메일(@ 포함) + 비밀번호 1자 이상 입력 시 버튼 활성화
  const isFormValid = isEmailValid && password.length > 0;

  /**
   * 폼 제출 핸들러
   * async: API 응답을 기다려야 하므로 비동기 함수로 선언
   */
  const handleSubmit = async (e: { preventDefault: () => void }) => {
    // HTML 폼의 기본 동작(페이지 새로고침)을 막음
    e.preventDefault();
    if (!isFormValid) return;

    setError(""); // 이전 에러 초기화
    try {
      await handleLogin(email, password);
      // 성공 시 useAuth 내부에서 / 로 이동
    } catch {
      // 실패 시 에러 메시지 표시
      setError("이메일 또는 비밀번호가 올바르지 않습니다.");
    }
  };

  return (
    // 페이지 전체: 연회색 배경, 수직/수평 중앙 정렬
    <div className="min-h-screen flex items-center justify-center bg-[#F5F5F5]">
      {/* 흰색 카드: 최대 너비 380px, 연회색 테두리, 둥근 모서리 12px */}
      <div className="bg-white rounded-xl p-7 w-full max-w-[380px] border border-[#DDDDDD]">
        {/* 1. 로고 영역 — 파란 원 + 서비스명 */}
        <div className="flex items-center justify-center gap-2 mb-6">
          <div className="w-2.5 h-2.5 rounded-full bg-[#378ADD]" />
          <span className="text-[18px] font-medium">JobRadar</span>
        </div>

        {/* 2. 제목 */}
        <h1 className="text-center text-[18px] font-medium mb-1">로그인</h1>

        {/* 3. 부제목 */}
        <p className="text-center text-[13px] text-[#888780] mb-6">
          계정에 로그인하세요
        </p>

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          {/* 4. 이메일 입력 필드 */}
          <div className="flex flex-col gap-1">
            <label className="text-[12px] font-medium text-[#888780]">
              이메일
            </label>
            <input
              type="email"
              placeholder="이메일을 입력하세요"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              // focus: 테두리 파란색, 배경 흰색으로 변경
              className="h-[38px] border border-[#DDDDDD] rounded-lg bg-[#F5F5F5] px-3 text-sm outline-none focus:border-[#378ADD] focus:bg-white transition-colors"
            />
            {/* 입력값이 있고 @ 없을 때만 에러 표시 (입력 전에는 에러 안 보임) */}
            {email && !isEmailValid && (
              <p className="text-[11px] text-[#E24B4A]">
                올바른 이메일 형식을 입력하세요.
              </p>
            )}
          </div>

          {/* 5. 비밀번호 입력 필드 */}
          <div className="flex flex-col gap-1">
            <label className="text-[12px] font-medium text-[#888780]">
              비밀번호
            </label>
            {/* relative: 표시/숨김 버튼을 input 오른쪽에 절대 위치로 배치 */}
            <div className="relative">
              {/* showPassword 상태에 따라 type 전환 */}
              <input
                type={showPassword ? "text" : "password"}
                placeholder="비밀번호를 입력하세요"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full h-[38px] border border-[#DDDDDD] rounded-lg bg-[#F5F5F5] pl-3 pr-12 text-sm outline-none focus:border-[#378ADD] focus:bg-white transition-colors"
              />
              {/* 비밀번호 표시/숨김 토글 버튼 */}
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-[11px] text-[#888780]"
              >
                {showPassword ? "숨김" : "표시"}
              </button>
            </div>
          </div>

          {/* API 실패 에러 메시지 */}
          {error && <p className="text-[11px] text-[#E24B4A]">{error}</p>}

          {/* 6. 비밀번호 찾기 링크 — 오른쪽 정렬 */}
          <div className="flex justify-end">
            <a href="#" className="text-[12px] text-[#378ADD]">
              비밀번호 찾기
            </a>
          </div>

          {/* 7. 로그인 버튼 — isFormValid가 false면 disabled, opacity 50% */}
          <button
            type="submit"
            disabled={!isFormValid}
            className="w-full h-[40px] bg-[#378ADD] text-white rounded-lg text-[14px] font-medium transition-opacity disabled:opacity-50 disabled:cursor-not-allowed"
          >
            로그인
          </button>
        </form>

        {/* 8. 구분선 — 양쪽 선 + 가운데 "또는" */}
        <div className="flex items-center gap-3 my-4">
          <div className="flex-1 h-px bg-[#DDDDDD]" />
          <span className="text-[12px] text-[#888780]">또는</span>
          <div className="flex-1 h-px bg-[#DDDDDD]" />
        </div>

        {/* 9. 회원가입 링크 */}
        <p className="text-center text-[13px] text-[#888780]">
          계정이 없으신가요?{" "}
          <Link to="/signup" className="text-[#378ADD]">
            회원가입
          </Link>
        </p>
      </div>
    </div>
  );
};

export default LoginPage;
