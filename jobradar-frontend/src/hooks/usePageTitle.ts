import { useEffect } from "react";

/**
 * 페이지별 document.title을 설정하는 훅
 * 빈 문자열을 넘기면 기본값("JobRadar")으로 설정
 */
export const usePageTitle = (title: string) => {
  useEffect(() => {
    document.title = title ? `${title} | JobRadar` : "JobRadar";
  }, [title]);
};
