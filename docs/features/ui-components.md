# UI组件库

QN Contest的UI组件库提供丰富的现代化组件，支持多种视觉效果和交互体验，为角色扮演应用提供沉浸式的用户界面。

## 组件库概述

基于React + TypeScript + Tailwind CSS构建的现代化UI组件库，包含20+精心设计的组件，支持主题系统、视觉效果和响应式设计。

## 核心特性

### 🎨 丰富组件
- **20+组件**: 涵盖常用业务场景的完整组件集
- **TypeScript**: 完整类型定义，提供优秀的开发体验
- **响应式**: 所有组件支持移动端适配
- **无障碍**: 遵循WAI-ARIA标准，支持键盘导航

### 🌈 主题系统
- **5种预设主题**: green、orange、purple、blue、rose
- **深色模式**: 完整的深色模式支持
- **动态切换**: 支持运行时主题切换
- **CSS变量**: 基于CSS变量的主题系统

### ⚡ 视觉效果
- **水波纹动画**: 鼠标交互时的水波扩散效果
- **玻璃拟态**: 半透明毛玻璃背景效果
- **3D翻转**: 卡片悬停/点击翻转动画
- **硬件加速**: 使用GPU加速提升性能

## 组件分类

### 基础组件

#### Button 按钮
```tsx
import { Button } from 'modern-ui-components';

// 基础用法
<Button>默认按钮</Button>
<Button variant="outline">轮廓按钮</Button>
<Button size="lg">大尺寸</Button>
<Button loading>加载中</Button>

// 预设样式
<Button preset="primary">主要按钮</Button>
<Button preset="danger">危险按钮</Button>
```

**特性**:
- 多种变体：solid、outline、ghost
- 多种尺寸：sm、md、lg、xl
- 状态支持：loading、disabled
- 预设样式：primary、danger、success

#### Card 卡片
```tsx
import { Card, CardHeader, CardTitle, CardContent } from 'modern-ui-components';

<Card className="max-w-md">
  <CardHeader>
    <CardTitle>卡片标题</CardTitle>
  </CardHeader>
  <CardContent>
    <p>卡片内容</p>
  </CardContent>
</Card>
```

**特性**:
- 模块化结构：Header、Content、Footer
- 阴影效果：多种阴影级别
- 边框样式：可自定义边框
- 响应式：自适应不同屏幕尺寸

#### Input 输入框
```tsx
import { Input } from 'modern-ui-components';

<Input 
  label="用户名" 
  placeholder="请输入用户名"
  helperText="提示信息"
/>
```

**特性**:
- 多种类型：text、password、email、search
- 状态支持：error、success、loading
- 标签和提示：label、helperText
- 验证反馈：实时验证状态显示

### 布局组件

#### Modal 模态框
```tsx
import { Modal, ModalHeader, ModalTitle, ModalBody, ModalFooter } from 'modern-ui-components';

const [open, setOpen] = useState(false);

<Modal open={open} onOpenChange={setOpen}>
  <ModalHeader>
    <ModalTitle>标题</ModalTitle>
  </ModalHeader>
  <ModalBody>
    内容区域
  </ModalBody>
  <ModalFooter>
    <Button onClick={() => setOpen(false)}>确定</Button>
  </ModalFooter>
</Modal>
```

**特性**:
- 拖拽支持：可拖拽移动
- 缩放功能：支持缩放调整
- 多步骤流程：支持多步骤表单
- 焦点管理：智能焦点管理

#### Accordion 手风琴
```tsx
import { Accordion, AccordionItem, AccordionTrigger, AccordionContent } from 'modern-ui-components';

<Accordion type="single" collapsible>
  <AccordionItem value="item-1">
    <AccordionTrigger>项目1</AccordionTrigger>
    <AccordionContent>内容1</AccordionContent>
  </AccordionItem>
</Accordion>
```

**特性**:
- 单选/多选模式
- 可折叠设计
- 动画过渡
- 键盘导航

#### Table 表格
```tsx
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from 'modern-ui-components';

<Table>
  <TableHeader>
    <TableRow>
      <TableHead>姓名</TableHead>
      <TableHead>年龄</TableHead>
    </TableRow>
  </TableHeader>
  <TableBody>
    <TableRow>
      <TableCell>张三</TableCell>
      <TableCell>25</TableCell>
    </TableRow>
  </TableBody>
</Table>
```

**特性**:
- 排序功能
- 分页支持
- 选择功能
- 响应式设计

