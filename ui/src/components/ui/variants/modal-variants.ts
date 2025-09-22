import { cva, type VariantProps } from 'class-variance-authority'

/** 遮罩层变体：控制背景与模糊强度 */
export const modalOverlayVariants = cva(
  'fixed inset-0 bg-background/70 backdrop-blur-[1.5px] transition-opacity duration-200 data-[state=open]:opacity-100 data-[state=closed]:opacity-0',
  {
    variants: {
      variant: {
        default: '',
        glass: 'backdrop-blur-md bg-background/50',
        transparent: 'bg-black/20 backdrop-blur-sm',
        dim: 'bg-black/60',
      },
    },
    defaultVariants: {
      variant: 'default',
    },
  },
)

/** 内容容器变体：控制外观与尺寸（含 fullscreen） */
export const modalContentVariants = cva(
  'fixed left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 outline-none focus-visible:ring-2 focus-visible:ring-primary/50 focus-visible:ring-offset-2 pointer-events-auto transition-all duration-200 data-[state=open]:opacity-100 data-[state=open]:scale-100 data-[state=closed]:opacity-0 data-[state=closed]:scale-95',
  {
    variants: {
      variant: {
        default: 'bg-card text-card-foreground border border-border shadow-2xl rounded-xl',
        glass: 'bg-white/70 dark:bg-gray-900/70 text-foreground backdrop-blur-xl border border-white/20 dark:border-gray-700/30 rounded-xl shadow-xl',
        primary: 'bg-primary-subtle text-primary border border-primary/20 rounded-xl shadow-primary',
        accent: 'bg-accent-subtle text-accent border border-accent/20 rounded-xl shadow-accent',
        destructive: 'bg-red-50 dark:bg-red-950/30 text-red-900 dark:text-red-100 border border-red-200 dark:border-red-800 rounded-xl',
      },
      size: {
        sm: 'w-[90vw] max-w-sm p-4',
        md: 'w-[90vw] max-w-lg p-6',
        lg: 'w-[92vw] max-w-2xl p-8',
        xl: 'w-[94vw] max-w-4xl p-10',
        fullscreen: 'inset-0 m-0 h-screen w-screen -translate-x-0 -translate-y-0 rounded-none p-4 md:p-6',
      },
    },
    defaultVariants: {
      variant: 'default',
      size: 'md',
    },
  },
)

/** 头部容器：随尺寸调整内边距 */
export const modalHeaderVariants = cva('flex flex-col space-y-1.5', {
  variants: {
    size: {
      sm: 'pb-3',
      md: 'pb-4',
      lg: 'pb-5',
      xl: 'pb-6',
      fullscreen: 'pb-4 md:pb-6',
    },
  },
  defaultVariants: {
    size: 'md',
  },
})

/** 标题：随尺寸调整字号 */
export const modalTitleVariants = cva('font-semibold leading-none tracking-tight', {
  variants: {
    size: {
      sm: 'text-base',
      md: 'text-lg',
      lg: 'text-xl',
      xl: 'text-2xl',
      fullscreen: 'text-xl md:text-2xl',
    },
  },
  defaultVariants: {
    size: 'md',
  },
})

/** 描述文本：随尺寸细调字号 */
export const modalDescriptionVariants = cva('text-sm text-muted-foreground', {
  variants: {
    size: {
      sm: '',
      md: '',
      lg: 'text-base',
      xl: 'text-base',
      fullscreen: '',
    },
  },
  defaultVariants: {
    size: 'md',
  },
})

/** 尾部容器：对齐与间距控制 */
export const modalFooterVariants = cva('flex items-center justify-end gap-2', {
  variants: {
    size: {
      sm: 'pt-3',
      md: 'pt-4',
      lg: 'pt-5',
      xl: 'pt-6',
      fullscreen: 'pt-4 md:pt-6',
    },
    align: {
      start: 'justify-start',
      center: 'justify-center',
      end: 'justify-end',
      between: 'justify-between',
    },
  },
  defaultVariants: {
    size: 'md',
    align: 'end',
  },
})

export type ModalContentVariants = VariantProps<typeof modalContentVariants>
export type ModalOverlayVariants = VariantProps<typeof modalOverlayVariants>
export type ModalHeaderVariants = VariantProps<typeof modalHeaderVariants>
export type ModalTitleVariants = VariantProps<typeof modalTitleVariants>
export type ModalDescriptionVariants = VariantProps<typeof modalDescriptionVariants>
export type ModalFooterVariants = VariantProps<typeof modalFooterVariants>

export const modalPresets = {
  default: { variant: 'default' as const, size: 'md' as const },
  small: { variant: 'default' as const, size: 'sm' as const },
  large: { variant: 'default' as const, size: 'lg' as const },
  glass: { variant: 'glass' as const, size: 'md' as const },
  fullscreen: { variant: 'default' as const, size: 'fullscreen' as const },
} as const


