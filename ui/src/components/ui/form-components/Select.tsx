import * as React from 'react'
import { cn } from '@/lib/utils'
import { ChevronDown, ChevronUp, Check, AlertCircle, CheckCircle, Search, X } from 'lucide-react'

/**
 * Select 模块
 *
 * 现代选择框：提供自定义下拉（`ModernSelect`）与原生封装（`NativeSelect`）。
 * 支持搜索、清除、状态图标、左/右自定义图标等能力。
 */

// 选择框变体类型
export type SelectVariant = 'default' | 'filled' | 'outline' | 'ghost' | 'glass' | 'primary' | 'accent'
export type SelectSize = 'sm' | 'md' | 'lg'
export type SelectState = 'default' | 'error' | 'success' | 'warning'

export interface SelectOption {
  value: string
  label: string
  disabled?: boolean
  description?: string
}

export interface ModernSelectProps extends Omit<React.SelectHTMLAttributes<HTMLSelectElement>, 'size'> {
  variant?: SelectVariant
  size?: SelectSize
  state?: SelectState
  label?: string
  helperText?: string
  error?: string
  success?: string
  leftIcon?: React.ReactNode
  rightIcon?: React.ReactNode
  loading?: boolean
  searchable?: boolean
  clearable?: boolean
  placeholder?: string
  options: SelectOption[]
  value?: string
  onValueChange?: (value: string) => void
  onClear?: () => void
}

export interface NativeSelectProps extends Omit<React.SelectHTMLAttributes<HTMLSelectElement>, 'size'> {
  variant?: SelectVariant
  size?: SelectSize
  state?: SelectState
  label?: string
  helperText?: string
  error?: string
  success?: string
  leftIcon?: React.ReactNode
  loading?: boolean
  options: SelectOption[]
}

const selectVariants = {
  default: 'border border-border bg-background text-foreground hover:border-ring/50 focus:border-ring focus:ring-ring/20',
  filled: 'border border-muted bg-muted text-foreground hover:bg-muted/80 focus:bg-background focus:border-primary focus:ring-primary/20',
  outline: 'border-2 border-primary/20 bg-transparent text-foreground hover:border-primary/40 focus:border-primary focus:ring-primary/20',
  ghost: 'border border-transparent bg-transparent text-foreground hover:bg-muted/50 hover:border-border focus:bg-muted focus:border-primary focus:ring-primary/20',
  glass: 'border border-white/20 bg-white/50 dark:bg-gray-800/50 backdrop-blur-sm text-foreground hover:bg-white/70 dark:hover:bg-gray-800/70 focus:bg-white/80 dark:focus:bg-gray-800/80 focus:ring-primary/20',
  primary: 'border border-primary/30 bg-primary-bg-subtle text-primary hover:bg-primary-bg-subtle/80 hover:border-primary/50 focus:ring-primary/30 focus:border-primary',
  accent: 'border border-accent/30 bg-accent-bg-subtle text-accent hover:bg-accent-bg-subtle/80 hover:border-accent/50 focus:ring-accent/30 focus:border-accent',
}

const selectSizes = {
  sm: 'h-8 px-3 text-xs rounded-md',
  md: 'h-10 px-3 text-sm rounded-md',
  lg: 'h-12 px-4 text-base rounded-lg',
}

const selectStates = {
  default: '',
  error: '!border-destructive !text-destructive focus:!ring-destructive/20 focus:!border-destructive',
  success: '!border-green-500 dark:!border-green-400 !text-green-700 dark:!text-green-300 focus:!ring-green-500/20 focus:!border-green-500',
  warning: '!border-yellow-500 dark:!border-yellow-400 !text-yellow-700 dark:!text-yellow-300 focus:!ring-yellow-500/20 focus:!border-yellow-500',
}

