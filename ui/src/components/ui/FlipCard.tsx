import * as React from 'react'
import { cn } from '@/lib/utils'

/**
 * FlipCard 模块
 *
 * 3D 翻转卡片：支持 hover/click 触发，水平/垂直方向与时长可配置；
 * 前后两面内容通过 `frontContent` 与 `backContent` 传入。
 */

/** 翻转卡片的属性 */
export interface FlipCardProps extends React.HTMLAttributes<HTMLDivElement> {
  frontContent: React.ReactNode
  backContent: React.ReactNode
  trigger?: 'hover' | 'click'
  direction?: 'horizontal' | 'vertical'
  duration?: number
  className?: string
  width?: string | number
  height?: string | number
}

/** 翻转卡片组件 */
export const FlipCard = React.forwardRef<HTMLDivElement, FlipCardProps>(
  ({ 
    frontContent, 
    backContent, 
    trigger = 'hover',
    direction = 'horizontal',
    duration = 600,
    className,
    width = '300px',
    height = '200px',
    ...props 
  }, ref) => {
    const [isFlipped, setIsFlipped] = React.useState(false)

    const handleClick = () => {
      if (trigger === 'click') {
        setIsFlipped(!isFlipped)
      }
    }

    const handleMouseEnter = () => {
      if (trigger === 'hover') {
        setIsFlipped(true)
      }
    }

    const handleMouseLeave = () => {
      if (trigger === 'hover') {
        setIsFlipped(false)
      }
    }

    const rotateAxis = direction === 'horizontal' ? 'rotateY' : 'rotateX'
    const flipDegree = isFlipped ? '180deg' : '0deg'

    return (
      <div
        ref={ref}
        className={cn('flip-card-container', className)}
        style={{
          width: typeof width === 'number' ? `${width}px` : width,
          height: typeof height === 'number' ? `${height}px` : height,
          perspective: '1000px',
        }}
        onClick={handleClick}
        onMouseEnter={handleMouseEnter}
        onMouseLeave={handleMouseLeave}
        {...props}
      >
        <div
          className="flip-card-inner"
          style={{
            position: 'relative',
            width: '100%',
            height: '100%',
            textAlign: 'center',
            transition: `transform ${duration}ms ease-in-out`,
            transformStyle: 'preserve-3d',
            transform: `${rotateAxis}(${flipDegree})`,
          }}
        >
          {/* 正面 */}
          <div
            className="flip-card-front"
            style={{
              position: 'absolute',
              width: '100%',
              height: '100%',
              backfaceVisibility: 'hidden',
              WebkitBackfaceVisibility: 'hidden',
              transform: `${rotateAxis}(0deg)`,
              zIndex: isFlipped ? 1 : 2,
              overflow: 'visible',
            }}
          >
            {frontContent}
          </div>
          
          {/* 背面 */}
          <div
            className="flip-card-back"
            style={{
              position: 'absolute',
              width: '100%',
              height: '100%',
              backfaceVisibility: 'hidden',
              WebkitBackfaceVisibility: 'hidden',
              transform: `${rotateAxis}(180deg)`,
              zIndex: isFlipped ? 2 : 1,
              overflow: 'visible',
            }}
          >
            {backContent}
          </div>
        </div>
      </div>
    )
  }
)

FlipCard.displayName = 'FlipCard'

/** 预设配置（常用触发与方向/时长组合） */
export const flipCardPresets = {
  basic: { trigger: 'hover' as const, direction: 'horizontal' as const, duration: 600 },
  quick: { trigger: 'hover' as const, direction: 'horizontal' as const, duration: 300 },
  slow: { trigger: 'hover' as const, direction: 'horizontal' as const, duration: 1000 },
  vertical: { trigger: 'hover' as const, direction: 'vertical' as const, duration: 600 },
  click: { trigger: 'click' as const, direction: 'horizontal' as const, duration: 600 },
}

export type FlipCardPreset = keyof typeof flipCardPresets

export default FlipCard

