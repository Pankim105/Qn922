# ThemeSwitcher

美化的主题切换器组件，提供现代化的用户界面来切换应用主题和明暗模式。

## 特性

- 🎨 **多主题支持** - 支持多种预设主题色彩
- 🌙 **明暗模式** - 优雅的明暗模式切换器
- 📱 **可最小化** - 支持最小化为圆形胶囊状
- 🎛️ **系统主题** - 支持跟随系统主题设置
- ✨ **动画效果** - 流畅的过渡动画
- 🎯 **易于使用** - 简单的 API 设计

## 基础用法

```tsx
import { ThemeSwitcher } from 'modern-ui-components'

function App() {
  const [theme, setTheme] = useState('green')
  const [isDark, setIsDark] = useState(false)

  return (
    <ThemeSwitcher
      currentTheme={theme}
      isDarkMode={isDark}
      onThemeChange={setTheme}
      onDarkModeChange={setIsDark}
    />
  )
}
```

## 固定定位用法

```tsx
<ThemeSwitcher
  className="fixed top-4 right-4"
  currentTheme={theme}
  isDarkMode={isDark}
  onThemeChange={setTheme}
  onDarkModeChange={setIsDark}
  minimizable={true}
  defaultMinimized={false}
/>
```

## Props

| 属性 | 类型 | 默认值 | 描述 |
|------|------|--------|------|
| `currentTheme` | `string` | `'green'` | 当前选中的主题名称 |
| `isDarkMode` | `boolean` | `false` | 是否为深色模式 |
| `themes` | `ThemeOption[]` | `defaultThemes` | 可选主题列表 |
| `onThemeChange` | `(theme: string) => void` | - | 主题变化回调 |
| `onDarkModeChange` | `(isDark: boolean) => void` | - | 深色模式切换回调 |
| `showSystemTheme` | `boolean` | `true` | 是否显示系统主题选项 |
| `className` | `string` | - | 自定义CSS类名 |
| `minimizable` | `boolean` | `true` | 是否可以最小化 |
| `defaultMinimized` | `boolean` | `false` | 初始是否最小化 |

## ThemeOption 接口

```tsx
interface ThemeOption {
  name: string                                          // 主题名称
  label: string                                         // 显示标签
  icon: React.ComponentType<{ className?: string }>    // 图标组件
  color: string                                         // 主题颜色
  description: string                                   // 主题描述
}
```

## 默认主题

组件内置了以下主题：

- 🌿 **绿色** (`green`) - 自然清新 `#10b981`
- 🔥 **橙色** (`orange`) - 活力充沛 `#f59e0b`
- 🍇 **紫色** (`purple`) - 神秘优雅 `#8b5cf6`
- 💧 **蓝色** (`blue`) - 冷静理性 `#3b82f6`
- 💖 **玫瑰色** (`rose`) - 温柔浪漫 `#f43f5e`

## 自定义主题

```tsx
import { Leaf, Sun, Star } from 'lucide-react'

const customThemes = [
  {
    name: 'forest',
    label: '森林',
    icon: Leaf,
    color: '#16a34a',
    description: '深邃的森林绿'
  },
  {
    name: 'sunset',
    label: '日落',
    icon: Sun,
    color: '#f97316',
    description: '温暖的日落橙'
  },
  {
    name: 'starlight',
    label: '星光',
    icon: Star,
    color: '#6366f1',
    description: '神秘的星光紫'
  }
]

<ThemeSwitcher
  themes={customThemes}
  currentTheme="forest"
  onThemeChange={setTheme}
/>
```

## 最小化状态

当设置为可最小化时，组件会显示一个最小化按钮。最小化后：

- 组件变为 56x56 像素的圆形胶囊
- 显示当前主题的图标和颜色
- 如果是深色模式，会显示一个小的黄色指示点
- 点击胶囊可以重新展开

## 样式定制

组件使用 Tailwind CSS 类名，可以通过 `className` 属性进行定制：

```tsx
<ThemeSwitcher
  className="fixed bottom-4 left-4 shadow-xl"
  // ... 其他props
/>
```

## 事件处理

### 主题切换

```tsx
const handleThemeChange = (newTheme: string) => {
  // 更新应用主题
  document.documentElement.className = `theme-${newTheme}`
  setTheme(newTheme)
}
```

### 深色模式切换

```tsx
const handleDarkModeChange = (isDark: boolean) => {
  // 更新深色模式
  document.documentElement.classList.toggle('dark', isDark)
  setIsDark(isDark)
}
```

### 系统主题检测

组件内置了系统主题检测功能：

```tsx
// 点击"跟随系统"按钮时会自动检测系统偏好
const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
```

## 注意事项

1. **图标依赖**: 组件使用 `lucide-react` 图标库，确保已安装
2. **主题应用**: 组件只负责UI交互，实际的主题应用需要在回调中处理
3. **响应式**: 组件已针对移动设备优化，在小屏幕上表现良好
4. **无障碍**: 支持键盘导航和屏幕阅读器

## 完整示例

```tsx
import React, { useState, useEffect } from 'react'
import { ThemeSwitcher } from 'modern-ui-components'

function App() {
  const [theme, setTheme] = useState('green')
  const [isDark, setIsDark] = useState(false)

  // 应用主题到DOM
  useEffect(() => {
    document.documentElement.className = `${isDark ? 'dark' : ''} theme-${theme}`
  }, [theme, isDark])

  return (
    <div className="min-h-screen bg-background text-foreground">
      <ThemeSwitcher
        className="fixed top-4 right-4"
        currentTheme={theme}
        isDarkMode={isDark}
        onThemeChange={setTheme}
        onDarkModeChange={setIsDark}
        minimizable={true}
        showSystemTheme={true}
      />
      
      {/* 应用内容 */}
      <div className="container mx-auto p-8">
        <h1 className="text-4xl font-bold text-primary">
          当前主题: {theme}
        </h1>
        <p className="text-muted-foreground">
          模式: {isDark ? '深色' : '浅色'}
        </p>
      </div>
    </div>
  )
}
```

这个组件提供了完整的主题切换解决方案，包括美观的UI、流畅的动画和良好的用户体验。
