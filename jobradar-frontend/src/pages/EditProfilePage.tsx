/**
 * EditProfilePage — 개인정보 수정 페이지 (개인정보 수정 2단계)
 * 닉네임 수정, 비밀번호 변경, 회원 탈퇴 기능을 제공한다.
 */

import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import {
  getMe,
  updateNickname,
  updatePassword,
  withdrawUser,
} from "../api/userApi";
import { usePageTitle } from "../hooks/usePageTitle";
import useAuthStore from "../store/authStore";

const EditProfilePage = () => {
  usePageTitle("정보 수정");
  const navigate = useNavigate();

  // 현재 닉네임 — API에서 불러와 표시
  const [currentNickname, setCurrentNickname] = useState("");

  // 새 닉네임 입력값 상태
  const [newNickname, setNewNickname] = useState("");

  // 새 비밀번호 입력값 상태
  const [newPassword, setNewPassword] = useState("");

  // 새 비밀번호 확인 입력값 상태
  const [newPasswordConfirm, setNewPasswordConfirm] = useState("");

  // 각 섹션별 로딩 상태 (닉네임/비밀번호/탈퇴 따로 관리)
  const [nicknameLoading, setNicknameLoading] = useState(false);
  const [passwordLoading, setPasswordLoading] = useState(false);
  const [withdrawLoading, setWithdrawLoading] = useState(false);

  // 각 섹션별 에러/성공 메시지 상태
  const [nicknameMsg, setNicknameMsg] = useState({ type: "", text: "" });
  const [passwordMsg, setPasswordMsg] = useState({ type: "", text: "" });

  // 컴포넌트 마운트 시 현재 닉네임 불러옴
  // useEffect: 렌더링 후 API 호출(사이드 이펙트)을 처리하기 위해 사용
  useEffect(() => {
    getMe()
      .then((res) => setCurrentNickname(res.data.data.nickname))
      .catch(() => {});
  }, []);

  // 닉네임 수정 핸들러
  const handleNicknameUpdate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newNickname) return;

    try {
      setNicknameLoading(true);
      setNicknameMsg({ type: "", text: "" });
      await updateNickname(newNickname);
      setCurrentNickname(newNickname); // 로컬 상태도 업데이트
      setNewNickname("");
      setNicknameMsg({ type: "success", text: "닉네임이 변경되었습니다." });
    } catch {
      setNicknameMsg({ type: "error", text: "닉네임 변경에 실패했습니다." });
    } finally {
      setNicknameLoading(false);
    }
  };

  // 비밀번호 변경 핸들러
  const handlePasswordUpdate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newPassword || !newPasswordConfirm) return;

    // 클라이언트 측 비밀번호 일치 검사
    if (newPassword !== newPasswordConfirm) {
      setPasswordMsg({ type: "error", text: "비밀번호가 일치하지 않습니다." });
      return;
    }

    try {
      setPasswordLoading(true);
      setPasswordMsg({ type: "", text: "" });
      await updatePassword(newPassword);
      setNewPassword("");
      setNewPasswordConfirm("");
      setPasswordMsg({ type: "success", text: "비밀번호가 변경되었습니다." });
    } catch {
      setPasswordMsg({ type: "error", text: "비밀번호 변경에 실패했습니다." });
    } finally {
      setPasswordLoading(false);
    }
  };

  // 회원 탈퇴 핸들러
  const handleWithdraw = async () => {
    if (!window.confirm("정말 탈퇴하시겠습니까? 모든 데이터가 삭제됩니다."))
      return;

    try {
      setWithdrawLoading(true);
      await withdrawUser();
      // 탈퇴 성공 시 토큰 삭제 후 로그인 페이지로 이동
      useAuthStore.getState().logout();
      navigate("/login");
    } catch {
      alert("회원 탈퇴에 실패했습니다.");
    } finally {
      setWithdrawLoading(false);
    }
  };

  return (
    <main className="min-h-screen bg-[#F5F5F5] px-4 py-8">
      <div className="max-w-4xl mx-auto flex flex-col gap-6">
        {/* 상단 스텝 인디케이터 */}
        <div className="flex items-center gap-3">
          {/* 1단계: 완료 */}
          <div className="flex items-center gap-2">
            <div className="w-7 h-7 rounded-full bg-[#DDDDDD] text-[#888780] text-[12px] font-bold flex items-center justify-center">
              1
            </div>
            <span className="text-[13px] text-[#888780]">비밀번호 확인</span>
          </div>

          {/* 구분선 */}
          <div className="flex-1 h-px bg-[#DDDDDD]" />

          {/* 2단계: 활성 */}
          <div className="flex items-center gap-2">
            <div className="w-7 h-7 rounded-full bg-[#378ADD] text-white text-[12px] font-bold flex items-center justify-center">
              2
            </div>
            <span className="text-[13px] font-medium text-[#378ADD]">
              정보 수정
            </span>
          </div>
        </div>

        {/* 닉네임 수정 섹션 */}
        <section className="bg-white border border-[#DDDDDD] rounded-xl p-6">
          <h2 className="text-[16px] font-semibold mb-4">닉네임 수정</h2>
          <form onSubmit={handleNicknameUpdate}>
            {/* 2열 배치 */}
            <div className="grid grid-cols-2 gap-4">
              {/* 현재 닉네임 (읽기 전용) */}
              <div className="flex flex-col gap-1">
                <label className="text-[12px] font-medium text-[#888780]">
                  현재 닉네임
                </label>
                <input
                  type="text"
                  value={currentNickname}
                  readOnly
                  className="h-[38px] border border-[#DDDDDD] rounded-lg bg-[#F5F5F5] px-3 text-sm text-[#888780] outline-none"
                />
              </div>

              {/* 새 닉네임 입력 */}
              <div className="flex flex-col gap-1">
                <label className="text-[12px] font-medium text-[#888780]">
                  새 닉네임
                </label>
                <input
                  type="text"
                  placeholder="새 닉네임을 입력하세요"
                  value={newNickname}
                  onChange={(e) => setNewNickname(e.target.value)}
                  className="h-[38px] border border-[#DDDDDD] rounded-lg bg-[#F5F5F5] px-3 text-sm outline-none focus:border-[#378ADD] focus:bg-white transition-colors"
                />
              </div>
            </div>

            {/* 피드백 메시지 */}
            {nicknameMsg.text && (
              <p
                className={`text-[11px] mt-2 ${nicknameMsg.type === "success" ? "text-[#27500A]" : "text-[#A32D2D]"}`}
              >
                {nicknameMsg.text}
              </p>
            )}

            <button
              type="submit"
              disabled={!newNickname || nicknameLoading}
              className="mt-4 h-[38px] px-5 bg-[#378ADD] text-white rounded-lg text-[13px] font-medium transition-opacity disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {nicknameLoading ? "변경 중..." : "닉네임 변경"}
            </button>
          </form>
        </section>

        {/* 비밀번호 변경 섹션 */}
        <section className="bg-white border border-[#DDDDDD] rounded-xl p-6">
          <h2 className="text-[16px] font-semibold mb-4">비밀번호 변경</h2>
          <form onSubmit={handlePasswordUpdate}>
            {/* 2열 배치 */}
            <div className="grid grid-cols-2 gap-4">
              {/* 새 비밀번호 */}
              <div className="flex flex-col gap-1">
                <label className="text-[12px] font-medium text-[#888780]">
                  새 비밀번호
                </label>
                <input
                  type="password"
                  placeholder="새 비밀번호를 입력하세요"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  className="h-[38px] border border-[#DDDDDD] rounded-lg bg-[#F5F5F5] px-3 text-sm outline-none focus:border-[#378ADD] focus:bg-white transition-colors"
                />
              </div>

              {/* 새 비밀번호 확인 */}
              <div className="flex flex-col gap-1">
                <label className="text-[12px] font-medium text-[#888780]">
                  새 비밀번호 확인
                </label>
                <input
                  type="password"
                  placeholder="새 비밀번호를 다시 입력하세요"
                  value={newPasswordConfirm}
                  onChange={(e) => setNewPasswordConfirm(e.target.value)}
                  className="h-[38px] border border-[#DDDDDD] rounded-lg bg-[#F5F5F5] px-3 text-sm outline-none focus:border-[#378ADD] focus:bg-white transition-colors"
                />
              </div>
            </div>

            {/* 피드백 메시지 */}
            {passwordMsg.text && (
              <p
                className={`text-[11px] mt-2 ${passwordMsg.type === "success" ? "text-[#27500A]" : "text-[#A32D2D]"}`}
              >
                {passwordMsg.text}
              </p>
            )}

            <button
              type="submit"
              disabled={!newPassword || !newPasswordConfirm || passwordLoading}
              className="mt-4 h-[38px] px-5 bg-[#378ADD] text-white rounded-lg text-[13px] font-medium transition-opacity disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {passwordLoading ? "변경 중..." : "비밀번호 변경"}
            </button>
          </form>
        </section>

        {/* 회원 탈퇴 섹션 */}
        <section className="bg-white border border-[#DDDDDD] rounded-xl p-6">
          <h2 className="text-[16px] font-semibold mb-2">회원 탈퇴</h2>
          <p className="text-[13px] text-[#888780] mb-4">
            탈퇴 시 모든 스크랩 및 지원 현황 데이터가 영구 삭제되며 복구할 수
            없습니다.
          </p>
          <button
            type="button"
            onClick={handleWithdraw}
            disabled={withdrawLoading}
            className="h-[38px] px-5 border border-[#A32D2D] text-[#A32D2D] rounded-lg text-[13px] font-medium hover:bg-[#FCEBEB] transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {withdrawLoading ? "처리 중..." : "회원 탈퇴"}
          </button>
        </section>

        {/* 뒤로가기 버튼 */}
        <button
          type="button"
          onClick={() => navigate("/my")}
          className="self-start text-[13px] text-[#888780] hover:text-[#378ADD] transition-colors"
        >
          ← 마이페이지로 돌아가기
        </button>
      </div>
    </main>
  );
};

export default EditProfilePage;
