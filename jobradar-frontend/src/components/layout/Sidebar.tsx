import { useState, useEffect } from "react";
import { getTechStacks } from "../../api/jobApi";

const Sidebar = () => {
  const [techStacks, setTechStacks] = useState<string[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getTechStacks()
      .then(res => setTechStacks(res.data.data))
      .catch(err => console.error("사이드바 데이터 조회 실패:", err))
      .finally(() => setLoading(false));
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

    </aside>
  );
};

export default Sidebar;
