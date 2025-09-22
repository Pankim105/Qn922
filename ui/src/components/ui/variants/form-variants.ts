import { cva, type VariantProps } from 'class-variance-authority'

/** 字段容器：密度与方向（垂直/水平） */
export const formFieldVariants = cva('grid gap-1.5', {
  variants: {
    density: {
      compact: 'gap-1',
      comfy: 'gap-1.5',
      spacious: 'gap-2',
    },
    orientation: {
      vertical: 'grid-cols-1',
      horizontal: 'grid-cols-[180px_minmax(0,1fr)] items-center gap-x-4',
    },
  },
  defaultVariants: {
    density: 'comfy',
    orientation: 'vertical',
  },
})

/** 标签：支持必填标记 */
export const formLabelVariants = cva('text-sm font-medium text-foreground', {
  variants: {
    required: {
      true: 'after:content-["*"] after:ml-0.5 after:text-destructive',
      false: '',
    },
  },
  defaultVariants: {
    required: false,
  },
})

/** 帮助/状态文本：根据状态切换颜色 */
export const formHelpTextVariants = cva('text-xs text-muted-foreground', {
  variants: {
    state: {
      default: '',
      error: 'text-destructive',
      success: 'text-green-600 dark:text-green-400',
      warning: 'text-yellow-600 dark:text-yellow-400',
    },
  },
  defaultVariants: {
    state: 'default',
  },
})

export type FormFieldVariants = VariantProps<typeof formFieldVariants>
export type FormLabelVariants = VariantProps<typeof formLabelVariants>
export type FormHelpTextVariants = VariantProps<typeof formHelpTextVariants>

export const formPresets = {
  default: { density: 'comfy' as const, orientation: 'vertical' as const },
  compact: { density: 'compact' as const, orientation: 'vertical' as const },
  horizontal: { density: 'comfy' as const, orientation: 'horizontal' as const },
} as const


