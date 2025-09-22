import { cva, type VariantProps } from 'class-variance-authority'

/** 表格根：密度与外观、圆角与字号（对齐 app 层设计） */
export const tableVariants = cva('w-full text-left border-collapse overflow-hidden', {
  variants: {
    // 行高/字体密度
    density: {
      compact: 'text-xs',
      comfy: 'text-sm',
      spacious: 'text-base',
    },
    // 外观风格
    variant: {
      default: 'bg-card text-card-foreground border border-border',
      bordered: 'bg-card text-card-foreground border-2 border-border',
      striped: 'bg-card text-card-foreground',
      minimal: 'bg-transparent',
      glass: 'bg-white/70 dark:bg-gray-900/70 backdrop-blur-xl border border-white/20 dark:border-gray-700/30',
      primary: 'bg-primary-subtle text-primary border border-primary/20',
      accent: 'bg-accent-subtle text-accent border border-accent/20',
      // 兼容旧值
      outline: 'bg-transparent text-foreground border border-border',
      ghost: 'bg-transparent text-foreground',
    },
    // 圆角与整体尺寸风格（字号）
    rounded: {
      none: 'rounded-none',
      sm: 'rounded-sm',
      md: 'rounded-md',
      lg: 'rounded-lg',
      xl: 'rounded-xl',
      '2xl': 'rounded-2xl',
    },
    size: {
      sm: 'text-sm',
      md: 'text-base',
      lg: 'text-lg',
      xl: 'text-xl',
    },
  },
  defaultVariants: {
    density: 'comfy',
    variant: 'default',
    rounded: 'lg',
    size: 'md',
  },
})

/** 表格容器：滚动、阴影与最大高度 */
export const tableContainerVariants = cva('relative overflow-auto', {
  variants: {
    shadow: {
      none: '',
      sm: 'shadow-sm',
      md: 'shadow-md',
      lg: 'shadow-lg',
      xl: 'shadow-xl',
    },
    maxHeight: {
      none: '',
      sm: 'max-h-64',
      md: 'max-h-96',
      lg: 'max-h-[32rem]',
      xl: 'max-h-[40rem]',
      '2xl': 'max-h-[48rem]',
    },
  },
  defaultVariants: {
    shadow: 'sm',
    maxHeight: 'none',
  },
})

/** 表头：可选吸顶 */
export const tableHeaderVariants = cva('text-muted-foreground', {
  variants: {
    sticky: {
      true: 'sticky top-0 z-10 bg-card/90 backdrop-blur supports-[backdrop-filter]:bg-card/60',
      false: '',
    },
  },
  defaultVariants: {
    sticky: false,
  },
})

/** 行：悬停、选中、斑马纹 */
export const tableRowVariants = cva('border-b border-border', {
  variants: {
    hoverable: {
      true: 'hover:bg-muted/50',
      false: '',
    },
    selected: {
      true: 'bg-primary/5',
      false: '',
    },
    zebra: {
      true: 'odd:bg-muted/40',
      false: '',
    },
  },
  defaultVariants: {
    hoverable: true,
    selected: false,
    zebra: false,
  },
})

/** 单元格：密度/数字对齐/是否为表头 */
export const tableCellVariants = cva('align-middle', {
  variants: {
    density: {
      compact: 'px-3 py-2',
      comfy: 'px-4 py-3',
      spacious: 'px-5 py-4',
    },
    numeric: {
      true: 'text-right',
      false: '',
    },
    header: {
      true: 'font-semibold',
      false: '',
    },
  },
  defaultVariants: {
    density: 'comfy',
    numeric: false,
    header: false,
  },
})

export type TableVariants = VariantProps<typeof tableVariants>
export type TableHeaderVariants = VariantProps<typeof tableHeaderVariants>
export type TableRowVariants = VariantProps<typeof tableRowVariants>
export type TableCellVariants = VariantProps<typeof tableCellVariants>

export const tablePresets = {
  default: { variant: 'default' as const, density: 'comfy' as const },
  compact: { variant: 'default' as const, density: 'compact' as const },
  spacious: { variant: 'default' as const, density: 'spacious' as const },
  striped: { variant: 'striped' as const, density: 'comfy' as const },
  outline: { variant: 'outline' as const, density: 'comfy' as const },
} as const


