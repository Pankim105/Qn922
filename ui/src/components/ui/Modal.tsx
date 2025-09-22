import * as React from 'react'
import { Slot } from '@radix-ui/react-slot'
import { X } from 'lucide-react'

import { cn } from '@/lib/utils'
import {
  modalContentVariants,
  modalOverlayVariants,
  modalHeaderVariants,
  modalTitleVariants,
  modalDescriptionVariants,
  modalFooterVariants,
  modalPresets,
  type ModalContentVariants,
  type ModalOverlayVariants,
  type ModalHeaderVariants,
  type ModalTitleVariants,
  type ModalDescriptionVariants,
  type ModalFooterVariants,
} from './variants/modal-variants'

/** Modal 上下文，用于在子组件间共享尺寸与开关状态 */
interface ModalContextType {
  open: boolean
  setOpen: (open: boolean) => void
  contentSize: NonNullable<ModalContentVariants['size']>
  contentVariant: NonNullable<ModalContentVariants['variant']>
}

/** 内部上下文实例 */
const ModalContext = React.createContext<ModalContextType | null>(null)

/** 获取 Modal 内部上下文（未在 Modal 内将报错） */
const useModalContext = () => {
  const ctx = React.useContext(ModalContext)
  if (!ctx) throw new Error('Modal components must be used within a Modal')
  return ctx
}

/**
 * Modal 根组件
 * - 支持受控/非受控 open
 * - 可选的可拖拽/可缩放增强（默认关闭）
 */
export interface ModalProps extends React.HTMLAttributes<HTMLDivElement>, ModalContentVariants {
  open?: boolean
  defaultOpen?: boolean
  onOpenChange?: (open: boolean) => void
  preset?: keyof typeof modalPresets
  asChild?: boolean
  closeOnOverlay?: boolean
  // enhancements
  draggable?: boolean
  resizable?: boolean
}

export const Modal = React.forwardRef<HTMLDivElement, ModalProps>(
  (
    { className, children, open, defaultOpen, onOpenChange, variant, size, preset, asChild = false, closeOnOverlay = true, draggable = false, resizable = false, ...props },
    ref,
  ) => {
    const [internalOpen, setInternalOpen] = React.useState<boolean>(defaultOpen ?? false)
    const isControlled = open !== undefined
    const isOpen = isControlled ? !!open : internalOpen

    const setOpen = (next: boolean) => {
      if (!isControlled) setInternalOpen(next)
      onOpenChange?.(next)
    }

    const presetConfig = preset ? modalPresets[preset] : undefined
    const contentVariant = (variant ?? presetConfig?.variant ?? 'default') as NonNullable<ModalContentVariants['variant']>
    const contentSize = (size ?? presetConfig?.size ?? 'md') as NonNullable<ModalContentVariants['size']>

    const Comp = asChild ? Slot : 'div'

    // drag/resize state
    const dragRef = React.useRef<HTMLDivElement | null>(null)
    const [pos, setPos] = React.useState({ x: 0, y: 0 })
    const [dragging, setDragging] = React.useState(false)
    const [start, setStart] = React.useState({ x: 0, y: 0 })
    const [sizeState, setSizeState] = React.useState<{ w: number | null; h: number | null }>({ w: null, h: null })

    React.useEffect(() => {
      const onMove = (e: MouseEvent) => {
        if (!dragging) return
        setPos({ x: e.clientX - start.x, y: e.clientY - start.y })
      }
      const onUp = () => setDragging(false)
      window.addEventListener('mousemove', onMove)
      window.addEventListener('mouseup', onUp)
      return () => {
        window.removeEventListener('mousemove', onMove)
        window.removeEventListener('mouseup', onUp)
      }
    }, [dragging, start.x, start.y])

    const onMouseDownDrag = (e: React.MouseEvent) => {
      if (!draggable) return
      const rect = dragRef.current?.getBoundingClientRect()
      setStart({ x: e.clientX - (rect?.left ?? 0), y: e.clientY - (rect?.top ?? 0) })
      setDragging(true)
    }

    const onResizeMouseDown = (e: React.MouseEvent) => {
      if (!resizable) return
      e.stopPropagation()
      const rect = dragRef.current?.getBoundingClientRect()
      const startX = e.clientX
      const startY = e.clientY
      const startW = rect?.width ?? 0
      const startH = rect?.height ?? 0

      const onMove = (ev: MouseEvent) => {
        const w = Math.max(320, Math.round(startW + (ev.clientX - startX)))
        const h = Math.max(200, Math.round(startH + (ev.clientY - startY)))
        setSizeState({ w, h })
      }
      const onUp = () => {
        window.removeEventListener('mousemove', onMove)
        window.removeEventListener('mouseup', onUp)
      }
      window.addEventListener('mousemove', onMove)
      window.addEventListener('mouseup', onUp)
    }

    const styleEnhance: React.CSSProperties = draggable || resizable ? {
      transform: draggable ? `translate(calc(-50% + ${pos.x}px), calc(-50% + ${pos.y}px))` : undefined,
      width: resizable && sizeState.w ? sizeState.w : undefined,
      height: resizable && sizeState.h ? sizeState.h : undefined,
      cursor: draggable ? (dragging ? 'grabbing' : 'grab') : undefined,
    } : {}

    return (
      <ModalContext.Provider value={{ open: isOpen, setOpen, contentSize, contentVariant }}>
        {isOpen && (
          <div className="fixed inset-0 z-50" data-state={isOpen ? 'open' : 'closed'}>
            <ModalOverlay onClick={closeOnOverlay ? () => setOpen(false) : undefined} />
            <Comp
              ref={(node: any) => { dragRef.current = node; if (typeof ref === 'function') ref(node); else if (ref) (ref as any).current = node }}
              role="dialog"
              aria-modal="true"
              className={cn(modalContentVariants({ variant: contentVariant, size: contentSize, className }), draggable && 'select-none')}
              style={styleEnhance}
              onMouseDown={onMouseDownDrag}
              {...props}
            >
              {children}
              {resizable && (
                <div
                  onMouseDown={onResizeMouseDown}
                  className="absolute bottom-1 right-1 h-4 w-4 cursor-se-resize rounded-sm bg-muted/60"
                  aria-hidden
                />
              )}
            </Comp>
          </div>
        )}
      </ModalContext.Provider>
    )
  },
)
Modal.displayName = 'Modal'

