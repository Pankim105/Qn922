import * as React from 'react'
import { cn } from '@/lib/utils'

/**
 * Switch 模块
 *
 * 轻量开关：受控组件，通过 `checked` 与 `onCheckedChange` 控制状态。
 */

export interface SwitchProps extends React.InputHTMLAttributes<HTMLInputElement> {
  checked?: boolean
  onCheckedChange?: (checked: boolean) => void
  label?: string
}

export const Switch = React.forwardRef<HTMLInputElement, SwitchProps>(({ className, checked, onCheckedChange, label, ...props }, ref) => {
  const id = React.useId()
  return (
    <div className="flex items-center gap-2">
      <input type="checkbox" id={id} ref={ref} checked={checked} onChange={(e) => onCheckedChange?.(e.target.checked)} className="sr-only" {...props} />
      <div className={cn('relative inline-flex h-6 w-11 items-center rounded-full transition-colors duration-200 cursor-pointer', checked ? 'bg-primary' : 'bg-gray-200 dark:bg-gray-700', className)} onClick={() => onCheckedChange?.(!checked)}>
        <div className={cn('inline-block h-4 w-4 transform rounded-full bg-white transition-transform duration-200 shadow-sm', checked ? 'translate-x-6' : 'translate-x-1')} />
      </div>
      {label && (
        <label htmlFor={id} className="text-sm text-foreground">
          {label}
        </label>
      )}
    </div>
  )
})

Switch.displayName = 'Switch'


