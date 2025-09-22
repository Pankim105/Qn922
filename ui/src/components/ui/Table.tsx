import * as React from 'react'
import { cva, type VariantProps } from 'class-variance-authority'
import { cn } from '@/lib/utils'
import { tableVariants, tableContainerVariants, type TableVariants } from './variants/table-variants'

/**
 * Table 模块（展示层）
 *
 * 纯展示层表格组件：不维护内部排序/分页状态；
 * 提供 `Table/THead/TBody/TFoot/TR/TH/TD/Caption` 等基础单元，
 * 并包含空态/加载态与简单分页组件以便快速组合。
 */

export interface TableProps extends React.TableHTMLAttributes<HTMLTableElement>, TableVariants {
  containerProps?: React.HTMLAttributes<HTMLDivElement> & {
    shadow?: 'none' | 'sm' | 'md' | 'lg' | 'xl'
    maxHeight?: 'none' | 'sm' | 'md' | 'lg' | 'xl' | '2xl'
  }
}

export const Table = React.forwardRef<HTMLTableElement, TableProps>(({ className, variant, size, rounded, density, containerProps, children, ...props }, ref) => {
  const { className: containerClassName, shadow, maxHeight, ...restContainer } = (containerProps as any) || {}
  return (
    <div className={cn(tableContainerVariants({ shadow, maxHeight }), containerClassName)} {...(restContainer as any)}>
      <table ref={ref} className={cn(tableVariants({ variant, size, rounded, density }), className)} {...props}>
        {children}
      </table>
    </div>
  )
})
Table.displayName = 'Table'

// Header/Body/Footer
/** 表头容器（可选吸顶） */
export interface THeadProps extends React.HTMLAttributes<HTMLTableSectionElement> {
  sticky?: boolean
}
export const THead = React.forwardRef<HTMLTableSectionElement, THeadProps>(({ className, sticky = false, ...props }, ref) => (
  <thead ref={ref} className={cn('bg-muted/50 dark:bg-muted/20', sticky && 'sticky top-0 z-10', className)} {...props} />
))
THead.displayName = 'THead'

/** 表体容器 */
export interface TBodyProps extends React.HTMLAttributes<HTMLTableSectionElement> {}
export const TBody = React.forwardRef<HTMLTableSectionElement, TBodyProps>(({ className, ...props }, ref) => (
  <tbody ref={ref} className={cn('[&_tr:last-child]:border-0', className)} {...props} />
))
TBody.displayName = 'TBody'

/** 表尾容器 */
export interface TFootProps extends React.HTMLAttributes<HTMLTableSectionElement> {}
export const TFoot = React.forwardRef<HTMLTableSectionElement, TFootProps>(({ className, ...props }, ref) => (
  <tfoot ref={ref} className={cn('border-t bg-muted/50 dark:bg-muted/20 font-medium [&>tr]:last:border-b-0', className)} {...props} />
))
TFoot.displayName = 'TFoot'

// Row variants（复刻 app 层）
const tableRowVariants2 = cva('border-b transition-colors', {
  variants: {
    variant: {
      default: 'hover:bg-muted/50 data-[state=selected]:bg-muted',
      striped: 'odd:bg-muted/25 even:bg-background hover:bg-muted/50',
      hoverable: 'hover:bg-muted/50 cursor-pointer',
      selectable: 'hover:bg-muted/50 cursor-pointer data-[state=selected]:bg-primary/10',
      none: '',
    },
  },
  defaultVariants: {
    variant: 'default',
  },
})

/** 行组件（支持默认/斑马/可选等行样式） */
export interface TRProps extends React.HTMLAttributes<HTMLTableRowElement>, VariantProps<typeof tableRowVariants2> {
  selected?: boolean
}
export const TR = React.forwardRef<HTMLTableRowElement, TRProps>(({ className, variant, selected, ...props }, ref) => (
  <tr ref={ref} className={cn(tableRowVariants2({ variant }), className)} data-state={selected ? 'selected' : undefined} {...props} />
))
TR.displayName = 'TR'

// Head/Cell variants（复刻 app 层）
const tableHeadVariants2 = cva('h-12 px-4 text-left align-middle font-medium text-muted-foreground [&:has([role=checkbox])]:pr-0', {
  variants: {
    align: { left: 'text-left', center: 'text-center', right: 'text-right' },
    size: { sm: 'h-8 px-2 text-xs', md: 'h-12 px-4 text-sm', lg: 'h-16 px-6 text-base' },
  },
  defaultVariants: { align: 'left', size: 'md' },
})

/** 表头单元格（支持排序交互标记） */
export interface THProps extends Omit<React.ThHTMLAttributes<HTMLTableCellElement>, 'align'>, VariantProps<typeof tableHeadVariants2> {
  sortable?: boolean
  sortDirection?: 'asc' | 'desc' | null
  onSort?: () => void
}
export const TH = React.forwardRef<HTMLTableCellElement, THProps>(({ className, align, size, sortable, sortDirection, onSort, children, ...props }, ref) => (
  <th ref={ref} className={cn(tableHeadVariants2({ align, size }), sortable && 'cursor-pointer select-none hover:bg-muted/50', className)} onClick={sortable ? onSort : undefined} {...props}>
    <div className="flex items-center gap-2">
      {children}
      {sortable && (
        <div className="flex flex-col">
          <span className={cn('text-xs leading-none', sortDirection === 'asc' ? 'text-foreground' : 'text-muted-foreground/50')}>▲</span>
          <span className={cn('text-xs leading-none', sortDirection === 'desc' ? 'text-foreground' : 'text-muted-foreground/50')}>▼</span>
        </div>
      )}
    </div>
  </th>
))
TH.displayName = 'TH'

