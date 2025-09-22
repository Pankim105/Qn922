import * as React from 'react'
import { cn } from '@/lib/utils'
import {
  formFieldVariants,
  formLabelVariants,
  formHelpTextVariants,
  formPresets,
  type FormFieldVariants,
  type FormLabelVariants,
  type FormHelpTextVariants,
} from './variants/form-variants'

/**
 * Form 根组件
 * - 提供密度/布局方向上下文
 */
export interface FormProps extends React.FormHTMLAttributes<HTMLFormElement> {
  density?: NonNullable<FormFieldVariants['density']>
  orientation?: NonNullable<FormFieldVariants['orientation']>
  preset?: keyof typeof formPresets
}

export const Form = React.forwardRef<HTMLFormElement, FormProps>(
  ({ className, children, density, orientation, preset, ...props }, ref) => {
    const presetConfig = preset ? formPresets[preset] : undefined
    const finalDensity = density ?? presetConfig?.density ?? 'comfy'
    const finalOrientation = orientation ?? presetConfig?.orientation ?? 'vertical'
    return (
      <form ref={ref} className={cn('space-y-4', className)} {...props}>
        <FormContext.Provider value={{ density: finalDensity, orientation: finalOrientation }}>
          {children}
        </FormContext.Provider>
      </form>
    )
  },
)
Form.displayName = 'Form'

/** 表单上下文，向 Field/Label 等传递布局密度与方向 */
interface FormContextType {
  density: NonNullable<FormFieldVariants['density']>
  orientation: NonNullable<FormFieldVariants['orientation']>
}

const FormContext = React.createContext<FormContextType | null>(null)

/** 获取Form上下文（未在Form内使用将报错，避免漏用） */
const useFormContext = () => {
  const ctx = React.useContext(FormContext)
  if (!ctx) throw new Error('Form components must be used within a Form')
  return ctx
}

/** 单个表单字段容器 */
export interface FormFieldProps extends React.HTMLAttributes<HTMLDivElement>, FormFieldVariants {
  name?: string
}

export const FormField = React.forwardRef<HTMLDivElement, FormFieldProps>(
  ({ className, density, orientation, ...props }, ref) => {
    const { density: ctxDensity, orientation: ctxOrientation } = useFormContext()
    return (
      <div
        ref={ref}
        className={cn(formFieldVariants({ density: density ?? ctxDensity, orientation: orientation ?? ctxOrientation, className }))}
        {...props}
      />
    )
  },
)
FormField.displayName = 'FormField'

/** 字段标签（支持必填标记） */
export interface FormLabelProps extends React.LabelHTMLAttributes<HTMLLabelElement>, FormLabelVariants {}
export const FormLabel = React.forwardRef<HTMLLabelElement, FormLabelProps>(({ className, required, ...props }, ref) => {
  return <label ref={ref} className={cn(formLabelVariants({ required, className }))} {...props} />
})
FormLabel.displayName = 'FormLabel'

/** 控件容器（用于包裹输入类组件） */
export interface FormControlProps extends React.HTMLAttributes<HTMLDivElement> {}
export const FormControl = React.forwardRef<HTMLDivElement, FormControlProps>(({ className, ...props }, ref) => {
  return <div ref={ref} className={cn('w-full', className)} {...props} />
})
FormControl.displayName = 'FormControl'

/** 字段辅助/校验信息 */
export interface FormHelpTextProps extends React.HTMLAttributes<HTMLParagraphElement>, FormHelpTextVariants {}
export const FormHelpText = React.forwardRef<HTMLParagraphElement, FormHelpTextProps>(({ className, state, ...props }, ref) => {
  return <p ref={ref} className={cn(formHelpTextVariants({ state, className }))} {...props} />
})
FormHelpText.displayName = 'FormHelpText'


