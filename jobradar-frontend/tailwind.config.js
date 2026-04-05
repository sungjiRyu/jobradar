/** @type {import('tailwindcss').Config} */
export default {
  // Tailwind가 클래스를 스캔할 파일 범위 지정
  // TypeScript 전환 후 .ts, .tsx 파일도 포함
  content: ["./index.html", "./src/**/*.{js,jsx,ts,tsx}"],
  theme: {
    extend: {},
  },
  plugins: [],
}

