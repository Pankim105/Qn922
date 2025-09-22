import * as React from 'react'
import { cn } from '@/lib/utils'
import { Check, Minus } from 'lucide-react'

/**
 * Checkbox 模块
 *
 * 现代复选框与复选组：支持变体/尺寸/标签/描述/半选/错误与帮助文案。
 */

export type CheckboxVariant = 'default' | 'primary' | 'accent' | 'success' | 'warning' | 'destructive'
export type CheckboxSize = 'sm' | 'md' | 'lg'

export interface ModernCheckboxProps extends Omit<React.InputHTMLAttributes<HTMLInputElement>, 'size'> {
  variant?: CheckboxVariant
  size?: CheckboxSize
  label?: string
  description?: string
  indeterminate?: boolean
  error?: string
  helperText?: string
}

const checkboxVariants = {
  default: {
    base: 'border-border data-[state=checked]:bg-primary data-[state=checked]:border-primary data-[state=checked]:text-primary-foreground',
    focus: 'focus-visible:ring-ring',
    hover: 'hover:border-ring/50',
  },
  primary: {
    base: 'border-primary/50 data-[state=checked]:bg-primary data-[state=checked]:border-primary data-[state=checked]:text-primary-foreground',
    focus: 'focus-visible:ring-primary',
    hover: 'hover:border-primary/70',
  },
  accent: {
    base: 'border-accent/50 data-[state=checked]:bg-accent data-[state=checked]:border-accent data-[state=checked]:text-accent-foreground',
    focus: 'focus-visible:ring-accent',
    hover: 'hover:border-accent/70',
  },
  success: {
    base: 'border-green-500/50 data-[state=checked]:bg-green-500 data-[state=checked]:border-green-500 data-[state=checked]:text-white',
    focus: 'focus-visible:ring-green-500',
    hover: 'hover:border-green-500/70',
  },
  warning: {
    base: 'border-yellow-500/50 data-[state=checked]:bg-yellow-500 data-[state=checked]:border-yellow-500 data-[state=checked]:text-white',
    focus: 'focus-visible:ring-yellow-500',
    hover: 'hover:border-yellow-500/70',
  },
  destructive: {
    base: 'border-destructive/50 data-[state=checked]:bg-destructive data-[state=checked]:border-destructive data-[state=checked]:text-destructive-foreground',
    focus: 'focus-visible:ring-destructive',
    hover: 'hover:border-destructive/70',
  },
}

const checkboxSizes = {
  sm: { box: 'h-3 w-3', icon: 'h-2.5 w-2.5', text: 'text-xs' },
  md: { box: 'h-4 w-4', icon: 'h-3 w-3', text: 'text-sm' },
  lg: { box: 'h-5 w-5', icon: 'h-4 w-4', text: 'text-base' },
}

