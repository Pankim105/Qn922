import React, { useState } from 'react'
import { 
  Sun, 
  Moon, 
  Palette, 
  Leaf, 
  Flame, 
  Grape, 
  Droplets, 
  Heart,
  Monitor,
  ChevronDown,
  Check,
  Minimize2
} from 'lucide-react'
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from './Card'
import { Button } from './Button'
import { cn } from '../../lib/utils'

export interface ThemeOption {
  name: string
  label: string
  icon: React.ComponentType<{ className?: string }>
  color: string
  description: string
}

export interface ThemeSwitcherProps {
  /** 当前主题 */
  currentTheme?: string
  /** 是否为深色模式 */
  isDarkMode?: boolean
  /** 主题选项 */
  themes?: ThemeOption[]
  /** 主题变化回调 */
  onThemeChange?: (theme: string) => void
  /** 深色模式切换回调 */
  onDarkModeChange?: (isDark: boolean) => void
  /** 是否显示系统主题选项 */
  showSystemTheme?: boolean
  /** 自定义位置类名 */
  className?: string
  /** 是否可以最小化 */
  minimizable?: boolean
  /** 初始是否最小化 */
  defaultMinimized?: boolean
}

const defaultThemes: ThemeOption[] = [
  { 
    name: 'green', 
    label: '绿色', 
    icon: Leaf, 
    color: '#10b981',
    description: '自然清新'
  },
  { 
    name: 'orange', 
    label: '橙色', 
    icon: Flame, 
    color: '#f59e0b',
    description: '活力充沛'
  },
  { 
    name: 'purple', 
    label: '紫色', 
    icon: Grape, 
    color: '#8b5cf6',
    description: '神秘优雅'
  },
  { 
    name: 'blue', 
    label: '蓝色', 
    icon: Droplets, 
    color: '#3b82f6',
    description: '冷静理性'
  },
  { 
    name: 'rose', 
    label: '玫瑰色', 
    icon: Heart, 
    color: '#f43f5e',
    description: '温柔浪漫'
  },
]

/**
 * ThemeSwitcher - 主题切换器组件
 * 
 * 提供现代化的主题切换界面，支持多种主题色彩和明暗模式切换
 */
