interface PaginationProps {
  page: number;       // 0-based
  totalPages: number;
  onPageChange: (page: number) => void; // 0-based
}

const Pagination = ({ page, totalPages, onPageChange }: PaginationProps) => {
  if (totalPages <= 1) return null;

  const maxVisible = 10;
  const half = Math.floor(maxVisible / 2);
  let start = Math.max(0, page - half);
  const end = Math.min(totalPages, start + maxVisible);
  if (end - start < maxVisible) start = Math.max(0, end - maxVisible);
  const pageNumbers = Array.from({ length: end - start }, (_, i) => start + i);

  const btnBase = "w-8 h-8 rounded text-[13px] bg-white text-[#888780] border border-[#DDDDDD] hover:border-[#378ADD] disabled:opacity-30 disabled:cursor-not-allowed";
  const btnActive = "w-8 h-8 rounded text-[13px] bg-[#378ADD] text-white";

  return (
    <div className="flex justify-center gap-1 mt-8">
      <button onClick={() => onPageChange(0)} disabled={page === 0} className={btnBase}>«</button>
      <button onClick={() => onPageChange(Math.max(0, page - 1))} disabled={page === 0} className={btnBase}>‹</button>

      {pageNumbers.map((i) => (
        <button key={i} onClick={() => onPageChange(i)} className={page === i ? btnActive : btnBase}>
          {i + 1}
        </button>
      ))}

      <button onClick={() => onPageChange(Math.min(totalPages - 1, page + 1))} disabled={page === totalPages - 1} className={btnBase}>›</button>
      <button onClick={() => onPageChange(totalPages - 1)} disabled={page === totalPages - 1} className={btnBase}>»</button>
    </div>
  );
};

export default Pagination;
