/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        brand: {
          50:  '#eef5ff',
          100: '#d9e8ff',
          500: '#3b6ef0',
          600: '#2c55c8',
          700: '#22429a',
          900: '#162a63',
        },
      },
    },
  },
  plugins: [],
}
