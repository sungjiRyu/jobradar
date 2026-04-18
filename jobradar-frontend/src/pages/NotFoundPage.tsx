import { useNavigate } from "react-router-dom";

const NotFoundPage = () => {
  const navigate = useNavigate();

  return (
    <div className="min-h-[calc(100vh-56px)] flex flex-col items-center justify-center text-center px-6">
      <p className="text-[64px] font-bold text-[#378ADD] leading-none mb-4">404</p>
      <h1 className="text-[20px] font-semibold text-[#1A1A1A] mb-2">
        페이지를 찾을 수 없습니다
      </h1>
      <p className="text-[14px] text-[#888780] mb-8">
        요청하신 페이지가 존재하지 않거나 이동되었습니다.
      </p>
      <button
        onClick={() => navigate("/")}
        className="px-6 py-2.5 bg-[#378ADD] text-white text-[14px] rounded-lg hover:opacity-90 transition-opacity"
      >
        공고 목록으로 돌아가기
      </button>
    </div>
  );
};

export default NotFoundPage;
