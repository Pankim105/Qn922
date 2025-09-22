# Web 项目


## 📋 项目概述

这是一个使用 Modern UI Components 组件库构建的演示项目，展示了如何在 React 应用中集成和使用组件库。

## 🚀 快速开始

### 环境要求

- Node.js >= 18.0.0
- npm >= 8.0.0

### 安装依赖

```bash
# 安装项目依赖
npm install

# 构建 UI 组件库（如果尚未构建）
cd ../ui
npm run build:lib
cd ../web
```

### 启动开发服务器

```bash
npm run dev
```

访问 [http://localhost:5173](http://localhost:5173) 查看应用。

## 📦 UI 组件库集成

### 1. 安装 UI 组件库

本项目通过本地文件路径引用 UI 组件库：

```json
{
  "dependencies": {
    "modern-ui-components": "file:../ui"
  }
}
```

### 2. 导入样式

在 `src/main.tsx` 中导入组件库的样式：

```tsx
// main.tsx
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
// 导入UI组件库的样式
import 'modern-ui-components/styles'
import Demo from './Demo.tsx'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <Demo />
  </StrictMode>,
)
```

### 3. 使用组件

在组件中导入和使用 UI 组件：

```tsx
import { 
  Button, 
  Card, 
  CardHeader, 
  CardTitle, 
  CardContent,
  ModernInput,
  Modal,
  ThemeSwitcher
} from 'modern-ui-components'

function MyComponent() {
  return (
    <Card className="max-w-md">
      <CardHeader>
        <CardTitle>示例卡片</CardTitle>
      </CardHeader>
      <CardContent>
        <ModernInput placeholder="请输入内容" />
        <Button className="mt-4">提交</Button>
      </CardContent>
    </Card>
  )
}
```

## 🎨 可用组件

### 基础组件
- **Button** - 按钮组件，支持多种变体和尺寸
- **Card** - 卡片容器，支持多种样式变体
- **Input** - 输入框组件，包括 ModernInput、SearchInput、PasswordInput

### 布局组件
- **Modal** - 模态框，支持拖拽、缩放、多步骤
- **Accordion** - 手风琴组件，支持单选/多选
- **Table** - 表格组件，支持排序、分页
- **DataTable** - 数据表格，内置搜索和筛选

### 特效组件
- **WaterGlassCard** - 水玻璃卡片，毛玻璃效果
- **FlipCard** - 翻转卡片，3D 翻转动画
- **WaterRipple** - 水波纹效果

### 表单组件
- **Form** - 表单容器
- **ModernSelect** - 现代化选择器
- **ModernTextarea** - 文本域组件
- **FormControls** - 表单控件集合

### 主题组件
- **ThemeSwitcher** - 主题切换器，支持多种预设主题

## 🛠️ 开发指南

### 项目结构

```
web/
├── src/
│   ├── Demo.tsx          # 主演示组件
│   ├── main.tsx          # 应用入口
│   ├── App.css           # 全局样式
│   └── assets/           # 静态资源
├── public/               # 公共资源
├── package.json          # 项目配置
├── vite.config.ts        # Vite 配置
├── tailwind.config.js    # Tailwind 配置
└── tsconfig.json         # TypeScript 配置
```

### 构建和部署

```bash
# 构建生产版本
npm run build

# 预览构建结果
npm run preview

# 代码检查
npm run lint
```

### 自定义配置

#### Tailwind CSS 配置

项目已配置 Tailwind CSS 以支持 UI 组件库的样式：

```js
// tailwind.config.js
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
    "../ui/dist/**/*.{js,jsx,ts,tsx}", // 包含 UI 组件库
  ],
  theme: {
    extend: {
      // 扩展主题配置
    },
  },
  plugins: [],
}
```

#### TypeScript 配置

项目已配置 TypeScript 以支持 UI 组件库的类型定义：

```json
{
  "compilerOptions": {
    "baseUrl": ".",
    "paths": {
      "modern-ui-components": ["../ui/dist/index.d.ts"]
    }
  }
}
```

## 🎯 演示功能

当前演示包含以下功能：

1. **组件展示** - 展示所有可用的 UI 组件
2. **主题切换** - 演示主题切换功能
3. **交互效果** - 展示各种动画和交互效果
4. **响应式设计** - 展示移动端适配

## 🔧 故障排除

### 常见问题

1. **样式不生效**
   - 确保已导入 `modern-ui-components/styles`
   - 检查 Tailwind CSS 配置是否正确

2. **组件导入错误**
   - 确保 UI 组件库已正确构建
   - 检查 package.json 中的依赖配置

3. **TypeScript 类型错误**
   - 确保 UI 组件库的类型定义文件存在
   - 检查 tsconfig.json 配置

