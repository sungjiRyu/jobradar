import { Navigate, useLocation } from "react-router-dom";

interface PrivateRouteProps {
  children: React.ReactNode;
}

const PrivateRoute = ({ children }: PrivateRouteProps) => {
  const accessToken = localStorage.getItem("accessToken");
  const location = useLocation();

  if (!accessToken) {
    // state.from에 현재 경로를 저장해서 로그인 후 복귀할 수 있도록 함
    return <Navigate to="/login" state={{ from: location.pathname }} replace />;
  }

  return <>{children}</>;
};

export default PrivateRoute;
