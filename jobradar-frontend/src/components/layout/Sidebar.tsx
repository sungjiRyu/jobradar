/**
 * Sidebar — 우측 사이드바 컴포넌트
 * 인기 기술스택 목록과 오늘의 현황 카드 표시
 * GET /api/tech-stacks API로 기술스택 목록을 가져옴
 */

import { useState, useEffect } from "react";
import { getTechStacks } from "../../api/jobApi";

const Sidebar = () => {
  // 기술스택 목록 상태
  const [techStacks, setTechStacks] = useState<string[]>([]);
  // 로딩 상태
  const [loading, setLoading] = useState(true);

  // 컴포넌트가 처음 렌더링될 때 기술스택 목록을 가져옴
  useEffect(() => {
    const fetchTechStacks = async () => {
      try {
        const res = await getTechStacks();
        setTechStacks(res.data.data);
      } catch (err) {
        console.error("기술스택 목록 조회 실패:", err);
      } finally {
        setLoading(false);
      }
    };

    fetchTechStacks();
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

      {/* 오늘의 현황 - 추후 /api/stats/today 연동 예정 */}
      <div className="bg-white rounded-lg border border-[#DDDDDD] p-4">
        <h4 className="text-[14px] font-semibold text-[#1A1A1A] mb-3">
          오늘의 현황
        </h4>
        <div className="flex flex-col gap-2 text-[13px]">
          <div className="flex justify-between">
            <span className="text-[#888780]">전체 공고</span>
            <span className="text-[#1A1A1A] font-medium">-</span>
          </div>
          <div className="flex justify-between">
            <span className="text-[#888780]">신규 공고</span>
            <span className="text-[#1A1A1A] font-medium">-</span>
          </div>
          <div className="flex justify-between">
            <span className="text-[#888780]">마감 임박</span>
            <span className="text-[#1A1A1A] font-medium">-</span>
          </div>
        </div>
      </div>
    </aside>
  );
};

export default Sidebar;