### 特效组件

#### WaterGlassCard 水玻璃卡片
```tsx
import { WaterGlassCard, WaterGlassCardHeader, WaterGlassCardTitle, WaterGlassCardContent } from 'modern-ui-components';

<WaterGlassCard 
  variant="gradient" 
  themeColor="#3b82f6"
  showWaterRipple={true}
>
  <WaterGlassCardHeader>
    <WaterGlassCardTitle>水玻璃卡片</WaterGlassCardTitle>
  </WaterGlassCardHeader>
  <WaterGlassCardContent>
    <p>具有水波纹和玻璃拟态效果的卡片</p>
  </WaterGlassCardContent>
</WaterGlassCard>
```

**特性**:
- 水波纹效果：鼠标交互时的水波扩散
- 玻璃拟态：半透明毛玻璃背景
- 主题色响应：自动适配当前主题色
- 多种变体：gradient、elevated、primary等

#### FlipCard 翻转卡片
```tsx
import { FlipCard } from 'modern-ui-components';

<FlipCard
  trigger="hover" // 或 "click"
  direction="horizontal" // 或 "vertical"
  frontContent={<div>正面内容</div>}
  backContent={<div>背面内容</div>}
/>
```

**特性**:
- 3D翻转动画
- 多种触发方式：hover、click
- 多种翻转方向：horizontal、vertical
- 流畅的动画过渡

#### WaterRipple 水波纹效果
```tsx
import { WaterRipple } from 'modern-ui-components';

<WaterRipple 
  themeColor="#3b82f6" 
  localEvents={true}
/>
```

**特性**:
- 水波纹扩散效果
- 主题色响应
- 本地事件处理
- 性能优化

### 表单组件

#### Form 表单容器
```tsx
import { Form, FormField, FormItem, FormLabel, FormControl, FormMessage } from 'modern-ui-components';

<Form>
  <FormField name="username">
    <FormItem>
      <FormLabel>用户名</FormLabel>
      <FormControl>
        <Input placeholder="请输入用户名" />
      </FormControl>
      <FormMessage />
    </FormItem>
  </FormField>
</Form>
```

**特性**:
- 表单验证
- 错误处理
- 布局配置
- 响应式设计

#### Select 选择器
```tsx
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from 'modern-ui-components';

<Select>
  <SelectTrigger>
    <SelectValue placeholder="选择选项" />
  </SelectTrigger>
  <SelectContent>
    <SelectItem value="option1">选项1</SelectItem>
    <SelectItem value="option2">选项2</SelectItem>
  </SelectContent>
</Select>
```

**特性**:
- 搜索功能
- 多选支持
- 键盘导航
- 自定义样式

## 主题系统

### 预设主题
```tsx
// 应用主题
useEffect(() => {
  document.documentElement.className = `theme-${theme} ${isDark ? 'dark' : ''}`
}, [theme, isDark])
```

可用主题：`green` | `orange` | `purple` | `blue` | `rose`

### 主题切换示例
```tsx
function ThemeSwitcher() {
  const [theme, setTheme] = useState('blue')
  const [isDark, setIsDark] = useState(false)

  return (
    <div>
      {/* 深色模式切换 */}
      <input 
        type="checkbox" 
        checked={isDark}
        onChange={(e) => setIsDark(e.target.checked)}
      />
      
      {/* 主题选择 */}
      {['blue', 'green', 'orange', 'purple', 'rose'].map(t => (
        <button key={t} onClick={() => setTheme(t)}>
          {t}
        </button>
      ))}
    </div>
  )
}
```

### CSS变量系统
```css
:root {
  --primary: 210 100% 50%;
  --primary-foreground: 0 0% 100%;
  --secondary: 210 40% 96%;
  --secondary-foreground: 210 40% 10%;
  /* ... 更多颜色变量 */
}
```

## 使用指南

### 安装配置

#### 1. 安装组件库
```bash
# 本地开发（推荐）
npm install file:../ui

# NPM 安装
npm install modern-ui-components
```

#### 2. 导入样式
```tsx
// main.tsx
import 'modern-ui-components/styles'
```

#### 3. 配置Tailwind CSS
```js
// tailwind.config.js
module.exports = {
  content: [
    "./src/**/*.{js,ts,jsx,tsx}",
    "./node_modules/modern-ui-components/dist/**/*.{js,jsx,ts,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        border: "hsl(var(--border))",
        background: "hsl(var(--background))",
        foreground: "hsl(var(--foreground))",
        primary: {
          DEFAULT: "hsl(var(--primary))",
          foreground: "hsl(var(--primary-foreground))",
        },
        // ... 更多颜色变量
      },
    },
  },
}
```

