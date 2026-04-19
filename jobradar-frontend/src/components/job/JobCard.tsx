import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { addScrap } from "../../api/scrapApi";

export interface Job {
  id: number;
  company: string;
  title: string;
  location: string;
  experienceLevel: string;
  employmentType: string;
  techStacks: string[];
  deadline: string | null;
  sourceSite: string;
  viewCount: number;
}

const calcDday = (deadline: string | null): number | null => {
  if (!deadline) return null;
  const today = new Date();
  const deadlineDate = new Date(deadline);
  const diff = deadlineDate.getTime() - today.getTime();
  return Math.ceil(diff / (1000 * 60 * 60 * 24));
};

interface JobCardProps {
  job: Job;
}

const JobCard = ({ job }: JobCardProps) => {
  const navigate = useNavigate();
  const dday = calcDday(job.deadline);

  // 스크랩 완료 여부 — 클릭 후 하트 색상 변경에 사용
  const [scrapped, setScrapped] = useState(false);
  const [loading, setLoading] = useState(false);

  const handleScrap = async (e: React.MouseEvent) => {
    // 카드 전체 클릭 이벤트로 전파되지 않도록 차단
    e.stopPropagation();

    const token = localStorage.getItem("accessToken");
    if (!token) {
      navigate("/login");
      return;
    }

    if (scrapped || loading) return;

    try {
      setLoading(true);
      await addScrap(job.id);
      setScrapped(true);
    } catch {
      alert("스크랩에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="bg-white rounded-lg border border-[#DDDDDD] p-5 hover:shadow-md transition-shadow">
      {/* 상단: 회사명 + 스크랩 버튼 */}
      <div className="flex justify-between items-start mb-2">
        <span className="text-[13px] text-[#888780]">{job.company}</span>
        <button
          onClick={handleScrap}
          disabled={loading}
          className={`transition-colors text-lg leading-none ${
            scrapped ? "text-red-400" : "text-[#DDDDDD] hover:text-red-400"
          }`}
          title={scrapped ? "스크랩됨" : "스크랩"}
        >
          {scrapped ? "♥" : "♡"}
        </button>
      </div>

      {/* 직무명 */}
      <h3 className="text-[15px] font-semibold text-[#1A1A1A] mb-3 leading-snug">
        {job.title}
      </h3>

      {/* 기술스택 태그 */}
      <div className="flex flex-wrap gap-1.5 mb-3">
        {job.techStacks.map((stack) => (
          <span
            key={stack}
            className="text-[11px] px-2 py-0.5 rounded-full bg-blue-50 text-[#378ADD]"
          >
            {stack}
          </span>
        ))}
      </div>

      {/* 하단: 지역 + 경력 + 마감일 */}
      <div className="flex justify-between items-center text-[12px] text-[#888780]">
        <div className="flex gap-2">
          <span>{job.location}</span>
          <span>·</span>
          <span>{job.experienceLevel}</span>
        </div>
        <span className={dday !== null && dday <= 3 ? "text-[#E24B4A] font-semibold" : ""}>
          {dday === null
            ? "상시채용"
            : dday < 0
            ? "마감"
            : dday === 0
            ? "D-Day"
            : `D-${dday}`}
        </span>
      </div>
    </div>
  );
};

export default JobCard;
