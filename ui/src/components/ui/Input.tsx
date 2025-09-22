import * as React from 'react'
import { cn } from '@/lib/utils'
import { Eye, EyeOff, Search, X, AlertCircle, CheckCircle } from 'lucide-react'

/**
 * Input 模块
 *
 * 现代输入框与衍生变体（搜索、密码）。支持多种视觉变体、尺寸与状态，
 * 并提供清除/加载/图标/密码显隐等增强能力。
 */

// 输入框变体类型
export type InputVariant = 'default' | 'filled' | 'outline' | 'ghost' | 'glass' | 'primary' | 'accent'
export type InputSize = 'sm' | 'md' | 'lg'
export type InputState = 'default' | 'error' | 'success' | 'warning'

export interface ModernInputProps extends Omit<React.InputHTMLAttributes<HTMLInputElement>, 'size'> {
  variant?: InputVariant
  size?: InputSize
  state?: InputState
  label?: string
  helperText?: string
  error?: string
  success?: string
  leftIcon?: React.ReactNode
  rightIcon?: React.ReactNode
  clearable?: boolean
  showPasswordToggle?: boolean
  loading?: boolean
  onClear?: () => void
}

/** 视觉变体到 Tailwind 类的映射 */
const inputVariants = {
  default: 'border border-border bg-background text-foreground hover:border-ring/50 focus:border-ring focus:ring-ring/20',
  filled: 'border border-muted bg-muted text-foreground hover:bg-muted/80 focus:bg-background focus:border-primary focus:ring-primary/20',
  outline: 'border-2 border-primary/20 bg-transparent text-foreground hover:border-primary/40 focus:border-primary focus:ring-primary/20',
  ghost: 'border border-transparent bg-transparent text-foreground hover:bg-muted/50 hover:border-border focus:bg-muted focus:border-primary focus:ring-primary/20',
  glass: 'border border-white/20 bg-white/50 dark:bg-gray-800/50 backdrop-blur-sm text-foreground hover:bg-white/70 dark:hover:bg-gray-800/70 focus:bg-white/80 dark:focus:bg-gray-800/80 focus:ring-primary/20',
  primary: 'border border-primary/30 bg-primary-bg-subtle text-primary hover:bg-primary-bg-subtle/80 hover:border-primary/50 focus:ring-primary/30 focus:border-primary',
  accent: 'border border-accent/30 bg-accent-bg-subtle text-accent hover:bg-accent-bg-subtle/80 hover:border-accent/50 focus:ring-accent/30 focus:border-accent',
}

/** 尺寸到高度/内边距/字号的映射 */
const inputSizes = {
  sm: 'h-8 px-3 text-xs rounded-md',
  md: 'h-10 px-3 text-sm rounded-md',
  lg: 'h-12 px-4 text-base rounded-lg',
}

/** 状态到颜色/边框的映射 */
const inputStates = {
  default: '',
  error: '!border-destructive !text-destructive focus:!ring-destructive/20 focus:!border-destructive',
  success: '!border-green-500 dark:!border-green-400 !text-green-700 dark:!text-green-300 focus:!ring-green-500/20 focus:!border-green-500',
  warning: '!border-yellow-500 dark:!border-yellow-400 !text-yellow-700 dark:!text-yellow-300 focus:!ring-yellow-500/20 focus:!border-yellow-500',
}

