import { useEffect, useRef, useState } from "react";
import {
  FiBookmark,
  FiChevronLeft,
  FiChevronRight,
  FiEye,
  FiTrendingUp,
} from "react-icons/fi";
import { Link } from "react-router-dom";
import { getTrendingJobs } from "../../api/statsApi";
import type { TrendingJobStat } from "../../api/statsApi";
import { calcDday } from "../../utils/dateUtils";

const TRENDING_LIMIT = 10;
const POLLING_INTERVAL_MS = 60_000;

const SOURCE_SITE_STYLES: Record<string, string> = {
  사람인: "border-[#7DB6EA] bg-[#EEF7FF] text-[#1E6FAE]",
  잡코리아: "border-[#9FD6B9] bg-[#F0FAF4] text-[#247A4D]",
};

const formatDeadline = (deadline: string | null) => {
  const dday = calcDday(deadline);
  if (dday === null) return "상시채용";
  if (dday < 0) return "마감";
  if (dday === 0) return "D-Day";
  return `D-${dday}`;
};

const TrendingJobsRanking = () => {
  const carouselRef = useRef<HTMLDivElement>(null);
  const [jobs, setJobs] = useState<TrendingJobStat[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  const fetchTrendingJobs = async (showLoading: boolean) => {
    try {
      if (showLoading) setLoading(true);
      setError(false);
      const res = await getTrendingJobs();
      setJobs(res.data.data);
    } catch (err) {
      console.error("인기 공고 랭킹 조회 실패:", err);
      setError(true);
    } finally {
      if (showLoading) setLoading(false);
    }
  };

  useEffect(() => {
    fetchTrendingJobs(true);

    // 60초마다 인기 공고 목록을 다시 불러온다.
    const intervalId = window.setInterval(
      () => fetchTrendingJobs(false),
      POLLING_INTERVAL_MS,
    );

    return () => window.clearInterval(intervalId);
  }, []);

  const scrollCarousel = (direction: "prev" | "next") => {
    const carousel = carouselRef.current;
    if (!carousel) return;

    carousel.scrollBy({
      left: direction === "prev" ? -carousel.clientWidth : carousel.clientWidth,
      behavior: "smooth",
    });
  };

  return (
    <section className="mb-6">
      <div className="mb-3 flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <div className="flex flex-wrap items-center gap-2">
            <span className="inline-flex h-7 w-7 items-center justify-center rounded-lg bg-[#EAF4FF] text-[#185FA5]">
              <FiTrendingUp />
            </span>
            <h2 className="text-[16px] font-bold text-[#1A1A1A]">
              인기 공고 랭킹
            </h2>
            <span className="rounded-full border border-[#DDDDDD] bg-white px-2 py-[2px] text-[11px] font-medium text-[#888780]">
              TOP {TRENDING_LIMIT}
            </span>
          </div>
          <p className="mt-1.5 text-[13px] text-[#888780]">
            조회와 스크랩이 많은 공고 TOP 10
          </p>
        </div>

        <div className="hidden items-center gap-1 sm:flex">
          <button
            type="button"
            onClick={() => scrollCarousel("prev")}
            className="inline-flex h-8 w-8 items-center justify-center rounded-lg border border-[#DDDDDD] bg-white text-[#555555] hover:bg-[#F5F5F5]"
            title="이전"
          >
            <FiChevronLeft />
          </button>
          <button
            type="button"
            onClick={() => scrollCarousel("next")}
            className="inline-flex h-8 w-8 items-center justify-center rounded-lg border border-[#DDDDDD] bg-white text-[#555555] hover:bg-[#F5F5F5]"
            title="다음"
          >
            <FiChevronRight />
          </button>
        </div>
      </div>

      {loading && (
        <div className="flex gap-3 overflow-hidden pb-2">
          {[0, 1, 2].map((index) => (
            <div
              key={index}
              className="h-[164px] shrink-0 basis-full rounded-lg border border-[#DDDDDD] bg-white p-3 sm:basis-[calc((100%_-_12px)/2)] lg:basis-[calc((100%_-_24px)/3)]"
            >
              <div className="h-8 w-24 rounded bg-gray-200 animate-pulse" />
              <div className="mt-4 h-5 w-4/5 rounded bg-gray-200 animate-pulse" />
              <div className="mt-2 h-5 w-2/3 rounded bg-gray-200 animate-pulse" />
              <div className="mt-5 h-7 w-full rounded bg-gray-100 animate-pulse" />
            </div>
          ))}
        </div>
      )}

      {!loading && error && (
        <div className="rounded-lg border border-[#DDDDDD] bg-white px-4 py-6 text-center text-[13px] text-[#888780]">
          인기 공고 랭킹을 불러오지 못했습니다.
        </div>
      )}

      {!loading && !error && jobs.length === 0 && (
        <div className="rounded-lg border border-[#DDDDDD] bg-white px-4 py-6 text-center text-[13px] text-[#888780]">
          아직 인기 공고가 없습니다.
        </div>
      )}

      {!loading && !error && jobs.length > 0 && (
        <div
          ref={carouselRef}
          className="flex snap-x snap-mandatory gap-3 overflow-x-auto scroll-px-0 scroll-smooth pb-2 [-ms-overflow-style:none] [scrollbar-width:none] [&::-webkit-scrollbar]:hidden"
        >
          {jobs.map((job) => {
            const sourceSiteStyle =
              SOURCE_SITE_STYLES[job.sourceSite] ??
              "border-[#DDDDDD] bg-[#F5F5F5] text-[#888780]";
            const deadlineLabel = formatDeadline(job.deadline);

            return (
              <Link
                key={job.id}
                to={`/jobs/${job.id}`}
                className="group flex h-[164px] shrink-0 snap-start basis-full flex-col rounded-lg border border-[#DDDDDD] bg-white p-3 text-left transition-all hover:border-[#B8D8F4] hover:shadow-md sm:basis-[calc((100%_-_12px)/2)] lg:basis-[calc((100%_-_24px)/3)]"
              >
                <div className="flex items-start justify-between gap-2">
                  <div className="flex min-w-0 items-center gap-2">
                    <div
                      className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-lg text-[13px] font-bold ${
                        job.rank <= 3
                          ? "bg-[#185FA5] text-white"
                          : "bg-[#F5F5F5] text-[#555555]"
                      }`}
                    >
                      {job.rank}
                    </div>
                    <div className="min-w-0">
                      <p className="truncate text-[13px] font-medium leading-4 text-[#555555]">
                        {job.company}
                      </p>
                      <div className="mt-1 flex items-center gap-1.5">
                        <span
                          className={`rounded-full border px-2 py-[2px] text-[11px] font-medium leading-4 ${sourceSiteStyle}`}
                        >
                          {job.sourceSite}
                        </span>
                      </div>
                    </div>
                  </div>
                </div>

                <h3 className="mt-2.5 min-h-10 break-words text-[15px] font-semibold leading-5 text-[#1A1A1A] line-clamp-2">
                  {job.title}
                </h3>

                <div className="mt-2 flex h-5 gap-1.5 overflow-hidden">
                  {job.techStacks.slice(0, 3).map((stack) => (
                    <span
                      key={stack}
                      className="h-5 shrink-0 rounded-full bg-blue-50 px-2 py-0.5 text-[11px] text-[#378ADD]"
                    >
                      {stack}
                    </span>
                  ))}
                </div>

                <div className="mt-auto flex items-center justify-between gap-3 border-t border-[#EEEEEE] pt-2">
                  <div className="min-w-0 text-[12px] text-[#888780]">
                    <p className="truncate">
                      {job.location} · {job.experienceLevel}
                    </p>
                    <p
                      className={
                        deadlineLabel.startsWith("D-")
                          ? "font-semibold text-[#E24B4A]"
                          : ""
                      }
                    >
                      {deadlineLabel}
                    </p>
                  </div>

                  <div className="flex shrink-0 items-center gap-1.5 text-[12px] text-[#555555]">
                    <span
                      className="inline-flex h-7 items-center gap-1 rounded-md bg-[#F7F8FA] px-2"
                      title="조회수"
                    >
                      <FiEye className="text-[#888780]" />
                      {job.viewCount.toLocaleString()}
                    </span>
                    <span
                      className="inline-flex h-7 items-center gap-1 rounded-md bg-[#F7F8FA] px-2"
                      title="스크랩 수"
                    >
                      <FiBookmark className="text-[#888780]" />
                      {job.scrapCount.toLocaleString()}
                    </span>
                  </div>
                </div>
              </Link>
            );
          })}
        </div>
      )}
    </section>
  );
};

export default TrendingJobsRanking;
