import * as React from 'react'
import { cn } from '@/lib/utils'
import { AlertCircle, CheckCircle, Copy, Maximize2, Minimize2 } from 'lucide-react'

/**
 * Textarea 模块
 *
 * 现代文本域与富文本增强：支持自动高度、字数统计、复制、展开；
 * `RichTextarea` 额外提供工具栏/Markdown 预览占位。
 */

// 文本区域变体类型
export type TextareaVariant = 'default' | 'filled' | 'outline' | 'ghost' | 'glass' | 'primary' | 'accent'
export type TextareaSize = 'sm' | 'md' | 'lg'
export type TextareaState = 'default' | 'error' | 'success' | 'warning'
export type ResizeMode = 'none' | 'vertical' | 'horizontal' | 'both'

export interface ModernTextareaProps extends Omit<React.TextareaHTMLAttributes<HTMLTextAreaElement>, 'size'> {
  variant?: TextareaVariant
  size?: TextareaSize
  state?: TextareaState
  label?: string
  helperText?: string
  error?: string
  success?: string
  loading?: boolean
  resize?: ResizeMode
  autoResize?: boolean
  showWordCount?: boolean
  maxLength?: number
  minRows?: number
  maxRows?: number
  copyable?: boolean
  expandable?: boolean
}

export interface RichTextareaProps extends ModernTextareaProps {
  formatting?: boolean
  toolbar?: boolean
  preview?: boolean
  markdownMode?: boolean
  onFormatChange?: (format: string) => void
}

const textareaVariants = {
  default: 'border border-border bg-background text-foreground hover:border-ring/50 focus:border-ring focus:ring-ring/20',
  filled: 'border border-muted bg-muted text-foreground hover:bg-muted/80 focus:bg-background focus:border-primary focus:ring-primary/20',
  outline: 'border-2 border-primary/20 bg-transparent text-foreground hover:border-primary/40 focus:border-primary focus:ring-primary/20',
  ghost: 'border border-transparent bg-transparent text-foreground hover:bg-muted/50 hover:border-border focus:bg-muted focus:border-primary focus:ring-primary/20',
  glass: 'border border-white/20 bg-white/50 dark:bg-gray-800/50 backdrop-blur-sm text-foreground hover:bg-white/70 dark:hover:bg-gray-800/70 focus:bg-white/80 dark:focus:bg-gray-800/80 focus:ring-primary/20',
  primary: 'border border-primary/30 bg-primary-bg-subtle text-primary hover:bg-primary-bg-subtle/80 hover:border-primary/50 focus:ring-primary/30 focus:border-primary',
  accent: 'border border-accent/30 bg-accent-bg-subtle text-accent hover:bg-accent-bg-subtle/80 hover:border-accent/50 focus:ring-accent/30 focus:border-accent',
}

const textareaSizes = {
  sm: 'min-h-[60px] px-3 py-2 text-xs rounded-md',
  md: 'min-h-[80px] px-3 py-2 text-sm rounded-md',
  lg: 'min-h-[100px] px-4 py-3 text-base rounded-lg',
}

const textareaStates = {
  default: '',
  error: '!border-destructive !text-destructive focus:!ring-destructive/20 focus:!border-destructive',
  success: '!border-green-500 dark:!border-green-400 !text-green-700 dark:!text-green-300 focus:!ring-green-500/20 focus:!border-green-500',
  warning: '!border-yellow-500 dark:!border-yellow-400 !text-yellow-700 dark:!text-yellow-300 focus:!ring-yellow-500/20 focus:!border-yellow-500',
}

const resizeModes = {
  none: 'resize-none',
  vertical: 'resize-y',
  horizontal: 'resize-x',
  both: 'resize',
}

