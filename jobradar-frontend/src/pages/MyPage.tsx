/**
 * MyPage — 마이페이지 메인 컴포넌트
 * 로그인한 사용자의 닉네임/이메일 표시, 스크랩 통계, 탭 구조(스크랩 공고/지원 현황)를 제공한다.
 */

import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { getMe } from "../api/userApi";
import { getScraps, updateScrapStatus, deleteScrap } from "../api/scrapApi";
import type { ScrapStatus } from "../api/scrapApi";

// 사용자 정보 타입 — API GET /api/users/me 응답 구조
interface UserInfo {
  email: string;
  nickname: string;
}

// 스크랩 아이템 타입 — API GET /api/scraps 응답 구조
interface ScrapItem {
  id: number;
  jobTitle: string;
  companyName: string;
  deadline: string; // "2025-05-01" 같은 날짜 문자열
  status: ScrapStatus; // "PENDING" | "APPLIED" | "REVIEWING" | "REJECTED"
}

// D-day 계산 함수: 마감일까지 남은 일수 반환
// 예: "2025-05-01" 넣으면 오늘 기준 몇 일 남았는지 숫자로 반환
const calcDday = (deadline: string): number => {
  const today = new Date();
  today.setHours(0, 0, 0, 0); // 시/분/초 제거 → 날짜만 비교하기 위해
  const end = new Date(deadline);
  end.setHours(0, 0, 0, 0);
  return Math.ceil((end.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
};

// 상태별 배지 색상 (Record<키타입, 값타입>: 4가지 키 모두 있어야 TS 에러 안 남)
const statusBadgeStyle: Record<ScrapStatus, string> = {
  PENDING: "bg-[#E6F1FB] text-[#0C447C]",
  APPLIED: "bg-[#EAF3DE] text-[#27500A]",
  REVIEWING: "bg-[#FAEEDA] text-[#633806]",
  REJECTED: "bg-[#FCEBEB] text-[#A32D2D]",
};

// 상태별 한글 라벨
const statusLabel: Record<ScrapStatus, string> = {
  PENDING: "지원예정",
  APPLIED: "지원완료",
  REVIEWING: "서류검토",
  REJECTED: "탈락",
};

const MyPage = () => {
  const navigate = useNavigate();

  // 사용자 정보 상태 — API 응답 후 닉네임/이메일 표시에 사용
  // <UserInfo | null>: 처음엔 데이터 없으니 null로 초기화
  const [user, setUser] = useState<UserInfo | null>(null);

  // 스크랩 목록 상태 — <ScrapItem[]>: ScrapItem 배열, 초기값 빈 배열
  const [scraps, setScraps] = useState<ScrapItem[]>([]);

  // 현재 활성 탭 — "scrap" 또는 "apply" 두 값만 허용
  const [activeTab, setActiveTab] = useState<"scrap" | "apply">("scrap");

  // 스크랩 탭 필터 — "ALL" 또는 ScrapStatus 4가지
  const [scrapFilter, setScrapFilter] = useState<"ALL" | ScrapStatus>("ALL");

  // 지원현황 탭 필터
  const [applyFilter, setApplyFilter] = useState<"ALL" | ScrapStatus>("ALL");

  // API 로딩 중 여부 — true일 때 스피너 표시
  const [loading, setLoading] = useState(true);

  // API 에러 메시지 — 빈 문자열이면 에러 없음
  const [error, setError] = useState("");

  // useEffect: 컴포넌트가 화면에 처음 렌더링된 후 실행
  // 의존성 배열 []이 비어있으면 → 마운트 시 딱 한 번만 실행
  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        // Promise.all: 두 API를 동시에 호출 → 순차 호출보다 빠름
        const [userRes, scrapRes] = await Promise.all([
          getMe(),
          getScraps(),
        ]);
        setUser(userRes.data.data);
        setScraps(scrapRes.data.data ?? []); // ??: null/undefined면 빈 배열
      } catch {
        setError("데이터를 불러오는 데 실패했습니다.");
      } finally {
        setLoading(false); // 성공/실패 상관없이 로딩 종료
      }
    };

    fetchData();
  }, []); // [] = 마운트 시 한 번만 실행

  // 스크랩 상태 변경 핸들러
  const handleStatusChange = async (id: number, status: ScrapStatus) => {
    try {
      await updateScrapStatus(id, status);
      // API 성공 후 전체 재요청 없이 해당 항목만 로컬에서 업데이트
      // prev.map: id 일치하는 것만 status 교체, 나머지는 그대로
      setScraps((prev) =>
        prev.map((s) => (s.id === id ? { ...s, status } : s))
      );
    } catch {
      alert("상태 변경에 실패했습니다.");
    }
  };

  // 스크랩 삭제 핸들러
  const handleDelete = async (id: number) => {
    if (!window.confirm("스크랩을 삭제하시겠습니까?")) return;
    try {
      await deleteScrap(id);
      // filter: id 일치하지 않는 것만 남김 → 해당 항목 제거 효과
      setScraps((prev) => prev.filter((s) => s.id !== id));
    } catch {
      alert("삭제에 실패했습니다.");
    }
  };

  // 파생 데이터: scraps 배열에서 계산 (별도 state 불필요)
  const totalCount = scraps.length;
  const pendingCount = scraps.filter((s) => s.status === "PENDING").length;
  const appliedCount = scraps.filter((s) => s.status === "APPLIED").length;
  const reviewingCount = scraps.filter((s) => s.status === "REVIEWING").length;

  // 필터 적용된 스크랩 목록
  const filteredScraps =
    scrapFilter === "ALL" ? scraps : scraps.filter((s) => s.status === scrapFilter);

  // 필터 적용된 지원현황 목록
  const filteredApply =
    applyFilter === "ALL" ? scraps : scraps.filter((s) => s.status === applyFilter);

  // 로딩 중이면 스피너만 표시 (나머지 JSX는 렌더링 안 함)
  if (loading) {
    return (
      <div className="min-h-screen bg-[#F5F5F5] flex items-center justify-center">
        <div className="w-8 h-8 border-4 border-[#378ADD] border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  // 에러 발생 시 메시지만 표시
  if (error) {
    return (
      <div className="min-h-screen bg-[#F5F5F5] flex items-center justify-center">
        <p className="text-[#A32D2D] text-sm">{error}</p>
      </div>
    );
  }

  return (
    <main className="min-h-screen bg-[#F5F5F5] px-4 py-8">
      <div className="max-w-4xl mx-auto flex flex-col gap-5">

        {/* 1. 사용자 정보 카드 */}
        <div className="bg-white border border-[#DDDDDD] rounded-xl p-6 flex items-center justify-between">
          <div>
            <h2 className="text-[18px] font-semibold">{user?.nickname}</h2>
            <p className="text-[13px] text-[#888780] mt-1">{user?.email}</p>
          </div>
          {/* 정보 수정 버튼: 비밀번호 확인 페이지로 이동 */}
          <button
            onClick={() => navigate("/my/verify")}
            className="text-[13px] text-[#378ADD] border border-[#378ADD] rounded-lg px-4 py-2 hover:bg-[#E6F1FB] transition-colors"
          >
            정보 수정
          </button>
        </div>

        {/* 2. 요약 통계 카드 4개 */}
        <section aria-label="스크랩 통계">
          <ul className="grid grid-cols-4 gap-3">
            {[
              { label: "전체 스크랩", count: totalCount },
              { label: "지원예정",    count: pendingCount },
              { label: "지원완료",    count: appliedCount },
              { label: "서류검토",    count: reviewingCount },
            ].map(({ label, count }) => (
              <li
                key={label}
                className="bg-white border border-[#DDDDDD] rounded-xl p-4 flex flex-col items-center gap-1"
              >
                <p className="text-[24px] font-bold text-[#378ADD]">{count}</p>
                <p className="text-[12px] text-[#888780]">{label}</p>
              </li>
            ))}
          </ul>
        </section>

        {/* 3. 탭 구조 */}
        <section className="bg-white border border-[#DDDDDD] rounded-xl overflow-hidden">

          {/* 탭 헤더 */}
          <div role="tablist" className="flex border-b border-[#DDDDDD]">
            {(["scrap", "apply"] as const).map((tab) => (
              <button
                key={tab}
                role="tab"
                aria-selected={activeTab === tab}
                onClick={() => setActiveTab(tab)}
                className={`flex-1 py-3 text-[14px] font-medium transition-colors ${
                  activeTab === tab
                    ? "text-[#378ADD] border-b-2 border-[#378ADD]"
                    : "text-[#888780]"
                }`}
              >
                {tab === "scrap" ? "스크랩 공고" : "지원 현황"}
              </button>
            ))}
          </div>

          {/* 탭 컨텐츠 */}
          <div role="tabpanel" className="p-5">

            {/* 스크랩 공고 탭 */}
            {activeTab === "scrap" && (
              <div className="flex flex-col gap-4">

                {/* 필터 칩 */}
                <div className="flex gap-2 flex-wrap">
                  {(["ALL", "PENDING", "APPLIED", "REVIEWING", "REJECTED"] as const).map((f) => (
                    <button
                      key={f}
                      onClick={() => setScrapFilter(f)}
                      className={`px-3 py-1 rounded-full text-[12px] border transition-colors ${
                        scrapFilter === f
                          ? "bg-[#378ADD] text-white border-[#378ADD]"
                          : "bg-white text-[#888780] border-[#DDDDDD]"
                      }`}
                    >
                      {f === "ALL" ? "전체" : statusLabel[f]}
                    </button>
                  ))}
                </div>

                {/* 스크랩 카드 목록 */}
                {filteredScraps.length === 0 ? (
                  <p className="text-center text-[13px] text-[#888780] py-8">
                    스크랩한 공고가 없습니다.
                  </p>
                ) : (
                  <ul className="flex flex-col gap-3">
                    {filteredScraps.map((scrap) => {
                      const dday = calcDday(scrap.deadline);
                      // D-3 이내(0~3일)면 빨간색 표시
                      const isUrgent = dday >= 0 && dday <= 3;
                      return (
                        <li
                          key={scrap.id}
                          className="border border-[#DDDDDD] rounded-xl p-4 flex items-center justify-between"
                        >
                          {/* 공고 정보 */}
                          <div className="flex flex-col gap-1">
                            <p className="text-[14px] font-medium">{scrap.jobTitle}</p>
                            <p className="text-[12px] text-[#888780]">{scrap.companyName}</p>
                            {/* D-day: 3일 이내면 빨간색 */}
                            <p className={`text-[12px] font-medium ${isUrgent ? "text-[#A32D2D]" : "text-[#888780]"}`}>
                              {dday < 0 ? "마감" : dday === 0 ? "D-day" : `D-${dday}`}
                            </p>
                          </div>

                          {/* 우측: 상태 배지 + 드롭다운 + 삭제 */}
                          <div className="flex items-center gap-2">
                            {/* 상태 배지 */}
                            <span className={`text-[11px] px-2 py-0.5 rounded-full font-medium ${statusBadgeStyle[scrap.status]}`}>
                              {statusLabel[scrap.status]}
                            </span>

                            {/* 상태 변경 드롭다운 */}
                            <select
                              value={scrap.status}
                              onChange={(e) =>
                                handleStatusChange(scrap.id, e.target.value as ScrapStatus)
                              }
                              className="text-[12px] border border-[#DDDDDD] rounded-lg px-2 py-1 outline-none focus:border-[#378ADD] bg-white"
                            >
                              <option value="PENDING">지원예정</option>
                              <option value="APPLIED">지원완료</option>
                              <option value="REVIEWING">서류검토</option>
                              <option value="REJECTED">탈락</option>
                            </select>

                            {/* 삭제 버튼 */}
                            <button
                              onClick={() => handleDelete(scrap.id)}
                              className="text-[12px] text-[#A32D2D] border border-[#A32D2D] rounded-lg px-2 py-1 hover:bg-[#FCEBEB] transition-colors"
                            >
                              삭제
                            </button>
                          </div>
                        </li>
                      );
                    })}
                  </ul>
                )}
              </div>
            )}

            {/* 지원 현황 탭 */}
            {activeTab === "apply" && (
              <div className="flex flex-col gap-4">

                {/* 상태별 요약 카드 4개 */}
                <ul className="grid grid-cols-4 gap-3">
                  {(["PENDING", "APPLIED", "REVIEWING", "REJECTED"] as const).map((status) => (
                    <li
                      key={status}
                      className="border border-[#DDDDDD] rounded-xl p-3 flex flex-col items-center gap-1"
                    >
                      <p className="text-[20px] font-bold text-[#378ADD]">
                        {scraps.filter((s) => s.status === status).length}
                      </p>
                      <p className="text-[11px] text-[#888780]">{statusLabel[status]}</p>
                    </li>
                  ))}
                </ul>

                {/* 필터 칩 */}
                <div className="flex gap-2 flex-wrap">
                  {(["ALL", "PENDING", "APPLIED", "REVIEWING", "REJECTED"] as const).map((f) => (
                    <button
                      key={f}
                      onClick={() => setApplyFilter(f)}
                      className={`px-3 py-1 rounded-full text-[12px] border transition-colors ${
                        applyFilter === f
                          ? "bg-[#378ADD] text-white border-[#378ADD]"
                          : "bg-white text-[#888780] border-[#DDDDDD]"
                      }`}
                    >
                      {f === "ALL" ? "전체" : statusLabel[f]}
                    </button>
                  ))}
                </div>

                {/* 공고 목록 */}
                {filteredApply.length === 0 ? (
                  <p className="text-center text-[13px] text-[#888780] py-8">
                    지원 현황이 없습니다.
                  </p>
                ) : (
                  <ul className="flex flex-col gap-3">
                    {filteredApply.map((scrap) => (
                      <li
                        key={scrap.id}
                        className="border border-[#DDDDDD] rounded-xl p-4 flex items-center justify-between"
                      >
                        <div className="flex flex-col gap-1">
                          <p className="text-[14px] font-medium">{scrap.jobTitle}</p>
                          <p className="text-[12px] text-[#888780]">{scrap.companyName}</p>
                        </div>
                        <span className={`text-[11px] px-2 py-0.5 rounded-full font-medium ${statusBadgeStyle[scrap.status]}`}>
                          {statusLabel[scrap.status]}
                        </span>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            )}

          </div>
        </section>
      </div>
    </main>
  );
};

export default MyPage;
