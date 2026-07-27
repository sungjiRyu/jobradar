const MEASUREMENT_ID = import.meta.env.VITE_GA4_MEASUREMENT_ID?.trim();
const MEASUREMENT_ID_PATTERN = /^G-[A-Z0-9]+$/;

type Gtag = (...args: unknown[]) => void;

declare global {
  interface Window {
    dataLayer: unknown[][];
    gtag?: Gtag;
  }
}

const PAGE_TITLES: Record<string, string> = {
  "/": "채용공고",
  "/jobs/detail": "공고 상세",
  "/dashboard": "채용 트렌드",
  "/login": "로그인",
  "/signup": "회원가입",
  "/account": "계정",
  "/not-found": "페이지를 찾을 수 없음",
};

let initialized = false;
let lastRoutePath: string | null = null;
let previousPagePath: string | null = null;

export const normalizeAnalyticsPath = (pathname: string) => {
  const normalizedPath = pathname.replace(/\/+$/, "") || "/";

  if (/^\/jobs\/[^/]+$/.test(normalizedPath)) return "/jobs/detail";
  if (/^\/my(?:\/|$)/.test(normalizedPath)) return "/account";
  if (normalizedPath in PAGE_TITLES) return normalizedPath;

  return "/not-found";
};

const getInitialReferrer = () => {
  if (!document.referrer) return null;

  try {
    const referrer = new URL(document.referrer);
    if (!["http:", "https:"].includes(referrer.protocol)) return null;

    if (referrer.origin === window.location.origin) {
      return `${referrer.origin}${normalizeAnalyticsPath(referrer.pathname)}`;
    }

    return referrer.origin;
  } catch {
    return null;
  }
};

export const initializeAnalytics = () => {
  if (initialized) return true;
  if (!MEASUREMENT_ID || !MEASUREMENT_ID_PATTERN.test(MEASUREMENT_ID)) {
    return false;
  }

  window.dataLayer = window.dataLayer || [];
  window.gtag = (...args: unknown[]) => {
    window.dataLayer.push(args);
  };

  window.gtag("consent", "default", {
    analytics_storage: "denied",
    ad_storage: "denied",
    ad_user_data: "denied",
    ad_personalization: "denied",
  });
  window.gtag("set", "ads_data_redaction", true);
  window.gtag("js", new Date());
  window.gtag("config", MEASUREMENT_ID, {
    send_page_view: false,
    allow_google_signals: false,
    allow_ad_personalization_signals: false,
    allow_interest_groups: false,
  });

  const script = document.createElement("script");
  script.async = true;
  script.src = `https://www.googletagmanager.com/gtag/js?id=${MEASUREMENT_ID}`;
  document.head.appendChild(script);

  initialized = true;
  return true;
};

export const trackPageView = (pathname: string) => {
  if (!window.gtag || pathname === lastRoutePath) return;

  const pagePath = normalizeAnalyticsPath(pathname);
  const pageReferrer = previousPagePath
    ? `${window.location.origin}${previousPagePath}`
    : getInitialReferrer();
  const parameters: Record<string, string> = {
    page_location: `${window.location.origin}${pagePath}`,
    page_title: PAGE_TITLES[pagePath],
  };

  if (pageReferrer) {
    parameters.page_referrer = pageReferrer;
  }

  window.gtag("event", "page_view", parameters);
  lastRoutePath = pathname;
  previousPagePath = pagePath;
};
