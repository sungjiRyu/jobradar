import { useState, useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { getJobs } from "../api/jobApi";
import SearchFilter from "../components/job/SearchFilter";
import type { FilterState } from "../components/job/SearchFilter";
import JobCard from "../components/job/JobCard";
import Pagination from "../components/job/Pagination";
import Sidebar from "../components/layout/Sidebar";
import type { Job } from "../components/job/JobCard";
import { getStatsToday } from "../api/statsApi";
import type { TodayStats } from "../api/statsApi";

const INITIAL_FILTER: FilterState = {
  keyword: "",
  job: null,
  locations: [],
  experiences: [],
  techStacks: [],
  sort: "",
};

const JobListPage = () => {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  // URL은 1-based (사용자 친화적), API는 0-based로 변환. 음수 방지
  const pageParam = Math.max(1, Number(searchParams.get("page") ?? "1"));
  const page = pageParam - 1;

  const [jobs, setJobs] = useState<Job[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [totalPages, setTotalPages] = useState(0);
  const [currentFilter, setCurrentFilter] = useState<FilterState>(INITIAL_FILTER);
  const [todayStats, setTodayStats] = useState<TodayStats | null>(null);
  const [activeCard, setActiveCard] = useState<"today" | "urgent" | "junior" | null>(null);

  // 페이지 변경 (Pagination은 0-based → URL은 1-based로 저장)
  const handlePageChange = (newPage: number) => {
    const next = new URLSearchParams(searchParams);
    next.set("page", String(newPage + 1));
    setSearchParams(next);
  };

  // 필터 변경 시 state 업데이트 + 페이지 1로 리셋 + 카드 선택 해제
  const handleFilterChange = (filter: FilterState) => {
    setCurrentFilter(filter);
    setActiveCard(null);
    setSearchParams(new URLSearchParams({ page: "1" }));
  };

  // 현황 카드 클릭 — 같은 카드 재클릭 시 해제
  const handleCardClick = (card: "today" | "urgent" | "junior") => {
    setActiveCard(prev => prev === card ? null : card);
    setSearchParams(new URLSearchParams({ page: "1" }));
  };

  // 오늘의 현황 통계 (최초 1회)
  useEffect(() => {
    getStatsToday()
      .then(res => setTodayStats(res.data.data))
      .catch(err => console.error("통계 조회 실패:", err));
  }, []);

  // currentFilter 또는 page 변경 시 API 재호출
  useEffect(() => {
    const fetchJobs = async () => {
      setLoading(true);
      setError("");

      try {
        const params: Record<string, string | number | string[] | boolean> = { page, size: 10 };

        if (currentFilter.keyword) params.keyword = currentFilter.keyword;
        if (currentFilter.job) params.jobType = currentFilter.job;
        if (currentFilter.locations.length > 0) params.location = currentFilter.locations;
        if (currentFilter.experiences.length > 0) params.experienceLevel = currentFilter.experiences;
        if (currentFilter.techStacks.length > 0) params.techStack = currentFilter.techStacks;
        if (currentFilter.sort) params.sort = currentFilter.sort;

        // 현황 카드 필터
        if (activeCard === "today")  params.todayOnly = true;
        if (activeCard === "urgent") params.urgentOnly = true;
        if (activeCard === "junior") params.experienceLevel = ["신입"];

        const res = await getJobs(params);
        setJobs(res.data.data.content);
        setTotalPages(res.data.data.totalPages);
      } catch (err) {
        console.error("공고 목록 조회 실패:", err);
        setError("공고 목록을 불러오는데 실패했습니다.");
      } finally {
        setLoading(false);
      }
    };

    fetchJobs();
  }, [currentFilter, page, activeCard]);

  return (
    <div className="max-w-[1100px] mx-auto px-6 py-6">
      {/* 검색 + 필터 */}
      <div className="mb-4">
        <SearchFilter onFilterChange={handleFilterChange} />
      </div>

      {/* 오늘의 현황 카드 */}
      <div className="grid grid-cols-4 gap-3 mb-6">
        {[
          { label: "전체 공고", value: todayStats?.totalCount, color: "text-[#1A1A1A]", card: null },
          { label: "오늘 신규", value: todayStats?.todayCount, color: "text-[#378ADD]", card: "today" as const },
          { label: "마감 임박", value: todayStats?.urgentCount, color: "text-[#E24B4A]", card: "urgent" as const },
          { label: "신입 공고", value: todayStats?.juniorCount, color: "text-[#1D9E75]", card: "junior" as const },
        ].map(item => (
          <div
            key={item.label}
            onClick={() => item.card ? handleCardClick(item.card) : handleFilterChange({ ...INITIAL_FILTER })}
            className={`rounded-[10px] border px-4 py-3 flex flex-col gap-0.5 cursor-pointer transition-all
              ${activeCard === item.card && item.card !== null
                ? "bg-[#F0F7FF] border-[#378ADD] shadow-sm"
                : "bg-white border-[#DDDDDD]"
              }`}
          >
            <span className="text-[11px] text-[#888780]">{item.label}</span>
            <span className={`text-[20px] font-bold ${item.color}`}>
              {item.value != null ? item.value.toLocaleString() : "-"}
            </span>
          </div>
        ))}
      </div>

      <div className="flex gap-6">
        <div className="flex-1">
          {/* 로딩 스피너 */}
          {loading && (
            <div className="flex justify-center py-20">
              <div className="w-8 h-8 border-4 border-[#378ADD] border-t-transparent rounded-full animate-spin" />
            </div>
          )}

          {/* 에러 */}
          {!loading && error && (
            <div className="text-center py-20 text-[#E24B4A] text-[14px]">{error}</div>
          )}

          {/* 결과 없음 */}
          {!loading && !error && jobs.length === 0 && (
            <div className="text-center py-20 text-[#888780] text-[14px]">검색 결과가 없습니다.</div>
          )}

          {/* 공고 목록 */}
          {!loading && !error && jobs.length > 0 && (
            <>
              <div className="flex flex-col gap-4">
                {jobs.map((job) => (
                  <div key={job.id} onClick={() => navigate(`/jobs/${job.id}`)} className="cursor-pointer">
                    <JobCard job={job} />
                  </div>
                ))}
              </div>
              <Pagination page={page} totalPages={totalPages} onPageChange={handlePageChange} />
            </>
          )}
        </div>

        <Sidebar />
      </div>
    </div>
  );
};

export default JobListPage;
