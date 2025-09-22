# Modern UI Components - 安装和使用指南

## 📦 安装

### 方式一：本地文件安装（推荐用于开发）

如果你的项目和UI组件库在同一个工作空间中：

```bash
npm install file:../ui
# 或者
yarn add file:../ui
# 或者
pnpm add file:../ui
```

### 方式二：发布到npm后安装

```bash
npm install modern-ui-components
# 或者
yarn add modern-ui-components
# 或者
pnpm add modern-ui-components
```

## 🚀 快速开始

### 1. 安装依赖

确保你的项目已安装以下peer dependencies：

```bash
npm install react react-dom @radix-ui/react-dialog @radix-ui/react-slot class-variance-authority clsx lucide-react tailwind-merge
```

### 2. 配置Tailwind CSS

在你的项目中安装和配置Tailwind CSS：

```bash
npm install -D tailwindcss postcss autoprefixer tailwindcss-animate
npx tailwindcss init -p
```

更新你的 `tailwind.config.js`：

```javascript
/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
    // 重要：包含UI组件库的内容
    "./node_modules/modern-ui-components/dist/**/*.{js,jsx,ts,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // 使用CSS变量以支持主题切换
        border: "hsl(var(--border))",
        input: "hsl(var(--input))",
        ring: "hsl(var(--ring))",
        background: "hsl(var(--background))",
        foreground: "hsl(var(--foreground))",
        primary: {
          DEFAULT: "hsl(var(--primary))",
          foreground: "hsl(var(--primary-foreground))",
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
      animation: {
        "accordion-down": "accordion-down 0.2s ease-out",
        "accordion-up": "accordion-up 0.2s ease-out",
      },
    },
  },
  plugins: [require("tailwindcss-animate")],
}
```

### 3. 导入样式

在你的项目入口文件（如 `main.tsx` 或 `index.tsx`）中导入样式：

```typescript
// 导入UI组件库的样式
import 'modern-ui-components/styles'

// 你的其他导入
import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.tsx'
```

### 4. 使用组件

现在你可以在项目中使用组件了：

```typescript
import React from 'react'
import {
  Button,
  Card,
  CardHeader,
  CardTitle,
  CardContent,
  ModernInput,
  Modal,
  WaterGlassCard
} from 'modern-ui-components'

function App() {
  return (
    <div className="p-8">
      <Card className="max-w-md">
        <CardHeader>
          <CardTitle>欢迎使用 Modern UI</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            <ModernInput 
              placeholder="输入一些文本..." 
              label="用户名"
            />
            <Button>点击我</Button>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}

export default App
```

## 🎨 主题系统

### 使用预设主题

组件库提供了多个预设主题：

```typescript
// 在你的根组件或布局组件中
function App() {
  const [theme, setTheme] = useState('blue')
  const [isDark, setIsDark] = useState(false)

  useEffect(() => {
    const root = document.documentElement
    root.className = `${isDark ? 'dark' : ''} theme-${theme}`
  }, [theme, isDark])

  return (
    <div className="min-h-screen bg-background text-foreground">
      {/* 你的应用内容 */}
    </div>
  )
}
```

可用主题：
- `theme-blue` - 蓝色主题（默认）
- `theme-green` - 绿色主题
- `theme-orange` - 橙色主题
- `theme-purple` - 紫色主题
- `theme-rose` - 玫瑰主题

### 深色模式

添加 `dark` 类到根元素以启用深色模式：

```typescript
// 切换深色模式
const toggleDarkMode = () => {
  document.documentElement.classList.toggle('dark')
}
```

## 📚 可用组件

### 基础组件
- `Button` - 按钮组件，支持多种变体
- `Card` - 卡片组件及其子组件
- `ModernInput`, `SearchInput`, `PasswordInput` - 输入框组件

### 布局组件
- `Modal` - 模态对话框及其子组件
- `Accordion` - 手风琴组件
- `Table` - 表格组件
- `DataTable` - 数据表格组件

### 特殊效果组件
- `WaterGlassCard` - 水玻璃效果卡片
- `FlipCard` - 翻转卡片
- `WaterRipple` - 水波纹效果

### 表单组件
- `Form` - 表单组件及其子组件
- `FormControls` - 表单控件集合（复选框、单选框、开关等）

## 🔧 TypeScript 支持

所有组件都提供完整的TypeScript类型定义。你可以导入类型：

```typescript
import type { 
  ButtonVariants, 
  CardVariants, 
  FlipCardProps 
} from 'modern-ui-components'
```

## 🛠️ 构建你自己的项目

### 构建UI组件库

```bash
cd ui
npm run build:lib
```

这将生成：
- `dist/index.esm.js` - ES模块版本
- `dist/index.cjs.js` - CommonJS版本
- `dist/index.d.ts` - TypeScript类型定义
- `dist/style.css` - 编译后的样式文件

### 发布到npm

1. 更新版本号：
```bash
npm version patch  # 或 minor, major
```

2. 发布：
```bash
npm publish
```

## ❓ 常见问题

### Q: 样式没有生效
A: 确保你已经：
1. 导入了 `'modern-ui-components/styles'`
2. 正确配置了Tailwind CSS
3. 在Tailwind配置中包含了组件库的内容路径

### Q: 主题颜色没有变化
A: 确保你已经在根元素上添加了主题类，如 `theme-blue`

### Q: 深色模式不工作
A: 确保你在根元素上添加了 `dark` 类

### Q: TypeScript类型错误
A: 确保你安装了所有peer dependencies，并且使用的是兼容的React版本（>=18.0.0）

## 📞 支持

如果你遇到问题或有建议，请：
1. 查看组件文档和示例
2. 检查控制台是否有错误信息
3. 确认所有依赖都已正确安装
4. 参考web项目中的使用示例

---

Happy coding! 🎉
