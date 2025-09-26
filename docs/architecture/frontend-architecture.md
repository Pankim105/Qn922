# 前端架构设计

QN Contest前端采用模块化架构，包含UI组件库和Web应用两个独立项目，通过构建系统实现组件复用。

## 架构概览

```
前端架构
├── ui/                    # UI组件库项目
│   ├── src/components/    # React组件源码
│   ├── dist/             # 编译后的组件库
│   └── package.json      # 组件库配置
└── web/                  # Web应用项目
    ├── src/              # 应用源码
    ├── package.json      # 应用配置
    └── vite.config.ts    # 构建配置
```

## 项目结构

### 1. UI组件库 (ui/)

#### 项目定位
- **独立组件库**: 可复用的React组件集合
- **设计系统**: 统一的UI设计规范和组件
- **构建产物**: 编译为可被其他项目引用的库

#### 目录结构
```
ui/
├── src/
│   ├── components/           # 组件源码
│   │   ├── ui/              # 基础UI组件
│   │   │   ├── Button.tsx   # 按钮组件
│   │   │   ├── Card.tsx     # 卡片组件
│   │   │   ├── Modal.tsx    # 模态框组件
│   │   │   └── ...          # 其他组件
│   │   └── visuals/         # 视觉组件
│   │       └── WaterRipple.tsx
│   ├── lib/                 # 工具函数
│   │   └── utils.ts
│   └── index.ts             # 导出入口
├── dist/                    # 构建产物
│   ├── index.d.ts          # TypeScript声明文件
│   ├── index.esm.js        # ES模块版本
│   ├── index.cjs.js        # CommonJS版本
│   └── style.css           # 样式文件
├── package.json            # 组件库配置
├── vite.config.ts          # 构建配置
└── tailwind.config.js      # 样式配置
```

#### 构建配置
```typescript
// vite.config.ts
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { resolve } from 'path'

export default defineConfig({
  plugins: [react()],
  build: {
    lib: {
      entry: resolve(__dirname, 'src/index.ts'),
      name: 'QnContestUI',
      fileName: (format) => `index.${format}.js`
    },
    rollupOptions: {
      external: ['react', 'react-dom'],
      output: {
        globals: {
          react: 'React',
          'react-dom': 'ReactDOM'
        }
      }
    }
  }
})
```

### 2. Web应用 (web/)

#### 项目定位
- **主应用**: 角色扮演AI系统的主界面
- **组件消费**: 使用UI组件库中的组件
- **业务逻辑**: 实现具体的业务功能

#### 目录结构
```
web/
├── src/
│   ├── components/          # 业务组件
│   │   ├── AIChat.tsx      # AI聊天组件
│   │   ├── AuthModal.tsx   # 认证模态框
│   │   ├── RoleplayChat.tsx # 角色扮演聊天
│   │   └── roleplay/       # 角色扮演相关组件
│   ├── utils/              # 工具函数
│   │   └── api.ts          # API调用
│   ├── main.tsx            # 应用入口
│   └── app.tsx             # 根组件
├── public/                 # 静态资源
├── package.json            # 应用配置
└── vite.config.ts          # 构建配置
```

#### 依赖配置
```json
{
  "dependencies": {
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "@qncontest/ui": "file:../ui/dist"
  }
}
```

## 构建流程

### 1. UI组件库构建

#### 构建命令
```bash
cd ui
npm install
npm run build
```

#### 构建过程
1. **TypeScript编译**: 将TSX文件编译为JavaScript
2. **样式处理**: 处理CSS和Tailwind样式
3. **打包优化**: 生成ES模块和CommonJS版本
4. **类型声明**: 生成TypeScript声明文件
5. **输出产物**: 生成到dist目录

#### 构建产物
- `index.d.ts`: TypeScript类型声明
- `index.esm.js`: ES模块版本
- `index.cjs.js`: CommonJS版本
- `style.css`: 样式文件

### 2. Web应用构建

#### 构建命令
```bash
cd web
npm install
npm run dev    # 开发模式
npm run build  # 生产构建
```

#### 依赖关系
- Web应用依赖UI组件库的构建产物
- 通过file:协议引用本地构建的组件库
- 开发时支持热重载和快速构建

## 开发工作流

### 1. 组件开发流程

