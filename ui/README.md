# Modern UI Components

> 基于 React + TypeScript + Tailwind CSS 的现代化 UI 组件库

## ✨ 特性

- 🎨 **丰富组件** - 20+ 精心设计的组件，涵盖常用业务场景
- 🌈 **主题系统** - 5种预设主题 + 深色模式，支持动态切换
- 🔧 **TypeScript** - 完整类型定义，提供优秀的开发体验
- ⚡ **高性能** - 基于现代化技术栈，轻量且高效
- 📱 **响应式** - 所有组件支持移动端适配
- 🎭 **视觉效果** - 内置水波纹、玻璃拟态、3D翻转等特效
- ♿ **无障碍** - 遵循 WAI-ARIA 标准，支持键盘导航

## 🚀 快速开始

### 安装

```bash
# 本地开发（推荐）
npm install file:../ui

# NPM 安装
npm install modern-ui-components
```

### 基础配置

1. **导入样式**（必需）
```tsx
// main.tsx
import 'modern-ui-components/styles'
```

2. **配置 Tailwind CSS**
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

### 使用组件

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

## 📦 组件列表

### 基础组件
- **Button** - 按钮，支持多种变体、尺寸、状态
- **Card** - 卡片容器，支持阴影、边框、渐变等效果
- **Input** - 输入框，包括搜索框、密码框等特殊类型

### 布局组件
- **Modal** - 模态框，支持拖拽、缩放、多步骤流程
- **Accordion** - 手风琴，支持单选/多选模式
- **Table** - 表格，支持排序、分页、选择
- **DataTable** - 数据表格，内置搜索、筛选功能

### 特效组件
- **WaterGlassCard** - 水玻璃卡片，毛玻璃效果 + 水波纹
- **FlipCard** - 翻转卡片，3D翻转动画
- **WaterRipple** - 水波纹效果

### 表单组件
- **Form** - 表单容器，支持布局配置
- **Select** - 选择器，支持搜索、多选
- **Textarea** - 文本域，支持自动调整高度
- **Checkbox/Radio/Switch** - 表单控件

## 🎨 主题系统

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

## 🔧 组件示例

### Button 组件
```tsx
// 基础用法
<Button>默认按钮</Button>
<Button variant="outline">轮廓按钮</Button>
<Button size="lg">大尺寸</Button>
<Button loading>加载中</Button>

// 预设样式
<Button preset="primary">主要按钮</Button>
<Button preset="danger">危险按钮</Button>
```

### Input 组件
```tsx
// 基础输入框
<ModernInput 
  label="用户名" 
  placeholder="请输入用户名"
  helperText="提示信息"
/>

// 特殊类型
<SearchInput placeholder="搜索..." />
<PasswordInput label="密码" />

// 状态
<ModernInput error="错误信息" />
<ModernInput success="输入正确" />
<ModernInput loading />
```

### Modal 组件
```tsx
const [open, setOpen] = useState(false)

<Modal open={open} onOpenChange={setOpen}>
  <ModalHeader>
    <ModalTitle>标题</ModalTitle>
    <ModalDescription>描述信息</ModalDescription>
  </ModalHeader>
  <ModalBody>
    内容区域
  </ModalBody>
  <ModalFooter>
    <Button onClick={() => setOpen(false)}>确定</Button>
  </ModalFooter>
</Modal>
```

### FlipCard 组件
```tsx
<FlipCard
  trigger="hover" // 或 "click"
  direction="horizontal" // 或 "vertical"
  frontContent={<div>正面内容</div>}
  backContent={<div>背面内容</div>}
/>
```

### DataTable 组件
```tsx
const columns = [
  { key: 'name', title: '姓名', sortable: true },
  { key: 'age', title: '年龄', sortable: true },
  { key: 'email', title: '邮箱' },
]

<DataTable
  columns={columns}
  dataSource={data}
  searchable
  selectable
  pagination={{ current: 1, pageSize: 10, total: 100 }}
/>
```

## 📤 导出到其他项目

### 1. 构建组件库
```bash
cd ui
npm run build:lib
```

### 2. 发布到 NPM（可选）
```bash
# 更新版本
npm version patch

# 发布
npm publish
```

### 3. 在新项目中使用
```bash
# 方式一：本地文件引用
npm install file:../path/to/ui

# 方式二：NPM 安装
npm install modern-ui-components
```

### 4. 项目配置
```tsx
// main.tsx - 导入样式
import 'modern-ui-components/styles'

// 使用组件
import { Button, Card } from 'modern-ui-components'
```

## 🛠️ 开发

```bash
# 安装依赖
npm install

# 开发模式
npm run dev

# 构建组件库
npm run build:lib

# 类型检查
npm run type-check

# 代码格式化
npm run format
```

## 📋 依赖要求

### Peer Dependencies
- React >= 18.0.0
- React DOM >= 18.0.0

### 自动安装的依赖
- @radix-ui/react-dialog
- @radix-ui/react-slot
- class-variance-authority
- clsx
- lucide-react
- tailwind-merge

## 🌟 特色功能

### 视觉效果
- **水波纹动画** - 鼠标交互时的水波扩散效果
- **玻璃拟态** - 半透明毛玻璃背景效果
- **3D翻转** - 卡片悬停/点击翻转动画
- **主题响应** - 所有效果自动适配当前主题色

### 交互体验
- **键盘导航** - 完整的键盘操作支持
- **Focus管理** - 智能焦点管理
- **动画过渡** - 流畅的状态切换动画
- **触摸优化** - 移动端触摸交互优化

### 开发体验
- **TypeScript支持** - 完整类型定义
- **组件文档** - 详细的组件API文档
- **示例代码** - 丰富的使用示例
- **最佳实践** - 遵循React最佳实践

## 📄 许可证

MIT License

---

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

1. Fork 这个仓库
2. 创建你的特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交你的更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开一个 Pull Request

---

**Happy Coding! 🎉**
