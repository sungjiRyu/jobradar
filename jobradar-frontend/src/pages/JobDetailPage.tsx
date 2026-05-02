import { useState, useEffect, useRef } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { getJobById, getJobDescription, getJobSummary } from "../api/jobApi";

// ─────────────────────────────────────────────
// 타입 정의
// ─────────────────────────────────────────────

// 상세 페이지에서 사용하는 공고 데이터 구조
interface JobDetail {
  id: number;
  company: string;
  title: string;
  description: string | null; // 크롤링된 원문 (없으면 null)
  summary: string | null;     // AI가 정리한 JSON 문자열 (없으면 null)
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

// AI가 반환하는 JSON 구조
interface JobSummaryJson {
  header: { summary: string };
  stacks: { core: string[]; infra: string[]; tools: string[] };
  details: { tasks: string[]; reqs: string[]; pref: string[] };
  conditions: { type: string | null; location: string | null; salary: string | null } | null;
  culture: string[];
  insight: { challenge: string | null; fit: string | null };
}

// ─────────────────────────────────────────────
// 유틸 함수 & 공통 컴포넌트
// ─────────────────────────────────────────────

// AI 응답 문자열을 JSON으로 파싱 (실패 시 null 반환)
const parseSummary = (raw: string): JobSummaryJson | null => {
  try {
    return JSON.parse(raw) as JobSummaryJson;
  } catch {
    return null;
  }
};

// 기술스택 등에 사용하는 태그 칩 컴포넌트
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

// ─────────────────────────────────────────────
// 메인 컴포넌트
// ─────────────────────────────────────────────

const JobDetailPage = () => {
  const { id } = useParams();     // URL에서 공고 id 추출 (/jobs/123 → "123")
  const navigate = useNavigate(); // 뒤로가기 등 페이지 이동에 사용

  // ── 공고 기본 정보 상태 ──────────────────────
  const [job, setJob] = useState<JobDetail | null>(null);
  const [loading, setLoading] = useState(true); // 공고 로딩 중 여부
  const [error, setError] = useState("");       // 에러 메시지

  // ── AI 요약 상태 ─────────────────────────────
  const [summary, setSummary] = useState<string | undefined>(undefined);
  // undefined = 로딩 중, string = 완료, (summaryFailReason로 실패 구분)
  const [summaryFailReason, setSummaryFailReason] = useState<"imageOnly" | "aiFailed" | "crawlerFailed" | undefined>(undefined);
  const [loadingStatus, setLoadingStatus] = useState<"crawling" | "ai" | null>(null); // 로딩 메시지 전환용

  // ── 점(dots) 애니메이션 ───────────────────────
  const [dots, setDots] = useState("");
  const dotsIntervalRef = useRef<ReturnType<typeof setInterval> | null>(null); // interval ID 보관 (메모리 누수 방지)

  // 0.5초마다 "." → ".." → "..." → "" 반복
  const startDots = () => {
    dotsIntervalRef.current = setInterval(() => {
      setDots(prev => prev.length >= 3 ? "" : prev + ".");
    }, 500);
  };

  // interval 정리 (컴포넌트 언마운트 또는 로딩 완료 시 호출)
  const stopDots = () => {
    if (dotsIntervalRef.current) {
      clearInterval(dotsIntervalRef.current);
      dotsIntervalRef.current = null;
    }
    setDots("");
  };

  // ── AI 요약 fetch ─────────────────────────────
  // hasDescription: DB에 이미 원문이 있으면 true → 크롤링 단계 건너뜀
  const fetchSummary = async (hasDescription: boolean) => {
    setSummaryFailReason(undefined);
    setSummary(undefined);
    startDots();

    try {
      // description이 없으면 먼저 크롤링
      if (!hasDescription) {
        setLoadingStatus("crawling");
        const descRes = await getJobDescription(Number(id));
        const desc = descRes.data.data;

        if (desc.status === "IMAGE") {
          setSummaryFailReason("imageOnly");
          return;
        }
        if (desc.status === "CRAWL_FAILED") {
          setSummaryFailReason("crawlerFailed");
          return;
        }
      }

      // AI 요약 요청
      setLoadingStatus("ai");
      const summaryRes = await getJobSummary(Number(id));
      const data = summaryRes.data.data;

      if (data.summary) {
        setSummary(data.summary);
      } else {
        setSummaryFailReason(data.imageOnly ? "imageOnly" : "aiFailed");
      }
    } catch {
      setSummaryFailReason("aiFailed");
    } finally {
      stopDots();
      setLoadingStatus(null);
    }
  };

  // ── 초기 데이터 로딩 ─────────────────────────
  useEffect(() => {
    const fetchJob = async () => {
      try {
        const res = await getJobById(Number(id));
        const jobData = res.data.data;
        
        setJob(jobData);

        // DB에 summary가 이미 있으면 바로 사용, 없으면 AI 요약 요청
        if (jobData.summary !== null) {
          setSummary(jobData.summary);
        } else {
          fetchSummary(!!jobData.description); // !!로 string|null → boolean 변환
        }
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
    return () => stopDots(); // 페이지 이탈 시 interval 정리
  }, [id]);

  // ─────────────────────────────────────────────
  // 얼리 리턴 (공고 로딩 전/오류 시 조기 반환)
  // ─────────────────────────────────────────────

  if (loading) {
    return (
      <div className="flex justify-center py-20">
        <div className="w-8 h-8 border-4 border-[#378ADD] border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

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

  if (!job) return null;

  // ─────────────────────────────────────────────
  // 렌더링
  // ─────────────────────────────────────────────

  return (
    <div className="max-w-[700px] mx-auto px-6 py-8">

      {/* 뒤로가기 */}
      <button
        onClick={() => navigate(-1)}
        className="text-[13px] text-[#888780] hover:text-[#1A1A1A] mb-6 block"
      >
        ← 목록으로
      </button>

      {/* ── 공고 기본 정보 카드 ── */}
      <div className="bg-white rounded-lg border border-[#DDDDDD] p-6 mb-6">
        {/* 회사명 + 출처 사이트 */}
        <div className="flex justify-between items-center mb-2">
          <span className="text-[13px] text-[#888780]">{job.company}</span>
          <span className="text-[12px] text-[#888780]">{job.sourceSite}</span>
        </div>

        {/* 직무명 */}
        <h1 className="text-[20px] font-bold text-[#1A1A1A] mb-4">
          {job.title}
        </h1>

        {/* 근무지역 · 경력 · 고용형태 */}
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

      {/* ── AI 요약 영역 (4가지 상태) ── */}

      {/* 1) 로딩 중: summary도 failReason도 아직 없는 상태 → 스켈레톤 UI */}
      {summary === undefined && summaryFailReason === undefined && (
        <div className="bg-white rounded-lg border border-[#DDDDDD] p-6 mb-6">
          <div className="flex items-center justify-between mb-4">
            <div className="h-4 w-20 bg-gray-200 rounded animate-pulse" />
            <div className="h-3 w-24 bg-gray-100 rounded animate-pulse" />
          </div>
          <div className="space-y-2">
            <div className="h-3 w-full bg-gray-100 rounded animate-pulse" />
            <div className="h-3 w-4/5 bg-gray-100 rounded animate-pulse" />
            <div className="h-3 w-3/5 bg-gray-100 rounded animate-pulse" />
          </div>
          <div className="mt-4 space-y-2">
            <div className="h-3 w-24 bg-gray-200 rounded animate-pulse" />
            <div className="h-3 w-full bg-gray-100 rounded animate-pulse" />
            <div className="h-3 w-5/6 bg-gray-100 rounded animate-pulse" />
            <div className="h-3 w-4/6 bg-gray-100 rounded animate-pulse" />
          </div>
          <p className="text-[12px] text-[#AAAAAA] text-center mt-4">
            {loadingStatus === "crawling"
              ? `채용공고 정보를 가져오는 중입니다${dots}`
              : `AI가 공고 정보를 정리하고 있습니다${dots}`}
          </p>
        </div>
      )}

      {/* 2) AI 실패: Gemini 오류 또는 네트워크 오류 → 재시도 버튼 */}
      {summaryFailReason === "aiFailed" && (
        <div className="bg-white rounded-lg border border-[#DDDDDD] p-6 mb-6 text-center">
          <p className="text-[13px] text-[#888780] mb-3">상세 내용을 불러오지 못했습니다.</p>
          <button
            onClick={() => fetchSummary(!!job?.description)}
            className="text-[13px] text-[#378ADD] hover:underline"
          >
            다시 시도
          </button>
        </div>
      )}

      {/* 3) 크롤링 실패: 네트워크 오류 등 → 재시도 버튼 */}
      {summaryFailReason === "crawlerFailed" && (
        <div className="bg-white rounded-lg border border-[#DDDDDD] p-6 mb-6 text-center">
          <p className="text-[13px] text-[#888780] mb-3">공고 정보를 가져오는데 실패했습니다.</p>
          <button
            onClick={() => fetchSummary(false)}
            className="text-[13px] text-[#378ADD] hover:underline"
          >
            다시 시도
          </button>
        </div>
      )}

      {/* 5) 이미지 공고: 텍스트 추출 불가 → 원본 링크 안내 */}
      {summaryFailReason === "imageOnly" && (
        <div className="bg-white rounded-lg border border-[#DDDDDD] p-6 mb-6 text-center">
          <p className="text-[13px] text-[#888780] mb-1">이미지 형식의 공고입니다.</p>
          <p className="text-[13px] text-[#888780]">상세 내용은 원본 공고에서 확인하세요.</p>
        </div>
      )}

      {/* 6) 요약 성공: JSON 파싱 후 섹션별 렌더링 */}
      {summary && (() => {
        const data = parseSummary(summary);
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

            {/* 기술스택 (Core / Infra / Tools) */}
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

      {/* ── 지원하기 버튼 ── */}
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