export const ModernCheckbox = React.forwardRef<HTMLInputElement, ModernCheckboxProps>(
  ({ className, variant = 'default', size = 'md', label, description, indeterminate = false, error, helperText, checked, disabled, ...props }, ref) => {
    const [isChecked, setIsChecked] = React.useState(checked || false)
    const [isIndeterminate, setIsIndeterminate] = React.useState(indeterminate)
    const inputRef = React.useRef<HTMLInputElement>(null)
    const inputId = React.useId()

    React.useImperativeHandle(ref, () => inputRef.current as HTMLInputElement)

    React.useEffect(() => {
      if (inputRef.current) inputRef.current.indeterminate = isIndeterminate
    }, [isIndeterminate])

    React.useEffect(() => setIsChecked(!!checked), [checked])
    React.useEffect(() => setIsIndeterminate(!!indeterminate), [indeterminate])

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
      const next = e.target.checked
      setIsChecked(next)
      setIsIndeterminate(false)
      props.onChange?.(e)
    }

    const variantStyles = checkboxVariants[variant]
    const sizeStyles = checkboxSizes[size]
    const hasError = Boolean(error)

    return (
      <div className="space-y-2">
        <div
          className={cn('flex items-start gap-3 cursor-pointer', disabled && 'cursor-not-allowed')}
          onClick={() => !disabled && inputRef.current?.click()}
          onMouseDown={(e) => !disabled && e.preventDefault()}
        >
          <div className="relative flex items-center">
            <input
              ref={inputRef}
              id={inputId}
              type="checkbox"
              checked={isChecked}
              disabled={disabled}
              className="sr-only peer"
              onChange={handleChange}
              {...props}
            />
            <div
              className={cn(
                'relative flex items-center justify-center rounded border-2 bg-background transition-all duration-200',
                'peer-focus-visible:outline-none peer-focus-visible:ring-2 peer-focus-visible:ring-offset-2',
                'peer-disabled:cursor-not-allowed peer-disabled:opacity-50',
                sizeStyles.box,
                variantStyles.base,
                variantStyles.focus,
                !disabled && variantStyles.hover,
                hasError && '!border-destructive',
                !disabled && 'cursor-pointer',
                className,
              )}
              data-state={isChecked ? 'checked' : 'unchecked'}
            >
              {isChecked && !isIndeterminate && <Check className={cn('transition-all duration-200 text-current', sizeStyles.icon)} />}
              {isIndeterminate && <Minus className={cn('transition-all duration-200 text-current', sizeStyles.icon)} />}
            </div>
          </div>

          {(label || description) && (
            <div className="flex-1 space-y-1">
              {label && (
                <label
                  htmlFor={inputId}
                  className={cn('font-medium transition-colors', sizeStyles.text, hasError ? 'text-destructive' : 'text-foreground', disabled && 'cursor-not-allowed opacity-50')}
                >
                  {label}
                </label>
              )}
              {description && (
                <p className={cn('text-muted-foreground', size === 'sm' ? 'text-xs' : 'text-sm', disabled && 'opacity-50')}>{description}</p>
              )}
            </div>
          )}
        </div>

        {(error || helperText) && <p className={cn('text-xs transition-colors', hasError ? 'text-destructive' : 'text-muted-foreground')}>{error || helperText}</p>}
      </div>
    )
  },
)

ModernCheckbox.displayName = 'ModernCheckbox'

export interface CheckboxGroupOption {
  value: string
  label: string
  description?: string
  disabled?: boolean
}

export interface CheckboxGroupProps {
  options: CheckboxGroupOption[]
  value?: string[]
  defaultValue?: string[]
  onChange?: (value: string[]) => void
  variant?: CheckboxVariant
  size?: CheckboxSize
  label?: string
  description?: string
  error?: string
  helperText?: string
  disabled?: boolean
  className?: string
}

export const CheckboxGroup = React.forwardRef<HTMLDivElement, CheckboxGroupProps>(
  ({ options, value, defaultValue = [], onChange, variant = 'default', size = 'md', label, description, error, helperText, disabled = false, className }, ref) => {
    const [selectedValues, setSelectedValues] = React.useState<string[]>(value || defaultValue)

    React.useEffect(() => {
      if (value) setSelectedValues(value)
    }, [value])

    const handleOptionChange = (optionValue: string, checked: boolean) => {
      const newValues = checked ? [...selectedValues, optionValue] : selectedValues.filter((v) => v !== optionValue)
      setSelectedValues(newValues)
      onChange?.(newValues)
    }

    return (
      <div ref={ref} className={cn('space-y-4', className)}>
        {(label || description) && (
          <div className="space-y-1">
            {label && <div className={cn('font-medium', size === 'sm' ? 'text-sm' : size === 'lg' ? 'text-base' : 'text-sm', error ? 'text-destructive' : 'text-foreground')}>{label}</div>}
            {description && <p className="text-sm text-muted-foreground">{description}</p>}
          </div>
        )}
        <div className="space-y-3">
          {options.map((option) => (
            <ModernCheckbox
              key={option.value}
              variant={variant}
              size={size}
              label={option.label}
              description={option.description}
              checked={selectedValues.includes(option.value)}
              disabled={disabled || option.disabled}
              onChange={(e) => handleOptionChange(option.value, e.target.checked)}
            />
          ))}
        </div>
        {(error || helperText) && <p className={cn('text-xs transition-colors', error ? 'text-destructive' : 'text-muted-foreground')}>{error || helperText}</p>}
      </div>
    )
  },
)

CheckboxGroup.displayName = 'CheckboxGroup'


