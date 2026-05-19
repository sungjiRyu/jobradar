/**
 * DashboardPage — 채용 공고 통계 대시보드
 *
 * 역할:
 * - 상단 요약 카드 4개: 전체 / 신규 / 마감 임박 / 신입 공고 수
 * - 기술스택 순위 막대 차트 (상위 8개)
 * - 지역별 채용 비중 가로 막대 차트
 * - 경력별 공고 비중 도넛 차트
 *
 * 모든 데이터는 백엔드 /api/stats/* 에서 받아온다.
 * 로딩 중에는 스피너, 에러 시에는 메시지를 표시한다.
 */

import { useEffect, useState } from "react";
import {
  Chart as ChartJS,
  CategoryScale, // x축 카테고리 스케일 (막대 차트)
  LinearScale, // y축 숫자 스케일
  BarElement, // 막대 요소
  ArcElement, // 도넛/파이 요소
  Title, // 차트 제목 플러그인
  Tooltip, // 마우스 오버 툴팁 플러그인
  Legend, // 범례 플러그인
} from "chart.js";
import type { TooltipItem } from "chart.js"; // 툴팁 콜백 파라미터 타입 (타입 전용 import)
import { Bar, Doughnut } from "react-chartjs-2";

import {
  getStatsToday,
  getStatsTechStacks,
  getStatsLocations,
  getStatsExperience,
  TodayStats,
  TechStackStat,
  LocationStat,
  ExperienceStat,
} from "../api/statsApi";
import {
  mockTodayStats,
  mockTechStackStats,
  mockLocationStats,
  mockExperienceStats,
} from "../mocks/stats";
import { usePageTitle } from "../hooks/usePageTitle";

// ─── 테스트용 플래그 ──────────────────────────────
// true  → mock 데이터로 렌더링 (백엔드 없이 UI 확인)
// false → 실제 API 호출
// 테스트 완료 후 false 로 바꾸면 된다.
const USE_MOCK = false;

// Chart.js 사용 전 반드시 필요한 컴포넌트를 등록해야 한다.
// 등록하지 않으면 "Category scale is not registered" 같은 에러가 발생한다.
ChartJS.register(
  CategoryScale,
  LinearScale,
  BarElement,
  ArcElement,
  Title,
  Tooltip,
  Legend,
);

// ─────────────────────────────────────────────
// 지역 차트(상위 10개) 그라데이션 — 1위가 가장 진함
// ─────────────────────────────────────────────
const BLUE_GRADIENT = [
  "#185FA5",
  "#2272BC",
  "#2E82CC",
  "#378ADD",
  "#5099E1",
  "#6BAAE6",
  "#86BCEB",
  "#A2CDF0",
  "#BDDDF5",
  "#D5E9F9",
];

// ─────────────────────────────────────────────
// 기술스택별 고정 색상 매핑 — 자주 등장하는 스택은 일관된 색을 가짐
// 매핑에 없는 스택은 TECH_STACK_FALLBACK 팔레트에서 index 순환으로 배정
// ─────────────────────────────────────────────
const TECH_STACK_COLOR_MAP: Record<string, string> = {
  java: "#378ADD",
  spring: "#2D6FBF",
  python: "#1D9E75",
  javascript: "#F5A623",
  typescript: "#185FA5",
  react: "#7F77DD",
  vue: "#42B883",
  node: "#1D7A3A",
  "node.js": "#1D7A3A",
  aws: "#D85A30",
  docker: "#0DB7ED",
  kubernetes: "#5A9FE3",
  mysql: "#B8820A",
  kotlin: "#A32D2D",
  go: "#00ADD8",
};

const TECH_STACK_FALLBACK = [
  "#378ADD",
  "#1D9E75",
  "#F5A623",
  "#7F77DD",
  "#D85A30",
  "#2D6FBF",
  "#B8820A",
  "#A32D2D",
];

