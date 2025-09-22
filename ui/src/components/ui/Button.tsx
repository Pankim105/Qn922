import * as React from 'react'
import { Slot } from '@radix-ui/react-slot'

import { cn } from '@/lib/utils'
import { buttonVariants, buttonPresets, type ButtonVariants } from './variants/button-variants'

/**
 * Button 组件模块
 *
 * 提供语义化按钮，支持多种视觉变体、尺寸、形状与状态；
 * 可通过 `asChild` 以保持语义/结构不变地包装任意元素。
 * 变体体系基于 `class-variance-authority`，并暴露 `buttonVariants` 与常用 `buttonPresets`。
 */

/**
 * Button 组件的属性
 * - 继承原生 `button` 属性
 * - 变体相关属性来自 `ButtonVariants`
 * - `asChild`：使用 Radix `Slot` 包裹自定义元素
 * - `loading`：加载态（自动禁用并显示加载指示）
 * - `preset`：使用预设的视觉组合（如 primary/secondary 等）
 */
export interface ButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement>,
    ButtonVariants {
  asChild?: boolean
  loading?: boolean
  preset?: keyof typeof buttonPresets
}

/**
 * Button 组件
 * - 非 asChild：渲染原生 `button`，自动处理 `disabled` 与加载占位
 * - asChild：渲染传入元素，不额外插入 DOM（保持结构/语义）
 */
export const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  (
    { 
      className, 
      variant, 
      size, 
      shape, 
      state, 
      asChild = false, 
      loading = false,
      preset,
      children,
      disabled,
      ...props 
    },
    ref,
  ) => {
    const Comp = asChild ? Slot : 'button'
    
    // 如果指定了 preset，使用预设配置
    const presetConfig = preset ? buttonPresets[preset] : undefined
    
    // 处理加载状态
    const finalState = loading ? 'loading' : state
    const isDisabled = disabled || loading

    // 当使用 asChild 时，不能添加额外的元素，需要简化结构
    if (asChild) {
      return (
        <Comp
          className={cn(
            buttonVariants({ 
              variant: variant ?? presetConfig?.variant, 
              size: size ?? presetConfig?.size, 
              shape: shape ?? (presetConfig as any)?.shape,
              state: finalState,
              className 
            })
          )}
          ref={ref}
          {...props}
        >
          {children}
        </Comp>
      )
    }

    return (
      <Comp
        className={cn(
          buttonVariants({ 
            variant: variant ?? presetConfig?.variant, 
            size: size ?? presetConfig?.size, 
            shape: shape ?? (presetConfig as any)?.shape,
            state: finalState,
            className 
          })
        )}
        ref={ref}
        disabled={isDisabled}
        {...props}
      >
        {loading && (
          <div className="absolute inset-0 flex items-center justify-center">
            <div className="h-4 w-4 animate-spin rounded-full border-2 border-current border-t-transparent" />
          </div>
        )}
        <span className={loading ? 'opacity-0' : ''}>{children}</span>
      </Comp>
    )
  },
)

Button.displayName = 'Button'

// 导出变体函数，供其他组件使用
export { buttonVariants }

