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
import { getScraps } from "../api/scrapApi";
import { usePageTitle } from "../hooks/usePageTitle";

const INITIAL_FILTER: FilterState = {
  keyword: "",
  job: null,
  locations: [],
  experiences: [],
  techStacks: [],
  sort: "",
};

const JobListPage = () => {
  usePageTitle("채용공고");
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  // URL은 1-based (사용자 친화적), API는 0-based로 변환. 음수 방지
  const pageParam = Math.max(1, Number(searchParams.get("page") ?? "1"));
  const page = pageParam - 1;

  const [jobs, setJobs] = useState<Job[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [currentFilter, setCurrentFilter] =
    useState<FilterState>(INITIAL_FILTER);
  const [todayStats, setTodayStats] = useState<TodayStats | null>(null);
  const [activeCard, setActiveCard] = useState<
    "all" | "today" | "urgent" | "junior" | null
  >(null);
  // jobPostId → scrapId 맵: 스크랩된 공고를 O(1)로 조회하기 위한 캐시
  const [scrapsMap, setScrapsMap] = useState<Record<number, number>>({});

  // 페이지 변경 (Pagination은 0-based → URL은 1-based로 저장)
  const handlePageChange = (newPage: number) => {
    const next = new URLSearchParams(searchParams);
    next.set("page", String(newPage + 1));
    setSearchParams(next);
  };

  // 정렬만 바뀐 경우엔 현황카드 선택을 유지 (카드 필터 + 정렬 동시 적용)
  const handleFilterChange = (newFilter: FilterState) => {
    const onlySortChanged =
      newFilter.keyword === currentFilter.keyword &&
      newFilter.job === currentFilter.job &&
      newFilter.locations.join() === currentFilter.locations.join() &&
      newFilter.experiences.join() === currentFilter.experiences.join() &&
      newFilter.techStacks.join() === currentFilter.techStacks.join();

    setCurrentFilter(newFilter);
    if (!onlySortChanged) setActiveCard(null);
    setSearchParams(new URLSearchParams({ page: "1" }));
  };

  // 현황 카드 클릭 — 같은 카드 재클릭 시 해제, "all" 클릭 시 필터 초기화
  const handleCardClick = (card: "all" | "today" | "urgent" | "junior") => {
    if (card === "all") setCurrentFilter({ ...INITIAL_FILTER });
    setActiveCard((prev) => (prev === card ? null : card));
    setSearchParams(new URLSearchParams({ page: "1" }));
  };

  // 오늘의 현황 통계 (최초 1회)
  useEffect(() => {
    getStatsToday()
      .then((res) => setTodayStats(res.data.data))
      .catch((err) => console.error("통계 조회 실패:", err));
  }, []);

  // 로그인 상태일 때 스크랩 목록 조회 (새로고침 후에도 스크랩 상태 복원)
  useEffect(() => {
    const token = localStorage.getItem("accessToken");
    if (!token) return;

    getScraps()
      .then((res) => {
        // { jobPostId: scrapId } 형태로 변환하여 빠른 조회에 사용
        const map: Record<number, number> = {};
        for (const scrap of res.data.data) {
          map[scrap.jobPostId] = scrap.scrapId;
        }
        setScrapsMap(map);
      })
      .catch((err) => console.error("스크랩 목록 조회 실패:", err));
  }, []);

  // currentFilter 또는 page 변경 시 API 재호출
  useEffect(() => {
    const fetchJobs = async () => {
      setLoading(true);
      setError("");

      try {
        const params: Record<string, string | number | string[] | boolean> = {
          page,
          size: 10,
        };

        if (currentFilter.keyword) params.keyword = currentFilter.keyword;
        if (currentFilter.job) params.jobType = currentFilter.job;
        if (currentFilter.locations.length > 0)
          params.location = currentFilter.locations;
        if (currentFilter.experiences.length > 0)
          params.experienceLevel = currentFilter.experiences;
        if (currentFilter.techStacks.length > 0)
          params.techStack = currentFilter.techStacks;
        if (currentFilter.sort) params.sort = currentFilter.sort;

        // 현황 카드 필터
        if (activeCard === "today") params.todayOnly = true;
        // 마감임박 카드: urgentOnly 적용 + 사용자가 정렬을 별도로 선택하지 않은 경우에만 기본값(마감일순) 적용
        if (activeCard === "urgent") {
          params.urgentOnly = true;
          if (!currentFilter.sort) params.sort = "deadline,asc";
        }
        if (activeCard === "junior") params.experienceLevel = ["신입"];

        const res = await getJobs(params);
        setJobs(res.data.data.content);
        setTotalPages(res.data.data.totalPages);
        setTotalElements(res.data.data.totalElements);
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
    <div className="max-w-[1100px] mx-auto px-4 sm:px-6 py-6">
      {/* 검색 + 필터 */}
      <div className="mb-4">
        <SearchFilter onFilterChange={handleFilterChange} />
      </div>

      {/* 오늘의 현황 카드 — 모바일: 2x2, lg+: 4x1 */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 mb-6">
        {[
          {
            label: "전체 공고",
            value: todayStats?.totalCount,
            color: "text-[#1A1A1A]",
            card: "all" as const,
          },
          {
            label: "오늘 신규",
            value: todayStats?.todayCount,
            color: "text-[#378ADD]",
            card: "today" as const,
          },
          {
            label: "마감 임박",
            value: todayStats?.urgentCount,
            color: "text-[#E24B4A]",
            card: "urgent" as const,
          },
          {
            label: "신입 공고",
            value: todayStats?.juniorCount,
            color: "text-[#1D9E75]",
            card: "junior" as const,
          },
        ].map((item) => (
          <div
            key={item.label}
            onClick={() => handleCardClick(item.card)}
            className={`rounded-[10px] border px-4 py-3 flex flex-col gap-0.5 cursor-pointer transition-all
              ${
                activeCard === item.card
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
          {loading && (
            <div className="flex justify-center py-20">
              <div className="w-8 h-8 border-4 border-[#378ADD] border-t-transparent rounded-full animate-spin" />
            </div>
          )}

          {!loading && error && (
            <div className="text-center py-20 text-[#E24B4A] text-[14px]">
              {error}
            </div>
          )}

          {!loading && !error && jobs.length === 0 && (
            <div className="text-center py-20 text-[#888780] text-[14px]">
              검색 결과가 없습니다.
            </div>
          )}

          {!loading && !error && jobs.length > 0 && (
            <>
              <div className="text-[13px] text-[#888780] mb-3">
                검색 결과{" "}
                <span className="font-semibold text-[#1A1A1A]">
                  {totalElements.toLocaleString()}
                </span>
                개
              </div>
              <div className="flex flex-col gap-4">
                {jobs.map((job) => (
                  <div
                    key={job.id}
                    onClick={() => navigate(`/jobs/${job.id}`)}
                    className="cursor-pointer"
                  >
                    <JobCard
                      job={job}
                      initialScrapId={scrapsMap[job.id] ?? null}
                    />
                  </div>
                ))}
              </div>
              <Pagination
                page={page}
                totalPages={totalPages}
                onPageChange={handlePageChange}
              />
            </>
          )}
        </div>
        <div className="hidden lg:block lg:mt-[30px]">
          <Sidebar />
        </div>
      </div>
    </div>
  );
};

export default JobListPage;
