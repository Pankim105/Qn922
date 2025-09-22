import React, { useRef } from 'react'
import { cva, type VariantProps } from 'class-variance-authority'
import { cn } from '@/lib/utils'
import WaterRipple from '@/components/visuals/WaterRipple'

/**
 * WaterGlassCard 模块
 *
 * 集成水波纹视觉效果与玻璃拟态的卡片容器：
 * - 通过 `themeColor` 响应主题色，支持透明度调节
 * - 可选择是否启用水波纹（`showWaterRipple`）
 * - 通过 CVA 暴露 variant/size/rounded/shadow/interactive/glassEffect
 */

const waterGlassCardVariants = cva(
  'relative overflow-hidden transition-all duration-300 ease-out group transform-gpu',
  {
    variants: {
      variant: {
        default: 'backdrop-blur-md border border-white/20 hover:border-white/30 hover:shadow-xl',
        elevated: 'backdrop-blur-lg border border-white/30 shadow-2xl transform-gpu hover:shadow-3xl',
        primary: 'backdrop-blur-md border border-white/25 ring-1 ring-white/20 hover:ring-white/30',
        accent: 'backdrop-blur-sm border border-white/30 shadow-lg hover:shadow-xl',
        glass: 'backdrop-blur-xl border border-white/40 bg-white/5 hover:bg-white/8',
        gradient: 'backdrop-blur-md border border-white/20 bg-gradient-to-br from-white/10 via-white/5 to-transparent hover:from-white/15',
        outline: 'backdrop-blur-sm border-2 border-white/40 bg-transparent shadow-none hover:border-white/60',
        ghost: 'backdrop-blur-sm border border-white/10 bg-white/2 hover:bg-white/5',
        destructive: 'backdrop-blur-md border border-red-400/30 bg-red-500/5 hover:border-red-400/50',
        success: 'backdrop-blur-md border border-green-400/30 bg-green-500/5 hover:border-green-400/50',
        warning: 'backdrop-blur-md border border-yellow-400/30 bg-yellow-500/5 hover:border-yellow-400/50',
        info: 'backdrop-blur-md border border-blue-400/30 bg-blue-500/5 hover:border-blue-400/50',
      },
      size: {
        sm: 'min-h-[120px] p-3',
        md: 'min-h-[180px] p-4',
        lg: 'min-h-[240px] p-6',
        xl: 'min-h-[320px] p-8',
      },
      rounded: {
        none: 'rounded-none',
        sm: 'rounded-sm',
        md: 'rounded-md',
        lg: 'rounded-lg',
        xl: 'rounded-xl',
        '2xl': 'rounded-2xl',
        '3xl': 'rounded-3xl',
        full: 'rounded-full',
      },
      shadow: {
        none: 'shadow-none',
        sm: 'shadow-sm',
        md: 'shadow-md',
        lg: 'shadow-lg',
        xl: 'shadow-xl',
        '2xl': 'shadow-2xl',
      },
      interactive: {
        none: '',
        hover: 'hover:shadow-xl cursor-pointer',
        clickable: 'hover:shadow-xl cursor-pointer',
        pressable: 'hover:shadow-xl cursor-pointer',
      },
      glassEffect: {
        light: 'bg-white/5',
        medium: 'bg-white/10',
        heavy: 'bg-white/15',
        ultra: 'bg-white/20',
      }
    },
    defaultVariants: {
      variant: 'default',
      size: 'md',
      rounded: 'xl',
      shadow: 'lg',
      interactive: 'none',
      glassEffect: 'medium',
    },
  }
)

/** 卡片属性 */
interface WaterGlassCardProps
  extends React.HTMLAttributes<HTMLDivElement>,
    VariantProps<typeof waterGlassCardVariants> {
  themeColor?: string
  showWaterRipple?: boolean
  waterOpacity?: number
  glassOpacity?: number
  height?: number | string
  width?: number | string
}

