import * as React from 'react'
import { Slot } from '@radix-ui/react-slot'
import { ChevronDown } from 'lucide-react'

import { cn } from '@/lib/utils'
import {
  accordionVariants,
  accordionItemVariants,
  accordionTriggerVariants,
  accordionContentVariants,
  accordionPresets,
  type AccordionVariants,
  type AccordionItemVariants,
  type AccordionTriggerVariants,
  type AccordionContentVariants,
} from './variants/accordion-variants'

/**
 * Accordion 根组件
 * - 支持 single/multiple 模式与 collapsible
 * - 受控/非受控均可
 * - 变体与预设与全局体系一致
 */
export interface AccordionProps
  extends React.HTMLAttributes<HTMLDivElement>,
    AccordionVariants {
  type?: 'single' | 'multiple'
  collapsible?: boolean
  defaultValue?: string | string[]
  value?: string | string[]
  onValueChange?: (value: string | string[]) => void
  preset?: keyof typeof accordionPresets
  asChild?: boolean
}

/** 手风琴根组件：提供上下文、支持受控/非受控与预设 */
export const Accordion = React.forwardRef<HTMLDivElement, AccordionProps>(
  (
    {
      className,
      type = 'single',
      collapsible = true,
      defaultValue,
      value,
      onValueChange,
      children,
      variant,
      size,
      preset,
      asChild = false,
      ...props
    },
    ref,
  ) => {
    const Comp = asChild ? Slot : 'div'

    const [internalValue, setInternalValue] = React.useState<string | string[]>(() => {
      if (value !== undefined) return value
      if (defaultValue !== undefined) return defaultValue
      return type === 'single' ? '' : []
    })

    const isControlled = value !== undefined
    const currentValue = isControlled ? (value as string | string[]) : internalValue

    // 统一更新展开值（支持受控/非受控）
    const handleValueChange = (newValue: string | string[]) => {
      if (!isControlled) setInternalValue(newValue)
      onValueChange?.(newValue)
    }

    const presetConfig = preset ? accordionPresets[preset] : undefined
    const rootVariant = variant ?? presetConfig?.variant ?? 'default'
    const rootSize = size ?? presetConfig?.size ?? 'md'

    const contextValue: AccordionContextType = {
      type,
      collapsible,
      value: currentValue,
      onValueChange: handleValueChange,
      rootVariant: rootVariant as NonNullable<AccordionVariants['variant']>,
      rootSize: rootSize as NonNullable<AccordionVariants['size']>,
    }

    return (
      <AccordionContext.Provider value={contextValue}>
        <Comp
          ref={ref}
          className={cn(accordionVariants({ variant: rootVariant, size: rootSize, className }))}
          {...props}
        >
          {children}
        </Comp>
      </AccordionContext.Provider>
    )
  },
)
Accordion.displayName = 'Accordion'

/** Accordion 上下文，在子组件间共享根的配置与当前展开状态 */
interface AccordionContextType {
  type: 'single' | 'multiple'
  collapsible: boolean
  value: string | string[]
  onValueChange: (value: string | string[]) => void
  rootVariant: NonNullable<AccordionVariants['variant']>
  rootSize: NonNullable<AccordionVariants['size']>
}

const AccordionContext = React.createContext<AccordionContextType | null>(null)

/** 获取 Accordion 根上下文（未在 Accordion 内使用将报错） */
const useAccordionContext = () => {
  const context = React.useContext(AccordionContext)
  if (!context) {
    throw new Error('Accordion components must be used within an Accordion')
  }
  return context
}

/** 单个手风琴项 */
export interface AccordionItemProps
  extends React.HTMLAttributes<HTMLDivElement>,
    AccordionItemVariants {
  value: string
  disabled?: boolean
}

