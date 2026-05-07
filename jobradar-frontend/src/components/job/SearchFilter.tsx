import { useState, useEffect, useRef } from "react";

// ─────────────────────────────────────────────
// 타입 정의
// ─────────────────────────────────────────────

export interface FilterState {
  keyword: string;
  job: string | null;       // 직무: 단일 선택
  locations: string[];      // 지역: 복수 선택
  experiences: string[];    // 경력: 복수 선택
  techStacks: string[];     // 기술스택: 복수 선택
}

export interface SearchFilterProps {
  onFilterChange: (filter: FilterState) => void;
}

// 드롭다운 종류 식별자
type FilterKey = "job" | "locations" | "experiences" | "techStacks";

// ─────────────────────────────────────────────
// 필터 옵션 데이터
// ─────────────────────────────────────────────

const FILTER_CONFIG: Record<FilterKey, {
  label: string;
  options: string[];
  multi: boolean;           // true: 복수 선택 / false: 단일 선택
  activeBtn: string;        // 선택값 있을 때 버튼 스타일
  badge: string;            // 선택 개수 뱃지 배경색
  checkBox: string;         // 선택된 항목 체크박스 스타일
}> = {
  job: {
    label: "직무",
    options: ["백엔드", "프론트엔드", "풀스택", "DevOps", "데이터", "AI/ML", "모바일"],
    multi: false,
    activeBtn: "bg-[#E6F1FB] text-[#0C447C] border-[#378ADD]",
    badge: "bg-[#378ADD]",
    checkBox: "bg-[#E6F1FB] border-[#85B7EB] text-[#378ADD]",
  },
  locations: {
    label: "지역",
    options: ["서울", "경기", "인천", "부산", "대구", "광주", "대전", "울산", "세종", "강원", "충북", "충남", "전북", "전남", "경북", "경남", "제주"],
    multi: true,
    activeBtn: "bg-[#EAF3DE] text-[#27500A] border-[#1D9E75]",
    badge: "bg-[#1D9E75]",
    checkBox: "bg-[#EAF3DE] border-[#97C459] text-[#27500A]",
  },
  experiences: {
    label: "경력",
    // DB 저장 형태: "신입", "경력무관", "경력1년↑", "경력3년↑", "경력5년↑", "경력10년↑"
    // LIKE '%value%' 로 검색하므로 DB 값에 포함되는 문자열을 사용
    options: ["신입", "경력무관", "경력1년", "경력3년", "경력5년", "경력10년"],
    multi: true,
    activeBtn: "bg-[#EEEDFE] text-[#3C3489] border-[#7F77DD]",
    badge: "bg-[#7F77DD]",
    checkBox: "bg-[#EEEDFE] border-[#AFA9EC] text-[#7F77DD]",
  },
  techStacks: {
    label: "기술스택",
    options: ["Java", "Spring", "Python", "React", "Vue", "Node.js", "AWS", "Docker", "Kotlin", "TypeScript", "MySQL", "Redis", "Kubernetes", "Jenkins"],
    multi: true,
    activeBtn: "bg-[#FAEEDA] text-[#633806] border-[#EF9F27]",
    badge: "bg-[#EF9F27]",
    checkBox: "bg-[#FAEEDA] border-[#EF9F27] text-[#EF9F27]",
  },
};

const FILTER_KEYS: FilterKey[] = ["job", "locations", "experiences", "techStacks"];

const INITIAL_FILTER: FilterState = {
  keyword: "",
  job: null,
  locations: [],
  experiences: [],
  techStacks: [],
};

// ─────────────────────────────────────────────
// 메인 컴포넌트
// ─────────────────────────────────────────────

