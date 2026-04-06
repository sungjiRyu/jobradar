/**
 * JobFilter — 필터 칩 컴포넌트
 * 공고 목록 페이지 상단에서 카테고리별 필터링 제공
 * 선택된 칩: 파란 배경 + 흰 텍스트 / 미선택: 흰 배경 + 회색 테두리
 */

// 필터 칩 목록 (카테고리별로 그룹핑)
const FILTER_CHIPS = [
  { label: "전체", value: "" },
  { label: "백엔드", value: "백엔드" },
  { label: "풀스택", value: "풀스택" },
  { label: "서울", value: "서울" },
  { label: "경기", value: "경기" },
  { label: "부산", value: "부산" },
  { label: "신입", value: "신입" },
  { label: "경력", value: "경력" },
];

interface JobFilterProps {
  selected: string;
  onSelect: (value: string) => void;
}

const JobFilter = ({ selected, onSelect }: JobFilterProps) => {
  return (
    <div className="flex flex-wrap gap-2">
      {FILTER_CHIPS.map((chip) => {
        // 현재 선택된 칩인지 확인
        const isActive = selected === chip.value;

        return (
          <button
            key={chip.label}
            onClick={() => onSelect(chip.value)}
            className={`px-4 py-1.5 rounded-full text-[13px] border transition-colors ${
              isActive
                ? "bg-[#378ADD] text-white border-[#378ADD]"
                : "bg-white text-[#888780] border-[#DDDDDD] hover:border-[#378ADD]"
            }`}
          >
            {chip.label}
          </button>
        );
      })}
    </div>
  );
};

export default JobFilter;