export const ThemeSwitcher = React.forwardRef<HTMLDivElement, ThemeSwitcherProps>(({
  currentTheme = 'green',
  isDarkMode = false,
  themes = defaultThemes,
  onThemeChange,
  onDarkModeChange,
  showSystemTheme = true,
  className,
  minimizable = true,
  defaultMinimized = false,
  ...props
}, ref) => {
  const [isOpen, setIsOpen] = useState(false)
  const [isMinimized, setIsMinimized] = useState(defaultMinimized)
  const [contentHeight, setContentHeight] = useState(320) // 预估初始高度
  const contentRef = React.useRef<HTMLDivElement>(null)

  const currentThemeOption = themes.find(t => t.name === currentTheme) || themes[0]

  // 测量内容高度（避免在动画过程中频繁更新导致跳动）
  React.useEffect(() => {
    const element = contentRef.current
    if (!element) return

    const measure = () => {
      setContentHeight(element.scrollHeight)
    }

    // 初始与延迟测量，确保内容渲染完成
    measure()
    const t1 = window.setTimeout(measure, 50)
    const t2 = window.setTimeout(measure, 200)

    return () => {
      window.clearTimeout(t1)
      window.clearTimeout(t2)
    }
   }, [isOpen, isMinimized, themes.length, showSystemTheme]) // 移除isOpen依赖，避免下拉菜单时重复测量

  const handleThemeChange = (theme: string) => {
    onThemeChange?.(theme)
    setIsOpen(false)
  }

  const handleDarkModeToggle = () => {
    onDarkModeChange?.(!isDarkMode)
  }

  const handleSystemTheme = () => {
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
    onDarkModeChange?.(prefersDark)
  }

  const handleMinimizeToggle = (minimize: boolean) => {
    if (minimize) {
      setIsMinimized(true)
    } else {
      setIsMinimized(false)
    }
  }

  return (
    <Card 
      ref={ref}
      className={cn(
        "backdrop-blur-xl bg-white/95 dark:bg-gray-900/95 border-white/20 shadow-2xl overflow-hidden",
        "hover:shadow-3xl",
        className
      )}
      style={{
        width: isMinimized ? '56px' : '320px',
        height: isMinimized ? '56px' : 'auto',
        borderRadius: isMinimized ? '50%' : '12px',
        transition: 'width 300ms linear, border-radius 300ms linear',
      }}
      {...props}
    >
      <CardHeader className={cn(
        "transition-all duration-500 ease-in-out",
        isMinimized 
          ? 'p-0 h-full flex items-center justify-center opacity-100' 
          : 'pb-3 opacity-100'
      )}>
        <div className={cn(
          "transition-all duration-500 ease-in-out",
          isMinimized ? 'relative' : 'flex items-center gap-2'
        )}>
          {isMinimized ? (
            <div 
              className={cn(
                "relative cursor-pointer w-8 h-8 rounded-full flex items-center justify-center",
                "transition-all duration-300 ease-out hover:scale-110 active:scale-95"
              )}
              style={{ 
                background: `linear-gradient(135deg, ${currentThemeOption.color}40, ${currentThemeOption.color}20)` 
              }}
              onClick={() => handleMinimizeToggle(false)}
              title={`当前主题: ${currentThemeOption.label} ${isDarkMode ? '(深色模式)' : '(浅色模式)'}`}
            >
              <div 
                style={{ color: currentThemeOption.color }}
                className="transition-transform duration-300 ease-out"
              >
                <Palette className="h-4 w-4" />
              </div>
              {isDarkMode && (
                <div
                  className="absolute -top-1 -right-1 w-3 h-3 rounded-full border border-white/50 animate-pulse"
                  style={{ backgroundColor: currentThemeOption.color }}
                />
              )}
            </div>
          ) : (
            <div className="animate-in fade-in-0 slide-in-from-left-2 duration-500 flex items-center gap-2 w-full">
              <div className="p-2 rounded-lg bg-gradient-to-br from-primary/20 to-accent/20 transition-all duration-300 hover:scale-105">
                <Palette className="h-4 w-4 text-primary" />
              </div>
              <div className="flex-1">
                <CardTitle className="text-base">主题设置</CardTitle>
                <CardDescription className="text-xs">个性化您的界面</CardDescription>
              </div>
              {minimizable && (
                <Button
                  variant="ghost"
                  size="sm"
                  className="h-6 w-6 p-0 text-muted-foreground hover:text-foreground transition-all duration-200 hover:scale-110 active:scale-95"
                  onClick={() => handleMinimizeToggle(true)}
                >
                  <Minimize2 className="h-3 w-3" />
                </Button>
              )}
            </div>
          )}
        </div>
      </CardHeader>
      
      <div className={cn(
        "overflow-hidden",
        isMinimized 
          ? 'opacity-0' 
          : 'opacity-100'
      )}
      style={{
        maxHeight: isMinimized 
          ? '0px' 
          : `${Math.max(contentHeight + (isOpen ? themes.length * 60 : 0), 300)}px`,
        transition: 'max-height 300ms linear, opacity 200ms linear'
      }}>
        <CardContent 
          ref={contentRef}
          className={cn(
            "space-y-4 transition-all duration-500 ease-in-out",
            isMinimized ? 'py-0' : 'animate-in fade-in-0 slide-in-from-top-2'
          )}
        >
          {/* 明暗模式切换 */}
          <div className="flex items-center justify-between p-3 rounded-xl bg-muted/50 hover:bg-muted/70 transition-all duration-300 hover:scale-[1.02]">
            <div className="flex items-center gap-3">
              <div className="p-2 rounded-lg bg-gradient-to-br from-yellow-400/20 to-blue-600/20 transition-all duration-300 hover:scale-105">
                <div className="transition-all duration-300 ease-in-out">
                  {isDarkMode ? (
                    <Moon className="h-4 w-4 text-blue-600" />
                  ) : (
                    <Sun className="h-4 w-4 text-yellow-600" />
                  )}
                </div>
              </div>
              <div>
                <div className="text-sm font-medium">外观模式</div>
                <div className="text-xs text-muted-foreground transition-all duration-300">
                  {isDarkMode ? '深色模式' : '浅色模式'}
                </div>
              </div>
            </div>
            <Button
              variant="ghost"
              size="sm"
              onClick={handleDarkModeToggle}
              className="h-8 w-14 p-0 bg-muted rounded-full relative transition-all duration-300 hover:scale-105 active:scale-95"
            >
              <div
                className={cn(
                  "absolute top-1 left-1 w-6 h-6 bg-background rounded-full shadow-md",
                  "transition-all duration-500 ease-out transform",
                  isDarkMode ? 'translate-x-7' : 'translate-x-0'
                )}
              />
            </Button>
          </div>

          {/* 主题色选择 */}
          <div className="space-y-3">
            <div className="flex items-center gap-2">
              <div className="text-sm font-medium">主题色彩</div>
              <div className="flex-1 h-px bg-border transition-all duration-300" />
            </div>
            
            {/* 当前选中的主题 */}
            <div 
              className="flex items-center justify-between p-3 rounded-xl bg-muted/50 cursor-pointer hover:bg-muted/70 transition-all duration-300 hover:scale-[1.02] active:scale-[0.98]"
              onClick={() => setIsOpen(!isOpen)}
            >
              <div className="flex items-center gap-3">
                <div 
                  className="p-2 rounded-lg transition-all duration-300 hover:scale-105" 
                  style={{ backgroundColor: `${currentThemeOption.color}20` }}
                >
                  <div style={{ color: currentThemeOption.color }}>
                    <currentThemeOption.icon className="h-4 w-4" />
                  </div>
                </div>
                <div>
                  <div className="text-sm font-medium">{currentThemeOption.label}</div>
                  <div className="text-xs text-muted-foreground">{currentThemeOption.description}</div>
                </div>
              </div>
              <ChevronDown className={cn(
                "h-4 w-4 transition-all duration-300 ease-out",
                isOpen ? 'rotate-180' : 'rotate-0'
              )} />
            </div>

            {/* 主题选项 */}
            <div className={cn(
              "overflow-hidden transition-all duration-300 ease-linear",
              isOpen ? 'opacity-100' : 'opacity-0'
            )}
            style={{
              maxHeight: isOpen ? `${themes.length * 60 + 24}px` : '0px' // 每个主题项约60px高度 + 容器padding
            }}>
              <div className="space-y-1 border rounded-xl p-2 bg-background/50 backdrop-blur-sm">
                {themes.map((theme) => {
                  const Icon = theme.icon
                  const isSelected = currentTheme === theme.name
                  
                  return (
                    <button
                      key={theme.name}
                      onClick={() => handleThemeChange(theme.name)}
                      className={cn(
                        "w-full flex items-center gap-3 p-3 rounded-lg text-left",
                        "transition-all duration-300 ease-out hover:scale-[1.02] active:scale-[0.98]",
                        isSelected 
                          ? 'bg-primary/10 border border-primary/20 shadow-sm scale-[1.02]' 
                          : 'hover:bg-muted/50'
                      )}
                    >
                      <div 
                        className="p-2 rounded-lg relative transition-all duration-300 hover:scale-110" 
                        style={{ backgroundColor: `${theme.color}20` }}
                      >
                        <div style={{ color: theme.color }}>
                          <Icon className="h-4 w-4" />
                        </div>
                        {isSelected && (
                          <div className="absolute -top-1 -right-1 w-3 h-3 bg-primary rounded-full flex items-center justify-center">
                            <Check className="h-2 w-2 text-primary-foreground" />
                          </div>
                        )}
                      </div>
                      <div className="flex-1">
                        <div className="text-sm font-medium">{theme.label}</div>
                        <div className="text-xs text-muted-foreground">{theme.description}</div>
                      </div>
                      {isSelected && (
                        <div className="w-2 h-2 bg-primary rounded-full animate-pulse" />
                      )}
                    </button>
                  )
                })}
              </div>
            </div>
          </div>

          {/* 系统主题选项 */}
          {showSystemTheme && (
            <div className="pt-2 border-t animate-in fade-in-0 slide-in-from-bottom-2 duration-500 delay-200">
              <Button
                variant="ghost"
                size="sm" 
                className="w-full justify-start gap-2 text-muted-foreground hover:text-foreground transition-all duration-300 hover:scale-[1.02] active:scale-[0.98]"
                onClick={handleSystemTheme}
              >
                <Monitor className="h-4 w-4" />
                <span className="text-xs">跟随系统</span>
              </Button>
            </div>
          )}
        </CardContent>
      </div>
    </Card>
  )
})

ThemeSwitcher.displayName = 'ThemeSwitcher'
