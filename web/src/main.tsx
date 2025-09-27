import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
// 导入UI组件库的样式
// @ts-ignore
import 'modern-ui-components/styles'
// 导入高级材质和纹理渲染样式
// @ts-ignore
import './styles/advanced-rendering.css'
import App from './app.tsx'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
