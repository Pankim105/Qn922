import * as React from 'react'
import { cn } from '@/lib/utils'

/**
 * Radio 模块
 *
 * 现代单选框：支持尺寸/变体/错误/帮助文本，配合 `name` 属性可组成单选组。
 */

export type RadioVariant = 'default' | 'primary' | 'accent' | 'success' | 'warning' | 'destructive'
export type RadioSize = 'sm' | 'md' | 'lg'

export interface ModernRadioProps extends Omit<React.InputHTMLAttributes<HTMLInputElement>, 'size'> {
  variant?: RadioVariant
  size?: RadioSize
  label?: string
  description?: string
  error?: string
  helperText?: string
}

const radioVariants = {
  default: { base: 'border-border data-[state=checked]:border-primary', dot: 'bg-primary', focus: 'focus-visible:ring-ring', hover: 'hover:border-ring/50' },
  primary: { base: 'border-primary/50 data-[state=checked]:border-primary', dot: 'bg-primary', focus: 'focus-visible:ring-primary', hover: 'hover:border-primary/70' },
  accent: { base: 'border-accent/50 data-[state=checked]:border-accent', dot: 'bg-accent', focus: 'focus-visible:ring-accent', hover: 'hover:border-accent/70' },
  success: { base: 'border-green-500/50 data-[state=checked]:border-green-500', dot: 'bg-green-500', focus: 'focus-visible:ring-green-500', hover: 'hover:border-green-500/70' },
  warning: { base: 'border-yellow-500/50 data-[state=checked]:border-yellow-500', dot: 'bg-yellow-500', focus: 'focus-visible:ring-yellow-500', hover: 'hover:border-yellow-500/70' },
  destructive: { base: 'border-destructive/50 data-[state=checked]:border-destructive', dot: 'bg-destructive', focus: 'focus-visible:ring-destructive', hover: 'hover:border-destructive/70' },
}

const radioSizes = {
  sm: { outer: 'h-3 w-3', inner: 'h-1.5 w-1.5', text: 'text-xs' },
  md: { outer: 'h-4 w-4', inner: 'h-2 w-2', text: 'text-sm' },
  lg: { outer: 'h-5 w-5', inner: 'h-2.5 w-2.5', text: 'text-base' },
}

export const ModernRadio = React.forwardRef<HTMLInputElement, ModernRadioProps>(
  ({ className, variant = 'default', size = 'md', label, description, error, helperText, checked, disabled, ...props }, ref) => {
    const inputId = React.useId()
    const variantStyles = radioVariants[variant]
    const sizeStyles = radioSizes[size]
    const hasError = Boolean(error)
    const inputRef = React.useRef<HTMLInputElement>(null)

    React.useImperativeHandle(ref, () => inputRef.current as HTMLInputElement)

    return (
      <div className="space-y-2">
        <div className={cn('flex items-start gap-3 cursor-pointer', disabled && 'cursor-not-allowed')} onClick={() => !disabled && inputRef.current?.click()} onMouseDown={(e) => !disabled && e.preventDefault()}>
          <div className="relative flex items-center">
            <input ref={inputRef} id={inputId} type="radio" checked={checked} disabled={disabled} className="sr-only peer" onChange={props.onChange} {...props} />
            <div className={cn('relative flex items-center justify-center rounded-full border-2 bg-background transition-all duration-200', 'peer-focus-visible:outline-none peer-focus-visible:ring-2 peer-focus-visible:ring-offset-2', 'peer-disabled:cursor-not-allowed peer-disabled:opacity-50', sizeStyles.outer, variantStyles.base, variantStyles.focus, !disabled && variantStyles.hover, hasError && '!border-destructive', !disabled && 'cursor-pointer', className)} data-state={checked ? 'checked' : 'unchecked'}>
              <div className={cn('rounded-full transition-all duration-200', sizeStyles.inner, checked ? 'scale-100 opacity-100' : 'scale-0 opacity-0', hasError ? 'bg-destructive' : variantStyles.dot)} />
            </div>
          </div>
          {(label || description) && (
            <div className="flex-1 space-y-1">
              {label && <label htmlFor={inputId} className={cn('font-medium transition-colors', sizeStyles.text, hasError ? 'text-destructive' : 'text-foreground', disabled && 'cursor-not-allowed opacity-50')}>{label}</label>}
              {description && <p className={cn('text-muted-foreground', size === 'sm' ? 'text-xs' : 'text-sm', disabled && 'opacity-50')}>{description}</p>}
            </div>
          )}
        </div>
        {(error || helperText) && <p className={cn('text-xs transition-colors', hasError ? 'text-destructive' : 'text-muted-foreground')}>{error || helperText}</p>}
      </div>
    )
  },
)

ModernRadio.displayName = 'ModernRadio'


