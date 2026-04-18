import { useState, useEffect } from "react";
import { getTechStacks } from "../../api/jobApi";
import { getStatsToday, TodayStats } from "../../api/statsApi";

const Sidebar = () => {
  const [techStacks, setTechStacks] = useState<string[]>([]);
  const [todayStats, setTodayStats] = useState<TodayStats | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [techRes, todayRes] = await Promise.all([
          getTechStacks(),
          getStatsToday(),
        ]);
        setTechStacks(techRes.data.data);
        setTodayStats(todayRes.data.data);
      } catch (err) {
        console.error("사이드바 데이터 조회 실패:", err);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, []);

  return (
    <aside className="w-[240px] flex-shrink-0 flex flex-col gap-5">
      {/* 인기 기술스택 */}
      <div className="bg-white rounded-lg border border-[#DDDDDD] p-4">
        <h4 className="text-[14px] font-semibold text-[#1A1A1A] mb-3">
          인기 기술스택
        </h4>
        {loading ? (
          <p className="text-[13px] text-[#888780]">로딩 중...</p>
        ) : (
          <div className="flex flex-wrap gap-1.5">
            {techStacks.map((stack) => (
              <span
                key={stack}
                className="text-[12px] px-2.5 py-1 rounded-full bg-blue-50 text-[#378ADD]"
              >
                {stack}
              </span>
            ))}
          </div>
        )}
      </div>

      {/* 오늘의 현황 */}
      <div className="bg-white rounded-lg border border-[#DDDDDD] p-4">
        <h4 className="text-[14px] font-semibold text-[#1A1A1A] mb-3">
          오늘의 현황
        </h4>
        <div className="flex flex-col gap-2 text-[13px]">
          <div className="flex justify-between">
            <span className="text-[#888780]">전체 공고</span>
            <span className="text-[#1A1A1A] font-medium">
              {loading ? "-" : todayStats?.totalCount.toLocaleString() ?? "-"}
            </span>
          </div>
          <div className="flex justify-between">
            <span className="text-[#888780]">신규 공고</span>
            <span className="text-[#1A1A1A] font-medium">
              {loading ? "-" : todayStats?.todayCount.toLocaleString() ?? "-"}
            </span>
          </div>
          <div className="flex justify-between">
            <span className="text-[#888780]">마감 임박</span>
            <span className="text-[#E24B4A] font-medium">
              {loading ? "-" : todayStats?.urgentCount.toLocaleString() ?? "-"}
            </span>
          </div>
        </div>
      </div>
    </aside>
  );
};

export default Sidebar;
