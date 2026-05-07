import { useState, useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { getJobs } from "../api/jobApi";
import SearchFilter from "../components/job/SearchFilter";
import type { FilterState } from "../components/job/SearchFilter";
import JobCard from "../components/job/JobCard";
import Pagination from "../components/job/Pagination";
import Sidebar from "../components/layout/Sidebar";
import type { Job } from "../components/job/JobCard";

const INITIAL_FILTER: FilterState = {
  keyword: "",
  job: null,
  locations: [],
  experiences: [],
  techStacks: [],
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

  // 페이지 변경 (Pagination은 0-based → URL은 1-based로 저장)
  const handlePageChange = (newPage: number) => {
    const next = new URLSearchParams(searchParams);
    next.set("page", String(newPage + 1));
    setSearchParams(next);
  };

  // 필터 변경 시 state 업데이트 + 페이지 1로 리셋
  const handleFilterChange = (filter: FilterState) => {
    setCurrentFilter(filter);
    setSearchParams(new URLSearchParams({ page: "1" }));
  };

  // currentFilter 또는 page 변경 시 API 재호출
  useEffect(() => {
    const fetchJobs = async () => {
      setLoading(true);
      setError("");

      try {
        const params: Record<string, string | number | string[]> = { page, size: 10 };

        // 직무(job)와 키워드 중 직무가 우선 (둘 다 있으면 직무로 검색)
        const effectiveKeyword = currentFilter.job ?? currentFilter.keyword;
        if (effectiveKeyword) params.keyword = effectiveKeyword;

        // location, experienceLevel, techStack: 배열로 전달 → axios가 ?location=서울&location=경기 형태로 직렬화
        if (currentFilter.locations.length > 0) params.location = currentFilter.locations;
        if (currentFilter.experiences.length > 0) params.experienceLevel = currentFilter.experiences;
        if (currentFilter.techStacks.length > 0) params.techStack = currentFilter.techStacks;

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
  }, [currentFilter, page]);

  return (
    <div className="max-w-[1100px] mx-auto px-6 py-6">
      {/* 검색 + 필터 */}
      <div className="mb-6">
        <SearchFilter onFilterChange={handleFilterChange} />
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
