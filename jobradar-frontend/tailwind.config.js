/** @type {import('tailwindcss').Config} */
export default {
  // Tailwind가 클래스를 스캔할 파일 범위 지정
  // src 폴더 안의 모든 .js, .jsx 파일과 index.html을 대상으로 함
  content: ["./index.html", "./src/**/*.{js,jsx}"],
  theme: {
    extend: {},
  },
  plugins: [],
}

