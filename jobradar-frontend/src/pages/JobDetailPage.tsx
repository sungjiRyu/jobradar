/**
 * JobDetailPage — 채용공고 상세 페이지
 * URL의 id 파라미터로 GET /api/jobs/{id} API를 호출
 * 공고 상세 정보(회사명, 직무명, 설명, 기술스택, 마감일 등) 표시
 * 조회할 때마다 viewCount가 자동 증가됨 (백엔드 처리)
 */

import { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { getJobById } from "../api/jobApi";

// 상세 페이지용 공고 타입 (목록보다 필드가 더 많음)
interface JobDetail {
  id: number;
  company: string;
  title: string;
  description: string;
  location: string;
  experienceLevel: string;
  employmentType: string;
  techStacks: string[];
  deadline: string;
  sourceUrl: string;
  sourceSite: string;
  status: string;
  viewCount: number;
  createdAt: string;
}

const JobDetailPage = () => {
  // URL에서 id 파라미터 추출 (예: /jobs/1 → id = "1")
  const { id } = useParams();
  const navigate = useNavigate();

  const [job, setJob] = useState<JobDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  // 컴포넌트 마운트 시 공고 상세 조회
  useEffect(() => {
    const fetchJob = async () => {
      try {
        const res = await getJobById(Number(id));
        setJob(res.data.data);
      } catch (err: any) {
        if (err.response?.status === 404) {
          setError("존재하지 않는 채용공고입니다.");
        } else {
          setError("공고를 불러오는데 실패했습니다.");
        }
      } finally {
        setLoading(false);
      }
    };

    fetchJob();
  }, [id]);

  // 로딩 중
  if (loading) {
    return (
      <div className="flex justify-center py-20">
        <div className="w-8 h-8 border-4 border-[#378ADD] border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  // 에러 발생
  if (error) {
    return (
      <div className="max-w-[700px] mx-auto px-6 py-20 text-center">
        <p className="text-[#E24B4A] text-[14px] mb-4">{error}</p>
        <button
          onClick={() => navigate("/")}
          className="text-[14px] text-[#378ADD] hover:underline"
        >
          목록으로 돌아가기
        </button>
      </div>
    );
  }

  // job이 null이면 아무것도 렌더링하지 않음
  if (!job) return null;

  return (
    <div className="max-w-[700px] mx-auto px-6 py-8">
      {/* 뒤로가기 */}
      <button
        onClick={() => navigate(-1)}
        className="text-[13px] text-[#888780] hover:text-[#1A1A1A] mb-6 block"
      >
        ← 목록으로
      </button>

      {/* 상단 정보 */}
      <div className="bg-white rounded-lg border border-[#DDDDDD] p-6 mb-6">
        {/* 회사명 + 출처 */}
        <div className="flex justify-between items-center mb-2">
          <span className="text-[13px] text-[#888780]">{job.company}</span>
          <span className="text-[12px] text-[#888780]">{job.sourceSite}</span>
        </div>

        {/* 직무명 */}
        <h1 className="text-[20px] font-bold text-[#1A1A1A] mb-4">
          {job.title}
        </h1>

        {/* 기본 정보 */}
        <div className="flex gap-4 text-[13px] text-[#888780] mb-4">
          <span>{job.location}</span>
          <span>·</span>
          <span>{job.experienceLevel}</span>
          <span>·</span>
          <span>{job.employmentType}</span>
        </div>

        {/* 기술스택 태그 */}
        <div className="flex flex-wrap gap-1.5 mb-4">
          {job.techStacks.map((stack) => (
            <span
              key={stack}
              className="text-[12px] px-2.5 py-1 rounded-full bg-blue-50 text-[#378ADD]"
            >
              {stack}
            </span>
          ))}
        </div>

        {/* 마감일 + 조회수 */}
        <div className="flex justify-between items-center text-[13px] text-[#888780] border-t border-[#DDDDDD] pt-4">
          <span>마감일: {job.deadline ?? "상시채용"}</span>
          <span>조회 {job.viewCount}회</span>
        </div>
      </div>

      {/* 상세 설명 */}
      <div className="bg-white rounded-lg border border-[#DDDDDD] p-6 mb-6">
        <h2 className="text-[16px] font-semibold text-[#1A1A1A] mb-3">
          상세 내용
        </h2>
        <p className="text-[14px] text-[#1A1A1A] leading-relaxed whitespace-pre-line">
          {job.description}
        </p>
      </div>

      {/* 지원하기 버튼 */}
      <a
        href={job.sourceUrl}
        target="_blank"
        rel="noopener noreferrer"
        className="block w-full text-center py-3 bg-[#378ADD] text-white text-[15px] font-medium rounded-lg hover:opacity-90 transition-opacity"
      >
        지원하기 ({job.sourceSite})
      </a>
    </div>
  );
};

export default JobDetailPage;
