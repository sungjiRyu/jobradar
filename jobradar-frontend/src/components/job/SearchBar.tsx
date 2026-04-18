interface SearchBarProps {
  onSearch: (keyword: string) => void;
  value: string;
  onChange: (value: string) => void;
}

const SearchBar = ({ onSearch, value, onChange }: SearchBarProps) => {
  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter") {
      onSearch(value);
    }
  };

  return (
    <div className="flex gap-2">
      <input
        type="text"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        onKeyDown={handleKeyDown}
        placeholder="회사명 또는 직무명으로 검색"
        className="flex-1 h-10 px-4 rounded-lg border border-[#DDDDDD] bg-white text-[14px] text-[#1A1A1A] placeholder-[#888780] focus:outline-none focus:border-[#378ADD]"
      />
      <button
        onClick={() => onSearch(value)}
        className="h-10 px-5 bg-[#378ADD] text-white text-[14px] rounded-lg hover:opacity-90 transition-opacity"
      >
        검색
      </button>
    </div>
  );
};

export default SearchBar;
