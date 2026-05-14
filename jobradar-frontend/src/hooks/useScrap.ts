import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { addScrap, deleteScrap } from "../api/scrapApi";

// JobCard와 JobDetailPage에서 공통으로 사용하는 스크랩 토글 로직
export const useScrap = (jobId: number | null, initialScrapId: number | null = null) => {
  const navigate = useNavigate();
  const [scrapId, setScrapId] = useState<number | null>(initialScrapId);
  const [scrapLoading, setScrapLoading] = useState(false);

  // e는 선택 인자 — JobCard처럼 클릭 이벤트 버블링 방지가 필요한 경우에만 전달
  const handleScrap = async (e?: React.MouseEvent) => {
    e?.stopPropagation();
    const token = localStorage.getItem("accessToken");
    if (!token) { navigate("/login"); return; }
    if (scrapLoading || jobId === null) return;

    setScrapLoading(true);
    try {
      if (scrapId !== null) {
        await deleteScrap(scrapId);
        setScrapId(null);
      } else {
        const res = await addScrap(jobId);
        setScrapId(res.data.data.scrapId);
      }
    } catch {
      alert("스크랩 처리에 실패했습니다.");
    } finally {
      setScrapLoading(false);
    }
  };

  return { scrapId, setScrapId, scrapLoading, handleScrap };
};
