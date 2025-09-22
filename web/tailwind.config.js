/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
    // 包含UI组件库的内容以确保样式正确生成
    "./node_modules/modern-ui-components/dist/**/*.{js,jsx,ts,tsx}",
    "../ui/src/**/*.{js,ts,jsx,tsx}", // 开发时的源文件
  ],
  theme: {
    extend: {
      colors: {
        border: "hsl(var(--border))",
        input: "hsl(var(--input))",
        ring: "hsl(var(--ring))",
        background: "hsl(var(--background))",
        foreground: "hsl(var(--foreground))",
        primary: {
          DEFAULT: "hsl(var(--primary))",
          foreground: "hsl(var(--primary-foreground))",
          bg: "hsl(var(--primary-bg))",
          "bg-subtle": "hsl(var(--primary-bg-subtle))",
        },
        secondary: {
          DEFAULT: "hsl(var(--secondary))",
          foreground: "hsl(var(--secondary-foreground))",
        },
        destructive: {
          DEFAULT: "hsl(var(--destructive))",
          foreground: "hsl(var(--destructive-foreground))",
        },
        muted: {
          DEFAULT: "hsl(var(--muted))",
          foreground: "hsl(var(--muted-foreground))",
        },
        accent: {
          DEFAULT: "hsl(var(--accent))",
          foreground: "hsl(var(--accent-foreground))",
          bg: "hsl(var(--accent-bg))",
          "bg-subtle": "hsl(var(--accent-bg-subtle))",
        },
        popover: {
          DEFAULT: "hsl(var(--popover))",
          foreground: "hsl(var(--popover-foreground))",
        },
        card: {
          DEFAULT: "hsl(var(--card))",
          foreground: "hsl(var(--card-foreground))",
        },
      },
      borderRadius: {
        lg: "var(--radius)",
        md: "calc(var(--radius) - 2px)",
        sm: "calc(var(--radius) - 4px)",
      },
      keyframes: {
        "accordion-down": {
          from: { height: "0" },
          to: { height: "var(--radix-accordion-content-height)" },
        },
        "accordion-up": {
          from: { height: "var(--radix-accordion-content-height)" },
          to: { height: "0" },
        },
        "water-ripple": {
          "0%": {
            transform: "scale(0)",
            opacity: "1",
          },
          "100%": {
            transform: "scale(4)",
            opacity: "0",
          },
        },
        "flip-in": {
          "0%": {
            transform: "rotateY(-90deg)",
            opacity: "0",
          },
          "100%": {
            transform: "rotateY(0deg)",
            opacity: "1",
          },
        },
        "flip-out": {
          "0%": {
            transform: "rotateY(0deg)",
            opacity: "1",
          },
          "100%": {
            transform: "rotateY(90deg)",
            opacity: "0",
          },
        },
      },
      animation: {
        "accordion-down": "accordion-down 0.2s ease-out",
        "accordion-up": "accordion-up 0.2s ease-out",
        "water-ripple": "water-ripple 0.6s ease-out",
        "flip-in": "flip-in 0.3s ease-out",
        "flip-out": "flip-out 0.3s ease-out",
      },
    },
  },
  plugins: [],
}
