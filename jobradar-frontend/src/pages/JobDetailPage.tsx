import { useState, useEffect, useRef } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { getJobById, getJobDescription, getJobSummary } from "../api/jobApi";
import { getScraps } from "../api/scrapApi";
import { useScrap } from "../hooks/useScrap";
import { usePageTitle } from "../hooks/usePageTitle";
import toast from "react-hot-toast";

// ─────────────────────────────────────────────
// 타입 정의
// ─────────────────────────────────────────────

interface JobDetail {
  id: number;
  company: string;
  title: string;
  description: string | null;
  descriptionStatus: "SUCCESS" | "IMAGE" | "EXTERNAL" | null; // 크롤링 결과 상태 (FAILED 제거됨)
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

interface JobSummaryJson {
  header: { summary: string };
  stacks: { core: string[]; infra: string[]; tools: string[] };
  details: { tasks: string[]; reqs: string[]; pref: string[] };
  conditions: {
    type: string | null;
    location: string | null;
    salary: string | null;
  } | null;
  culture: string[];
  insight: { challenge: string | null; fit: string | null };
}

interface JobSummaryResponse {
  summary: string | null;
  imageOnly: boolean;
  closed: boolean;
  inProgress?: boolean;
}

// ─────────────────────────────────────────────
// 유틸 함수 & 공통 컴포넌트
// ─────────────────────────────────────────────

const parseSummary = (raw: string): JobSummaryJson | null => {
  try {
    return JSON.parse(raw) as JobSummaryJson;
  } catch {
    return null;
  }
};

const LOCK_RETRY_DELAY_MS = 1500;
const MAX_LOCK_RETRY_COUNT = 5;

const wait = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

/**
 * deadline(YYYY-MM-DD)이 오늘보다 이전인지 판단(T/F)
 * (오늘 마감은 false)
 */
const isDeadlinePassed = (deadline: string | null | undefined): boolean => {
  if (!deadline) return false;
  const today = new Date();
  const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, "0")}-${String(today.getDate()).padStart(2, "0")}`;
  return deadline < todayStr;
};

const Chip = ({
  label,
  color = "blue",
}: {
  label: string;
  color?: "blue" | "gray" | "purple";
}) => {
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
  const { id } = useParams();
  const navigate = useNavigate();

  // ── 공고 기본 정보 상태 ──────────────────────
  const [job, setJob] = useState<JobDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  usePageTitle(job ? job.title : "공고 상세");

  // ── AI 요약 상태 ─────────────────────────────
  const [summary, setSummary] = useState<string | undefined>(undefined);
  // descStatus: DB에서 읽어온 상태 + lazy fetch 결과로 업데이트됨
  // null = 아직 fetch 안 됨(또는 크롤링 실패), "CRAWL_FAILED" = 이번 방문에서 크롤링 실패
  const [descStatus, setDescStatus] = useState<
    | "SUCCESS"
    | "IMAGE"
    | "EXTERNAL"
    | "CRAWL_FAILED"
    | "IN_PROGRESS"
    | null
  >(null);
  const [aiSummaryFailed, setAiSummaryFailed] = useState(false); // AI 요약만 실패한 경우
  const [loadingStatus, setLoadingStatus] = useState<"crawling" | "ai" | null>(
    null,
  );

  const { scrapId, setScrapId, scrapLoading, handleScrap } = useScrap(
    job?.id ?? null,
  );

  // ── 점(dots) 애니메이션 ───────────────────────
  const [dots, setDots] = useState("");
  const dotsIntervalRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const startDots = () => {
    dotsIntervalRef.current = setInterval(() => {
      setDots((prev) => (prev.length >= 3 ? "" : prev + "."));
    }, 500);
  };

  const stopDots = () => {
    if (dotsIntervalRef.current) {
      clearInterval(dotsIntervalRef.current);
      dotsIntervalRef.current = null;
    }
    setDots("");
  };

  const fetchDescriptionUntilReady = async () => {
    for (let attempt = 0; attempt <= MAX_LOCK_RETRY_COUNT; attempt++) {
      const descRes = await getJobDescription(Number(id));
      const desc = descRes.data.data;

      if (desc.status !== "IN_PROGRESS") {
        return desc;
      }

      setDescStatus("IN_PROGRESS");
      if (attempt < MAX_LOCK_RETRY_COUNT) {
        await wait(LOCK_RETRY_DELAY_MS);
      }
    }

    return { status: "IN_PROGRESS", description: null };
  };

  const fetchSummaryUntilReady = async (): Promise<JobSummaryResponse> => {
    for (let attempt = 0; attempt <= MAX_LOCK_RETRY_COUNT; attempt++) {
      const summaryRes = await getJobSummary(Number(id));
      const data = summaryRes.data.data as JobSummaryResponse;

      if (!data.inProgress) {
        return data;
      }

      if (attempt < MAX_LOCK_RETRY_COUNT) {
        await wait(LOCK_RETRY_DELAY_MS);
      }
    }

    return {
      summary: null,
      imageOnly: false,
      closed: false,
      inProgress: true,
    };
  };

  // ── AI 요약 + description lazy fetch 흐름 ──────
  // initialStatus: DB에서 읽어온 descriptionStatus (SUCCESS면 description fetch 생략)
  const fetchSummaryFlow = async (initialStatus: "SUCCESS" | null) => {
    setAiSummaryFailed(false);
    setSummary(undefined);
    startDots();

    try {
      if (initialStatus === null) {
        // DB에 description이 없음 → 백엔드에서 lazy fetch 실행
        setLoadingStatus("crawling");
        const desc = await fetchDescriptionUntilReady();

        if (desc.status === "CRAWL_FAILED") {
          setDescStatus("CRAWL_FAILED");
          return;
        }
        if (desc.status === "IN_PROGRESS") {
          setAiSummaryFailed(true);
          return;
        }
        if (desc.status === "IMAGE") {
          setDescStatus("IMAGE");
          return;
        }
        // EXTERNAL은 fetchJob에서 이미 처리되므로 여기까지 오지 않음
        setDescStatus("SUCCESS");
      }

      // description 있음 → AI 요약 요청
      setLoadingStatus("ai");
      const data = await fetchSummaryUntilReady();

      if (data.summary) {
        setSummary(data.summary);
      } else if (data.inProgress) {
        setAiSummaryFailed(true);
      } else if (data.imageOnly) {
        // getSummary 내부 lazy fetch에서 IMAGE로 확인된 경우
        setDescStatus("IMAGE");
      } else {
        setAiSummaryFailed(true);
      }
    } catch {
      setAiSummaryFailed(true);
    } finally {
      stopDots();
      setLoadingStatus(null);
    }
  };

  // AI 요약만 재시도 (description은 이미 SUCCESS 상태)
  const retryAiSummary = async () => {
    setAiSummaryFailed(false);
    setSummary(undefined);
    startDots();
    setLoadingStatus("ai");

    try {
      const data = await fetchSummaryUntilReady();
      if (data.summary) {
        setSummary(data.summary);
      } else if (data.inProgress) {
        setAiSummaryFailed(true);
      } else if (data.imageOnly) {
        setDescStatus("IMAGE");
      } else {
        setAiSummaryFailed(true);
      }
    } catch {
      setAiSummaryFailed(true);
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

        // DB에 summary가 이미 있으면 바로 사용
        if (jobData.summary !== null) {
          setSummary(jobData.summary);
          return;
        }

        // 마감된 공고 + 요약 미생성 → 크롤링/AI 호출 안 함 (비용 절감)
        // 마감 안내는 별도 렌더링 분기에서 처리
        const closed =
          jobData.status === "CLOSED" || isDeadlinePassed(jobData.deadline);
        if (closed) return;

        const status = jobData.descriptionStatus;
        setDescStatus(status);

        // IMAGE / EXTERNAL은 상태 확정 → API 호출 불필요
        if (status === "IMAGE" || status === "EXTERNAL") return;

        // SUCCESS 또는 null(미fetch) → summary 흐름 진행
        fetchSummaryFlow(status === "SUCCESS" ? "SUCCESS" : null);
      } catch (err: any) {
        if (err.response?.status === 404) {
          navigate("/not-found", { replace: true });
        } else {
          setError("공고를 불러오는데 실패했습니다.");
        }
      } finally {
        setLoading(false);
      }
    };

    // 로그인 상태면 스크랩 여부 확인
    const token = localStorage.getItem("accessToken");
    if (token) {
      getScraps()
        .then((res) => {
          const found = res.data.data.find(
            (s: { jobPostId: number; scrapId: number }) =>
              s.jobPostId === Number(id),
          );
          if (found) setScrapId(found.scrapId);
        })
        .catch(() => {});
    }

    fetchJob();
    return () => stopDots();
  }, [id]);

  // ─────────────────────────────────────────────
  // 얼리 리턴
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
      <div className="max-w-[700px] mx-auto px-4 sm:px-6 py-20 text-center">
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

  // 마감 여부 — status가 CLOSED이거나 deadline이 오늘 이전이면 마감으로 간주
  const isClosed = job.status === "CLOSED" || isDeadlinePassed(job.deadline);

  // ─────────────────────────────────────────────
  // 렌더링
  // ─────────────────────────────────────────────

  return (
    <div className="max-w-[700px] mx-auto px-4 sm:px-6 py-8">
      {/* 뒤로가기 */}
      <button
        onClick={() => navigate(-1)}
        className="text-[13px] text-[#888780] hover:text-[#1A1A1A] mb-6 block"
      >
        ← 목록으로
      </button>

      {/* ── 공고 기본 정보 카드 ── */}
      <div className="bg-white rounded-lg border border-[#DDDDDD] p-6 mb-6">
        <div className="flex justify-between items-center mb-2">
          <span className="text-[13px] text-[#888780]">{job.company}</span>
          <div className="flex items-center gap-3">
            <span className="text-[12px] text-[#888780]">{job.sourceSite}</span>
            <button
              onClick={(e) => {
                // 마감된 공고에 신규 스크랩 시도 → 토스트로 안내, 기존 스크랩은 정상 동작
                if (isClosed && scrapId === null) {
                  e.stopPropagation();
                  toast("마감된 공고는 스크랩할 수 없습니다", {
                    icon: (
                      <div className="w-4 h-4 bg-[#E24B4A] rounded-full flex items-center justify-center text-white text-[10px] font-bold leading-none">
                        !
                      </div>
                    ),
                  });
                  return;
                }
                handleScrap();
              }}
              disabled={scrapLoading}
              className={`transition-colors text-lg leading-none ${
                scrapId !== null
                  ? "text-red-400"
                  : "text-[#DDDDDD] hover:text-red-400"
              }`}
              title={scrapId !== null ? "스크랩 취소" : "스크랩"}
            >
              {scrapId !== null ? "♥" : "♡"}
            </button>
          </div>
        </div>

        <h1 className="text-[20px] font-bold text-[#1A1A1A] mb-4">
          {job.title}
        </h1>

        <div className="flex gap-4 text-[13px] text-[#888780] mb-4">
          <span>{job.location}</span>
          <span>·</span>
          <span>{job.experienceLevel}</span>
          <span>·</span>
          <span>{job.employmentType}</span>
        </div>

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

        <div className="flex justify-between items-center text-[13px] text-[#888780] border-t border-[#DDDDDD] pt-4">
          <span>마감일: {job.deadline ?? "상시채용"}</span>
          <span>조회 {job.viewCount}회</span>
        </div>
      </div>

      {/* ── 마감 공고 안내 ── */}
      {isClosed && (
        <div className="bg-[#FCEBEB] border border-[#F5C6C6] rounded-lg p-4 mb-6 text-center">
          <p className="text-[14px] font-medium text-[#A32D2D] mb-1">
            마감된 공고입니다
          </p>
          {job.deadline && (
            <p className="text-[12px] text-[#A32D2D]/80">
              마감일: {job.deadline}
            </p>
          )}
        </div>
      )}

      {/* ── AI 요약 영역 — 마감 공고는 API 호출 안 함 ── */}

      {/* 1) 로딩 중: 스켈레톤 UI (마감 공고는 표시 안 함) */}
      {!isClosed &&
        summary === undefined &&
        !aiSummaryFailed &&
        descStatus !== "IMAGE" &&
        descStatus !== "EXTERNAL" &&
        descStatus !== "CRAWL_FAILED" && (
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

      {/* 2) AI 요약 실패: 재시도 버튼 */}
      {aiSummaryFailed && (
        <div className="bg-white rounded-lg border border-[#DDDDDD] p-6 mb-6 text-center">
          <p className="text-[13px] text-[#888780] mb-3">
            상세 내용을 불러오지 못했습니다.
          </p>
          <button
            onClick={retryAiSummary}
            className="text-[13px] text-[#378ADD] hover:underline"
          >
            다시 시도
          </button>
        </div>
      )}

      {/* 3) 크롤링 실패: 원본 링크 안내 */}
      {descStatus === "CRAWL_FAILED" && (
        <div className="bg-white rounded-lg border border-[#DDDDDD] p-6 mb-6 text-center">
          <p className="text-[13px] text-[#888780] mb-1">
            공고 정보를 가져오는데 실패했습니다.
          </p>
          <p className="text-[13px] text-[#888780]">
            상세 내용은 원본 공고에서 확인하세요.
          </p>
        </div>
      )}

      {/* 4) 외부 공고: 알바몬·고용24 등 → 원본 링크 버튼 */}
      {descStatus === "EXTERNAL" && (
        <div className="bg-white rounded-lg border border-[#DDDDDD] p-6 mb-6 text-center">
          <p className="text-[13px] text-[#888780] mb-3">
            이 공고는 외부 사이트에서 확인할 수 있습니다.
          </p>
          <a
            href={job.sourceUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="inline-block bg-[#378ADD] text-white text-[13px] font-medium px-5 py-2 rounded-lg hover:bg-[#2B6CB0] transition-colors"
          >
            원본 공고 바로가기
          </a>
        </div>
      )}

      {/* 5) 이미지 공고: 텍스트 추출 불가 */}
      {descStatus === "IMAGE" && (
        <div className="bg-white rounded-lg border border-[#DDDDDD] p-6 mb-6 text-center">
          <p className="text-[13px] text-[#888780] mb-1">
            이미지 형식의 공고입니다.
          </p>
          <p className="text-[13px] text-[#888780]">
            상세 내용은 원본 공고에서 확인하세요.
          </p>
        </div>
      )}

      {/* 6) 요약 성공: JSON 파싱 후 섹션별 렌더링 */}
      {summary &&
        (() => {
          const data = parseSummary(summary);
          if (!data) return null;
          return (
            <div className="bg-white rounded-lg border border-[#DDDDDD] p-6 mb-6">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-[16px] font-semibold text-[#1A1A1A]">
                  상세 내용
                </h2>
                <span className="flex items-center gap-1 text-[11px] text-[#888780]">
                  <svg
                    width="12"
                    height="12"
                    viewBox="0 0 24 24"
                    fill="none"
                    xmlns="http://www.w3.org/2000/svg"
                  >
                    <defs>
                      <linearGradient
                        id="gemini-grad"
                        x1="0"
                        y1="0"
                        x2="1"
                        y2="1"
                      >
                        <stop offset="0%" stopColor="#4285F4" />
                        <stop offset="100%" stopColor="#9C27B0" />
                      </linearGradient>
                    </defs>
                    <path
                      d="M12 2C12 2 13.5 8.5 22 12C13.5 15.5 12 22 12 22C12 22 10.5 15.5 2 12C10.5 8.5 12 2Z"
                      fill="url(#gemini-grad)"
                    />
                  </svg>
                  AI로 정리한 내용입니다
                </span>
              </div>

              {data.header?.summary && (
                <p className="text-[14px] text-[#378ADD] font-medium mb-4 pb-4 border-b border-[#DDDDDD]">
                  "{data.header.summary}"
                </p>
              )}

              {(data.stacks?.core?.length > 0 ||
                data.stacks?.infra?.length > 0 ||
                data.stacks?.tools?.length > 0) && (
                <div className="mb-4">
                  <p className="text-[13px] font-semibold text-[#1A1A1A] mb-2">
                    🛠 기술스택
                  </p>
                  <div className="space-y-1.5">
                    {data.stacks.core?.length > 0 && (
                      <div className="flex items-center gap-1.5 flex-wrap">
                        <span className="text-[11px] text-[#888780] w-10">
                          Core
                        </span>
                        {data.stacks.core.map((s) => (
                          <Chip key={s} label={s} color="blue" />
                        ))}
                      </div>
                    )}
                    {data.stacks.infra?.length > 0 && (
                      <div className="flex items-center gap-1.5 flex-wrap">
                        <span className="text-[11px] text-[#888780] w-10">
                          Infra
                        </span>
                        {data.stacks.infra.map((s) => (
                          <Chip key={s} label={s} color="gray" />
                        ))}
                      </div>
                    )}
                    {data.stacks.tools?.length > 0 && (
                      <div className="flex items-center gap-1.5 flex-wrap">
                        <span className="text-[11px] text-[#888780] w-10">
                          Tools
                        </span>
                        {data.stacks.tools.map((s) => (
                          <Chip key={s} label={s} color="purple" />
                        ))}
                      </div>
                    )}
                  </div>
                </div>
              )}

              <div className="grid grid-cols-1 gap-3 mb-4">
                {data.details?.tasks?.length > 0 && (
                  <div>
                    <p className="text-[13px] font-semibold text-[#1A1A1A] mb-1.5">
                      📋 주요업무
                    </p>
                    <ul className="space-y-1">
                      {data.details.tasks.map((t, i) => (
                        <li
                          key={i}
                          className="text-[13px] text-[#444] flex gap-1.5"
                        >
                          <span className="text-[#888780] mt-0.5">•</span>
                          {t}
                        </li>
                      ))}
                    </ul>
                  </div>
                )}
                {data.details?.reqs?.length > 0 && (
                  <div>
                    <p className="text-[13px] font-semibold text-[#1A1A1A] mb-1.5">
                      ✅ 자격요건
                    </p>
                    <ul className="space-y-1">
                      {data.details.reqs.map((r, i) => (
                        <li
                          key={i}
                          className="text-[13px] text-[#444] flex gap-1.5"
                        >
                          <span className="text-[#888780] mt-0.5">•</span>
                          {r}
                        </li>
                      ))}
                    </ul>
                  </div>
                )}
                {data.details?.pref?.length > 0 && (
                  <div>
                    <p className="text-[13px] font-semibold text-[#1A1A1A] mb-1.5">
                      ⭐ 우대사항
                    </p>
                    <ul className="space-y-1">
                      {data.details.pref.map((p, i) => (
                        <li
                          key={i}
                          className="text-[13px] text-[#444] flex gap-1.5"
                        >
                          <span className="text-[#888780] mt-0.5">•</span>
                          {p}
                        </li>
                      ))}
                    </ul>
                  </div>
                )}
              </div>

              {data.conditions &&
                (data.conditions.type ||
                  data.conditions.location ||
                  data.conditions.salary) && (
                  <div className="mb-4">
                    <p className="text-[13px] font-semibold text-[#1A1A1A] mb-2">
                      🏠 근무조건
                    </p>
                    <div className="flex flex-wrap gap-4 text-[13px] text-[#444]">
                      {data.conditions.type && (
                        <span>
                          <span className="text-[#888780]">형태 </span>
                          {data.conditions.type}
                        </span>
                      )}
                      {data.conditions.location && (
                        <span>
                          <span className="text-[#888780]">지역 </span>
                          {data.conditions.location}
                        </span>
                      )}
                      {data.conditions.salary && (
                        <span>
                          <span className="text-[#888780]">급여 </span>
                          {data.conditions.salary}
                        </span>
                      )}
                    </div>
                  </div>
                )}

              {data.culture?.length > 0 && (
                <div className="mb-4">
                  <p className="text-[13px] font-semibold text-[#1A1A1A] mb-2">
                    🏢 조직문화
                  </p>
                  <div className="flex flex-wrap gap-1.5">
                    {data.culture.map((c) => (
                      <Chip key={c} label={c} color="gray" />
                    ))}
                  </div>
                </div>
              )}

              {(data.insight?.challenge || data.insight?.fit) && (
                <div className="bg-[#F8F9FA] rounded-lg p-4">
                  <p className="text-[13px] font-semibold text-[#1A1A1A] mb-2">
                    💡 인사이트
                  </p>
                  {data.insight.challenge && (
                    <p className="text-[12px] text-[#555] mb-1">
                      <span className="text-[#888780]">기술 도전 </span>
                      {data.insight.challenge}
                    </p>
                  )}
                  {data.insight.fit && (
                    <p className="text-[12px] text-[#555]">
                      <span className="text-[#888780]">적합한 개발자 </span>
                      {data.insight.fit}
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
