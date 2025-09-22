/**
 * Modern UI Components - 入口模块
 *
 * 该文件集中导出组件库对外 API（组件、hooks、工具、类型等）。
 * 使用方应仅从本入口导入，避免依赖内部实现细节。
 */

// 核心组件导出
export { Button, buttonVariants } from './components/ui/Button'
export { 
  Card, 
  CardHeader, 
  CardTitle, 
  CardDescription, 
  CardContent, 
  CardFooter, 
  cardVariants 
} from './components/ui/Card'
export { 
  ModernInput, 
  SearchInput, 
  PasswordInput 
} from './components/ui/Input'
export { 
  Accordion,
  AccordionItem,
  AccordionTrigger,
  AccordionContent
} from './components/ui/Accordion'
export { 
  WaterGlassCard, 
  WaterGlassCardHeader, 
  WaterGlassCardTitle, 
  WaterGlassCardDescription, 
  WaterGlassCardContent, 
  WaterGlassCardFooter,
  waterGlassCardVariants 
} from './components/ui/WaterGlassCard'
export { 
  FlipCard, 
  flipCardPresets 
} from './components/ui/FlipCard'
export { 
  Modal,
  ModalOverlay,
  ModalHeader,
  ModalTitle,
  ModalDescription,
  ModalBody,
  ModalFooter,
  ModalClose,
  ModalSteps
} from './components/ui/Modal'
export {
  Table,
  THead,
  TBody,
  TFoot,
  TR,
  TH,
  TD,
  Caption,
  TableEmpty,
  TableLoading,
  TablePagination
} from './components/ui/Table'
export {
  Form,
  FormField,
  FormLabel,
  FormControl,
  FormHelpText,
} from './components/ui/Form'
export * as FormControls from './components/ui/form-components'
export { ModernSelect, NativeSelect, ModernTextarea, RichTextarea } from './components/ui/form-components'
export { DataTable } from './components/ui/DataTable'
export { 
  ThemeSwitcher,
  type ThemeOption 
} from './components/ui/ThemeSwitcher'

// 视觉效果组件
export { default as WaterRipple } from './components/visuals/WaterRipple'

// 工具函数
export { cn, formatCreatedAt } from './lib/utils'

// 变体类型导出
export type { ButtonVariants } from './components/ui/variants/button-variants'
export type { CardVariants } from './components/ui/variants/card-variants'
export type { AccordionVariants } from './components/ui/variants/accordion-variants'
export type { WaterGlassCardProps } from './components/ui/WaterGlassCard'
export type { FlipCardProps, FlipCardPreset } from './components/ui/FlipCard'
export type { ModalContentVariants } from './components/ui/variants/modal-variants'
export type { TableVariants } from './components/ui/variants/table-variants'
export type { FormFieldVariants } from './components/ui/variants/form-variants'

// 导入样式文件以确保它们被包含在构建中
import './styles/globals.css'