// 现代化文本区域
export const ModernTextarea = React.forwardRef<HTMLTextAreaElement, ModernTextareaProps>(
  ({
    className,
    variant = 'default',
    size = 'md',
    state = 'default',
    label,
    helperText,
    error,
    success,
    loading = false,
    resize = 'vertical',
    autoResize = false,
    showWordCount = false,
    maxLength,
    minRows = 3,
    maxRows = 10,
    copyable = false,
    expandable = false,
    disabled,
    value,
    onChange,
    ...props
  }, ref) => {
    const [isFocused, setIsFocused] = React.useState(false)
    const [isExpanded, setIsExpanded] = React.useState(false)
    const [wordCount, setWordCount] = React.useState(0)
    const textareaRef = React.useRef<HTMLTextAreaElement>(null)
    const textareaId = React.useId()

    // 合并 ref
    React.useImperativeHandle(ref, () => textareaRef.current!)

    // 确定最终状态
    const finalState = error ? 'error' : success ? 'success' : state

    // 计算字数
    React.useEffect(() => {
      if (value && typeof value === 'string') {
        setWordCount(value.length)
      } else {
        setWordCount(0)
      }
    }, [value])

    // 自动调整高度
    const adjustHeight = React.useCallback(() => {
      const textarea = textareaRef.current
      if (!textarea || !autoResize) return

      textarea.style.height = 'auto'
      const scrollHeight = textarea.scrollHeight
      const minHeight = minRows * 24 // 假设行高为24px
      const maxHeight = maxRows * 24

      const newHeight = Math.min(Math.max(scrollHeight, minHeight), maxHeight)
      textarea.style.height = `${newHeight}px`
    }, [autoResize, minRows, maxRows])

    // 处理值变化
    const handleChange = (event: React.ChangeEvent<HTMLTextAreaElement>) => {
      onChange?.(event)
      adjustHeight()
    }

    // 初始化高度调整
    React.useEffect(() => {
      adjustHeight()
    }, [adjustHeight, value])

    // 复制内容
    const handleCopy = async () => {
      if (value && typeof value === 'string') {
        try {
          await navigator.clipboard.writeText(value)
        } catch (err) {
          console.error('Failed to copy text:', err)
        }
      }
    }

    // 切换展开状态
    const toggleExpanded = () => {
      setIsExpanded(!isExpanded)
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
      <div className="space-y-2">
        {/* 标签和工具栏 */}
        <div className="flex items-center justify-between">
          {label && (
            <label 
              htmlFor={textareaId}
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

          {/* 工具栏 */}
          <div className="flex items-center gap-1">
            {copyable && value && (
              <button
                type="button"
                onClick={handleCopy}
                className="p-1 text-muted-foreground hover:text-foreground transition-colors rounded"
                title="复制内容"
              >
                <Copy className="h-4 w-4" />
              </button>
            )}
            
            {expandable && (
              <button
                type="button"
                onClick={toggleExpanded}
                className="p-1 text-muted-foreground hover:text-foreground transition-colors rounded"
                title={isExpanded ? '收起' : '展开'}
              >
                {isExpanded ? (
                  <Minimize2 className="h-4 w-4" />
                ) : (
                  <Maximize2 className="h-4 w-4" />
                )}
              </button>
            )}
          </div>
        </div>

        {/* 文本区域容器 */}
        <div className="relative">
          <textarea
            ref={textareaRef}
            id={textareaId}
            value={value}
            disabled={disabled}
            maxLength={maxLength}
            rows={isExpanded ? maxRows : minRows}
            className={cn(
              // 基础样式
              'w-full transition-all duration-200 placeholder:text-muted-foreground',
              'focus:outline-none focus:ring-2 focus:ring-offset-0',
              'disabled:cursor-not-allowed disabled:opacity-50',
              
              // 变体样式
              textareaVariants[variant],
              
              // 尺寸样式
              textareaSizes[size],
              
              // 状态样式
              textareaStates[finalState],
              
              // 调整大小模式
              !autoResize && resizeModes[resize],
              autoResize && 'resize-none',
              
              // 焦点状态
              isFocused && 'ring-2',
              
              // 展开状态下的固定高度
              isExpanded && 'min-h-[200px]',
              
              className
            )}
            onChange={handleChange}
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

          {/* 右下角状态图标 */}
          {(getStateIcon() || showWordCount) && (
            <div className="absolute bottom-2 right-2 flex items-center gap-2">
              {getStateIcon()}
              {showWordCount && (
                <div className={cn(
                  'text-xs text-muted-foreground',
                  maxLength && wordCount > maxLength * 0.9 && 'text-yellow-600',
                  maxLength && wordCount >= maxLength && 'text-destructive'
                )}>
                  {wordCount}{maxLength && `/${maxLength}`}
                </div>
              )}
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

ModernTextarea.displayName = 'ModernTextarea'

// 富文本编辑器（增强版本）
export const RichTextarea = React.forwardRef<HTMLTextAreaElement, RichTextareaProps>(
  ({
    formatting = false,
    toolbar = false,
    preview = false,
    markdownMode = false,
    onFormatChange,
    ...props
  }, ref) => {
    const [showPreview, setShowPreview] = React.useState(false)
    const [selectedFormat, setSelectedFormat] = React.useState('')

    // 格式化文本
    const applyFormat = (format: string) => {
      setSelectedFormat(format)
      onFormatChange?.(format)
    }

    // 工具栏按钮
    const formatButtons = [
      { label: '粗体', format: 'bold', icon: 'B', shortcut: 'Ctrl+B' },
      { label: '斜体', format: 'italic', icon: 'I', shortcut: 'Ctrl+I' },
      { label: '代码', format: 'code', icon: '</>', shortcut: 'Ctrl+`' },
      { label: '引用', format: 'quote', icon: '"', shortcut: 'Ctrl+Shift+.' },
      { label: '链接', format: 'link', icon: '🔗', shortcut: 'Ctrl+K' },
    ]

    return (
      <div className="space-y-2">
        {/* 工具栏 */}
        {toolbar && (
          <div className="flex items-center gap-1 p-2 border border-border rounded-md bg-muted/30">
            {formatButtons.map((button) => (
              <button
                key={button.format}
                type="button"
                onClick={() => applyFormat(button.format)}
                className={cn(
                  'px-2 py-1 text-sm rounded transition-colors',
                  'hover:bg-muted/50',
                  selectedFormat === button.format && 'bg-muted text-foreground'
                )}
                title={`${button.label} (${button.shortcut})`}
              >
                {button.icon}
              </button>
            ))}
            
            <div className="h-4 w-px bg-border mx-1" />
            
            {markdownMode && (
              <button
                type="button"
                onClick={() => setShowPreview(!showPreview)}
                className={cn(
                  'px-2 py-1 text-sm rounded transition-colors',
                  'hover:bg-muted/50',
                  showPreview && 'bg-muted text-foreground'
                )}
                title="预览"
              >
                👁️
              </button>
            )}
          </div>
        )}

        {/* 编辑器区域 */}
        <div className="relative">
          {preview && showPreview ? (
            // 预览模式
            <div className={cn(
              'w-full min-h-[80px] p-3 text-sm rounded-md',
              'border border-border bg-background text-foreground',
              'prose prose-sm max-w-none'
            )}>
              <div className="text-muted-foreground">
                预览模式 - 请实现markdown渲染逻辑
              </div>
            </div>
          ) : (
            // 编辑模式
            <ModernTextarea
              ref={ref}
              {...props}
              placeholder={markdownMode ? '支持 Markdown 语法...' : props.placeholder}
            />
          )}
        </div>

        {/* Markdown 帮助提示 */}
        {markdownMode && !showPreview && (
          <div className="text-xs text-muted-foreground">
            支持 Markdown 语法：**粗体**、*斜体*、`代码`、&gt; 引用、[链接](url)
          </div>
        )}
      </div>
    )
  }
)

RichTextarea.displayName = 'RichTextarea'
