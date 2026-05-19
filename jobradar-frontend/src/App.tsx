import { BrowserRouter } from "react-router-dom";
import { Toaster } from "react-hot-toast";
import Navbar from "./components/layout/Navbar";
import Footer from "./components/layout/Footer";
import AppRouter from "./router";

const App = () => {
  return (
    <BrowserRouter>
      <div className="min-h-screen bg-[#F5F5F5] flex flex-col">
        <Navbar />
        <main className="flex-1">
          <AppRouter />
        </main>
        <Footer />
      </div>
      {/* 전역 토스트 — 프로젝트 톤앤매너(흰 카드 + 옅은 보더) */}
      <Toaster
        position="top-center"
        toastOptions={{
          duration: 2500,
          style: {
            background: "#FFFFFF",
            color: "#1A1A1A",
            border: "1px solid #DDDDDD",
            borderRadius: "10px",
            padding: "12px 16px",
            fontSize: "13px",
            boxShadow: "0 4px 12px rgba(0, 0, 0, 0.08)",
            maxWidth: "360px",
          },
          success: {
            iconTheme: { primary: "#1D9E75", secondary: "#FFFFFF" },
          },
          error: {
            iconTheme: { primary: "#E24B4A", secondary: "#FFFFFF" },
          },
        }}
      />
    </BrowserRouter>
  );
};

export default App;