### 基础使用
```tsx
import { Button, Card, CardHeader, CardTitle, CardContent } from 'modern-ui-components'

function App() {
  return (
    <Card className="max-w-md">
      <CardHeader>
        <CardTitle>Hello World</CardTitle>
      </CardHeader>
      <CardContent>
        <Button>点击我</Button>
      </CardContent>
    </Card>
  )
}
```

## 性能优化

### 组件优化
- **React.memo**: 使用memo避免不必要的重新渲染
- **useMemo**: 缓存计算结果和样式对象
- **useCallback**: 缓存回调函数
- **条件渲染**: 只在需要时渲染组件

### 样式优化
- **CSS变量**: 使用CSS变量提升主题切换性能
- **硬件加速**: 使用transform3d启用硬件加速
- **will-change**: 优化动画性能
- **contain**: 使用CSS contain属性优化布局

### 打包优化
- **Tree Shaking**: 支持按需导入
- **代码分割**: 支持动态导入
- **压缩优化**: 生产环境自动压缩
- **类型定义**: 完整的TypeScript类型定义

## 开发指南

### 组件开发
```tsx
// 组件开发模板
import React from 'react'
import { cva, type VariantProps } from 'class-variance-authority'
import { cn } from '@/lib/utils'

const componentVariants = cva(
  'base-classes',
  {
    variants: {
      variant: {
        default: 'default-classes',
        secondary: 'secondary-classes',
      },
      size: {
        sm: 'small-classes',
        md: 'medium-classes',
      },
    },
    defaultVariants: {
      variant: 'default',
      size: 'md',
    },
  }
)

interface ComponentProps
  extends React.HTMLAttributes<HTMLDivElement>,
    VariantProps<typeof componentVariants> {
  // 自定义属性
}

const Component = React.forwardRef<HTMLDivElement, ComponentProps>(
  ({ className, variant, size, ...props }, ref) => {
    return (
      <div
        className={cn(componentVariants({ variant, size, className }))}
        ref={ref}
        {...props}
      />
    )
  }
)

Component.displayName = 'Component'

export { Component, componentVariants }
export type { ComponentProps }
```

### 样式规范
- **Tailwind CSS**: 使用Tailwind CSS进行样式开发
- **CVA**: 使用class-variance-authority管理变体
- **cn函数**: 使用cn函数合并类名
- **响应式**: 优先使用响应式设计

### 类型定义
- **TypeScript**: 完整的TypeScript类型定义
- **接口继承**: 继承HTML元素属性
- **变体类型**: 使用VariantProps类型
- **泛型支持**: 支持泛型组件

## 最佳实践

### 组件使用
1. **按需导入**: 只导入需要的组件
2. **类型安全**: 使用TypeScript类型检查
3. **主题一致**: 保持主题风格一致
4. **响应式**: 考虑不同屏幕尺寸

### 性能优化
1. **避免重复渲染**: 使用memo和useMemo
2. **懒加载**: 使用动态导入
3. **样式优化**: 避免内联样式
4. **事件处理**: 优化事件处理函数

### 可访问性
1. **键盘导航**: 支持键盘操作
2. **屏幕阅读器**: 提供适当的ARIA标签
3. **颜色对比**: 确保足够的颜色对比度
4. **焦点管理**: 合理的焦点管理

## 故障排除

### 常见问题

#### 1. 样式不生效
**可能原因**:
- Tailwind CSS配置不正确
- 样式文件未正确导入
- CSS变量未定义

**解决方案**:
- 检查tailwind.config.js配置
- 确认样式文件导入
- 检查CSS变量定义

#### 2. 类型错误
**可能原因**:
- TypeScript配置问题
- 类型定义缺失
- 版本不兼容

**解决方案**:
- 检查tsconfig.json配置
- 更新类型定义
- 检查版本兼容性

#### 3. 性能问题
**可能原因**:
- 组件重复渲染
- 样式计算过多
- 动画性能问题

**解决方案**:
- 使用React.memo优化
- 优化样式计算
- 使用硬件加速

---

**文档版本**: v1.0  
**最后更新**: 2025-01-27  
**维护者**: QN Contest Team

