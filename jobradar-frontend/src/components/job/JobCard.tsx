import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { addScrap, deleteScrap } from "../../api/scrapApi";

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
  initialScrapId?: number | null; // 이미 스크랩된 경우 scrapId 전달
}

const JobCard = ({ job, initialScrapId = null }: JobCardProps) => {
  const navigate = useNavigate();
  const dday = calcDday(job.deadline);

  const [scrapId, setScrapId] = useState<number | null>(initialScrapId);
  const [loading, setLoading] = useState(false);

  const handleScrap = async (e: React.MouseEvent) => {
    e.stopPropagation();

    const token = localStorage.getItem("accessToken");
    if (!token) {
      navigate("/login");
      return;
    }

    if (loading) return;

    try {
      setLoading(true);
      if (scrapId !== null) {
        await deleteScrap(scrapId);
        setScrapId(null);
      } else {
        const res = await addScrap(job.id);
        setScrapId(res.data.data.scrapId);
      }
    } catch {
      alert("스크랩 처리에 실패했습니다.");
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
            scrapId !== null ? "text-red-400" : "text-[#DDDDDD] hover:text-red-400"
          }`}
          title={scrapId !== null ? "스크랩 취소" : "스크랩"}
        >
          {scrapId !== null ? "♥" : "♡"}
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
