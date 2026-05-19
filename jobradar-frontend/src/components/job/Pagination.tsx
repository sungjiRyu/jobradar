import { useState, useEffect } from "react";

interface PaginationProps {
  page: number; // 0-based
  totalPages: number;
  onPageChange: (page: number) => void; // 0-based
}

const Pagination = ({ page, totalPages, onPageChange }: PaginationProps) => {
  // 모바일(sm 미만)에서는 페이지 번호 5개, 데스크톱은 10개 표시
  const [maxVisible, setMaxVisible] = useState(() =>
    typeof window !== "undefined" && window.innerWidth < 640 ? 5 : 10,
  );

  useEffect(() => {
    const update = () => setMaxVisible(window.innerWidth < 640 ? 5 : 10);
    window.addEventListener("resize", update);
    return () => window.removeEventListener("resize", update);
  }, []);

  if (totalPages <= 1) return null;

  const half = Math.floor(maxVisible / 2);
  let start = Math.max(0, page - half);
  const end = Math.min(totalPages, start + maxVisible);
  if (end - start < maxVisible) start = Math.max(0, end - maxVisible);
  const pageNumbers = Array.from({ length: end - start }, (_, i) => start + i);

  const btnBase =
    "w-7 h-7 sm:w-8 sm:h-8 rounded text-[12px] sm:text-[13px] bg-white text-[#888780] border border-[#DDDDDD] hover:border-[#378ADD] disabled:opacity-30 disabled:cursor-not-allowed";
  const btnActive =
    "w-7 h-7 sm:w-8 sm:h-8 rounded text-[12px] sm:text-[13px] bg-[#378ADD] text-white";

  return (
    <div className="flex flex-wrap justify-center gap-1 mt-8">
      <button
        onClick={() => onPageChange(0)}
        disabled={page === 0}
        className={btnBase}
      >
        «
      </button>
      <button
        onClick={() => onPageChange(Math.max(0, page - 1))}
        disabled={page === 0}
        className={btnBase}
      >
        ‹
      </button>

      {pageNumbers.map((i) => (
        <button
          key={i}
          onClick={() => onPageChange(i)}
          className={page === i ? btnActive : btnBase}
        >
          {i + 1}
        </button>
      ))}

      <button
        onClick={() => onPageChange(Math.min(totalPages - 1, page + 1))}
        disabled={page === totalPages - 1}
        className={btnBase}
      >
        ›
      </button>
      <button
        onClick={() => onPageChange(totalPages - 1)}
        disabled={page === totalPages - 1}
        className={btnBase}
      >
        »
      </button>
    </div>
  );
};

export default Pagination;
