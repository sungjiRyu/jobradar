/**
 * PrivateRoute — 인증된 사용자만 접근 가능한 라우트 보호 컴포넌트
 * localStorage의 accessToken 유무로 로그인 여부를 판단한다.
 * 토큰이 없으면 /login으로 리다이렉트한다.
 */

import { Navigate } from "react-router-dom";

interface PrivateRouteProps {
  children: React.ReactNode;
}

const PrivateRoute = ({ children }: PrivateRouteProps) => {
  // localStorage에서 accessToken을 읽어 로그인 여부 확인
  // 토큰이 있으면 children(요청한 페이지)을 렌더링
  // 없으면 /login으로 리다이렉트 (replace: 히스토리에 남기지 않음)
  const accessToken = localStorage.getItem("accessToken");

  if (!accessToken) {
    return <Navigate to="/login" replace />;
  }

  return <>{children}</>;
};

export default PrivateRoute;
