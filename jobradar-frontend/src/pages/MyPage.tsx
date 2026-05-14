import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { getMe } from "../api/userApi";
import { getScraps, updateScrapStatus, deleteScrap } from "../api/scrapApi";
import type { ScrapStatus } from "../api/scrapApi";
import { calcDday } from "../utils/dateUtils";

interface UserInfo {
  email: string;
  nickname: string;
}

interface ScrapItem {
  scrapId: number;
  jobPostId: number;
  title: string;
  company: string;
  deadline: string;
  status: ScrapStatus;
  createdAt: string;
}

const statusBadgeStyle: Record<ScrapStatus, string> = {
  PENDING: "bg-[#E6F1FB] text-[#0C447C]",
  APPLIED: "bg-[#EAF3DE] text-[#27500A]",
  REVIEWING: "bg-[#FAEEDA] text-[#633806]",
  REJECTED: "bg-[#FCEBEB] text-[#A32D2D]",
};

const statusLabel: Record<ScrapStatus, string> = {
  PENDING: "지원예정",
  APPLIED: "지원완료",
  REVIEWING: "서류검토",
  REJECTED: "탈락",
};

const MyPage = () => {
  const navigate = useNavigate();

  const [user, setUser] = useState<UserInfo | null>(null);
  const [scraps, setScraps] = useState<ScrapItem[]>([]);
  const [activeTab, setActiveTab] = useState<"scrap" | "apply">("scrap");
  const [scrapFilter, setScrapFilter] = useState<"ALL" | ScrapStatus>("ALL");
  const [applyFilter, setApplyFilter] = useState<"ALL" | ScrapStatus>("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        const [userRes, scrapRes] = await Promise.all([getMe(), getScraps()]);
        setUser(userRes.data.data);
        setScraps(scrapRes.data.data ?? []);
      } catch {
        setError("데이터를 불러오는 데 실패했습니다.");
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  const handleStatusChange = async (id: number, status: ScrapStatus) => {
    try {
      await updateScrapStatus(id, status);
      setScraps((prev) => prev.map((s) => (s.scrapId === id ? { ...s, status } : s)));
    } catch {
      alert("상태 변경에 실패했습니다.");
    }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm("스크랩을 삭제하시겠습니까?")) return;
    try {
      await deleteScrap(id);
      setScraps((prev) => prev.filter((s) => s.scrapId !== id));
    } catch {
      alert("삭제에 실패했습니다.");
    }
  };

  const totalCount = scraps.length;
  const pendingCount = scraps.filter((s) => s.status === "PENDING").length;
  const appliedCount = scraps.filter((s) => s.status === "APPLIED").length;
  const reviewingCount = scraps.filter((s) => s.status === "REVIEWING").length;

  const filteredScraps = scrapFilter === "ALL" ? scraps : scraps.filter((s) => s.status === scrapFilter);
  const filteredApply = applyFilter === "ALL" ? scraps : scraps.filter((s) => s.status === applyFilter);

  if (loading) {
    return (
      <div className="min-h-screen bg-[#F5F5F5] flex items-center justify-center">
        <div className="w-8 h-8 border-4 border-[#378ADD] border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

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

        <div className="bg-white border border-[#DDDDDD] rounded-xl p-6 flex items-center justify-between">
          <div>
            <h2 className="text-[18px] font-semibold">{user?.nickname}</h2>
            <p className="text-[13px] text-[#888780] mt-1">{user?.email}</p>
          </div>
          <button
            onClick={() => navigate("/my/verify")}
            className="text-[13px] text-[#378ADD] border border-[#378ADD] rounded-lg px-4 py-2 hover:bg-[#E6F1FB] transition-colors"
          >
            정보 수정
          </button>
        </div>

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

        <section className="bg-white border border-[#DDDDDD] rounded-xl overflow-hidden">
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

          <div role="tabpanel" className="p-5">
            {activeTab === "scrap" && (
              <div className="flex flex-col gap-4">
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

                {filteredScraps.length === 0 ? (
                  <p className="text-center text-[13px] text-[#888780] py-8">스크랩한 공고가 없습니다.</p>
                ) : (
                  <ul className="flex flex-col gap-3">
                    {filteredScraps.map((scrap) => {
                      const dday = calcDday(scrap.deadline);
                      const isUrgent = dday !== null && dday >= 0 && dday <= 3;
                      return (
                        <li
                          key={scrap.scrapId}
                          className="border border-[#DDDDDD] rounded-xl p-4 flex items-center justify-between cursor-pointer hover:shadow-md transition-shadow"
                          onClick={() => navigate(`/jobs/${scrap.jobPostId}`)}
                        >
                          <div className="flex flex-col gap-1">
                            <p className="text-[14px] font-medium">{scrap.title}</p>
                            <p className="text-[12px] text-[#888780]">{scrap.company}</p>
                            <p className={`text-[12px] font-medium ${isUrgent ? "text-[#A32D2D]" : "text-[#888780]"}`}>
                              {dday === null ? "상시채용" : dday < 0 ? "마감" : dday === 0 ? "D-day" : `D-${dday}`}
                            </p>
                          </div>
                          <div className="flex items-center gap-2">
                            <span className={`text-[11px] px-2 py-0.5 rounded-full font-medium ${statusBadgeStyle[scrap.status]}`}>
                              {statusLabel[scrap.status]}
                            </span>
                            <select
                              value={scrap.status}
                              onClick={(e) => e.stopPropagation()}
                              onChange={(e) => handleStatusChange(scrap.scrapId, e.target.value as ScrapStatus)}
                              className="text-[12px] border border-[#DDDDDD] rounded-lg px-2 py-1 outline-none focus:border-[#378ADD] bg-white"
                            >
                              <option value="PENDING">지원예정</option>
                              <option value="APPLIED">지원완료</option>
                              <option value="REVIEWING">서류검토</option>
                              <option value="REJECTED">탈락</option>
                            </select>
                            <button
                              onClick={(e) => { e.stopPropagation(); handleDelete(scrap.scrapId); }}
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

            {activeTab === "apply" && (
              <div className="flex flex-col gap-4">
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

                {filteredApply.length === 0 ? (
                  <p className="text-center text-[13px] text-[#888780] py-8">지원 현황이 없습니다.</p>
                ) : (
                  <ul className="flex flex-col gap-3">
                    {filteredApply.map((scrap) => (
                      <li
                        key={scrap.scrapId}
                        className="border border-[#DDDDDD] rounded-xl p-4 flex items-center justify-between cursor-pointer hover:shadow-md transition-shadow"
                        onClick={() => navigate(`/jobs/${scrap.jobPostId}`)}
                      >
                        <div className="flex flex-col gap-1">
                          <p className="text-[14px] font-medium">{scrap.title}</p>
                          <p className="text-[12px] text-[#888780]">{scrap.company}</p>
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
