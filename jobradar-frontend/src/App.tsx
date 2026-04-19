import { BrowserRouter } from "react-router-dom";
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
    </BrowserRouter>
  );
};

export default App;