const SearchFilter = ({ onFilterChange }: SearchFilterProps) => {
  const [filter, setFilter] = useState<FilterState>(INITIAL_FILTER);
  const [keywordInput, setKeywordInput] = useState("");

  // 현재 열린 드롭다운 패널 (하나만 열림)
  const [openPanel, setOpenPanel] = useState<FilterKey | null>(null);

  // 각 패널 내부 검색창 입력값
  const [panelSearches, setPanelSearches] = useState<Record<FilterKey, string>>({
    job: "",
    locations: "",
    experiences: "",
    techStacks: "",
  });

  // 컨테이너 외부 클릭 감지용 ref
  const containerRef = useRef<HTMLDivElement>(null);

  // 컨테이너 외부 클릭 시 모든 패널 닫기
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setOpenPanel(null);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  // 필터 변경 시마다 부모에게 알림
  useEffect(() => {
    onFilterChange(filter);
  }, [filter]);

  // ── 핸들러 ───────────────────────────────────

  // 드롭다운 패널 열기/닫기 (다른 패널은 자동 닫힘)
  const togglePanel = (key: FilterKey) => {
    setOpenPanel(prev => prev === key ? null : key);
    setPanelSearches(prev => ({ ...prev, [key]: "" }));
  };

  // 항목 선택 (직무: 단일, 나머지: 복수 토글)
  const handleSelect = (key: FilterKey, value: string) => {
    setFilter(prev => {
      if (key === "job") {
        return { ...prev, job: prev.job === value ? null : value };
      }
      const arr = prev[key] as string[];
      return {
        ...prev,
        [key]: arr.includes(value) ? arr.filter(v => v !== value) : [...arr, value],
      };
    });
  };

  // 특정 필터만 초기화
  const handleReset = (key: FilterKey) => {
    setFilter(prev => ({ ...prev, [key]: key === "job" ? null : [] }));
  };

  // 모든 필터 초기화
  const handleResetAll = () => {
    setFilter(INITIAL_FILTER);
    setKeywordInput("");
  };

  // 활성 태그 ✕ 클릭 → 해당 항목만 해제
  const handleRemoveTag = (key: FilterKey, value: string) => {
    setFilter(prev => {
      if (key === "job") return { ...prev, job: null };
      const arr = prev[key] as string[];
      return { ...prev, [key]: arr.filter(v => v !== value) };
    });
  };

  // 검색어 확정 (버튼 클릭 또는 Enter)
  const handleSearch = () => {
    setFilter(prev => ({ ...prev, keyword: keywordInput }));
  };

  // 검색어 초기화 (✕ 버튼)
  const handleClearKeyword = () => {
    setKeywordInput("");
    setFilter(prev => ({ ...prev, keyword: "" }));
  };

  // ── 헬퍼 ─────────────────────────────────────

  // 특정 필터의 선택된 값 배열 반환
  const getSelected = (key: FilterKey): string[] => {
    if (key === "job") return filter.job ? [filter.job] : [];
    return filter[key] as string[];
  };

  // 특정 값이 선택되어 있는지 여부
  const isSelected = (key: FilterKey, value: string) => getSelected(key).includes(value);

  // 활성 태그 목록 (모든 필터 합산)
  const activeTags = FILTER_KEYS.flatMap(key =>
    getSelected(key).map(value => ({ key, value }))
  );

  // ─────────────────────────────────────────────
  // 렌더링
  // ─────────────────────────────────────────────

  return (
    <div ref={containerRef} className="bg-white rounded-[12px] border border-[#DDDDDD] p-5">

      {/* ── 검색바 ── */}
      <div className="flex gap-2 mb-4">
        <div className="relative flex-1">
          <input
            type="text"
            value={keywordInput}
            onChange={e => setKeywordInput(e.target.value)}
            onKeyDown={e => e.key === "Enter" && handleSearch()}
            placeholder="검색어를 입력하세요"
            className="w-full h-[40px] bg-[#F5F5F5] border border-[#DDDDDD] rounded-[8px] px-3 pr-8 text-[13px] outline-none transition-all focus:border-[#378ADD] focus:bg-white focus:shadow-[0_0_0_3px_rgba(55,138,221,0.08)]"
          />
          {/* 검색어 초기화 버튼 */}
          {keywordInput && (
            <button
              onClick={handleClearKeyword}
              className="absolute right-2.5 top-1/2 -translate-y-1/2 text-[#AAAAAA] hover:text-[#555] text-[12px]"
            >
              ✕
            </button>
          )}
        </div>
        <button
          onClick={handleSearch}
          className="h-[40px] px-4 bg-[#378ADD] text-white text-[13px] rounded-[8px] hover:opacity-90 transition-opacity"
        >
          검색
        </button>
      </div>

      {/* 구분선 */}
      <div className="h-px bg-[#F0F0F0] mb-4" />

      {/* ── 드롭다운 버튼 행 ── */}
      <div className="flex gap-2 flex-wrap">
        {FILTER_KEYS.map(key => {
          const config = FILTER_CONFIG[key];
          const selected = getSelected(key);
          const isActive = selected.length > 0;
          const isOpen = openPanel === key;

          return (
            <div key={key} className="relative">
              {/* 드롭다운 버튼 */}
              <button
                onClick={() => togglePanel(key)}
                className={`flex items-center gap-1.5 h-[34px] px-3 rounded-[8px] border text-[12px] transition-all
                  ${isActive
                    ? config.activeBtn
                    : "bg-white text-[#555] border-[#DDDDDD] hover:border-[#BBBBBB]"
                  }`}
              >
                <span>{config.label}</span>
                {/* 복수 선택 시 개수 뱃지 */}
                {isActive && config.multi && selected.length > 1 && (
                  <span className={`flex items-center justify-center w-4 h-4 rounded-full text-white text-[10px] font-semibold ${config.badge}`}>
                    {selected.length}
                  </span>
                )}
                {/* 화살표 (열리면 180도 회전) */}
                <span className={`text-[10px] transition-transform duration-200 ${isOpen ? "rotate-180" : ""}`}>
                  ▾
                </span>
              </button>

              {/* 드롭다운 패널 */}
              {isOpen && (
                <div className="absolute top-[calc(100%+6px)] left-0 min-w-[220px] bg-white border border-[#DDDDDD] rounded-[10px] shadow-[0_4px_20px_rgba(0,0,0,0.10)] z-[100]">
                  {/* 패널 내 검색창 */}
                  <div className="px-3 py-2.5 border-b border-[#F0F0F0]">
                    <input
                      type="text"
                      value={panelSearches[key]}
                      onChange={e => setPanelSearches(prev => ({ ...prev, [key]: e.target.value }))}
                      placeholder={`${config.label} 검색`}
                      className="w-full text-[12px] outline-none text-[#333] placeholder:text-[#CCCCCC]"
                    />
                  </div>

                  {/* 항목 목록 */}
                  <div className="max-h-[200px] overflow-y-auto py-1">
                    {config.options
                      .filter(opt => opt.toLowerCase().includes(panelSearches[key].toLowerCase()))
                      .map(opt => {
                        const checked = isSelected(key, opt);
                        return (
                          <button
                            key={opt}
                            onClick={() => handleSelect(key, opt)}
                            className="w-full flex items-center gap-2 px-3 py-[7px] text-[12px] text-[#555] hover:bg-[#F8F8F8] text-left"
                          >
                            {/* 체크박스 */}
                            <span className={`flex items-center justify-center w-4 h-4 rounded border flex-shrink-0 transition-colors ${checked ? config.checkBox : "border-[#DDDDDD] bg-white"}`}>
                              {checked && (
                                <svg width="9" height="7" viewBox="0 0 9 7" fill="none">
                                  <path d="M1 3.5L3.5 6L8 1" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
                                </svg>
                              )}
                            </span>
                            {opt}
                          </button>
                        );
                      })}
                  </div>

                  {/* 패널 하단: 초기화 + 선택 개수 */}
                  <div className="flex items-center justify-between border-t border-[#F0F0F0] px-3 py-2">
                    <button
                      onClick={() => handleReset(key)}
                      className="text-[12px] text-[#AAAAAA] hover:text-[#E24B4A] transition-colors"
                    >
                      초기화
                    </button>
                    <span className="text-[12px] text-[#888780]">{getSelected(key).length}개 선택</span>
                  </div>
                </div>
              )}
            </div>
          );
        })}
      </div>

      {/* ── 활성 필터 태그 ── */}
      <div className="mt-3 pt-3 border-t border-[#F0F0F0]">
        {activeTags.length === 0 ? (
          <p className="text-[11px] text-[#CCCCCC] italic">적용된 필터 없음</p>
        ) : (
          <div className="flex items-center gap-1.5 flex-wrap">
            {activeTags.map(({ key, value }) => (
              <span
                key={`${key}-${value}`}
                className="flex items-center gap-1 text-[11px] px-2 py-[3px] rounded-full bg-[#F5F5F5] border border-[#E8E8E8] text-[#555]"
              >
                {value}
                <button
                  onClick={() => handleRemoveTag(key, value)}
                  className="text-[#AAAAAA] hover:text-[#E24B4A] transition-colors leading-none"
                >
                  ✕
                </button>
              </span>
            ))}
            <button
              onClick={handleResetAll}
              className="text-[11px] text-[#E24B4A] hover:underline ml-1"
            >
              전체 초기화
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

export default SearchFilter;