export interface ModalOverlayProps extends React.HTMLAttributes<HTMLDivElement>, ModalOverlayVariants {}

export const ModalOverlay = React.forwardRef<HTMLDivElement, ModalOverlayProps>(({ className, ...props }, ref) => {
  const { open } = useModalContext()
  return (
    <div
      ref={ref}
      aria-hidden
      data-state={open ? 'open' : 'closed'}
      className={cn('pointer-events-auto', modalOverlayVariants({ className }))}
      {...props}
    />
  )
})
ModalOverlay.displayName = 'ModalOverlay'

export interface ModalHeaderProps extends React.HTMLAttributes<HTMLDivElement>, ModalHeaderVariants {}

export const ModalHeader = React.forwardRef<HTMLDivElement, ModalHeaderProps>(({ className, ...props }, ref) => {
  const { contentSize } = useModalContext()
  return <div ref={ref} className={cn(modalHeaderVariants({ size: contentSize, className }))} {...props} />
})
ModalHeader.displayName = 'ModalHeader'

export interface ModalTitleProps extends React.HTMLAttributes<HTMLHeadingElement>, ModalTitleVariants {}

export const ModalTitle = React.forwardRef<HTMLHeadingElement, ModalTitleProps>(({ className, ...props }, ref) => {
  const { contentSize } = useModalContext()
  return <h3 ref={ref} className={cn(modalTitleVariants({ size: contentSize, className }))} {...props} />
})
ModalTitle.displayName = 'ModalTitle'

export interface ModalDescriptionProps extends React.HTMLAttributes<HTMLParagraphElement>, ModalDescriptionVariants {}

export const ModalDescription = React.forwardRef<HTMLParagraphElement, ModalDescriptionProps>(({ className, ...props }, ref) => {
  const { contentSize } = useModalContext()
  return <p ref={ref} className={cn(modalDescriptionVariants({ size: contentSize, className }))} {...props} />
})
ModalDescription.displayName = 'ModalDescription'

export interface ModalBodyProps extends React.HTMLAttributes<HTMLDivElement> {}
export const ModalBody = React.forwardRef<HTMLDivElement, ModalBodyProps>(({ className, ...props }, ref) => {
  return <div ref={ref} className={cn('pt-0', className)} {...props} />
})
ModalBody.displayName = 'ModalBody'

export interface ModalFooterProps extends React.HTMLAttributes<HTMLDivElement>, ModalFooterVariants {}
export const ModalFooter = React.forwardRef<HTMLDivElement, ModalFooterProps>(({ className, ...props }, ref) => {
  const { contentSize } = useModalContext()
  return <div ref={ref} className={cn(modalFooterVariants({ size: contentSize, className }))} {...props} />
})
ModalFooter.displayName = 'ModalFooter'

export interface ModalCloseProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  asChild?: boolean
}

/**
 * 关闭按钮：默认渲染 X 图标
 * - `asChild` 可将关闭行为赋予自定义触发元素
 */
export const ModalClose = React.forwardRef<HTMLButtonElement, ModalCloseProps>(
  ({ className, asChild = false, children, ...props }, ref) => {
    const { setOpen } = useModalContext()
    const Comp = asChild ? Slot : 'button'
    return (
      <Comp
        ref={ref}
        type={asChild ? undefined : 'button'}
        onClick={(e: any) => {
          props.onClick?.(e)
          setOpen(false)
        }}
        className={cn(
          'inline-flex h-9 w-9 items-center justify-center rounded-md text-muted-foreground hover:text-foreground hover:bg-muted/40 focus-visible:ring-2 focus-visible:ring-primary/50 outline-none transition-colors',
          className,
        )}
        {...props}
      >
        {children ?? <X className="h-4 w-4" />}
      </Comp>
    )
  },
)
ModalClose.displayName = 'ModalClose'

// Steps
export interface ModalStepsProps extends React.HTMLAttributes<HTMLDivElement> {
  current: number
  total: number
}

export const ModalSteps = React.forwardRef<HTMLDivElement, ModalStepsProps>(({ className, current, total, ...props }, ref) => {
  const steps = Array.from({ length: total })
  return (
    <div ref={ref} className={cn('flex items-center gap-2', className)} {...props}>
      {steps.map((_, i) => (
        <div
          key={i}
          className={cn(
            'h-1.5 rounded-full bg-muted',
            i < current ? 'bg-primary' : 'bg-muted',
            'w-8'
          )}
        />
      ))}
    </div>
  )
})
ModalSteps.displayName = 'ModalSteps'

export { modalContentVariants }