/** 通用输入框组件 */
export const ModernInput = React.forwardRef<HTMLInputElement, ModernInputProps>(
  ({
    className,
    variant = 'default',
    size = 'md',
    state = 'default',
    label,
    helperText,
    error,
    success,
    leftIcon,
    rightIcon,
    clearable = false,
    showPasswordToggle = false,
    loading = false,
    onClear,
    type = 'text',
    value,
    disabled,
    ...props
  }, ref) => {
    const [showPassword, setShowPassword] = React.useState(false)
    const [isFocused, setIsFocused] = React.useState(false)
    const inputId = React.useId()

    // 确定最终状态
    const finalState = error ? 'error' : success ? 'success' : state
    const hasValue = value !== undefined && value !== ''

    // 处理密码显示切换
    const togglePasswordVisibility = () => {
      setShowPassword(!showPassword)
    }

    // 处理清除
    const handleClear = () => {
      onClear?.()
    }

    // 获取状态图标
    const getStateIcon = () => {
      if (loading) {
        return <div className="animate-spin h-4 w-4 border-2 border-current border-t-transparent rounded-full" />
      }
      if (finalState === 'error') {
        return <AlertCircle className="h-4 w-4 text-destructive" />
      }
      if (finalState === 'success') {
        return <CheckCircle className="h-4 w-4 text-green-500" />
      }
      return null
    }

    const inputType = showPasswordToggle && type === 'password' 
      ? (showPassword ? 'text' : 'password')
      : type

    return (
      <div className="space-y-2">
        {/* 标签 */}
        {label && (
          <label 
            htmlFor={inputId}
            className={cn(
              'block text-sm font-medium transition-colors',
              finalState === 'error' ? 'text-destructive' : 
              finalState === 'success' ? 'text-green-600 dark:text-green-400' :
              finalState === 'warning' ? 'text-yellow-600 dark:text-yellow-400' :
              'text-foreground',
              disabled && 'opacity-50'
            )}
          >
            {label}
          </label>
        )}

        {/* 输入框容器 */}
        <div className="relative">
          {/* 左侧图标 */}
          {leftIcon && (
            <div className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground">
              {leftIcon}
            </div>
          )}

          {/* 输入框 */}
          <input
            ref={ref}
            id={inputId}
            type={inputType}
            value={value}
            disabled={disabled}
            className={cn(
              // 基础样式
              'w-full transition-all duration-200 placeholder:text-muted-foreground',
              'focus:outline-none focus:ring-2 focus:ring-offset-0',
              'disabled:cursor-not-allowed disabled:opacity-50',
              
              // 变体样式
              inputVariants[variant],
              
              // 尺寸样式
              inputSizes[size],
              
              // 状态样式
              inputStates[finalState],
              
              // 图标间距调整
              leftIcon && 'pl-10',
              (rightIcon || clearable || showPasswordToggle || getStateIcon()) && 'pr-10',
              
              // 焦点状态
              isFocused && 'ring-2',
              
              className
            )}
            onFocus={(e) => {
              setIsFocused(true)
              props.onFocus?.(e)
            }}
            onBlur={(e) => {
              setIsFocused(false)
              props.onBlur?.(e)
            }}
            {...props}
          />

          {/* 右侧图标区域 */}
          <div className="absolute right-3 top-1/2 -translate-y-1/2 flex items-center gap-1">
            {/* 状态图标 */}
            {getStateIcon()}
            
            {/* 清除按钮 */}
            {clearable && hasValue && !disabled && (
              <button
                type="button"
                onClick={handleClear}
                className="text-muted-foreground hover:text-foreground transition-colors"
              >
                <X className="h-4 w-4" />
              </button>
            )}
            
            {/* 密码显示切换 */}
            {showPasswordToggle && type === 'password' && (
              <button
                type="button"
                onClick={togglePasswordVisibility}
                className="text-muted-foreground hover:text-foreground transition-colors"
              >
                {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
              </button>
            )}
            
            {/* 自定义右侧图标 */}
            {rightIcon && !getStateIcon() && !clearable && !showPasswordToggle && (
              <div className="text-muted-foreground">
                {rightIcon}
              </div>
            )}
          </div>
        </div>

        {/* 帮助文本/错误信息 */}
        {(helperText || error || success) && (
          <p className={cn(
            'text-xs transition-colors',
            finalState === 'error' ? 'text-destructive' :
            finalState === 'success' ? 'text-green-600 dark:text-green-400' :
            finalState === 'warning' ? 'text-yellow-600 dark:text-yellow-400' :
            'text-muted-foreground'
          )}>
            {error || success || helperText}
          </p>
        )}
      </div>
    )
  }
)

ModernInput.displayName = 'ModernInput'

/** 搜索输入框（带左侧搜索图标的快捷封装） */
export const SearchInput = React.forwardRef<HTMLInputElement, Omit<ModernInputProps, 'leftIcon' | 'type'>>(
  (props, ref) => (
    <ModernInput
      ref={ref}
      type="search"
      leftIcon={<Search className="h-4 w-4" />}
      placeholder="搜索..."
      {...props}
    />
  )
)

SearchInput.displayName = 'SearchInput'

/** 密码输入框（内置显隐切换） */
export const PasswordInput = React.forwardRef<HTMLInputElement, Omit<ModernInputProps, 'type' | 'showPasswordToggle'>>(
  (props, ref) => (
    <ModernInput
      ref={ref}
      type="password"
      showPasswordToggle={true}
      {...props}
    />
  )
)

PasswordInput.displayName = 'PasswordInput'
