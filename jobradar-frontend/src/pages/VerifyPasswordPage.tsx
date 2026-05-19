/**
 * VerifyPasswordPage — 비밀번호 확인 페이지 (개인정보 수정 1단계)
 * 현재 비밀번호를 확인한 후 /my/edit 으로 이동한다.
 * 스텝 인디케이터로 현재 단계(1/2)를 시각적으로 표시한다.
 */

import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { verifyPassword } from "../api/authApi";
import { usePageTitle } from "../hooks/usePageTitle";

const VerifyPasswordPage = () => {
  usePageTitle("비밀번호 확인");
  const navigate = useNavigate();

  // 현재 비밀번호 입력값 상태
  const [password, setPassword] = useState("");

  // 비밀번호 표시/숨김 상태
  const [showPassword, setShowPassword] = useState(false);

  // API 요청 중 로딩 상태 — true일 때 버튼 비활성화
  const [loading, setLoading] = useState(false);

  // API 에러 메시지 상태
  const [error, setError] = useState("");

  // 확인 버튼 핸들러
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault(); // HTML form 기본 동작(페이지 새로고침) 방지
    if (!password) return;

    try {
      setLoading(true);
      setError("");
      await verifyPassword(password);
      // 비밀번호 확인 성공 시 개인정보 수정 페이지로 이동
      navigate("/my/edit");
    } catch {
      setError("비밀번호가 올바르지 않습니다.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="min-h-screen bg-[#F5F5F5] px-4 py-8">
      <div className="max-w-4xl mx-auto flex flex-col gap-6">

        {/* 상단 스텝 인디케이터 */}
        <div className="flex items-center gap-3">
          {/* 1단계: 활성 */}
          <div className="flex items-center gap-2">
            <div className="w-7 h-7 rounded-full bg-[#378ADD] text-white text-[12px] font-bold flex items-center justify-center">
              1
            </div>
            <span className="text-[13px] font-medium text-[#378ADD]">비밀번호 확인</span>
          </div>

          {/* 구분선 */}
          <div className="flex-1 h-px bg-[#DDDDDD]" />

          {/* 2단계: 비활성 */}
          <div className="flex items-center gap-2">
            <div className="w-7 h-7 rounded-full bg-[#DDDDDD] text-[#888780] text-[12px] font-bold flex items-center justify-center">
              2
            </div>
            <span className="text-[13px] text-[#888780]">정보 수정</span>
          </div>
        </div>

        {/* 비밀번호 확인 카드 */}
        <section className="bg-white border border-[#DDDDDD] rounded-xl p-8">
          <h2 className="text-[18px] font-semibold mb-1">비밀번호 확인</h2>
          <p className="text-[13px] text-[#888780] mb-6">
            개인정보 보호를 위해 현재 비밀번호를 확인합니다.
          </p>

          <form onSubmit={handleSubmit} className="flex flex-col gap-4">
            <div className="flex flex-col gap-1">
              <label className="text-[12px] font-medium text-[#888780]">
                현재 비밀번호
              </label>
              <div className="relative">
                <input
                  type={showPassword ? "text" : "password"}
                  placeholder="현재 비밀번호를 입력하세요"
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
            </div>

            {/* 에러 메시지 */}
            {error && <p className="text-[11px] text-[#A32D2D]">{error}</p>}

            {/* 버튼 영역 */}
            <div className="flex gap-3 mt-2">
              {/* 뒤로가기 */}
              <button
                type="button"
                onClick={() => navigate("/my")}
                className="flex-1 h-[40px] border border-[#DDDDDD] rounded-lg text-[14px] text-[#888780] hover:bg-[#F5F5F5] transition-colors"
              >
                뒤로가기
              </button>

              {/* 확인 버튼 */}
              <button
                type="submit"
                disabled={!password || loading}
                className="flex-1 h-[40px] bg-[#378ADD] text-white rounded-lg text-[14px] font-medium transition-opacity disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {loading ? "확인 중..." : "확인"}
              </button>
            </div>
          </form>
        </section>

      </div>
    </main>
  );
};

export default VerifyPasswordPage;