/** 手风琴单项：负责单项开合与 ids 关联 */
export const AccordionItem = React.forwardRef<HTMLDivElement, AccordionItemProps>(
  (
    { className, value, disabled = false, children, variant, spacing, ...props },
    ref,
  ) => {
    // 从根上下文读取工作模式与状态
    const context = useAccordionContext()

    const isOpen =
      context.type === 'single'
        ? context.value === value
        : Array.isArray(context.value) && context.value.includes(value)

    const reactId = React.useId()
    const triggerId = `accordion-trigger-${reactId}-${value}`
    const contentId = `accordion-content-${reactId}-${value}`

    const derivedItemVariant: NonNullable<AccordionItemVariants['variant']> =
      (variant as any) ?? (context.rootVariant === 'filled' ? 'filled' : context.rootVariant === 'ghost' ? 'ghost' : 'default')

    const contextValue: AccordionItemContextType = {
      value,
      disabled,
      isOpen,
      triggerId,
      contentId,
      onToggle: () => {
        if (disabled) return
        if (context.type === 'single') {
          const newValue = isOpen && context.collapsible ? '' : value
          context.onValueChange(newValue)
        } else {
          const currentArray = Array.isArray(context.value) ? context.value : []
          const newValue = isOpen ? currentArray.filter(v => v !== value) : [...currentArray, value]
          context.onValueChange(newValue)
        }
      },
    }

    return (
      <AccordionItemContext.Provider value={contextValue}>
        <div
          ref={ref}
          data-state={isOpen ? 'open' : 'closed'}
          data-disabled={disabled ? '' : undefined}
          className={cn(
            accordionItemVariants({ variant: derivedItemVariant, spacing: spacing ?? 'none' }),
            disabled && 'opacity-50 pointer-events-none',
            className,
          )}
          {...props}
        >
          {children}
        </div>
      </AccordionItemContext.Provider>
    )
  },
)
AccordionItem.displayName = 'AccordionItem'

/** AccordionItem 内部上下文，供 Trigger/Content 使用 */
interface AccordionItemContextType {
  value: string
  disabled: boolean
  isOpen: boolean
  triggerId: string
  contentId: string
  onToggle: () => void
}

const AccordionItemContext = React.createContext<AccordionItemContextType | null>(null)

/** 获取单个 AccordionItem 的上下文（未在 AccordionItem 内使用将报错） */
const useAccordionItemContext = () => {
  const context = React.useContext(AccordionItemContext)
  if (!context) {
    throw new Error('AccordionItem components must be used within an AccordionItem')
  }
  return context
}

/**
 * 手风琴触发器
 * - 支持 asChild 包装为自定义元素
 */
export interface AccordionTriggerProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement>,
    AccordionTriggerVariants {
  hideChevron?: boolean
  asChild?: boolean
}

/** 触发器：按钮或自定义元素，负责切换当前项状态 */
export const AccordionTrigger = React.forwardRef<HTMLButtonElement, AccordionTriggerProps>(
  (
    { className, hideChevron = false, children, variant, size, asChild = false, ...props },
    ref,
  ) => {
    const { disabled, isOpen, onToggle, triggerId, contentId } = useAccordionItemContext()
    const { rootVariant, rootSize } = useAccordionContext()

    const derivedTriggerVariant: NonNullable<AccordionTriggerVariants['variant']> =
      (variant as any) ?? (rootVariant === 'filled' ? 'filled' : rootVariant === 'ghost' ? 'ghost' : 'default')
    const derivedTriggerSize: NonNullable<AccordionTriggerVariants['size']> =
      (size as any) ?? (rootSize === 'sm' ? 'sm' : rootSize === 'lg' ? 'lg' : 'md')

    const Comp = asChild ? Slot : 'button'

    return (
      <Comp
        id={triggerId}
        ref={ref}
        type={asChild ? undefined : 'button'}
        className={cn(
          accordionTriggerVariants({ variant: derivedTriggerVariant, size: derivedTriggerSize }),
          disabled && 'cursor-not-allowed',
          className,
        )}
        disabled={disabled}
        onClick={onToggle}
        aria-expanded={isOpen}
        aria-controls={contentId}
        data-state={isOpen ? 'open' : 'closed'}
        {...props}
      >
        <span className={cn(derivedTriggerSize === 'sm' ? 'text-sm' : 'text-base', 'font-medium')}>{children}</span>
        {!hideChevron && (
          <ChevronDown
            className={cn('ml-2 h-4 w-4 shrink-0 transition-transform duration-200', isOpen && 'rotate-180')}
            aria-hidden="true"
          />
        )}
      </Comp>
    )
  },
)
AccordionTrigger.displayName = 'AccordionTrigger'