#### 在UI组件库中开发新组件
```bash
# 1. 进入UI组件库目录
cd ui

# 2. 开发新组件
# 在 src/components/ui/ 下创建新组件

# 3. 更新导出文件
# 在 src/index.ts 中添加导出

# 4. 构建组件库
npm run build

# 5. 在Web应用中使用
cd ../web
# 组件会自动更新（开发模式）
```

#### 组件使用示例
```typescript
// web/src/components/Example.tsx
import { Button, Card, Modal } from '@qncontest/ui'

export function Example() {
  return (
    <Card>
      <Button onClick={() => console.log('clicked')}>
        点击我
      </Button>
      <Modal isOpen={true}>
        模态框内容
      </Modal>
    </Card>
  )
}
```

### 2. 样式系统

#### Tailwind CSS配置
```javascript
// ui/tailwind.config.js
module.exports = {
  content: [
    './src/**/*.{js,ts,jsx,tsx}',
  ],
  theme: {
    extend: {
      colors: {
        primary: {
          50: '#eff6ff',
          500: '#3b82f6',
          900: '#1e3a8a',
        }
      }
    },
  },
  plugins: [],
}
```

#### 样式使用
```typescript
// 在组件中使用Tailwind类名
<Button className="bg-primary-500 hover:bg-primary-600 text-white px-4 py-2 rounded">
  按钮
</Button>
```

## 技术栈

### UI组件库技术栈
- **React 18**: 组件开发框架
- **TypeScript**: 类型安全
- **Vite**: 构建工具
- **Tailwind CSS**: 样式框架
- **PostCSS**: CSS处理

### Web应用技术栈
- **React 18**: 应用框架
- **TypeScript**: 类型安全
- **Vite**: 构建工具
- **Axios**: HTTP客户端
- **React Router**: 路由管理

## 部署策略

### 1. 开发环境
```bash
# 启动开发服务器
cd ui && npm run build && cd ../web && npm run dev
```

### 2. 生产环境
```bash
# 构建UI组件库
cd ui
npm run build

# 构建Web应用
cd ../web
npm run build

# 部署dist目录到服务器
```

### 3. CI/CD流程
```yaml
# GitHub Actions示例
- name: Build UI Library
  run: |
    cd ui
    npm ci
    npm run build

- name: Build Web App
  run: |
    cd web
    npm ci
    npm run build
```

## 性能优化

### 1. 组件库优化
- **Tree Shaking**: 只打包使用的组件
- **代码分割**: 按需加载组件
- **类型优化**: 生成精简的类型声明

### 2. Web应用优化
- **懒加载**: 路由级别的代码分割
- **缓存策略**: 静态资源缓存
- **压缩优化**: 代码和资源压缩

## 最佳实践

### 1. 组件设计
- **单一职责**: 每个组件只负责一个功能
- **可复用性**: 设计通用的组件接口
- **类型安全**: 完整的TypeScript类型定义

### 2. 样式管理
- **设计系统**: 统一的颜色、字体、间距规范
- **响应式设计**: 支持多种屏幕尺寸
- **主题支持**: 支持明暗主题切换

### 3. 开发规范
- **命名规范**: 统一的文件和组件命名
- **代码风格**: 使用ESLint和Prettier
- **测试覆盖**: 组件单元测试

## 故障排除

### 1. 构建失败
- 检查Node.js版本 >= 16
- 清除node_modules重新安装
- 检查TypeScript配置

### 2. 组件不显示
- 确保UI组件库已构建
- 检查组件导入路径
- 验证样式文件加载

### 3. 类型错误
- 检查TypeScript声明文件
- 更新组件库版本
- 验证类型定义

## 扩展计划

### 1. 短期目标
- [ ] 添加更多基础组件
- [ ] 完善组件文档
- [ ] 添加组件测试

### 2. 中期目标
- [ ] 支持主题定制
- [ ] 添加动画组件
- [ ] 国际化支持

### 3. 长期目标
- [ ] 组件库独立发布
- [ ] 支持多框架
- [ ] 可视化组件编辑器

## 总结

前端架构采用模块化设计，通过UI组件库和Web应用的分离，实现了：

- ✅ **组件复用**: UI组件库可在多个项目中使用
- ✅ **开发效率**: 组件开发和应用开发并行进行
- ✅ **维护性**: 清晰的职责分离和依赖关系
- ✅ **扩展性**: 易于添加新组件和功能
- ✅ **性能优化**: 按需加载和代码分割

这种架构设计为前端开发提供了良好的基础，支持快速迭代和功能扩展。
