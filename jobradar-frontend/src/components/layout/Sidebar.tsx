import { useState, useEffect } from "react";
import { getStatsTechStacks } from "../../api/statsApi";
import type { TechStackStat } from "../../api/statsApi";
import {
  SiSpring,
  SiPython,
  SiReact,
  SiVuedotjs,
  SiNodedotjs,
  SiDocker,
  SiMysql,
  SiRedis,
  SiKotlin,
  SiTypescript,
  SiKubernetes,
} from "react-icons/si";
import { FaJava, FaAws } from "react-icons/fa";
import type { IconType } from "react-icons";

// 기술스택 이름 → 아이콘 컴포넌트 + 공식 브랜드 색상 매핑
// 매핑에 없는 기술스택은 회색 도트로 fallback 렌더링
const TECH_ICON_MAP: Record<string, { Icon: IconType; color: string }> = {
  "Java":       { Icon: FaJava,              color: "#E76F00" },
  "Spring":     { Icon: SiSpring,            color: "#6DB33F" },
  "Python":     { Icon: SiPython,            color: "#3776AB" },
  "React":      { Icon: SiReact,             color: "#61DAFB" },
  "Vue":        { Icon: SiVuedotjs,          color: "#4FC08D" },
  "Node.js":    { Icon: SiNodedotjs,         color: "#5FA04E" },
  "Docker":     { Icon: SiDocker,            color: "#2496ED" },
  "AWS":        { Icon: FaAws,               color: "#FF9900" },
  "MySQL":      { Icon: SiMysql,             color: "#4479A1" },
  "Redis":      { Icon: SiRedis,             color: "#DC382D" },
  "Kotlin":     { Icon: SiKotlin,            color: "#7F52FF" },
  "TypeScript": { Icon: SiTypescript,        color: "#3178C6" },
  "Kubernetes": { Icon: SiKubernetes,        color: "#326CE5" },
};

const Sidebar = () => {
  const [techStacks, setTechStacks] = useState<TechStackStat[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getStatsTechStacks()
      .then(res => setTechStacks(res.data.data))
      .catch(err => console.error("사이드바 데이터 조회 실패:", err))
      .finally(() => setLoading(false));
  }, []);

  // 막대 너비 계산: 가장 많은 공고 수를 100%로 기준
  // 빈 배열일 때 0으로 나누는 것을 막기 위해 fallback 1
  const maxCount = techStacks.length > 0
    ? Math.max(...techStacks.map(s => s.count))
    : 1;

  return (
    <aside className="w-[240px] flex-shrink-0 flex flex-col gap-5">
      {/* 인기 기술스택 — 가로 막대 그래프 + 브랜드 아이콘 */}
      <div className="bg-white rounded-lg border border-[#DDDDDD] p-4">
        <h4 className="text-[14px] font-semibold text-[#1A1A1A] mb-3">
          인기 기술스택
        </h4>
        {loading ? (
          <p className="text-[13px] text-[#888780]">로딩 중...</p>
        ) : (
          <div className="flex flex-col gap-2">
            {techStacks.map((stack) => {
              const info = TECH_ICON_MAP[stack.name];
              return (
                <div key={stack.name} className="flex items-center gap-2 text-[12px]">
                  {/* 브랜드 아이콘 (매핑 없으면 회색 점 fallback) */}
                  <span className="w-4 flex-shrink-0 flex items-center justify-center">
                    {info ? (
                      <info.Icon size={16} color={info.color} />
                    ) : (
                      <span className="w-1.5 h-1.5 rounded-full bg-[#CCCCCC]" />
                    )}
                  </span>
                  {/* 기술스택 이름 */}
                  <span className="w-[58px] text-[#1A1A1A] font-medium truncate">
                    {stack.name}
                  </span>
                  {/* 비율 막대 — 최대값 대비 비율로 너비 결정 */}
                  <div className="flex-1 h-[6px] bg-[#E6F1FB] rounded-full overflow-hidden">
                    <div
                      className="h-full bg-[#378ADD] rounded-full transition-[width] duration-300"
                      style={{ width: `${(stack.count / maxCount) * 100}%` }}
                    />
                  </div>
                  {/* 공고 수 (우측) — tabular-nums로 숫자 폭 통일 */}
                  <span className="text-[#888780] tabular-nums w-[28px] text-right">
                    {stack.count}
                  </span>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </aside>
  );
};

export default Sidebar;
