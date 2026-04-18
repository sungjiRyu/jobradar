/**
 * JobListPage — 채용공고 목록 페이지 (메인 페이지)
 * 검색, 필터, 공고 카드 목록, 사이드바를 조합한 전체 레이아웃
 * GET /api/jobs API를 호출해서 공고 데이터를 가져옴
 * 검색어/필터 변경 시 API를 다시 호출
 */

import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { getJobs } from "../api/jobApi";
import SearchBar from "../components/job/SearchBar";
import JobFilter from "../components/job/JobFilter";
import JobCard from "../components/job/JobCard";
import Sidebar from "../components/layout/Sidebar";
import type { Job } from "../components/job/JobCard";

const JobListPage = () => {
  const navigate = useNavigate();

  // 공고 목록 상태
  const [jobs, setJobs] = useState<Job[]>([]);
  // 로딩 상태
  const [loading, setLoading] = useState(true);
  // 에러 메시지 상태
  const [error, setError] = useState("");
  // 검색 키워드
  const [keyword, setKeyword] = useState("");
  // 선택된 필터 값
  const [filter, setFilter] = useState("");
  // 현재 페이지 번호 (0부터 시작)
  const [page, setPage] = useState(0);
  // 전체 페이지 수
  const [totalPages, setTotalPages] = useState(0);

  // 공고 목록 API 호출
  // keyword, filter, page가 변경될 때마다 자동 실행
  useEffect(() => {
    const fetchJobs = async () => {
      setLoading(true);
      setError("");

      try {
        // 쿼리 파라미터 구성 — 빈 값은 제외
        const params: Record<string, string | number> = {
          page,
          size: 10,
        };

        if (keyword) params.keyword = keyword;

        // 필터 값을 적절한 API 파라미터에 매핑
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

  // 검색 실행 시 페이지를 0으로 초기화
  const handleSearch = (newKeyword: string) => {
    setKeyword(newKeyword);
    setPage(0);
  };

  // 필터 변경 시 페이지를 0으로 초기화
  const handleFilter = (newFilter: string) => {
    setFilter(newFilter);
    setPage(0);
  };

  return (
    <div className="max-w-[1100px] mx-auto px-6 py-6">
      {/* 검색바 */}
      <div className="mb-4">
        <SearchBar onSearch={handleSearch} />
      </div>

      {/* 필터 칩 */}
      <div className="mb-6">
        <JobFilter selected={filter} onSelect={handleFilter} />
      </div>

      {/* 본문: 공고 카드 목록 + 사이드바 */}
      <div className="flex gap-6">
        {/* 왼쪽: 공고 카드 목록 */}
        <div className="flex-1">
          {/* 로딩 중 */}
          {loading && (
            <div className="flex justify-center py-20">
              <div className="w-8 h-8 border-4 border-[#378ADD] border-t-transparent rounded-full animate-spin" />
            </div>
          )}

          {/* 에러 발생 */}
          {!loading && error && (
            <div className="text-center py-20 text-[#E24B4A] text-[14px]">
              {error}
            </div>
          )}

          {/* 검색 결과 없음 */}
          {!loading && !error && jobs.length === 0 && (
            <div className="text-center py-20 text-[#888780] text-[14px]">
              검색 결과가 없습니다.
            </div>
          )}

          {/* 공고 카드 목록 */}
          {!loading && !error && jobs.length > 0 && (
            <>
              <div className="flex flex-col gap-4">
                {jobs.map((job) => (
                  <div
                    key={job.id}
                    onClick={() => navigate(`/jobs/${job.id}`)}
                    className="cursor-pointer"
                  >
                    <JobCard job={job} />
                  </div>
                ))}
              </div>

              {/* 페이지네이션 */}
              {totalPages > 1 && (
                <div className="flex justify-center gap-2 mt-8">
                  {Array.from({ length: totalPages }, (_, i) => (
                    <button
                      key={i}
                      onClick={() => setPage(i)}
                      className={`w-8 h-8 rounded text-[13px] ${
                        page === i
                          ? "bg-[#378ADD] text-white"
                          : "bg-white text-[#888780] border border-[#DDDDDD] hover:border-[#378ADD]"
                      }`}
                    >
                      {i + 1}
                    </button>
                  ))}
                </div>
              )}
            </>
          )}
        </div>

        {/* 오른쪽: 사이드바 */}
        <Sidebar />
      </div>
    </div>
  );
};

export default JobListPage;
