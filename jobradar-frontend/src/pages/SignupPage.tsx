import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { signup } from "../api/authApi";
import { usePageTitle } from "../hooks/usePageTitle";

/**
 * SignupPage — 회원가입 페이지 컴포넌트
 * 입력값 상태 관리, 실시간 유효성 검사, API 연동, 완료 화면 전환 담당
 */
const SignupPage = () => {
  usePageTitle("회원가입");

  // 각 입력 필드 상태
  const [nickname, setNickname] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  // 비밀번호 표시/숨김 상태
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);

  // API 에러 메시지 상태
  const [error, setError] = useState("");

  // 가입 성공 여부 — true가 되면 완료 화면으로 전환
  const [isSuccess, setIsSuccess] = useState(false);

  // 가입 성공 후 / 로 이동하기 위한 navigate
  const navigate = useNavigate();

  // ── 유효성 검사 ──────────────────────────────────────
  const isNicknameValid = nickname.length > 0;
  const isEmailValid = email.includes("@");
  // 비밀번호 3가지 규칙 — 각각 독립적으로 관리해서 UI에 실시간 표시
  const hasMinLength = password.length >= 8;
  const hasLetter = /[a-zA-Z]/.test(password);
  const hasNumber = /[0-9]/.test(password);
  const isPasswordValid = hasMinLength && hasLetter && hasNumber;
  const isConfirmValid =
    confirmPassword.length > 0 && password === confirmPassword;

  // 버튼 활성화: 모든 유효성 통과 시에만 활성화
  const isFormValid =
    isNicknameValid &&
    isEmailValid &&
    isPasswordValid &&
    isConfirmValid;

  // ── 폼 제출 핸들러 ────────────────────────────────────
  const handleSubmit = async (e: { preventDefault: () => void }) => {
    e.preventDefault();
    if (!isFormValid) return;
    setError("");
    try {
      await signup(email, password, nickname);
      // 성공 시 완료 화면으로 전환 (페이지 이동 아닌 조건부 렌더링)
      setIsSuccess(true);
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string } } };
      setError(axiosErr.response?.data?.message ?? "");
    }
  };

  // ── 가입 완료 화면 ────────────────────────────────────
  // isSuccess가 true면 카드 내용 전체를 완료 화면으로 교체
  if (isSuccess) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-[#F5F5F5]">
        <div className="bg-white rounded-xl p-7 w-full max-w-[380px] border border-[#DDDDDD] flex flex-col items-center gap-4">
          {/* 초록 체크 아이콘 원 */}
          <div
            className="w-16 h-16 rounded-full flex items-center justify-center text-2xl"
            style={{ backgroundColor: "#EAF3DE" }}
          >
            ✓
          </div>
          <h1 className="text-[18px] font-medium">가입 완료!</h1>
          <p className="text-center text-[13px] text-[#888780]">
            {nickname}님, 환영합니다.
            <br />
            지금 바로 채용공고를 확인해보세요.
          </p>
          {/* / 로 이동 */}
          <button
            onClick={() => navigate("/")}
            className="w-full h-[40px] bg-[#378ADD] text-white rounded-lg text-[14px] font-medium"
          >
            공고 보러 가기
          </button>
        </div>
      </div>
    );
  }

  // ── 회원가입 폼 화면 ──────────────────────────────────
  return (
    <div className="min-h-screen flex items-center justify-center bg-[#F5F5F5]">
      <div className="bg-white rounded-xl p-7 w-full max-w-[380px] border border-[#DDDDDD]">

        {/* 1. 로고 영역 */}
        <div className="flex items-center justify-center gap-2 mb-6">
          <div className="w-2.5 h-2.5 rounded-full bg-[#378ADD]" />
          <span className="text-[18px] font-medium">JobRadar</span>
        </div>

        {/* 2. 제목 */}
        <h1 className="text-center text-[18px] font-medium mb-1">회원가입</h1>

        {/* 3. 부제목 */}
        <p className="text-center text-[13px] text-[#888780] mb-6">
          무료로 시작하세요
        </p>

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">

          {/* 4. 닉네임 입력 */}
          <div className="flex flex-col gap-1">
            <label className="text-[12px] font-medium text-[#888780]">
              닉네임
            </label>
            <input
              type="text"
              placeholder="닉네임을 입력하세요"
              value={nickname}
              onChange={(e) => setNickname(e.target.value)}
              className="h-[38px] border border-[#DDDDDD] rounded-lg bg-[#F5F5F5] px-3 text-sm outline-none focus:border-[#378ADD] focus:bg-white transition-colors"
            />
          </div>

          {/* 5. 이메일 입력 */}
          <div className="flex flex-col gap-1">
            <label className="text-[12px] font-medium text-[#888780]">
              이메일
            </label>
            <input
              type="email"
              placeholder="이메일을 입력하세요"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="h-[38px] border border-[#DDDDDD] rounded-lg bg-[#F5F5F5] px-3 text-sm outline-none focus:border-[#378ADD] focus:bg-white transition-colors"
            />
            {email && !isEmailValid && (
              <p className="text-[11px] text-[#E24B4A]">
                올바른 이메일 형식을 입력하세요.
              </p>
            )}
          </div>

          {/* 6. 비밀번호 입력 + 실시간 유효성 표시 */}
          <div className="flex flex-col gap-1">
            <label className="text-[12px] font-medium text-[#888780]">
              비밀번호
            </label>
            <div className="relative">
              <input
                type={showPassword ? "text" : "password"}
                placeholder="비밀번호를 입력하세요"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full h-[38px] border border-[#DDDDDD] rounded-lg bg-[#F5F5F5] pl-3 pr-12 text-sm outline-none focus:border-[#378ADD] focus:bg-white transition-colors"
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-[11px] text-[#888780]"
              >
                {showPassword ? "숨김" : "표시"}
              </button>
            </div>
            {/* 비밀번호 입력 시에만 유효성 규칙 표시 */}
            {password && (
              <div className="flex flex-col gap-1 mt-1">
                {/* 규칙마다 충족 여부에 따라 점 색상 변경 */}
                <ValidationRule met={hasMinLength} text="8자 이상" />
                <ValidationRule met={hasLetter} text="영문 포함" />
                <ValidationRule met={hasNumber} text="숫자 포함" />
              </div>
            )}
          </div>

          {/* 7. 비밀번호 확인 */}
          <div className="flex flex-col gap-1">
            <label className="text-[12px] font-medium text-[#888780]">
              비밀번호 확인
            </label>
            <div className="relative">
              <input
                type={showConfirm ? "text" : "password"}
                placeholder="비밀번호를 다시 입력하세요"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                className="w-full h-[38px] border border-[#DDDDDD] rounded-lg bg-[#F5F5F5] pl-3 pr-12 text-sm outline-none focus:border-[#378ADD] focus:bg-white transition-colors"
              />
              <button
                type="button"
                onClick={() => setShowConfirm(!showConfirm)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-[11px] text-[#888780]"
              >
                {showConfirm ? "숨김" : "표시"}
              </button>
            </div>
            {/* 입력값이 있을 때만 일치 여부 표시 */}
            {confirmPassword && (
              <p
                className="text-[11px]"
                style={{ color: isConfirmValid ? "#1D9E75" : "#E24B4A" }}
              >
                {isConfirmValid
                  ? "비밀번호가 일치합니다."
                  : "비밀번호가 일치하지 않습니다."}
              </p>
            )}
          </div>

          {/* API 에러 메시지 */}
          {error && <p className="text-[11px] text-[#E24B4A]">{error}</p>}

          {/* 8. 회원가입 버튼 */}
          <button
            type="submit"
            disabled={!isFormValid}
            className="w-full h-[40px] bg-[#378ADD] text-white rounded-lg text-[14px] font-medium transition-opacity disabled:opacity-50 disabled:cursor-not-allowed"
          >
            회원가입
          </button>
        </form>

        {/* 9. 로그인 링크 */}
        <p className="text-center text-[13px] text-[#888780] mt-4">
          이미 계정이 있으신가요?{" "}
          <Link to="/login" className="text-[#378ADD]">
            로그인
          </Link>
        </p>
      </div>
    </div>
  );
};

/**
 * ValidationRule — 비밀번호 유효성 규칙 표시 컴포넌트
 * met: 규칙 충족 여부, text: 규칙 설명 텍스트
 * 충족 시 초록 점, 미충족 시 회색 점으로 표시
 */
const ValidationRule = ({ met, text }: { met: boolean; text: string }) => (
  <div className="flex items-center gap-1.5">
    <div
      className="w-1.5 h-1.5 rounded-full"
      style={{ backgroundColor: met ? "#1D9E75" : "#DDDDDD" }}
    />
    <span
      className="text-[11px]"
      style={{ color: met ? "#1D9E75" : "#888780" }}
    >
      {text}
    </span>
  </div>
);

export default SignupPage;
