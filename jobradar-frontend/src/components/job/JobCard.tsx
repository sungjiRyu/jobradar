import { useScrap } from "../../hooks/useScrap";
import { calcDday } from "../../utils/dateUtils";

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

interface JobCardProps {
  job: Job;
  initialScrapId?: number | null;
}

const JobCard = ({ job, initialScrapId = null }: JobCardProps) => {
  const dday = calcDday(job.deadline);
  const { scrapId, scrapLoading, handleScrap } = useScrap(job.id, initialScrapId);

  return (
    <div className="bg-white rounded-lg border border-[#DDDDDD] p-5 hover:shadow-md transition-shadow">
      <div className="flex justify-between items-start mb-2">
        <span className="text-[13px] text-[#888780]">{job.company}</span>
        <button
          onClick={handleScrap}
          disabled={scrapLoading}
          className={`transition-colors text-lg leading-none ${
            scrapId !== null ? "text-red-400" : "text-[#DDDDDD] hover:text-red-400"
          }`}
          title={scrapId !== null ? "스크랩 취소" : "스크랩"}
        >
          {scrapId !== null ? "♥" : "♡"}
        </button>
      </div>

      <h3 className="text-[15px] font-semibold text-[#1A1A1A] mb-3 leading-snug">
        {job.title}
      </h3>

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
