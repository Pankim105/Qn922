import { cva, type VariantProps } from 'class-variance-authority'

/** Accordion 组件变体配置：根容器（控制整体间距与字号） */
export const accordionVariants = cva(
  'space-y-2',
  {
    variants: {
      variant: {
        default: '',
        bordered: 'border border-border rounded-lg p-2',
        filled: 'bg-muted/50 rounded-lg p-2',
        ghost: '',
      },
      size: {
        sm: 'text-sm',
        md: '',
        lg: 'text-lg',
      },
    },
    defaultVariants: {
      variant: 'default',
      size: 'md',
    },
  }
)

export const accordionItemVariants = cva(
  'border border-border rounded-lg overflow-hidden transition-all duration-200',
  {
    variants: {
      variant: {
        default: 'border-border',
        filled: 'bg-card border-border',
        outline: 'border-2 border-primary/20',
        ghost: 'border-transparent shadow-none',
      },
      spacing: {
        none: '',
        sm: 'mb-1',
        md: 'mb-2',
        lg: 'mb-4',
      },
    },
    defaultVariants: {
      variant: 'default',
      spacing: 'sm',
    },
  }
)

export const accordionTriggerVariants = cva(
  'w-full flex items-center justify-between p-4 font-medium text-left transition-all duration-200 outline-none focus-visible:ring-2 focus-visible:ring-primary/50 focus-visible:ring-offset-2 rounded-md',
  {
    variants: {
      variant: {
        default: 'hover:bg-muted/50',
        filled: 'hover:bg-muted/80',
        ghost: 'hover:bg-muted/30',
        primary: 'hover:bg-primary/10 text-primary',
      },
      size: {
        sm: 'p-3 text-xs',
        md: 'p-4 text-sm',
        lg: 'p-5 text-base',
      },
    },
    defaultVariants: {
      variant: 'default',
      size: 'md',
    },
  }
)

export const accordionContentVariants = cva(
  'overflow-hidden transition-all duration-200',
  {
    variants: {
      variant: {
        default: '',
        padded: 'px-4 pb-4',
        filled: 'bg-muted/20',
      },
      size: {
        sm: 'text-sm',
        md: 'text-sm',
        lg: 'text-base',
      },
    },
    defaultVariants: {
      variant: 'default',
      size: 'md',
    },
  }
)

// 导出类型
export type AccordionVariants = VariantProps<typeof accordionVariants>
export type AccordionItemVariants = VariantProps<typeof accordionItemVariants>
export type AccordionTriggerVariants = VariantProps<typeof accordionTriggerVariants>
export type AccordionContentVariants = VariantProps<typeof accordionContentVariants>

// 预设配置
export const accordionPresets = {
  default: { variant: 'default' as const, size: 'md' as const },
  bordered: { variant: 'bordered' as const, size: 'md' as const },
  filled: { variant: 'filled' as const, size: 'md' as const },
  compact: { variant: 'default' as const, size: 'sm' as const },
  large: { variant: 'default' as const, size: 'lg' as const },
} as const
