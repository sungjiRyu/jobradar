import { BrowserRouter } from "react-router-dom";
import Navbar from "./components/layout/Navbar";
import AppRouter from "./router";

const App = () => {
  return (
    <BrowserRouter>
      <div className="min-h-screen bg-[#F5F5F5]">
        <Navbar />
        <AppRouter />
      </div>
    </BrowserRouter>
  );
};

export default App;