const tableCellVariants2 = cva('px-4 py-3 align-middle [&:has([role=checkbox])]:pr-0', {
  variants: {
    align: { left: 'text-left', center: 'text-center', right: 'text-right' },
    size: { sm: 'px-2 py-1 text-xs', md: 'px-4 py-3 text-sm', lg: 'px-6 py-4 text-base' },
  },
  defaultVariants: { align: 'left', size: 'md' },
})

/** 普通单元格 */
export interface TDProps extends Omit<React.TdHTMLAttributes<HTMLTableCellElement>, 'align'>, VariantProps<typeof tableCellVariants2> {}
export const TD = React.forwardRef<HTMLTableCellElement, TDProps>(({ className, align, size, ...props }, ref) => (
  <td ref={ref} className={cn(tableCellVariants2({ align, size }), className)} {...props} />
))
TD.displayName = 'TD'

/** 标题/说明 */
export interface CaptionProps extends React.HTMLAttributes<HTMLTableCaptionElement> {}
export const Caption = React.forwardRef<HTMLTableCaptionElement, CaptionProps>(({ className, ...props }, ref) => (
  <caption ref={ref} className={cn('mt-2 text-sm text-muted-foreground', className)} {...props} />
))
Caption.displayName = 'Caption'

// 保持与旧导出兼容的空/加载/分页占位组件
/** 空数据占位行 */
export interface TableEmptyProps extends React.HTMLAttributes<HTMLTableRowElement> {
  colSpan: number
  message?: string
}
export const TableEmpty = React.forwardRef<HTMLTableRowElement, TableEmptyProps>(({ colSpan, message = '暂无数据', className, ...props }, ref) => (
  <tr ref={ref} className={cn('border-b border-border', className)} {...props}>
    <td colSpan={colSpan} className="px-4 py-6 text-center text-muted-foreground">{message}</td>
  </tr>
))
TableEmpty.displayName = 'TableEmpty'

/** 加载占位行 */
export interface TableLoadingProps extends React.HTMLAttributes<HTMLTableRowElement> {
  colSpan: number
  message?: string
}
export const TableLoading = React.forwardRef<HTMLTableRowElement, TableLoadingProps>(({ colSpan, message = '加载中...', className, ...props }, ref) => (
  <tr ref={ref} className={cn('border-b border-border', className)} {...props}>
    <td colSpan={colSpan} className="px-4 py-6 text-center text-muted-foreground">
      <span className="inline-flex items-center gap-2 justify-center">
        <span className="h-4 w-4 animate-spin rounded-full border-2 border-current border-t-transparent" />
        {message}
      </span>
    </td>
  </tr>
))
TableLoading.displayName = 'TableLoading'

/** 简易分页控件（外部驱动） */
export interface TablePaginationProps extends React.HTMLAttributes<HTMLDivElement> {
  page?: number
  pageSize?: number
  total?: number | null
  onPageChange?: (page: number) => void
  onPageSizeChange?: (size: number) => void
  pageSizeOptions?: number[]
}
export const TablePagination = React.forwardRef<HTMLDivElement, TablePaginationProps>(({ className, page = 1, pageSize = 10, total = null, onPageChange, onPageSizeChange, pageSizeOptions = [10, 20, 50, 100], ...props }, ref) => {
  const totalPages = total && pageSize ? Math.max(1, Math.ceil(total / pageSize)) : null
  const canPrev = totalPages ? page > 1 : true
  const canNext = totalPages ? page < totalPages : true
  return (
    <div ref={ref} className={cn('flex items-center justify-end gap-2 py-2', className)} {...props}>
      <div className="flex items-center gap-2">
        <button className="inline-flex h-8 w-8 items-center justify-center rounded-md text-muted-foreground hover:text-foreground hover:bg-muted/40 disabled:opacity-50" onClick={() => canPrev && onPageChange?.(Math.max(1, page - 1))} disabled={!canPrev}>
          ‹
        </button>
        <div className="text-sm text-muted-foreground">
          {totalPages ? (
            <span>
              第 <span className="text-foreground">{page}</span> / {totalPages} 页
            </span>
          ) : (
            <span>
              第 <span className="text-foreground">{page}</span> 页
            </span>
          )}
        </div>
        <button className="inline-flex h-8 w-8 items-center justify-center rounded-md text-muted-foreground hover:text-foreground hover:bg-muted/40 disabled:opacity-50" onClick={() => canNext && onPageChange?.(totalPages ? Math.min(totalPages, page + 1) : page + 1)} disabled={!canNext}>
          ›
        </button>
      </div>
      <div className="ml-4 flex items-center gap-2">
        <span className="text-sm text-muted-foreground">每页</span>
        <select className="h-8 rounded-md border border-border bg-background px-2 text-sm" value={pageSize} onChange={(e) => onPageSizeChange?.(Number(e.target.value))}>
          {pageSizeOptions.map((opt) => (
            <option key={opt} value={opt}>
              {opt}
            </option>
          ))}
        </select>
      </div>
    </div>
  )
})
TablePagination.displayName = 'TablePagination'


