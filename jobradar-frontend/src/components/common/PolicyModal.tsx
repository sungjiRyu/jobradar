import { useEffect } from "react";

interface PolicyModalProps {
  type: "terms" | "privacy";
  onClose: () => void;
}

const CONTENT = {
  terms: {
    title: "이용약관",
    sections: [
      {
        heading: "제1조 (목적)",
        body: "본 약관은 JobRadar(이하 '서비스')가 제공하는 채용공고 수집 서비스의 이용 조건 및 절차에 관한 사항을 규정함을 목적으로 합니다.",
      },
      {
        heading: "제2조 (서비스 제공)",
        body: "서비스는 외부 채용 플랫폼에서 수집된 공고 정보를 제공합니다. 수집된 공고는 원본과 다를 수 있으며, 채용 여부 및 공고 내용의 정확성은 보장하지 않습니다.",
      },
      {
        heading: "제3조 (이용자 의무)",
        body: "이용자는 서비스를 이용함에 있어 타인의 권리를 침해하거나 법령에 위반되는 행위를 하여서는 안 됩니다. 타인의 계정을 도용하거나 서비스의 정상적인 운영을 방해하는 행위는 금지됩니다.",
      },
      {
        heading: "제4조 (서비스 변경 및 중단)",
        body: "본 서비스는 포트폴리오 목적으로 운영되는 개인 프로젝트로, 운영자의 사정에 따라 서비스가 변경되거나 예고 없이 종료될 수 있습니다.",
      },
      {
        heading: "제5조 (면책)",
        body: "서비스는 이용자가 서비스를 통해 얻은 정보로 인해 발생한 손해에 대해 책임을 지지 않습니다. 채용 결과 및 취업 여부에 대해서도 책임을 부담하지 않습니다.",
      },
    ],
  },
  privacy: {
    title: "개인정보처리방침",
    sections: [
      {
        heading: "1. 수집하는 개인정보 항목",
        body: "회원가입 시 이메일 주소, 비밀번호(암호화 저장), 닉네임을 수집합니다. 서비스 이용 과정에서 공고 조회 기록(조회수)이 자동으로 기록됩니다.",
      },
      {
        heading: "2. 개인정보 수집 및 이용 목적",
        body: "수집된 개인정보는 회원 식별 및 로그인, 스크랩 기능 제공, 서비스 운영 및 개선을 위해 사용됩니다. 수집된 정보는 해당 목적 외에 사용되지 않습니다.",
      },
      {
        heading: "3. 개인정보 보유 및 이용 기간",
        body: "회원 탈퇴 시 수집된 개인정보는 즉시 삭제됩니다. 단, 관련 법령에 의해 보존이 필요한 경우 해당 기간 동안 보관됩니다.",
      },
      {
        heading: "4. 개인정보 제3자 제공",
        body: "서비스는 이용자의 개인정보를 외부에 제공하지 않습니다. 단, 이용자가 사전에 동의한 경우 또는 법령에 의한 경우는 예외로 합니다.",
      },
      {
        heading: "5. 개인정보 보호 조치",
        body: "비밀번호는 암호화하여 저장하며, 모든 개인정보는 회원탈퇴시 삭제됩니다."
      },
      {
        heading: "6. 문의",
        body: "개인정보 처리에 관한 문의는 sungjiryu220@gmail.com으로 연락해 주세요.",
      },
    ],
  },
};

const PolicyModal = ({ type, onClose }: PolicyModalProps) => {
  const { title, sections } = CONTENT[type];

  // ESC 키로 닫기
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    document.addEventListener("keydown", handleKeyDown);
    // 모달 열린 동안 배경 스크롤 방지
    document.body.style.overflow = "hidden";
    return () => {
      document.removeEventListener("keydown", handleKeyDown);
      document.body.style.overflow = "";
    };
  }, [onClose]);

  return (
    // 오버레이 — 클릭 시 닫기
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40"
      onClick={onClose}
    >
      {/* 모달 본문 — 클릭 이벤트 버블링 방지 */}
      <div
        className="bg-white rounded-lg border border-[#DDDDDD] w-full max-w-[560px] mx-4 max-h-[80vh] flex flex-col"
        onClick={(e) => e.stopPropagation()}
      >
        {/* 헤더 */}
        <div className="flex justify-between items-center px-6 py-4 border-b border-[#DDDDDD]">
          <h2 className="text-[15px] font-semibold text-[#1A1A1A]">{title}</h2>
          <button
            onClick={onClose}
            className="text-[#888780] hover:text-[#1A1A1A] text-xl leading-none transition-colors"
            aria-label="닫기"
          >
            ✕
          </button>
        </div>

        {/* 내용 — 스크롤 가능 */}
        <div className="overflow-y-auto px-6 py-5 flex flex-col gap-4">
          {sections.map((section) => (
            <div key={section.heading}>
              <p className="text-[13px] font-semibold text-[#1A1A1A] mb-1">{section.heading}</p>
              <p className="text-[12px] text-[#888780] leading-relaxed">{section.body}</p>
            </div>
          ))}
        </div>

        {/* 하단 닫기 버튼 */}
        <div className="px-6 py-4 border-t border-[#DDDDDD] flex justify-end">
          <button
            onClick={onClose}
            className="text-sm bg-[#378ADD] text-white px-5 py-1.5 rounded-md hover:opacity-90 transition-opacity"
          >
            확인
          </button>
        </div>
      </div>
    </div>
  );
};

export default PolicyModal;