// 现代化自定义选择框
export const ModernSelect = React.forwardRef<HTMLDivElement, ModernSelectProps>(
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
    loading = false,
    searchable = false,
    clearable = false,
    placeholder = '请选择...',
    options,
    value,
    onValueChange,
    onClear,
    disabled
  }, ref) => {
    const [isOpen, setIsOpen] = React.useState(false)
    const [searchQuery, setSearchQuery] = React.useState('')
    const [isFocused, setIsFocused] = React.useState(false)
    const selectId = React.useId()
    const containerRef = React.useRef<HTMLDivElement>(null)

    // 确定最终状态
    const finalState = error ? 'error' : success ? 'success' : state
    const selectedOption = options.find(option => option.value === value)

    // 过滤选项
    const filteredOptions = React.useMemo(() => {
      if (!searchable || !searchQuery) return options
      return options.filter(option => 
        option.label.toLowerCase().includes(searchQuery.toLowerCase()) ||
        option.value.toLowerCase().includes(searchQuery.toLowerCase())
      )
    }, [options, searchQuery, searchable])

    // 点击外部关闭
    React.useEffect(() => {
      const handleClickOutside = (event: MouseEvent) => {
        if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
          setIsOpen(false)
          setSearchQuery('')
        }
      }

      if (isOpen) {
        document.addEventListener('mousedown', handleClickOutside)
      }

      return () => {
        document.removeEventListener('mousedown', handleClickOutside)
      }
    }, [isOpen])

    // 选择选项
    const handleSelectOption = (option: SelectOption) => {
      if (option.disabled) return
      onValueChange?.(option.value)
      setIsOpen(false)
      setSearchQuery('')
    }

    // 清除选择
    const handleClear = (event: React.MouseEvent) => {
      event.stopPropagation()
      onClear?.()
      onValueChange?.('')
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

    return (
      <div className="space-y-2" ref={containerRef}>
        {/* 标签 */}
        {label && (
          <label 
            htmlFor={selectId}
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

        {/* 选择框容器 */}
        <div className="relative" ref={ref}>
          {/* 左侧图标 */}
          {leftIcon && (
            <div className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground z-10">
              {leftIcon}
            </div>
          )}

          {/* 选择框触发器 */}
          <div
            id={selectId}
            role="combobox"
            aria-expanded={isOpen}
            aria-haspopup="listbox"
            tabIndex={disabled ? -1 : 0}
            className={cn(
              'w-full cursor-pointer transition-all duration-200 flex items-center justify-between',
              'focus:outline-none focus:ring-2 focus:ring-offset-0',
              'disabled:cursor-not-allowed disabled:opacity-50',
              selectVariants[variant],
              selectSizes[size],
              selectStates[finalState],
              leftIcon && 'pl-10',
              isFocused && 'ring-2',
              className
            )}
            onClick={() => !disabled && setIsOpen(!isOpen)}
            onFocus={() => setIsFocused(true)}
            onBlur={() => setIsFocused(false)}
          >
            <span className={cn(
              'truncate',
              !selectedOption && 'text-muted-foreground'
            )}>
              {selectedOption ? selectedOption.label : placeholder}
            </span>
            
            {/* 右侧图标区域 */}
            <div className="flex items-center gap-1 ml-2">
              {getStateIcon()}
              
              {clearable && selectedOption && !disabled && (
                <button
                  type="button"
                  onClick={handleClear}
                  className="text-muted-foreground hover:text-foreground transition-colors"
                >
                  <X className="h-4 w-4" />
                </button>
              )}
              
              {!loading && (
                <div className="text-muted-foreground">
                  {isOpen ? (
                    <ChevronUp className="h-4 w-4" />
                  ) : (
                    <ChevronDown className="h-4 w-4" />
                  )}
                </div>
              )}
              
              {rightIcon && !getStateIcon() && !clearable && !loading && (
                <div className="text-muted-foreground">
                  {rightIcon}
                </div>
              )}
            </div>
          </div>

          {/* 下拉选项 */}
          {isOpen && (
            <div className="absolute top-full left-0 right-0 z-50 mt-1 max-h-80 overflow-auto rounded-md border border-border bg-popover shadow-lg">
              {/* 搜索框 */}
              {searchable && (
                <div className="p-2 border-b border-border">
                  <div className="relative">
                    <Search className="absolute left-2 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                    <input
                      type="text"
                      placeholder="搜索选项..."
                      value={searchQuery}
                      onChange={(e) => setSearchQuery(e.target.value)}
                      className="w-full pl-8 pr-2 py-1 text-sm bg-transparent border-none outline-none placeholder:text-muted-foreground"
                      onClick={(e) => e.stopPropagation()}
                    />
                  </div>
                </div>
              )}
              
              {/* 选项列表 */}
              <div role="listbox">
                {filteredOptions.length === 0 ? (
                  <div className="py-2 px-3 text-sm text-muted-foreground">
                    {searchQuery ? '未找到匹配的选项' : '暂无选项'}
                  </div>
                ) : (
                  filteredOptions.map((option) => (
                    <div
                      key={option.value}
                      role="option"
                      aria-selected={option.value === value}
                      className={cn(
                        'flex items-center justify-between py-2 px-3 cursor-pointer transition-colors text-sm',
                        'hover:bg-muted/50 focus:bg-muted/50',
                        option.disabled && 'opacity-50 cursor-not-allowed',
                        option.value === value && 'bg-muted font-medium'
                      )}
                      onClick={() => handleSelectOption(option)}
                    >
                      <div className="flex-1 min-w-0">
                        <div className="truncate">{option.label}</div>
                        {option.description && (
                          <div className="text-xs text-muted-foreground truncate">
                            {option.description}
                          </div>
                        )}
                      </div>
                      {option.value === value && (
                        <Check className="h-4 w-4 text-primary flex-shrink-0 ml-2" />
                      )}
                    </div>
                  ))
                )}
              </div>
            </div>
          )}
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

ModernSelect.displayName = 'ModernSelect'

// 原生选择框（简化版本）
export const NativeSelect = React.forwardRef<HTMLSelectElement, NativeSelectProps>(
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
    loading = false,
    options,
    disabled,
    ...props
  }, ref) => {
    const selectId = React.useId()
    const finalState = error ? 'error' : success ? 'success' : state

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

    return (
      <div className="space-y-2">
        {label && (
          <label 
            htmlFor={selectId}
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

        <div className="relative">
          {leftIcon && (
            <div className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground z-10">
              {leftIcon}
            </div>
          )}

          <select
            ref={ref}
            id={selectId}
            disabled={disabled}
            className={cn(
              'w-full transition-all duration-200 appearance-none',
              'focus:outline-none focus:ring-2 focus:ring-offset-0',
              'disabled:cursor-not-allowed disabled:opacity-50',
              selectVariants[variant],
              selectSizes[size],
              selectStates[finalState],
              leftIcon && 'pl-10',
              'pr-10',
              className
            )}
            {...props}
          >
            {options.map((option) => (
              <option 
                key={option.value} 
                value={option.value}
                disabled={option.disabled}
              >
                {option.label}
              </option>
            ))}
          </select>

          <div className="absolute right-3 top-1/2 -translate-y-1/2 flex items-center gap-1 pointer-events-none">
            {getStateIcon()}
            {!loading && <ChevronDown className="h-4 w-4 text-muted-foreground" />}
          </div>
        </div>

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

NativeSelect.displayName = 'NativeSelect'
