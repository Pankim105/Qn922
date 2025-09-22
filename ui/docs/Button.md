# Button 按钮组件

功能强大的按钮组件，支持多种样式、尺寸、形状和状态。

## 基础用法

```tsx
import { Button } from 'modern-ui-components'

// 基础按钮
<Button>点击我</Button>

// 带变体的按钮
<Button variant="outline" size="lg">
  轮廓按钮
</Button>
```

## 样式变体

### 基础变体

| 变体 | 描述 | 使用场景 |
|------|------|----------|
| `solid` | 实心按钮 | 主要操作 |
| `outline` | 轮廓按钮 | 次要操作 |
| `ghost` | 幽灵按钮 | 文本操作 |
| `glass` | 玻璃按钮 | 导航栏 |
| `destructive` | 危险按钮 | 删除操作 |
| `link` | 链接按钮 | 文本链接 |

```tsx
<div className="flex gap-2">
  <Button variant="solid">Solid</Button>
  <Button variant="outline">Outline</Button>
  <Button variant="ghost">Ghost</Button>
  <Button variant="glass">Glass</Button>
  <Button variant="destructive">Destructive</Button>
  <Button variant="link">Link</Button>
</div>
```

### 尺寸变体

| 尺寸 | 高度 | 使用场景 |
|------|------|----------|
| `sm` | 32px | 紧凑空间 |
| `md` | 40px | 标准按钮 |
| `lg` | 44px | 重要操作 |
| `xl` | 48px | 页面标题 |
| `icon` | 40px | 图标按钮 |

```tsx
<div className="flex items-center gap-2">
  <Button size="sm">Small</Button>
  <Button size="md">Medium</Button>
  <Button size="lg">Large</Button>
  <Button size="xl">Extra Large</Button>
  <Button size="icon">
    <Heart className="h-4 w-4" />
  </Button>
</div>
```

### 形状变体

| 形状 | 描述 | 使用场景 |
|------|------|----------|
| `rounded` | 圆角矩形 | 通用按钮 |
| `pill` | 胶囊形状 | 标签按钮 |
| `square` | 方形 | 图标按钮 |

```tsx
<div className="flex gap-2">
  <Button shape="rounded">Rounded</Button>
  <Button shape="pill">Pill</Button>
  <Button shape="square">Square</Button>
</div>
```

## 状态变体

### 基础状态

```tsx
<div className="flex gap-2">
  <Button>Default</Button>
  <Button state="active">Active</Button>
  <Button disabled>Disabled</Button>
</div>
```

### 加载状态

```tsx
const [loading, setLoading] = useState(false)

const handleClick = () => {
  setLoading(true)
  setTimeout(() => setLoading(false), 2000)
}

<Button loading={loading} onClick={handleClick}>
  {loading ? 'Loading...' : 'Click Me'}
</Button>
```

## 预设配置

组件提供了常用的预设配置，简化使用：

```tsx
<div className="flex gap-2">
  <Button preset="primary">Primary</Button>
  <Button preset="secondary">Secondary</Button>
  <Button preset="danger">Danger</Button>
  <Button preset="iconButton">
    <Settings className="h-4 w-4" />
  </Button>
  <Button preset="linkButton">Link</Button>
  <Button preset="navButton">Nav</Button>
</div>
```

## 高级用法

### 作为子组件

```tsx
<Button asChild>
  <a href="/dashboard">Go to Dashboard</a>
</Button>
```

### 自定义样式

```tsx
<Button 
  className="bg-gradient-to-r from-purple-500 to-pink-500"
  variant="solid"
>
  Gradient Button
</Button>
```

### 事件处理

```tsx
<Button 
  onClick={() => console.log('Clicked!')}
  onMouseEnter={() => console.log('Hovered!')}
>
  Interactive Button
</Button>
```

## API 参考

### ButtonProps

| 属性 | 类型 | 默认值 | 描述 |
|------|------|--------|------|
| variant | `'solid' \| 'outline' \| 'ghost' \| 'glass' \| 'destructive' \| 'link'` | `'solid'` | 按钮样式变体 |
| size | `'sm' \| 'md' \| 'lg' \| 'xl' \| 'icon'` | `'md'` | 按钮尺寸 |
| shape | `'rounded' \| 'pill' \| 'square'` | `'rounded'` | 按钮形状 |
| state | `'default' \| 'loading' \| 'active'` | `'default'` | 按钮状态 |
| preset | `keyof typeof buttonPresets` | - | 预设配置 |
| loading | `boolean` | `false` | 是否显示加载状态 |
| asChild | `boolean` | `false` | 是否作为子组件渲染 |
| className | `string` | - | 自定义类名 |
| ...props | `React.ButtonHTMLAttributes<HTMLButtonElement>` | - | 原生按钮属性 |

### 预设配置

| 预设 | 配置 | 描述 |
|------|------|------|
| `primary` | `{ variant: 'solid', size: 'md' }` | 主要操作按钮 |
| `secondary` | `{ variant: 'outline', size: 'md' }` | 次要操作按钮 |
| `danger` | `{ variant: 'destructive', size: 'md' }` | 危险操作按钮 |
| `iconButton` | `{ variant: 'ghost', size: 'icon', shape: 'pill' }` | 图标按钮 |
| `linkButton` | `{ variant: 'link', size: 'sm' }` | 链接样式按钮 |
| `navButton` | `{ variant: 'glass', size: 'sm', shape: 'pill' }` | 导航栏按钮 |

## 最佳实践

### 1. 选择合适的变体

```tsx
// ✅ 推荐：根据操作重要性选择变体
<Button variant="solid">确认</Button>        // 主要操作
<Button variant="outline">取消</Button>      // 次要操作
<Button variant="ghost">更多</Button>        // 辅助操作
<Button variant="destructive">删除</Button>  // 危险操作
```

### 2. 保持尺寸一致性

```tsx
// ✅ 推荐：在同一组操作中保持尺寸一致
<div className="flex gap-2">
  <Button size="md">保存</Button>
  <Button size="md" variant="outline">取消</Button>
</div>
```

### 3. 合理使用加载状态

```tsx
// ✅ 推荐：在异步操作时显示加载状态
const handleSubmit = async () => {
  setLoading(true)
  try {
    await submitForm()
  } finally {
    setLoading(false)
  }
}

<Button loading={loading} onClick={handleSubmit}>
  提交
</Button>
```

### 4. 图标按钮的使用

```tsx
// ✅ 推荐：图标按钮使用 icon 尺寸
<Button size="icon" variant="ghost">
  <Settings className="h-4 w-4" />
</Button>

// ✅ 推荐：使用预设配置
<Button preset="iconButton">
  <Heart className="h-4 w-4" />
</Button>
```

## 注意事项

1. **无障碍性**: 按钮会自动处理键盘导航和屏幕阅读器支持
2. **加载状态**: 加载状态下按钮会自动禁用，防止重复点击
3. **图标使用**: 建议使用 Lucide React 图标库保持一致性
4. **主题适配**: 按钮会自动适配当前主题色彩
5. **响应式**: 按钮在不同屏幕尺寸下会自动调整

## 相关组件

- [Card](./Card.md) - 卡片容器
- [Input](./Input.md) - 输入框组件
- [Modal](./Modal.md) - 模态框组件

