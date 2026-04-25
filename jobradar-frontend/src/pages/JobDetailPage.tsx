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
  description: string | null;
  summary: string | null;
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

// Gemini JSON 응답 타입
interface JobSummaryJson {
  header: { summary: string };
  stacks: { core: string[]; infra: string[]; tools: string[] };
  details: { tasks: string[]; reqs: string[]; pref: string[] };
  conditions: { type: string | null; location: string | null; salary: string | null } | null;
  culture: string[];
  insight: { challenge: string | null; fit: string | null };
}

// summary JSON 파싱 (실패 시 null 반환)
const parseSummary = (raw: string): JobSummaryJson | null => {
  try {
    return JSON.parse(raw) as JobSummaryJson;
  } catch {
    return null;
  }
};

// 태그 칩 컴포넌트
const Chip = ({ label, color = "blue" }: { label: string; color?: "blue" | "gray" | "purple" }) => {
  const styles = {
    blue: "bg-blue-50 text-[#378ADD]",
    gray: "bg-gray-100 text-[#555]",
    purple: "bg-purple-50 text-purple-600",
  };
  return (
    <span className={`text-[12px] px-2.5 py-1 rounded-full ${styles[color]}`}>
      {label}
    </span>
  );
};

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

      {/* 상세 내용 — AI 정리 결과 */}
      {job.summary && (() => {
        const data = parseSummary(job.summary!);
        if (!data) return null;
        return (
          <div className="bg-white rounded-lg border border-[#DDDDDD] p-6 mb-6">
            {/* 헤더 */}
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-[16px] font-semibold text-[#1A1A1A]">상세 내용</h2>
              <span className="flex items-center gap-1 text-[11px] text-[#888780]">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <defs>
                    <linearGradient id="gemini-grad" x1="0" y1="0" x2="1" y2="1">
                      <stop offset="0%" stopColor="#4285F4" />
                      <stop offset="100%" stopColor="#9C27B0" />
                    </linearGradient>
                  </defs>
                  <path d="M12 2C12 2 13.5 8.5 22 12C13.5 15.5 12 22 12 22C12 22 10.5 15.5 2 12C10.5 8.5 12 2Z" fill="url(#gemini-grad)" />
                </svg>
                AI로 정리한 내용입니다
              </span>
            </div>

            {/* 한줄 캐치프레이즈 */}
            {data.header?.summary && (
              <p className="text-[14px] text-[#378ADD] font-medium mb-4 pb-4 border-b border-[#DDDDDD]">
                "{data.header.summary}"
              </p>
            )}

            {/* 기술스택 */}
            {(data.stacks?.core?.length > 0 || data.stacks?.infra?.length > 0 || data.stacks?.tools?.length > 0) && (
              <div className="mb-4">
                <p className="text-[13px] font-semibold text-[#1A1A1A] mb-2">🛠 기술스택</p>
                <div className="space-y-1.5">
                  {data.stacks.core?.length > 0 && (
                    <div className="flex items-center gap-1.5 flex-wrap">
                      <span className="text-[11px] text-[#888780] w-10">Core</span>
                      {data.stacks.core.map((s) => <Chip key={s} label={s} color="blue" />)}
                    </div>
                  )}
                  {data.stacks.infra?.length > 0 && (
                    <div className="flex items-center gap-1.5 flex-wrap">
                      <span className="text-[11px] text-[#888780] w-10">Infra</span>
                      {data.stacks.infra.map((s) => <Chip key={s} label={s} color="gray" />)}
                    </div>
                  )}
                  {data.stacks.tools?.length > 0 && (
                    <div className="flex items-center gap-1.5 flex-wrap">
                      <span className="text-[11px] text-[#888780] w-10">Tools</span>
                      {data.stacks.tools.map((s) => <Chip key={s} label={s} color="purple" />)}
                    </div>
                  )}
                </div>
              </div>
            )}

            {/* 주요업무 / 자격요건 / 우대사항 */}
            <div className="grid grid-cols-1 gap-3 mb-4">
              {data.details?.tasks?.length > 0 && (
                <div>
                  <p className="text-[13px] font-semibold text-[#1A1A1A] mb-1.5">📋 주요업무</p>
                  <ul className="space-y-1">
                    {data.details.tasks.map((t, i) => (
                      <li key={i} className="text-[13px] text-[#444] flex gap-1.5">
                        <span className="text-[#888780] mt-0.5">•</span>{t}
                      </li>
                    ))}
                  </ul>
                </div>
              )}
              {data.details?.reqs?.length > 0 && (
                <div>
                  <p className="text-[13px] font-semibold text-[#1A1A1A] mb-1.5">✅ 자격요건</p>
                  <ul className="space-y-1">
                    {data.details.reqs.map((r, i) => (
                      <li key={i} className="text-[13px] text-[#444] flex gap-1.5">
                        <span className="text-[#888780] mt-0.5">•</span>{r}
                      </li>
                    ))}
                  </ul>
                </div>
              )}
              {data.details?.pref?.length > 0 && (
                <div>
                  <p className="text-[13px] font-semibold text-[#1A1A1A] mb-1.5">⭐ 우대사항</p>
                  <ul className="space-y-1">
                    {data.details.pref.map((p, i) => (
                      <li key={i} className="text-[13px] text-[#444] flex gap-1.5">
                        <span className="text-[#888780] mt-0.5">•</span>{p}
                      </li>
                    ))}
                  </ul>
                </div>
              )}
            </div>

            {/* 근무조건 */}
            {data.conditions && (data.conditions.type || data.conditions.location || data.conditions.salary) && (
              <div className="mb-4">
                <p className="text-[13px] font-semibold text-[#1A1A1A] mb-2">🏠 근무조건</p>
                <div className="flex flex-wrap gap-4 text-[13px] text-[#444]">
                  {data.conditions.type && (
                    <span><span className="text-[#888780]">형태  </span>{data.conditions.type}</span>
                  )}
                  {data.conditions.location && (
                    <span><span className="text-[#888780]">지역  </span>{data.conditions.location}</span>
                  )}
                  {data.conditions.salary && (
                    <span><span className="text-[#888780]">급여  </span>{data.conditions.salary}</span>
                  )}
                </div>
              </div>
            )}

            {/* 조직문화 */}
            {data.culture?.length > 0 && (
              <div className="mb-4">
                <p className="text-[13px] font-semibold text-[#1A1A1A] mb-2">🏢 조직문화</p>
                <div className="flex flex-wrap gap-1.5">
                  {data.culture.map((c) => <Chip key={c} label={c} color="gray" />)}
                </div>
              </div>
            )}

            {/* 인사이트 */}
            {(data.insight?.challenge || data.insight?.fit) && (
              <div className="bg-[#F8F9FA] rounded-lg p-4">
                <p className="text-[13px] font-semibold text-[#1A1A1A] mb-2">💡 인사이트</p>
                {data.insight.challenge && (
                  <p className="text-[12px] text-[#555] mb-1">
                    <span className="text-[#888780]">기술 도전 </span>{data.insight.challenge}
                  </p>
                )}
                {data.insight.fit && (
                  <p className="text-[12px] text-[#555]">
                    <span className="text-[#888780]">적합한 개발자 </span>{data.insight.fit}
                  </p>
                )}
              </div>
            )}
          </div>
        );
      })()}

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