/** WaterGlassCard 主组件 */
const WaterGlassCard = React.forwardRef<HTMLDivElement, WaterGlassCardProps>(
  ({
    className,
    variant,
    size,
    rounded,
    shadow,
    interactive,
    glassEffect,
    themeColor = '#3b82f6',
    showWaterRipple = true,
    waterOpacity = 0.4,
    glassOpacity = 0.08,
    height,
    width,
    children,
    style,
    ...props
  }, ref) => {
    const containerRef = useRef<HTMLDivElement>(null)

    // 将任意 CSS 颜色字符串应用透明度
    const withOpacity = (color: string, alpha: number): string => {
      const trimmed = color.trim()
      if (trimmed.startsWith('hsl(')) {
        return trimmed.replace(/^hsl\((.*)\)$/i, (_m, inner) => `hsl(${inner} / ${alpha})`)
      }
      if (trimmed.startsWith('hsla(')) {
        return trimmed.replace(/^hsla\((.*)\)$/i, (_m, inner) => `hsla(${inner.split(',').slice(0,3).join(',')}, ${alpha})`)
      }
      if (trimmed.startsWith('var(')) {
        return `hsl(${trimmed} / ${alpha})`
      }
      if (trimmed.startsWith('rgb(')) {
        return trimmed.replace(/^rgb\((.*)\)$/i, (_m, inner) => `rgba(${inner}, ${alpha})`)
      }
      if (trimmed.startsWith('rgba(')) {
        return trimmed.replace(/^rgba\((.*)\)$/i, (_m, inner) => {
          const parts = inner.split(',').map((segment: string) => segment.trim())
          return `rgba(${parts.slice(0,3).join(', ')}, ${alpha})`
        })
      }
      if (trimmed.startsWith('#')) {
        if (trimmed.length === 7) {
          const a = Math.round(alpha * 255).toString(16).padStart(2, '0')
          return `${trimmed}${a}`
        }
      }
      return trimmed
    }

    const cardStyle = {
      height: typeof height === 'number' ? `${height}px` : height,
      width: typeof width === 'number' ? `${width}px` : width,
      ...style,
    }

     return (
       <div
         ref={containerRef}
         className={cn(
           waterGlassCardVariants({ variant, size, rounded, shadow, interactive, glassEffect }), 
           "transform-gpu perspective-1000",
           className
         )}
         style={{
           ...cardStyle,
           transform: 'translateZ(0)', // 硬件加速
         }}
         {...props}
       >
         {/* 主题色背景层 */}
         <div 
           className="absolute inset-0 rounded-[inherit] transition-all duration-300 ease-out will-change-transform overflow-hidden"
           style={{ 
             background: variant === 'gradient' 
               ? `linear-gradient(135deg, ${withOpacity(themeColor, 0.53)} 0%, ${withOpacity(themeColor, 0.27)} 50%, transparent 100%)`
               : themeColor,
             opacity: variant === 'gradient' ? 1 : waterOpacity,
             zIndex: 1,
             transform: 'translateZ(0)', // 硬件加速，避免scale变换
           }}
         />

         {/* 玻璃磨砂效果层 */}
         <div 
           className="absolute inset-0 backdrop-blur-md pointer-events-none rounded-[inherit] transition-all duration-300 ease-out will-change-transform group-hover:backdrop-blur-lg overflow-hidden"
           style={{ 
             backgroundColor: `rgba(255, 255, 255, ${glassOpacity})`,
             zIndex: 2,
             mixBlendMode: 'overlay',
             transform: 'translateZ(0)', // 硬件加速
           }}
         />
         
         {/* 边框高光效果 */}
         <div 
           className="absolute inset-0 rounded-[inherit] pointer-events-none transition-all duration-300 ease-out will-change-transform group-hover:opacity-60 overflow-hidden"
           style={{ 
             background: `linear-gradient(135deg, rgba(255,255,255,0.2) 0%, transparent 30%, transparent 70%, rgba(255,255,255,0.1) 100%)`,
             zIndex: 3,
             transform: 'translateZ(0)', // 硬件加速
           }}
         />
         
         {/* 精细边框光晕 */}
         <div 
           className="absolute inset-0 rounded-[inherit] pointer-events-none transition-all duration-300 ease-out will-change-transform group-hover:opacity-0 overflow-hidden"
           style={{ 
             background: `linear-gradient(to right, rgba(255,255,255,0.1), transparent 20%, transparent 80%, rgba(255,255,255,0.1))`,
             zIndex: 3,
             transform: 'translateZ(0)', // 硬件加速
           }}
         />

         {/* 水波纹效果层 */}
         {showWaterRipple && (
           <div 
             className="absolute inset-0 rounded-[inherit] pointer-events-none transition-transform duration-300 ease-out will-change-transform overflow-hidden"
             style={{ 
               zIndex: 4,
               transform: 'translateZ(0)', // 硬件加速
             }}
           >
             <WaterRipple localEvents themeColor={themeColor} />
           </div>
         )}

         {/* 额外的柔化层 */}
         <div 
           className="absolute inset-0 rounded-[inherit] backdrop-blur-sm pointer-events-none transition-transform duration-300 ease-out will-change-transform overflow-hidden"
           style={{ 
             backgroundColor: `rgba(255, 255, 255, ${glassOpacity * 0.2})`,
             zIndex: 5,
             mixBlendMode: 'soft-light',
             transform: 'translateZ(0)', // 硬件加速
           }}
         />

         {/* 内容层 */}
         <div 
           className="relative w-full h-full rounded-[inherit] pointer-events-auto transition-all duration-300 ease-out will-change-transform group-hover:translate-y-[-1px] group-hover:shadow-lg overflow-hidden"
           style={{ 
             zIndex: 10,
             backgroundColor: `rgba(0, 0, 0, 0.02)`,
             backdropFilter: 'contrast(1.1)',
             transform: 'translateZ(0)', // 硬件加速
           }}
           ref={ref}
         >
           {children}
         </div>
       </div>
     )
  }
)

