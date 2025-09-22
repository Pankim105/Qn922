import { cva, type VariantProps } from 'class-variance-authority'

/**
 * Card 组件变体配置
 * 现代设计语言的卡片系统
 */
export const cardVariants = cva(
  // 基础样式
  'relative overflow-hidden transition-all duration-200',
  {
    variants: {
      // 样式变体
      variant: {
        // 默认卡片
        default: 'bg-card text-card-foreground border border-border shadow-sm',
        
        // 突出卡片
        elevated: 'bg-card text-card-foreground border-0 shadow-lg hover:shadow-xl',
        
        // 主题色卡片
        primary: 'bg-primary-subtle text-primary border border-primary/20 shadow-primary',
        
        // 强调色卡片
        accent: 'bg-accent-subtle text-accent border border-accent/20 shadow-accent',
        
        // 玻璃效果卡片
        glass: 'bg-white/70 dark:bg-gray-900/70 backdrop-blur-xl border border-white/20 dark:border-gray-700/30 text-foreground shadow-lg',
        
        // 渐变卡片
        gradient: 'bg-gradient-primary text-primary-foreground border-0 shadow-primary-md',
        
        // 轮廓卡片
        outline: 'bg-transparent border-2 border-primary text-primary hover:bg-primary-subtle/50',
        
        // 幽灵卡片
        ghost: 'bg-transparent border-0 text-foreground hover:bg-muted/50',
        
        // 危险卡片
        destructive: 'bg-red-50 dark:bg-red-950/30 text-red-900 dark:text-red-100 border border-red-200 dark:border-red-800',
        
        // 成功卡片
        success: 'bg-green-50 dark:bg-green-950/30 text-green-900 dark:text-green-100 border border-green-200 dark:border-green-800',
        
        // 警告卡片
        warning: 'bg-yellow-50 dark:bg-yellow-950/30 text-yellow-900 dark:text-yellow-100 border border-yellow-200 dark:border-yellow-800',
        
        // 信息卡片
        info: 'bg-blue-50 dark:bg-blue-950/30 text-blue-900 dark:text-blue-100 border border-blue-200 dark:border-blue-800',
      },
      
      // 尺寸变体
      size: {
        sm: 'p-4',
        md: 'p-6',
        lg: 'p-8',
        xl: 'p-10',
        compact: 'p-3',
      },
      
      // 圆角变体
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
      
      // 交互性
      interactive: {
        none: '',
        hover: 'hover:scale-[1.02] hover:shadow-lg cursor-default',
        clickable: 'hover:scale-[1.02] hover:shadow-lg cursor-pointer active:scale-[0.98]',
        pressable: 'hover:shadow-lg cursor-pointer active:scale-[0.95] active:shadow-sm',
      },
      
      // 特殊效果
      effect: {
        none: '',
        glow: 'shadow-primary/25 hover:shadow-primary/40',
        float: 'hover:-translate-y-1 hover:shadow-xl',
        tilt: 'hover:rotate-1 hover:scale-105 transform-gpu',
        flip: 'hover:rotateY-12 [transform-style:preserve-3d]',
      }
    },
    
    // 复合变体
    compoundVariants: [
      // 玻璃效果 + 悬停
      {
        variant: 'glass',
        interactive: 'hover',
        className: 'hover:bg-white/80 dark:hover:bg-gray-900/80',
      },
      
      // 渐变 + 悬停
      {
        variant: 'gradient',
        interactive: 'hover',
        className: 'hover:shadow-primary-md hover:shadow-primary/30',
      },
      
      // 主题色 + 发光效果
      {
        variant: 'primary',
        effect: 'glow',
        className: 'shadow-primary/20 hover:shadow-primary/40',
      },
      
      // 强调色 + 发光效果
      {
        variant: 'accent',
        effect: 'glow',
        className: 'shadow-accent/20 hover:shadow-accent/40',
      },
      
      // 小尺寸 + 紧凑模式
      {
        size: 'compact',
        rounded: 'lg',
        className: 'text-sm',
      },
      
      // 大尺寸 + 特殊圆角
      {
        size: 'xl',
        rounded: '3xl',
        className: 'text-lg',
      },
    ],
    
    // 默认变体
    defaultVariants: {
      variant: 'default',
      size: 'md',
      rounded: 'lg',
      interactive: 'none',
      effect: 'none',
    },
  }
)

// 导出类型
export type CardVariants = VariantProps<typeof cardVariants>
export type CardVariant = NonNullable<CardVariants['variant']>
export type CardSize = NonNullable<CardVariants['size']>
export type CardRounded = NonNullable<CardVariants['rounded']>
export type CardInteractive = NonNullable<CardVariants['interactive']>
export type CardEffect = NonNullable<CardVariants['effect']>

// 预设组合
export const cardPresets: Record<string, Partial<CardVariants>> = {
  // 基础卡片
  basic: { variant: 'default', size: 'md' },
  
  // 产品卡片
  product: { variant: 'elevated', size: 'md', interactive: 'hover', effect: 'float' },
  
  // 功能卡片
  feature: { variant: 'primary', size: 'lg', interactive: 'clickable', effect: 'glow' },
  
  // 统计卡片
  stats: { variant: 'glass', size: 'md', rounded: 'xl' },
  
  // 行动卡片
  cta: { variant: 'gradient', size: 'lg', interactive: 'pressable', rounded: 'xl' },
  
  // 通知卡片
  notification: { variant: 'outline', size: 'sm', rounded: 'md' },
  
  // 头像卡片
  avatar: { variant: 'ghost', size: 'compact', rounded: 'full', interactive: 'hover' },
  
  // 媒体卡片
  media: { variant: 'default', size: 'md', rounded: '2xl', interactive: 'clickable', effect: 'tilt' },
  
  // 状态卡片
  status: { variant: 'accent', size: 'sm', rounded: 'lg' },
  
  // 仪表板卡片
  dashboard: { variant: 'elevated', size: 'lg', rounded: 'xl', interactive: 'hover' },
}

