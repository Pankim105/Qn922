import { cva, type VariantProps } from 'class-variance-authority'

/**
 * Button 组件变体配置
 * 使用 CVA 管理所有变体组合
 */
export const buttonVariants = cva(
  // 基础样式（始终应用）
  'inline-flex items-center justify-center font-medium outline-none focus-visible:ring-2 focus-visible:ring-primary/50 transition-all duration-200 disabled:pointer-events-none disabled:opacity-50',
  {
    variants: {
      // 样式变体
      variant: {
        solid: 'bg-primary text-primary-foreground hover:bg-primary-hover shadow-primary',
        outline: 'border-2 border-primary bg-transparent text-primary hover:bg-primary-subtle hover:border-primary-hover',
        ghost: 'bg-transparent text-primary hover:bg-primary-subtle',
        glass: 'bg-white/70 dark:bg-gray-700/70 backdrop-blur-sm border-0 text-gray-700 dark:text-gray-200 hover:bg-white/90 dark:hover:bg-gray-700/90',
        destructive: 'bg-destructive text-destructive-foreground hover:bg-destructive/90 shadow-destructive',
        link: 'text-primary underline-offset-4 hover:underline',
      },
      // 尺寸变体
      size: {
        sm: 'h-8 px-3 text-xs',
        md: 'h-10 px-4 text-sm',
        lg: 'h-11 px-6 text-base',
        xl: 'h-12 px-8 text-lg',
        icon: 'h-10 w-10',
      },
      // 形状变体
      shape: {
        rounded: 'rounded-md',
        pill: 'rounded-full',
        square: 'rounded-none',
      },
      // 状态变体
      state: {
        default: '',
        loading: 'cursor-wait',
        active: 'ring-2 ring-primary ring-offset-2',
      }
    },
    // 复合变体（特定组合的特殊样式）
    compoundVariants: [
      // 图标按钮的特殊样式
      {
        size: 'icon',
        shape: 'pill',
        className: 'rounded-full',
      },
      // 链接样式的特殊处理
      {
        variant: 'link',
        size: 'sm',
        className: 'h-auto p-0 text-xs',
      },
      // 加载状态的特殊样式
      {
        state: 'loading',
        variant: 'solid',
        className: 'relative overflow-hidden',
      },
      // 玻璃效果 + 小尺寸的特殊处理
      {
        variant: 'glass',
        size: 'sm',
        className: 'backdrop-blur-md',
      },
    ],
    // 默认变体
    defaultVariants: {
      variant: 'solid',
      size: 'md',
      shape: 'rounded',
      state: 'default',
    },
  }
)

// 导出变体类型，供组件使用
export type ButtonVariants = VariantProps<typeof buttonVariants>

// 导出具体的变体选项类型
export type ButtonVariant = NonNullable<ButtonVariants['variant']>
export type ButtonSize = NonNullable<ButtonVariants['size']>
export type ButtonShape = NonNullable<ButtonVariants['shape']>
export type ButtonState = NonNullable<ButtonVariants['state']>

// 导出预设组合（常用的变体组合）
export const buttonPresets = {
  // 主要操作按钮
  primary: { variant: 'solid' as const, size: 'md' as const },
  
  // 次要操作按钮
  secondary: { variant: 'outline' as const, size: 'md' as const },
  
  // 危险操作按钮
  danger: { variant: 'destructive' as const, size: 'md' as const },
  
  // 图标按钮
  iconButton: { variant: 'ghost' as const, size: 'icon' as const, shape: 'pill' as const },
  
  // 链接样式按钮
  linkButton: { variant: 'link' as const, size: 'sm' as const },
  
  // 玻璃效果按钮（导航栏用）
  navButton: { variant: 'glass' as const, size: 'sm' as const, shape: 'pill' as const },
} as const
