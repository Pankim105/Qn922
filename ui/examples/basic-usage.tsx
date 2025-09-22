import React, { useState } from 'react'
import { 
  Button, 
  Card, 
  CardHeader, 
  CardTitle, 
  CardContent,
  ModernInput,
  WaterGlassCard,
  WaterGlassCardHeader,
  WaterGlassCardTitle,
  FlipCard
} from '../src/index'

// 基础使用示例
export const BasicUsageExample = () => {
  const [inputValue, setInputValue] = useState('')
  const [isFlipped, setIsFlipped] = useState(false)

  return (
    <div className="min-h-screen bg-background p-8 space-y-8">
      {/* 标题 */}
      <div className="text-center">
        <h1 className="text-3xl font-bold text-foreground mb-4">
          Modern UI Components 基础使用示例
        </h1>
        <p className="text-muted-foreground">
          展示组件库的基本用法和功能
        </p>
      </div>

      {/* 按钮示例 */}
      <Card>
        <CardHeader>
          <CardTitle>按钮组件示例</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex flex-wrap gap-2">
            <Button variant="solid">实心按钮</Button>
            <Button variant="outline">轮廓按钮</Button>
            <Button variant="ghost">幽灵按钮</Button>
            <Button variant="glass">玻璃按钮</Button>
          </div>
          
          <div className="flex flex-wrap gap-2">
            <Button size="sm">小按钮</Button>
            <Button size="md">中按钮</Button>
            <Button size="lg">大按钮</Button>
          </div>

          <div className="flex flex-wrap gap-2">
            <Button preset="primary">主要按钮</Button>
            <Button preset="secondary">次要按钮</Button>
            <Button preset="danger">危险按钮</Button>
          </div>
        </CardContent>
      </Card>

      {/* 输入框示例 */}
      <Card>
        <CardHeader>
          <CardTitle>输入框组件示例</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <ModernInput
            label="用户名"
            placeholder="请输入用户名"
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
          />
          
          <ModernInput
            variant="outline"
            label="邮箱"
            type="email"
            placeholder="请输入邮箱地址"
          />
          
          <ModernInput
            variant="filled"
            label="密码"
            type="password"
            placeholder="请输入密码"
          />
        </CardContent>
      </Card>

      {/* 卡片示例 */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        <Card variant="default">
          <CardHeader>
            <CardTitle className="text-sm">默认卡片</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-xs text-muted-foreground">
              这是一个默认样式的卡片
            </p>
          </CardContent>
        </Card>

        <Card variant="elevated">
          <CardHeader>
            <CardTitle className="text-sm">突出卡片</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-xs text-muted-foreground">
              这是一个突出样式的卡片
            </p>
          </CardContent>
        </Card>

        <Card variant="primary">
          <CardHeader>
            <CardTitle className="text-sm">主题色卡片</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-xs text-muted-foreground">
              这是一个主题色卡片
            </p>
          </CardContent>
        </Card>
      </div>

      {/* 水波纹玻璃卡片示例 */}
      <WaterGlassCard 
        variant="gradient" 
        themeColor="#3b82f6"
        className="h-64"
      >
        <WaterGlassCardHeader>
          <WaterGlassCardTitle>水波纹玻璃卡片</WaterGlassCardTitle>
        </WaterGlassCardHeader>
        <div className="p-6 pt-0">
          <p className="text-sm text-white/90">
            这是一个具有水波纹效果的玻璃卡片。移动鼠标查看效果。
          </p>
        </div>
      </WaterGlassCard>

      {/* 翻转卡片示例 */}
      <FlipCard
        trigger="click"
        direction="horizontal"
        width="100%"
        height="200px"
        frontContent={
          <Card className="h-full">
            <CardHeader>
              <CardTitle className="text-center">点击翻转</CardTitle>
            </CardHeader>
            <CardContent className="text-center">
              <p className="text-sm text-muted-foreground">
                点击查看背面内容
              </p>
            </CardContent>
          </Card>
        }
        backContent={
          <Card className="h-full">
            <CardHeader>
              <CardTitle className="text-center">背面内容</CardTitle>
            </CardHeader>
            <CardContent className="text-center">
              <p className="text-sm text-muted-foreground">
                这是卡片的背面
              </p>
            </CardContent>
          </Card>
        }
      />

      {/* 主题切换示例 */}
      <Card>
        <CardHeader>
          <CardTitle>主题切换</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex gap-2">
            <button 
              onClick={() => document.documentElement.className = 'theme-green'}
              className="px-3 py-1 text-sm bg-green-500 text-white rounded"
            >
              绿色主题
            </button>
            <button 
              onClick={() => document.documentElement.className = 'theme-orange'}
              className="px-3 py-1 text-sm bg-orange-500 text-white rounded"
            >
              橙色主题
            </button>
            <button 
              onClick={() => document.documentElement.className = 'theme-purple'}
              className="px-3 py-1 text-sm bg-purple-500 text-white rounded"
            >
              紫色主题
            </button>
            <button 
              onClick={() => document.documentElement.className = 'dark theme-blue'}
              className="px-3 py-1 text-sm bg-blue-500 text-white rounded"
            >
              深色模式
            </button>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}

export default BasicUsageExample

