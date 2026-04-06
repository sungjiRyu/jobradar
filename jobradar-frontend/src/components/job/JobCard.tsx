/**
 * JobCard — 채용공고 카드 컴포넌트
 * 공고 목록 페이지에서 각 공고를 카드 형태로 표시
 * 회사명, 직무명, 기술스택 태그, 마감일(D-day), 스크랩 버튼 포함
 */

// 공고 데이터 타입 (API 응답 기준)
export interface Job {
  id: number;
  company: string;
  title: string;
  location: string;
  experienceLevel: string;
  employmentType: string;
  techStacks: string[];
  deadline: string;
  sourceSite: string;
  viewCount: number;
}

// D-day 계산 함수
// deadline(마감일)과 오늘 날짜의 차이를 일(day) 단위로 반환
const calcDday = (deadline: string): number => {
  const today = new Date();
  const deadlineDate = new Date(deadline);
  const diff = deadlineDate.getTime() - today.getTime();
  return Math.ceil(diff / (1000 * 60 * 60 * 24));
};

interface JobCardProps {
  job: Job;
  onScrap: (jobId: number) => void;
}

const JobCard = ({ job, onScrap }: JobCardProps) => {
  const dday = calcDday(job.deadline);

  return (
    <div className="bg-white rounded-lg border border-[#DDDDDD] p-5 hover:shadow-md transition-shadow">
      {/* 상단: 회사명 + 스크랩 버튼 */}
      <div className="flex justify-between items-start mb-2">
        <span className="text-[13px] text-[#888780]">{job.company}</span>
        <button
          onClick={() => onScrap(job.id)}
          className="text-[#DDDDDD] hover:text-red-400 transition-colors"
        >
          ♥
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
        <span className={dday <= 3 ? "text-[#E24B4A] font-semibold" : ""}>
          {dday > 0 ? `D-${dday}` : dday === 0 ? "D-Day" : "마감"}
        </span>
      </div>
    </div>
  );
};

export default JobCard;
