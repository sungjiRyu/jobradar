const Footer = () => {
  return (
    <footer className="w-full border-t border-[#DDDDDD] bg-white mt-auto py-5">
      <div className="max-w-[1100px] mx-auto px-6 flex flex-col gap-1.5 text-[12px] text-[#888780]">
        <div className="flex justify-between items-center">
          <div className="flex flex-col gap-0.5">
            <span>본 서비스는 포트폴리오 목적으로 제작된 개인 프로젝트입니다.</span>
            <span>수집된 채용공고는 실제 공고와 다를 수 있으며, 서비스는 예고 없이 종료될 수 있습니다.</span>
          </div>
          <span className="ml-6 flex-shrink-0">sungjiryu220@gmail.com</span>
        </div>
        <span className="mt-4">© 2026 JobRadar. 개발자 취업준비생을 위한 채용공고 수집 서비스</span>
      </div>
    </footer>
  );
};

export default Footer;
