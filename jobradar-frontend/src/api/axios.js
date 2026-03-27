import axios from "axios";

const api = axios.create({
  baseURL: "https://localhost:8080",
  withCredentials: true,
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("accessToken");
  if (token) {
    config.headers.Authorization = `Berer ${token}`;
  }
  return config;
});

export default api;
