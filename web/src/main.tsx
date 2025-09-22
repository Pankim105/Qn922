import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
// 导入UI组件库的样式
// @ts-ignore
import 'modern-ui-components/styles'
import Test from './test.tsx'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <Test />
  </StrictMode>,
)