/**
 * 手风琴内容区域
 * - 使用 grid rows 过渡确保展开收起动画稳定
 */
export interface AccordionContentProps
  extends React.HTMLAttributes<HTMLDivElement>,
    AccordionContentVariants {}

/** 内容区域：带展开/收起过渡与 aria 语义 */
export const AccordionContent = React.forwardRef<HTMLDivElement, AccordionContentProps>(
  ({ className, children, variant, size, ...props }, ref) => {
    const { isOpen, triggerId, contentId } = useAccordionItemContext()
    const { rootVariant, rootSize } = useAccordionContext()

    const derivedContentVariant: NonNullable<AccordionContentVariants['variant']> =
      (variant as any) ?? (rootVariant === 'filled' ? 'filled' : 'padded')
    const derivedContentSize: NonNullable<AccordionContentVariants['size']> =
      (size as any) ?? (rootSize === 'sm' ? 'sm' : rootSize === 'lg' ? 'lg' : 'md')

    const wrapperRef = React.useRef<HTMLDivElement | null>(null)
    const contentRef = React.useRef<HTMLDivElement | null>(null)

    // 合并外部 ref 到内容节点
    React.useImperativeHandle(ref, () => contentRef.current as HTMLDivElement)

    // 使用高度过渡实现更稳定的展开/收起动画
    React.useLayoutEffect(() => {
      const wrapper = wrapperRef.current
      const contentEl = contentRef.current
      if (!wrapper || !contentEl) return

      const end = () => {
        // 展开结束后设置为 auto，保证自适应内容高度；收起时保持 0
        if (isOpen) {
          wrapper.style.height = 'auto'
        }
        wrapper.removeEventListener('transitionend', end)
      }

      // 先移除事件，避免重复绑定
      wrapper.removeEventListener('transitionend', end)

      if (isOpen) {
        // 从 0 -> 内容高度
        const target = contentEl.scrollHeight
        wrapper.style.height = '0px'
        // 下一帧应用目标高度，触发过渡
        requestAnimationFrame(() => {
          wrapper.addEventListener('transitionend', end)
          wrapper.style.height = `${target}px`
        })
      } else {
        // 从当前高度 -> 0
        const current = wrapper.getBoundingClientRect().height || contentEl.scrollHeight
        wrapper.style.height = `${current}px`
        // 强制回流，确保过渡生效
        void wrapper.offsetHeight
        wrapper.addEventListener('transitionend', end)
        wrapper.style.height = '0px'
      }
    }, [isOpen])

    return (
      <div
        id={contentId}
        role="region"
        aria-labelledby={triggerId}
        aria-hidden={!isOpen}
        data-state={isOpen ? 'open' : 'closed'}
        ref={wrapperRef}
        className={cn('overflow-hidden transition-[height] duration-300 ease-out')}
      >
        <div
          ref={contentRef}
          className={cn(
            'min-h-0 overflow-hidden text-muted-foreground',
            accordionContentVariants({ variant: derivedContentVariant, size: derivedContentSize }),
            !isOpen && 'opacity-0 pointer-events-none',
            className,
          )}
          {...props}
        >
          {children}
        </div>
      </div>
    )
  },
)
AccordionContent.displayName = 'AccordionContent'

export { accordionVariants }