const getTechStackColor = (name: string, index: number): string => {
  return (
    TECH_STACK_COLOR_MAP[name.toLowerCase()] ??
    TECH_STACK_FALLBACK[index % TECH_STACK_FALLBACK.length]
  );
};

// ─────────────────────────────────────────────
// 상단 요약 카드 컴포넌트
// ─────────────────────────────────────────────
interface SummaryCardProps {
  label: string;
  value: number | null; // null이면 로딩 중
  color: string; // 텍스트/아이콘 강조색
  bgColor: string; // 카드 왼쪽 줄 색상
}

const SummaryCard = ({ label, value, color, bgColor }: SummaryCardProps) => (
  // 카드: 흰색 배경, 테두리 0.5px #DDDDDD, 둥근 모서리 12px
  <article className="bg-white border border-[#DDDDDD] rounded-xl p-5 flex items-center gap-4">
    {/* 왼쪽 색상 막대 */}
    <div
      className="w-1.5 self-stretch rounded-full"
      style={{ backgroundColor: bgColor }}
    />
    <div>
      <p className="text-sm text-gray-500">{label}</p>
      {/* value가 null이면 로딩 중이므로 skeleton 표시 */}
      {value === null ? (
        <div className="mt-1 h-8 w-16 bg-gray-200 rounded animate-pulse" />
      ) : (
        <p className="text-3xl font-bold mt-1" style={{ color }}>
          {value.toLocaleString()}
          <span className="text-base font-normal text-gray-500 ml-1">개</span>
        </p>
      )}
    </div>
  </article>
);

