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

const SOURCE_SITE_STYLES: Record<string, string> = {
  사람인: "border-[#7DB6EA] bg-[#EEF7FF] text-[#1E6FAE]",
  잡코리아: "border-[#9FD6B9] bg-[#F0FAF4] text-[#247A4D]",
};

const JobCard = ({ job, initialScrapId = null }: JobCardProps) => {
  const dday = calcDday(job.deadline);
  const { scrapId, scrapLoading, handleScrap } = useScrap(job.id, initialScrapId);
  const sourceSiteStyle =
    SOURCE_SITE_STYLES[job.sourceSite] ??
    "border-[#DDDDDD] bg-[#F5F5F5] text-[#888780]";

  return (
    <div className="bg-white rounded-lg border border-[#DDDDDD] p-4 sm:p-5 hover:shadow-md transition-shadow">
      <div className="flex justify-between items-start gap-3 mb-2">
        <span className="text-[13px] text-[#888780] break-words min-w-0">
          {job.company}
        </span>
        <div className="flex items-center gap-2 flex-shrink-0">
          <span
            className={`text-[11px] px-2 py-[2px] rounded-full border font-medium tracking-normal leading-4 shadow-[inset_0_1px_0_rgba(255,255,255,0.7)] ${sourceSiteStyle}`}
          >
            {job.sourceSite}
          </span>
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
      </div>

      <h3 className="text-[15px] font-semibold text-[#1A1A1A] mb-3 leading-snug break-words">
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
