import { useState } from "react";
import PolicyModal from "../common/PolicyModal";

const Footer = () => {
  const [modal, setModal] = useState<"terms" | "privacy" | null>(null);

  return (
    <>
      <footer className="w-full border-t border-[#DDDDDD] bg-white mt-auto py-5">
        <div className="max-w-[1100px] mx-auto px-6 flex flex-col gap-1.5 text-[12px] text-[#888780]">
          <div className="flex justify-end">
            <span className="ml-6 flex-shrink-0">sungjiryu220@gmail.com</span>
          </div>

          <div className="flex items-center justify-between mt-3">
            <span>© 2026 JobRadar. All rights reserved.</span>
            <div className="flex gap-3 flex-shrink-0">
              <button
                onClick={() => setModal("terms")}
                className="hover:text-[#1A1A1A] transition-colors"
              >
                이용약관
              </button>
              <span>|</span>
              <button
                onClick={() => setModal("privacy")}
                className="hover:text-[#1A1A1A] transition-colors"
              >
                개인정보처리방침
              </button>
            </div>
          </div>
        </div>
      </footer>

      {modal && <PolicyModal type={modal} onClose={() => setModal(null)} />}
    </>
  );
};

export default Footer;
