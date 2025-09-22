import * as React from 'react'
import { Slot } from '@radix-ui/react-slot'

import { cn } from '@/lib/utils'
import { cardVariants, cardPresets, type CardVariants } from './variants/card-variants'

/**
 * Card 模块
 *
 * 提供容器型卡片与常见的 Header/Title/Description/Content/Footer 分区组件；
 * 支持多种视觉变体、尺寸、圆角、交互与特效，并提供 `cardPresets` 用于快速套用常见组合。
 */

/** Card 根组件的属性 */
export interface CardProps
  extends React.HTMLAttributes<HTMLDivElement>,
    CardVariants {
  preset?: keyof typeof cardPresets
  asChild?: boolean
}

/** 卡片容器组件 */
export const Card = React.forwardRef<HTMLDivElement, CardProps>(
  (
    { 
      className, 
      variant, 
      size, 
      rounded, 
      interactive, 
      effect, 
      preset,
      asChild = false,
      children,
      ...props 
    },
    ref,
  ) => {
    const Comp = asChild ? Slot : 'div'
    
    // 如果指定了 preset，使用预设配置
    const presetConfig = preset ? cardPresets[preset] : {} as Partial<CardVariants>

    return (
      <Comp
        ref={ref}
        className={cn(
          cardVariants({
            variant: variant ?? presetConfig?.variant,
            size: size ?? presetConfig?.size,
            rounded: rounded ?? presetConfig?.rounded,
            interactive: interactive ?? presetConfig?.interactive,
            effect: effect ?? presetConfig?.effect,
            className,
          })
        )}
        {...props}
      >
        {children}
      </Comp>
    )
  },
)

Card.displayName = 'Card'

/** 卡片头部区域 */
export interface CardHeaderProps extends React.HTMLAttributes<HTMLDivElement> {}

export const CardHeader = React.forwardRef<HTMLDivElement, CardHeaderProps>(
  ({ className, ...props }, ref) => (
    <div
      ref={ref}
      className={cn('flex flex-col space-y-1.5 pb-4', className)}
      {...props}
    />
  ),
)
CardHeader.displayName = 'CardHeader'

/** 卡片标题 */
export interface CardTitleProps extends React.HTMLAttributes<HTMLHeadingElement> {}

export const CardTitle = React.forwardRef<HTMLParagraphElement, CardTitleProps>(
  ({ className, ...props }, ref) => (
    <h3
      ref={ref}
      className={cn('text-lg font-semibold leading-none tracking-tight', className)}
      {...props}
    />
  ),
)
CardTitle.displayName = 'CardTitle'

/** 卡片描述文本 */
export interface CardDescriptionProps extends React.HTMLAttributes<HTMLParagraphElement> {}

export const CardDescription = React.forwardRef<HTMLParagraphElement, CardDescriptionProps>(
  ({ className, ...props }, ref) => (
    <p
      ref={ref}
      className={cn('text-sm text-muted-foreground', className)}
      {...props}
    />
  ),
)
CardDescription.displayName = 'CardDescription'

/** 卡片内容区域 */
export interface CardContentProps extends React.HTMLAttributes<HTMLDivElement> {}

export const CardContent = React.forwardRef<HTMLDivElement, CardContentProps>(
  ({ className, ...props }, ref) => (
    <div ref={ref} className={cn('pt-0', className)} {...props} />
  ),
)
CardContent.displayName = 'CardContent'

/** 卡片底部区域 */
export interface CardFooterProps extends React.HTMLAttributes<HTMLDivElement> {}

export const CardFooter = React.forwardRef<HTMLDivElement, CardFooterProps>(
  ({ className, ...props }, ref) => (
    <div
      ref={ref}
      className={cn('flex items-center pt-4', className)}
      {...props}
    />
  ),
)
CardFooter.displayName = 'CardFooter'

/** 导出卡片变体函数 */
export { cardVariants }
