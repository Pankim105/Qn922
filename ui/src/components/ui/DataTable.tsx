import * as React from 'react'
import { cn } from '@/lib/utils'
import { Table, THead, TBody, TR, TH, TD, Caption, TableEmpty, TablePagination } from './Table'
import type { TableVariants } from './variants/table-variants'

/**
 * DataTable 模块
 *
 * 提供轻量的数据表组件，内置搜索、选择与分页渲染（外部驱动），
 * 列定义通过 `columns` 描述，支持自定义单元格渲染与对齐。
 */

export type Align = 'left' | 'center' | 'right'

/** 列定义 */
export interface DataTableColumn<T = any> {
  key: keyof T | string
  title: React.ReactNode
  align?: Align
  sortable?: boolean
  width?: number
  fixed?: 'left' | 'right'
  render?: (value: any, record: T, index: number) => React.ReactNode
}

/** 分页配置（外部受控） */
export interface DataTablePaginationConfig {
  current: number
  pageSize: number
  total: number
  showTotal?: boolean
  showSizeChanger?: boolean
  onChange?: (page: number, pageSize: number) => void
}

/** DataTable 组件属性 */
export interface DataTableProps<T = any> extends Omit<React.HTMLAttributes<HTMLDivElement>, 'children'> {
  columns: DataTableColumn<T>[]
  dataSource: T[]
  rowKey?: keyof T | 'id' | ((record: T, index: number) => string)
  searchable?: boolean
  selectable?: boolean
  selectedRowKeys?: string[]
  onSelectChange?: (keys: string[]) => void
  pagination?: DataTablePaginationConfig
  variant?: TableVariants['variant']
  size?: TableVariants['size']
  rounded?: TableVariants['rounded']
  density?: TableVariants['density']
  caption?: React.ReactNode
  containerProps?: React.ComponentProps<typeof Table>['containerProps']
}

/**
 * 生成每行记录的唯一 key
 * - 支持函数、自定义字段名或默认 `id`
 */
function getRecordKey<T>(record: T, index: number, rowKey?: DataTableProps<T>['rowKey']): string {
  if (typeof rowKey === 'function') return rowKey(record, index)
  if (typeof rowKey === 'string') return String((record as any)[rowKey] ?? index)
  return String((record as any)['id'] ?? index)
}

/** 提取单元格原始值（供默认渲染使用） */
function getCellValue<T>(record: T, col: DataTableColumn<T>) {
  const key = col.key as any
  return (record as any)?.[key]
}

/**
 * DataTable 组件
 * - 仅管理前端层面的筛选/分页/选择渲染，排序交互由外部驱动
 */
export function DataTable<T = any>(props: DataTableProps<T>) {
  const {
    className,
    columns,
    dataSource,
    rowKey,
    searchable,
    selectable,
    selectedRowKeys = [],
    onSelectChange,
    pagination,
    variant,
    size,
    rounded,
    density,
    caption,
    containerProps,
    ...rest
  } = props

  // 映射表头/单元格尺寸：TH/TD 仅支持 'sm' | 'md' | 'lg'
  // - 将 'xl' 映射为 'lg'；将 null 归一为 undefined
  const mapHeadCellSize = (
    s: TableVariants['size'] | null | undefined,
  ): 'sm' | 'md' | 'lg' | undefined => {
    if (s === 'xl') return 'lg'
    if (s === 'sm' || s === 'md' || s === 'lg') return s
    return undefined
  }
  const headCellSize = mapHeadCellSize(size)

  // 搜索关键字（前端过滤）
  const [search, setSearch] = React.useState('')

  // 依据搜索关键字过滤数据
  const filteredData = React.useMemo(() => {
    if (!searchable || !search) return dataSource
    const q = search.toLowerCase()
    return dataSource.filter((row) =>
      Object.values(row as any).some((v) => String(v).toLowerCase().includes(q))
    )
  }, [dataSource, searchable, search])

  // 计算分页
  const current = pagination?.current ?? 1
  const pageSize = pagination?.pageSize ?? Math.max(1, filteredData.length)
  const start = (current - 1) * pageSize
  const end = start + pageSize
  const pageData = pagination ? filteredData.slice(start, end) : filteredData

  // 本页可见行的 key 集合
  const allVisibleKeys = React.useMemo(
    () => pageData.map((r, i) => getRecordKey(r, start + i, rowKey)),
    [pageData, rowKey, start]
  )

  // 全选状态
  const allChecked = selectable && allVisibleKeys.length > 0 && allVisibleKeys.every((k) => selectedRowKeys.includes(k))

  // 切换全选
  const toggleAll = () => {
    if (!selectable || !onSelectChange) return
    if (allChecked) {
      onSelectChange(selectedRowKeys.filter((k) => !allVisibleKeys.includes(k)))
    } else {
      const merged = Array.from(new Set([...selectedRowKeys, ...allVisibleKeys]))
      onSelectChange(merged)
    }
  }

  // 切换单条选择
  const toggleOne = (key: string) => {
    if (!selectable || !onSelectChange) return
    if (selectedRowKeys.includes(key)) {
      onSelectChange(selectedRowKeys.filter((k) => k !== key))
    } else {
      onSelectChange([...selectedRowKeys, key])
    }
  }

  return (
    <div className={cn('space-y-2', className)} {...rest}>
      {searchable && (
        <div className="flex items-center justify-between gap-2">
          <input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="搜索..."
            className="h-9 w-full max-w-sm rounded-md border border-border bg-background px-3 text-sm outline-none ring-offset-background placeholder:text-muted-foreground focus-visible:ring-2 focus-visible:ring-primary/50 focus-visible:ring-offset-2"
          />
        </div>
      )}

      <Table variant={variant} size={size} rounded={rounded} density={density} containerProps={containerProps}>
        {caption && <Caption>{caption}</Caption>}
        <THead sticky>
          <TR>
            {selectable && (
              <TH align="center" size={headCellSize}>
                <input type="checkbox" checked={!!allChecked} onChange={toggleAll} />
              </TH>
            )}
            {columns.map((col) => (
              <TH key={String(col.key)} align={col.align} size={headCellSize}>
                {col.title}
              </TH>
            ))}
          </TR>
        </THead>
        <TBody>
          {pageData.length === 0 ? (
            <TableEmpty colSpan={(columns.length + (selectable ? 1 : 0)) || 1} />
          ) : (
            pageData.map((row, idx) => {
              const key = getRecordKey(row, start + idx, rowKey)
              return (
                <TR key={key} selected={selectable ? selectedRowKeys.includes(key) : undefined}>
                  {selectable && (
                    <TD align="center" size={headCellSize}>
                      <input type="checkbox" checked={selectedRowKeys.includes(key)} onChange={() => toggleOne(key)} />
                    </TD>
                  )}
                  {columns.map((col) => {
                    const value = getCellValue(row, col)
                    return (
                      <TD key={String(col.key)} align={col.align} size={headCellSize}>
                        {col.render ? col.render(value, row, start + idx) : String(value ?? '')}
                      </TD>
                    )
                  })}
                </TR>
              )
            })
          )}
        </TBody>
      </Table>

      {pagination && (
        <TablePagination
          page={current}
          pageSize={pageSize}
          total={pagination.total}
          onPageChange={(p) => pagination.onChange?.(p, pageSize)}
          onPageSizeChange={(ps) => pagination.onChange?.(1, ps)}
        />
      )}
    </div>
  )
}


