import { useState, useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { getJobs } from "../api/jobApi";
import SearchBar from "../components/job/SearchBar";
import JobFilter from "../components/job/JobFilter";
import JobCard from "../components/job/JobCard";
import Pagination from "../components/job/Pagination";
import Sidebar from "../components/layout/Sidebar";
import type { Job } from "../components/job/JobCard";

const JobListPage = () => {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  const pageParam = Number(searchParams.get("page") ?? "1");
  const page = pageParam - 1;
  const keyword = searchParams.get("keyword") ?? "";
  const filter = searchParams.get("filter") ?? "";

  // 검색창 입력값은 URL 불필요 — useState 유지
  const [searchInput, setSearchInput] = useState(keyword);
  const [jobs, setJobs] = useState<Job[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [totalPages, setTotalPages] = useState(0);

  const updateParams = (updates: Record<string, string>) => {
    const next = new URLSearchParams(searchParams);
    Object.entries(updates).forEach(([k, v]) => {
      if (v) next.set(k, v);
      else next.delete(k);
    });
    setSearchParams(next);
  };

  const handleSearch = (newKeyword: string) => {
    updateParams({ keyword: newKeyword, filter: "", page: "1" });
    setSearchInput(newKeyword);
  };

  const handleFilter = (newFilter: string) => {
    updateParams({ filter: newFilter, keyword: "", page: "1" });
    setSearchInput("");
  };

  // Pagination 컴포넌트는 0-based → URL 저장 시 1-based로 변환
  const handlePageChange = (newPage: number) => {
    updateParams({ page: String(newPage + 1) });
  };

  useEffect(() => {
    const fetchJobs = async () => {
      setLoading(true);
      setError("");

      try {
        const params: Record<string, string | number> = { page, size: 10 };

        if (keyword) params.keyword = keyword;

        if (filter) {
          if (["서울", "경기", "부산"].includes(filter)) {
            params.location = filter;
          } else if (["신입", "경력"].includes(filter)) {
            params.experienceLevel = filter;
          } else {
            params.keyword = filter;
          }
        }

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
  }, [keyword, filter, page]);

  return (
    <div className="max-w-[1100px] mx-auto px-6 py-6">
      <div className="mb-4">
        <SearchBar value={searchInput} onChange={setSearchInput} onSearch={handleSearch} />
      </div>

      <div className="mb-6">
        <JobFilter selected={filter} onSelect={handleFilter} />
      </div>

      <div className="flex gap-6">
        <div className="flex-1">
          {loading && (
            <div className="flex justify-center py-20">
              <div className="w-8 h-8 border-4 border-[#378ADD] border-t-transparent rounded-full animate-spin" />
            </div>
          )}

          {!loading && error && (
            <div className="text-center py-20 text-[#E24B4A] text-[14px]">{error}</div>
          )}

          {!loading && !error && jobs.length === 0 && (
            <div className="text-center py-20 text-[#888780] text-[14px]">검색 결과가 없습니다.</div>
          )}

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