WaterGlassCard.displayName = 'WaterGlassCard'

// 子组件
export const WaterGlassCardHeader = React.forwardRef<
  HTMLDivElement,
  React.HTMLAttributes<HTMLDivElement>
>(({ className, ...props }, ref) => (
  <div
    ref={ref}
    className={cn('flex flex-col space-y-1.5 p-6 pb-4', className)}
    {...props}
  />
))
WaterGlassCardHeader.displayName = 'WaterGlassCardHeader'

export const WaterGlassCardTitle = React.forwardRef<
  HTMLParagraphElement,
  React.HTMLAttributes<HTMLHeadingElement>
>(({ className, ...props }, ref) => (
  <h3
    ref={ref}
    className={cn('text-lg font-semibold leading-none tracking-tight text-white', className)}
    {...props}
  />
))
WaterGlassCardTitle.displayName = 'WaterGlassCardTitle'

export const WaterGlassCardDescription = React.forwardRef<
  HTMLParagraphElement,
  React.HTMLAttributes<HTMLParagraphElement>
>(({ className, ...props }, ref) => (
  <p
    ref={ref}
    className={cn('text-sm text-white/80', className)}
    {...props}
  />
))
WaterGlassCardDescription.displayName = 'WaterGlassCardDescription'

export const WaterGlassCardContent = React.forwardRef<
  HTMLDivElement,
  React.HTMLAttributes<HTMLDivElement>
>(({ className, ...props }, ref) => (
  <div ref={ref} className={cn('p-6 pt-4', className)} {...props} />
))
WaterGlassCardContent.displayName = 'WaterGlassCardContent'

export const WaterGlassCardFooter = React.forwardRef<
  HTMLDivElement,
  React.HTMLAttributes<HTMLDivElement>
>(({ className, ...props }, ref) => (
  <div
    ref={ref}
    className={cn('flex items-center p-6 pt-4', className)}
    {...props}
  />
))
WaterGlassCardFooter.displayName = 'WaterGlassCardFooter'

export { WaterGlassCard, waterGlassCardVariants }
export type { WaterGlassCardProps }

