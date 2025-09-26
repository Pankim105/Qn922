# QN Contest - 角色扮演AI系统

一个基于Spring Boot和LangChain4j的智能角色扮演AI系统，支持多种世界类型的沉浸式角色扮演体验。

## 核心特性

- **智能AI对话**: 基于LangChain4j的流式AI聊天系统
- **多世界角色扮演**: 5种专业化世界类型和AI角色
- **游戏机制集成**: 自动骰子检定、任务系统、学习挑战
- **智能记忆管理**: 自动存储和检索重要事件
- **安全认证**: JWT令牌认证和权限管理
- **状态持久化**: 世界状态版本控制和数据完整性

## 支持的世界类型

| 世界类型 | AI角色 | 特色功能 |
|---------|-------|----------|
| 异世界探险 | 游戏主持人(DM) | 魔法、战斗、冒险 |
| 西方魔幻 | 贤者向导 | 法师、骑士、龙族 |
| 东方武侠 | 江湖前辈 | 武功、门派、侠义 |
| 日式校园 | 校园向导 | 社团、祭典、青春 |
| 寓教于乐 | 智慧导师 | 学习、挑战、成长 |

## 快速开始

### 环境要求
- Java 17+
- Maven 3.8+
- MySQL 8.0+
- Node.js 16+

### 1. 克隆项目
```bash
git clone https://github.com/your-repo/qncontest.git
cd qncontest
```

### 2. 启动后端
```bash
cd backend
# 配置数据库和API Key
mvn spring-boot:run
```

### 3. 启动前端
```bash
# 先构建UI组件库
cd ui
npm install
npm run build

# 再启动Web前端
cd ../web
npm install
npm run dev
```

### 4. 访问系统
打开浏览器访问 `http://localhost:5173`

## 测试账户

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | admin123 | 管理员 |
| panzijian1234 | 123456 | 普通用户 |

## 文档

- [📖 完整文档](docs/README.md) - 项目文档中心
- [🚀 快速开始](docs/getting-started/README.md) - 快速上手指南
- [🏗️ 系统架构](docs/architecture/README.md) - 架构设计文档
- [⭐ 功能特性](docs/features/README.md) - 功能模块详解
- [📚 API文档](docs/api/README.md) - API接口文档
- [🔧 开发指南](docs/development/README.md) - 开发环境搭建
- [🛠️ 维护指南](docs/maintenance/README.md) - 系统维护文档

## 技术栈

### 后端
- **框架**: Spring Boot 3.x + Spring Security
- **AI集成**: LangChain4j + DashScope (通义千问)
- **数据库**: MySQL 8.0 + JPA/Hibernate
- **认证**: JWT + 刷新令牌机制
- **构建**: Maven

### 前端
- **框架**: React 18 + TypeScript
- **构建工具**: Vite
- **UI库**: 自定义组件库 + Tailwind CSS
- **包管理**: npm/yarn

## 项目结构

```
qncontest/
├── docs/                    # 项目文档
│   ├── getting-started/     # 快速开始指南
│   ├── architecture/        # 系统架构文档
│   ├── development/         # 开发文档
│   ├── features/           # 功能文档
│   ├── api/                # API文档
│   └── maintenance/        # 维护文档
├── backend/                # 后端服务
│   ├── src/main/java/      # Java源码
│   ├── src/main/resources/ # 配置文件
│   └── database-init.sql   # 数据库初始化脚本
├── web/                    # 前端应用
│   ├── src/                # React源码
│   ├── public/             # 静态资源
│   └── package.json        # 前端依赖
├── ui/                     # UI组件库
│   ├── src/components/     # React组件
│   └── package.json        # 组件库依赖
└── plan/                   # 项目规划文档
```

## 核心功能

### 🎭 角色扮演系统
- 5种世界类型，每种都有专业的AI角色
- 智能故事生成和收敛机制
- 骰子检定、任务系统、学习挑战

### 🧠 记忆系统
- 智能记忆存储和检索
- 8种记忆类型自动分类
- 重要性评估和自动清理

### 🌍 世界模板系统
- 预置世界模板
- 动态配置和自定义
- 收敛场景设计

### 🤖 AI集成系统
- LangChain4j集成
- 流式响应处理
- 智能提示词构建

## 性能指标

- **AI响应延迟**: < 2秒
- **记忆检索**: < 100ms
- **页面加载**: < 1秒
- **并发支持**: 1000+ 同时在线用户

## 开发状态

- [x] 基础聊天系统
- [x] 用户认证系统
- [x] 角色扮演功能
- [x] LangChain优化
- [x] 技能动作系统
- [x] 记忆管理系统
- [x] 接口架构重构
- [ ] 向量数据库集成
- [ ] 多模态支持
- [ ] 语音交互

## 贡献指南

1. Fork 项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

## 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 支持

如有问题或建议，请：

- 📖 查看[完整文档](docs/README.md)
- 🐛 提交[Issue](https://github.com/your-repo/issues)
- 💬 加入社区讨论

---

**开始您的角色扮演之旅吧！** 🎭

**版本**: 2.0  
**最后更新**: 2025-01-27  
**许可证**: MIT