// ─────────────────────────────────────────────
// DashboardPage 메인 컴포넌트
// ─────────────────────────────────────────────
const DashboardPage = () => {
  usePageTitle("채용공고 대시보드");
  // useState — 각 API 응답 데이터를 상태로 관리
  // null: 아직 데이터를 받지 못한 초기 상태 (로딩 스피너 판단에 사용)
  const [todayStats, setTodayStats] = useState<TodayStats | null>(null);
  const [techStacks, setTechStacks] = useState<TechStackStat[]>([]);
  const [locations, setLocations] = useState<LocationStat[]>([]);
  const [experiences, setExperiences] = useState<ExperienceStat[]>([]);

  // useState — 로딩 상태: 모든 API 요청이 끝나면 false로 바뀐다
  const [loading, setLoading] = useState(true);

  // useState — 에러 메시지: API 요청 실패 시 저장
  const [error, setError] = useState<string | null>(null);

  // useEffect — 컴포넌트가 처음 렌더링될 때 단 한 번 모든 통계 API를 병렬 호출
  // 의존성 배열이 []이므로 마운트 시 한 번만 실행된다.
  // Promise.all을 사용해 4개의 API를 동시에 호출하여 대기 시간을 최소화한다.
  useEffect(() => {
    const fetchAll = async () => {
      try {
        setLoading(true);
        setError(null);

        if (USE_MOCK) {
          // ── mock 모드: 백엔드 없이 즉시 데이터 주입 ──
          // 실제 API와 동일한 구조이므로 UI 검증에 사용
          setTodayStats(mockTodayStats);
          setTechStacks(mockTechStackStats);
          setLocations(mockLocationStats);
          setExperiences(mockExperienceStats);
        } else {
          // ── 실제 API 모드 ──
          // 4개 API를 병렬 호출 — 순차 호출 대비 응답 시간 단축
          const [todayRes, techRes, locRes, expRes] = await Promise.all([
            getStatsToday(),
            getStatsTechStacks(),
            getStatsLocations(),
            getStatsExperience(),
          ]);

          // 백엔드 ApiResponse 구조: { success, message, data }
          setTodayStats(todayRes.data.data);
          setTechStacks(techRes.data.data);
          setLocations(locRes.data.data);
          setExperiences(expRes.data.data);
        }
      } catch (err) {
        console.error("통계 데이터 로딩 실패:", err);
        setError(
          "데이터를 불러오는 데 실패했습니다. 잠시 후 다시 시도해주세요.",
        );
      } finally {
        // 성공/실패 여부와 관계없이 로딩 종료
        setLoading(false);
      }
    };

    fetchAll();
  }, []);

  // ─── 기술스택 막대 차트 데이터 ───────────────
  // techStacks 배열을 count 내림차순 정렬 후 상위 8개만 사용
  // 파생 데이터이므로 별도 state 없이 기존 state에서 계산한다.
  const top8Stacks = [...techStacks]
    .sort((a, b) => b.count - a.count)
    .slice(0, 8);

  const techChartData = {
    labels: top8Stacks.map((s) => s.name),
    datasets: [
      {
        label: "공고 수",
        data: top8Stacks.map((s) => s.count),
        // 스택 이름별 고정 색 (매핑에 없으면 폴백 팔레트 순환)
        backgroundColor: top8Stacks.map((s, i) => getTechStackColor(s.name, i)),
        // 막대 모서리 둥글게
        borderRadius: 6,
        borderSkipped: false, // 모든 모서리에 borderRadius 적용
      },
    ],
  };

  const techChartOptions = {
    responsive: true, // 컨테이너 크기에 맞춰 자동 리사이즈
    maintainAspectRatio: false, // 높이를 CSS로 직접 제어하기 위해 false
    plugins: {
      legend: { display: false }, // 데이터셋이 1개이므로 범례 불필요
      tooltip: {
        // 툴팁에 "개" 단위 추가
        callbacks: {
          label: (ctx: TooltipItem<"bar">) => ` ${ctx.parsed.y ?? 0}개`,
        },
      },
    },
    scales: {
      x: {
        grid: { display: false }, // x축 격자선 숨김 (가독성 향상)
        ticks: { color: "#555" },
      },
      y: {
        beginAtZero: true, // y축을 0부터 시작 (데이터 왜곡 방지)
        grid: { color: "#F0F0F0" },
        ticks: {
          color: "#555",
          // y축 숫자에 "개" 단위 추가
          callback: (value: number | string) => `${value}개`,
        },
      },
    },
  };

  // ─── 지역별 가로 막대 차트 ────────────────────
  // 전체 공고 수 합산 → 각 지역 비중(%) 계산에 사용 (상위 10개의 비중도 전체 대비로 표시)
  const totalLocationCount = locations.reduce((sum, l) => sum + l.count, 0);

  // 상위 10개만 추출 (count 내림차순)
  const top10Locations = [...locations]
    .sort((a, b) => b.count - a.count)
    .slice(0, 10);

  const locationChartData = {
    labels: top10Locations.map((l) => l.location),
    datasets: [
      {
        label: "공고 수",
        data: top10Locations.map((l) => l.count),
        backgroundColor: top10Locations.map((_, i) => BLUE_GRADIENT[i]),
        borderRadius: 4,
        borderSkipped: false,
      },
    ],
  };

  const locationChartOptions = {
    // indexAxis: "y" → 가로 막대 차트로 전환
    // 지역명이 길어서 세로로 나열하면 읽기 어렵기 때문
    indexAxis: "y" as const,
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false },
      tooltip: {
        callbacks: {
          label: (ctx: TooltipItem<"bar">) => {
            const x = ctx.parsed.x ?? 0;
            const pct =
              totalLocationCount > 0
                ? ((x / totalLocationCount) * 100).toFixed(1)
                : "0.0";
            return ` ${x}개 (${pct}%)`;
          },
        },
      },
    },
    scales: {
      x: {
        beginAtZero: true,
        grid: { color: "#F0F0F0" },
        ticks: { color: "#555", callback: (v: number | string) => `${v}개` },
      },
      y: {
        grid: { display: false },
        ticks: { color: "#555" },
      },
    },
  };

  // ─── 경력별 도넛 차트 ─────────────────────────
  // 백엔드 경력 값이 너무 세분화되어 있어 3개 그룹으로 합산
  const groupExperience = (exp: string): string => {
    if (exp.includes("신입")) return "신입";
    const match = exp.match(/경력(\d+)/);
    if (match) {
      return parseInt(match[1]) <= 4 ? "경력 1~3년" : "경력 5년 이상";
    }
    return "경력 1~3년"; // "경력무관", "경력" 등 → 경력 1~3년으로 분류
  };

  const GROUP_LABELS = ["신입", "경력 1~3년", "경력 5년 이상"];
  const grouped = experiences.reduce<Record<string, number>>((acc, item) => {
    const group = groupExperience(item.experience);
    acc[group] = (acc[group] ?? 0) + item.count;
    return acc;
  }, {});
  const compressedExp = GROUP_LABELS.map((label) => ({
    experience: label,
    count: grouped[label] ?? 0,
  }));

  const expColors = ["#378ADD", "#1D9E75", "#F5A623"];

  const expChartData = {
    labels: compressedExp.map((e) => e.experience),
    datasets: [
      {
        data: compressedExp.map((e) => e.count),
        backgroundColor: expColors,
        // 도넛 중앙 구멍의 공백 비율 (0~1)
        hoverOffset: 8,
        borderWidth: 2,
        borderColor: "#fff",
      },
    ],
  };

  const expChartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: "right" as const, // 오른쪽에 범례 배치 — 도넛과 나란히 읽기 편함
        labels: { color: "#333", padding: 16 },
      },
      tooltip: {
        callbacks: {
          label: (ctx: TooltipItem<"doughnut">) => {
            const data = ctx.dataset.data as number[];
            const total = data.reduce((a, b) => a + b, 0);
            const value = ctx.parsed;
            const pct = total > 0 ? ((value / total) * 100).toFixed(1) : "0.0";
            return ` ${ctx.label}: ${value}개 (${pct}%)`;
          },
        },
      },
    },
  };

  // ─── 로딩 화면 ───────────────────────────────
  if (loading) {
    return (
      <main className="min-h-screen bg-[#F5F5F5] flex items-center justify-center">
        {/* 스피너: animate-spin으로 회전 애니메이션 */}
        <div className="flex flex-col items-center gap-3">
          <div className="w-10 h-10 border-4 border-[#378ADD] border-t-transparent rounded-full animate-spin" />
          <p className="text-gray-500 text-sm">통계 데이터를 불러오는 중...</p>
        </div>
      </main>
    );
  }

  // ─── 에러 화면 ───────────────────────────────
  if (error) {
    return (
      <main className="min-h-screen bg-[#F5F5F5] flex items-center justify-center">
        <div className="bg-white border border-[#DDDDDD] rounded-xl p-8 text-center max-w-sm">
          <p className="text-[#A32D2D] font-semibold">{error}</p>
          <button
            onClick={() => window.location.reload()}
            className="mt-4 px-5 py-2 bg-[#378ADD] text-white rounded-lg text-sm hover:bg-[#2e6fb5] transition-colors"
          >
            다시 시도
          </button>
        </div>
      </main>
    );
  }

  // ─── 메인 렌더링 ─────────────────────────────
  return (
    <main className="min-h-screen bg-[#F5F5F5] py-8 px-6">
      <div className="max-w-6xl mx-auto">
        {/* 페이지 헤더 */}
        <header className="mb-6">
          <h1 className="text-2xl font-bold text-gray-800">
            채용공고 대시보드
          </h1>
          <p className="text-sm text-gray-500 mt-1">
            현재 등록된 채용공고의 통계를 한눈에 확인하세요.
          </p>
        </header>

        {/* ── 상단 요약 카드 4개: 4열 그리드 ──────── */}
        <section
          aria-label="요약 통계"
          className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6"
        >
          <SummaryCard
            label="전체 공고"
            value={todayStats?.totalCount ?? null}
            color="#1A1A1A"
            bgColor="#1A1A1A"
          />
          <SummaryCard
            label="오늘 신규 공고"
            value={todayStats?.todayCount ?? null}
            color="#378ADD"
            bgColor="#378ADD"
          />
          <SummaryCard
            label="마감 임박 (D-7)"
            value={todayStats?.urgentCount ?? null}
            color="#E24B4A"
            bgColor="#E24B4A"
          />
          <SummaryCard
            label="신입 공고"
            value={todayStats?.juniorCount ?? null}
            color="#1D9E75"
            bgColor="#1D9E75"
          />
        </section>

        {/* ── 기술스택 + 지역별: 2열 그리드 ──────── */}
        <section
          aria-label="기술스택 및 지역별 차트"
          className="grid grid-cols-1 lg:grid-cols-2 gap-4 mb-4"
        >
          {/* 기술스택 순위 막대 차트 */}
          <article className="bg-white border border-[#DDDDDD] rounded-xl p-5">
            <h2 className="text-base font-semibold text-gray-700 mb-4">
              기술스택 순위{" "}
              <span className="text-xs font-normal text-gray-400">
                (상위 8개)
              </span>
            </h2>
            {top8Stacks.length === 0 ? (
              <p className="text-sm text-gray-400 text-center py-10">
                데이터 없음
              </p>
            ) : (
              // 높이를 300px로 고정 — maintainAspectRatio: false이므로 CSS 높이가 실제 크기
              <div className="h-[300px]">
                <Bar data={techChartData} options={techChartOptions} />
              </div>
            )}
          </article>

          {/* 지역별 채용 비중 가로 막대 차트 */}
          <article className="bg-white border border-[#DDDDDD] rounded-xl p-5">
            <h2 className="text-base font-semibold text-gray-700 mb-4">
              지역별 채용 비중{" "}
              <span className="text-xs font-normal text-gray-400">
                (상위 10개)
              </span>
            </h2>
            {top10Locations.length === 0 ? (
              <p className="text-sm text-gray-400 text-center py-10">
                데이터 없음
              </p>
            ) : (
              <div className="h-[300px]">
                <Bar data={locationChartData} options={locationChartOptions} />
              </div>
            )}
          </article>
        </section>

        {/* ── 경력별 공고 비중: 전체 너비 ──────────── */}
        <section aria-label="경력별 공고 비중" className="mb-4">
          <article className="bg-white border border-[#DDDDDD] rounded-xl p-5">
            <h2 className="text-base font-semibold text-gray-700 mb-4">
              경력별 공고 비중
            </h2>
            {compressedExp.every((e) => e.count === 0) ? (
              <p className="text-sm text-gray-400 text-center py-10">
                데이터 없음
              </p>
            ) : (
              <div className="flex flex-col md:flex-row items-center gap-6">
                <div className="h-[260px] w-full md:w-[320px]">
                  <Doughnut data={expChartData} options={expChartOptions} />
                </div>
                <ul className="flex-1 space-y-3 w-full">
                  {compressedExp.map((e, i) => {
                    const total = compressedExp.reduce(
                      (sum, x) => sum + x.count,
                      0,
                    );
                    const pct =
                      total > 0 ? ((e.count / total) * 100).toFixed(1) : "0.0";
                    return (
                      <li
                        key={e.experience}
                        className="flex items-center gap-3"
                      >
                        <span
                          className="w-3 h-3 rounded-full flex-shrink-0"
                          style={{ backgroundColor: expColors[i] }}
                        />
                        <span className="flex-1 text-sm text-gray-600">
                          {e.experience}
                        </span>
                        <div className="w-32 bg-gray-100 rounded-full h-2 overflow-hidden">
                          <div
                            className="h-2 rounded-full"
                            style={{
                              width: `${pct}%`,
                              backgroundColor: expColors[i],
                            }}
                          />
                        </div>
                        <span className="text-sm font-medium text-gray-700 w-20 text-right">
                          {e.count.toLocaleString()}개 ({pct}%)
                        </span>
                      </li>
                    );
                  })}
                </ul>
              </div>
            )}
          </article>
        </section>
      </div>
    </main>
  );
};

export default DashboardPage;
