const getKstDateParts = () => {
  const formatter = new Intl.DateTimeFormat("en-CA", {
    timeZone: "Asia/Seoul",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  });
  const parts = formatter.formatToParts(new Date());
  const year = Number(parts.find((part) => part.type === "year")?.value);
  const month = Number(parts.find((part) => part.type === "month")?.value);
  const day = Number(parts.find((part) => part.type === "day")?.value);
  return { year, month, day };
};

const parseDateParts = (date: string) => {
  const [year, month, day] = date.split("-").map(Number);
  return { year, month, day };
};

const toUtcDay = ({
  year,
  month,
  day,
}: {
  year: number;
  month: number;
  day: number;
}) => Date.UTC(year, month - 1, day);

// 마감일까지 남은 일수 반환 (null이면 상시채용)
export const calcDday = (deadline: string | null): number | null => {
  if (!deadline) return null;
  const today = toUtcDay(getKstDateParts());
  const end = toUtcDay(parseDateParts(deadline));
  return Math.ceil((end - today) / (1000 * 60 * 60 * 24));
};
