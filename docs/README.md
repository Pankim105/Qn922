# QN Contest 项目文档中心

## 文档结构

本项目采用分层文档架构，所有文档统一管理在 `docs/` 目录下：

```
docs/
├── README.md                    # 文档中心首页（本文件）
├── getting-started/             # 快速开始指南
│   ├── README.md               # 快速开始总览
│   ├── installation.md         # 安装指南
│   ├── quick-start.md          # 快速体验
│   └── configuration.md        # 配置说明
├── architecture/               # 系统架构文档
│   ├── README.md              # 架构总览
│   ├── backend-architecture.md # 后端架构
│   ├── frontend-architecture.md # 前端架构
│   ├── database-design.md     # 数据库设计
│   └── api-design.md          # API设计
├── development/                # 开发文档
│   ├── README.md              # 开发指南
│   ├── coding-standards.md    # 编码规范
│   ├── testing.md             # 测试指南
│   └── deployment.md          # 部署指南
├── features/                   # 功能文档
│   ├── README.md              # 功能总览
│   ├── roleplay-system.md     # 角色扮演系统
│   ├── memory-system.md       # 记忆系统
│   ├── world-templates.md     # 世界模板
│   └── ai-integration.md      # AI集成
├── api/                       # API文档
│   ├── README.md              # API总览
│   ├── authentication.md      # 认证API
│   ├── chat.md                # 聊天API
│   ├── roleplay.md            # 角色扮演API
│   └── reference.md           # API参考
└── maintenance/               # 维护文档
    ├── README.md              # 维护指南
    ├── troubleshooting.md     # 故障排除
    ├── performance.md         # 性能优化
    └── changelog.md           # 更新日志
```

## 文档导航

### 🚀 快速开始
- [安装指南](getting-started/installation.md) - 环境配置和项目安装
- [快速体验](getting-started/quick-start.md) - 5分钟快速上手
- [配置说明](getting-started/configuration.md) - 详细配置选项

### 🏗️ 系统架构
- [架构总览](architecture/README.md) - 整体架构设计
- [后端架构](architecture/backend-architecture.md) - Spring Boot后端设计
- [前端架构](architecture/frontend-architecture.md) - React前端设计
- [数据库设计](architecture/database-design.md) - MySQL数据库结构
- [更新日志](CHANGELOG.md) - 版本更新记录

### 💻 开发指南
- [开发环境搭建](development/README.md) - 开发环境配置
- [编码规范](development/coding-standards.md) - 代码规范和最佳实践
- [测试指南](development/testing.md) - 单元测试和集成测试
- [部署指南](development/deployment.md) - 生产环境部署

### ⭐ 核心功能
- [角色扮演系统](features/roleplay-system.md) - 核心功能详解
- [记忆系统](features/memory-system.md) - 智能记忆管理
- [评估系统](features/assessment-system.md) - AI评估和游戏逻辑处理
- [收敛机制](features/convergence-system.md) - 智能故事收敛引导
- [世界模板](features/world-templates.md) - 5种世界类型
- [AI集成](features/ai-integration.md) - LangChain4j集成
- [语音交互系统](features/voice-system.md) - 语音识别和交互
- [UI组件库](features/ui-components.md) - 现代化UI组件

### 📚 API文档
- [API总览](api/README.md) - 所有API接口概览
- [认证API](api/authentication.md) - 用户认证相关接口
- [聊天API](api/chat.md) - 流式聊天接口
- [角色扮演API](api/roleplay.md) - 角色扮演专用接口

### 🔧 维护指南
- [故障排除](maintenance/troubleshooting.md) - 常见问题解决
- [性能优化](maintenance/performance.md) - 性能调优建议
- [更新日志](maintenance/changelog.md) - 版本更新记录

## 文档维护原则

### 📝 文档规范
1. **统一格式**：所有文档使用Markdown格式
2. **清晰结构**：使用标准的标题层次结构
3. **及时更新**：代码变更时同步更新文档
4. **避免重复**：相同信息只在一个地方维护

### 🔄 更新流程
1. **代码变更** → 更新相关文档
2. **功能新增** → 创建对应文档
3. **架构调整** → 更新架构文档
4. **定期审查** → 每月检查文档时效性

### 📋 文档检查清单
- [ ] 文档结构清晰
- [ ] 内容准确完整
- [ ] 示例代码可运行
- [ ] 链接有效
- [ ] 格式统一

## 贡献指南

### 如何更新文档
1. 在对应目录下找到相关文档
2. 按照现有格式进行修改
3. 确保内容准确且易于理解
4. 提交Pull Request

### 如何添加新文档
1. 确定文档所属分类
2. 在对应目录下创建新文件
3. 更新本README.md的导航链接
4. 遵循现有的文档格式

## 联系方式

如有文档相关问题，请：
- 提交Issue到项目仓库
- 联系项目维护者
- 查看[故障排除指南](maintenance/troubleshooting.md)

---

**维护者**: QN Contest Team
