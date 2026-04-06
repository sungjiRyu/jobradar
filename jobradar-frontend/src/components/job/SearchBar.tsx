/**
 * SearchBar — 키워드 검색 컴포넌트
 * 공고 목록 페이지 상단에서 회사명/직무명 키워드 검색 제공
 * 엔터 키 또는 검색 버튼 클릭 시 onSearch 콜백 호출
 */

import { useState } from "react";

interface SearchBarProps {
  onSearch: (keyword: string) => void;
}

const SearchBar = ({ onSearch }: SearchBarProps) => {
  // 입력 중인 검색어 상태
  const [keyword, setKeyword] = useState("");

  // 엔터 키 입력 시 검색 실행
  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter") {
      onSearch(keyword);
    }
  };

  return (
    <div className="flex gap-2">
      <input
        type="text"
        value={keyword}
        onChange={(e) => setKeyword(e.target.value)}
        onKeyDown={handleKeyDown}
        placeholder="회사명 또는 직무명으로 검색"
        className="flex-1 h-10 px-4 rounded-lg border border-[#DDDDDD] bg-white text-[14px] text-[#1A1A1A] placeholder-[#888780] focus:outline-none focus:border-[#378ADD]"
      />
      <button
        onClick={() => onSearch(keyword)}
        className="h-10 px-5 bg-[#378ADD] text-white text-[14px] rounded-lg hover:opacity-90 transition-opacity"
      >
        검색
      </button>
    </div>
  );
};

export default SearchBar;
