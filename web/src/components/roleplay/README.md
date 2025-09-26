# RoleplayChat 组件重构

## 概述

原始的 `RoleplayChat.tsx` 组件有 2094 行代码，功能耦合度高，难以维护。现在已重构为多个更小、更专注的组件。

## 文件结构

```
roleplay/
├── index.ts                 # 导出所有组件和工具
├── types.ts                 # 类型定义
├── constants.tsx            # 常量和配置
├── messageParser.ts         # 消息解析工具
├── messageFormatter.ts      # 消息格式化工具
├── RoleplayChat.tsx         # 主组件
├── WorldSelector.tsx        # 世界选择组件
├── MessageList.tsx          # 消息列表组件
├── MessageInput.tsx         # 消息输入组件
├── MessageSection.tsx       # 消息部分渲染组件
└── README.md               # 本文档
```

## 组件职责

### 1. RoleplayChat.tsx (主组件)
- 管理整体状态和业务逻辑
- 处理 API 调用和会话管理
- 协调各个子组件

### 2. WorldSelector.tsx
- 显示世界模板选择界面
- 处理世界选择逻辑

### 3. MessageList.tsx
- 渲染消息列表
- 处理消息显示和滚动
- 管理消息可见性

### 4. MessageInput.tsx
- 处理用户输入
- 管理输入状态和自由行动模式
- 提供快捷操作按钮

### 5. MessageSection.tsx
- 渲染不同类型的消息部分
- 处理结构化内容的显示
- 管理选择项和交互

## 工具模块

### types.ts
- 定义所有接口和类型
- 提供类型安全

### constants.tsx
- 图标映射
- 颜色配置
- 角色名称映射

### messageParser.ts
- 解析结构化消息内容
- 提取指令标记
- 内容验证

### messageFormatter.ts
- 格式化不同类型的消息内容
- 处理任务、评估、状态等信息
- 生成 HTML 内容

## 使用方式

```tsx
import { RoleplayChat } from './roleplay';

<RoleplayChat
  isAuthenticated={isAuthenticated}
  user={user}
  onAuthFailure={onAuthFailure}
/>
```

## 重构优势

1. **可维护性**: 每个组件职责单一，易于理解和修改
2. **可复用性**: 子组件可以在其他地方复用
3. **可测试性**: 小组件更容易进行单元测试
4. **类型安全**: 完整的 TypeScript 类型定义
5. **性能优化**: 减少不必要的重新渲染
6. **代码组织**: 逻辑清晰，文件结构合理

## 迁移说明

原有的 `RoleplayChat.tsx` 现在只是一个简单的包装器，直接使用重构后的组件。所有功能保持不变，但代码结构更加清晰。
