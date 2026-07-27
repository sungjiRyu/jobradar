import { useEffect } from "react";
import { useLocation } from "react-router-dom";
import {
  initializeAnalytics,
  trackPageView,
} from "../../utils/analytics";

const AnalyticsPageTracker = () => {
  const { pathname } = useLocation();

  useEffect(() => {
    if (initializeAnalytics()) {
      trackPageView(pathname);
    }
  }, [pathname]);

  return null;
};

export default AnalyticsPageTracker;
