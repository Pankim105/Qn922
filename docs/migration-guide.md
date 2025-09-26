# 文档迁移指南

本指南说明如何将现有文档迁移到新的文档结构中。

## 迁移概述

### 迁移目标
- 统一文档管理，所有文档集中在 `docs/` 目录
- 建立清晰的文档层次结构
- 消除重复和过时的文档
- 提供统一的文档导航

### 迁移原则
- 保留有价值的内容
- 更新过时的信息
- 统一文档格式
- 建立清晰的链接关系

## 文档映射表

### 现有文档 → 新文档结构

| 原文档路径 | 新文档路径 | 状态 | 说明 |
|-----------|-----------|------|------|
| `backend/README.md` | `docs/getting-started/installation.md` | ✅ 已迁移 | 安装指南 |
| `backend/API_REFERENCE.md` | `docs/api/reference.md` | 🔄 待迁移 | API参考 |
| `backend/DATABASE_README.md` | `docs/architecture/database-design.md` | 🔄 待迁移 | 数据库设计 |
| `backend/PROJECT_DOCUMENTATION.md` | `docs/architecture/README.md` | ✅ 已整合 | 架构总览 |
| `backend/INTERFACE_ARCHITECTURE.md` | `docs/architecture/backend-architecture.md` | ✅ 已整合 | 后端架构 |
| `backend/REFACTORING_SUMMARY.md` | `docs/development/refactoring-history.md` | 🔄 待迁移 | 重构历史 |
| `README_MEMORY_SYSTEM.md` | `docs/features/memory-system.md` | ✅ 已迁移 | 记忆系统 |
| `PROMPT_SYSTEM_REFACTOR.md` | `docs/development/prompt-system.md` | 🔄 待迁移 | 提示词系统 |
| `plan/ROLEPLAY_APP_PLAN.md` | `docs/features/roleplay-system.md` | ✅ 已整合 | 角色扮演系统 |
| `plan/BACKEND_DATA_ARCHITECTURE.md` | `docs/architecture/database-design.md` | 🔄 待迁移 | 数据架构 |
| `plan/EXISTING_ARCHITECTURE_IMPLEMENTATION.md` | `docs/development/architecture-implementation.md` | 🔄 待迁移 | 架构实现 |
| `plan/TREE_BASED_STORY_SYSTEM_DESIGN.md` | `docs/features/story-system.md` | 🔄 待迁移 | 故事系统 |
| `web/README.md` | `docs/architecture/frontend-architecture.md` | 🔄 待迁移 | 前端架构 |
| `web/AUTH_TEST_README.md` | `docs/development/testing.md` | 🔄 待迁移 | 测试指南 |
| `ui/README.md` | `docs/architecture/ui-components.md` | 🔄 待迁移 | UI组件 |

## 迁移步骤

### 1. 创建新文档结构
```bash
mkdir -p docs/{getting-started,architecture,development,features,api,maintenance}
```

### 2. 迁移核心文档
- ✅ 创建文档中心首页 (`docs/README.md`)
- ✅ 创建快速开始指南 (`docs/getting-started/`)
- ✅ 创建架构文档 (`docs/architecture/`)
- ✅ 创建功能文档 (`docs/features/`)
- ✅ 创建API文档 (`docs/api/`)

### 3. 迁移现有文档
```bash
# 迁移API参考文档
cp backend/API_REFERENCE.md docs/api/reference.md

# 迁移数据库文档
cp backend/DATABASE_README.md docs/architecture/database-design.md

# 迁移前端文档
cp web/README.md docs/architecture/frontend-architecture.md

# 迁移测试文档
cp web/AUTH_TEST_README.md docs/development/testing.md
```

### 4. 更新文档链接
- 更新所有内部链接
- 统一文档格式
- 添加导航链接

### 5. 清理旧文档
- 删除重复文档
- 更新README文件
- 添加迁移说明

## 文档更新清单

### 需要更新的文档
- [ ] `backend/README.md` - 更新为指向新文档结构
- [ ] `web/README.md` - 更新为指向新文档结构
- [ ] `ui/README.md` - 更新为指向新文档结构
- [ ] 项目根目录README - 更新文档链接

### 需要删除的文档
- [ ] `backend/PROJECT_DOCUMENTATION.md` - 内容已整合
- [ ] `backend/INTERFACE_ARCHITECTURE.md` - 内容已整合
- [ ] `backend/REFACTORING_SUMMARY.md` - 内容已整合
- [ ] `README_MEMORY_SYSTEM.md` - 内容已迁移
- [ ] `PROMPT_SYSTEM_REFACTOR.md` - 内容已整合

### 需要保留的文档
- [ ] `backend/database-init.sql` - 数据库脚本
- [ ] `backend/pom.xml` - Maven配置
- [ ] `web/package.json` - 前端配置
- [ ] `ui/package.json` - UI组件配置

## 文档格式统一

### Markdown格式规范
- 使用标准Markdown语法
- 统一标题层次结构
- 使用表格展示结构化数据
- 添加代码块语法高亮

### 链接格式规范
- 使用相对路径链接
- 统一链接格式
- 添加链接描述

### 图片格式规范
- 统一图片路径
- 添加图片描述
- 优化图片大小

## 验证检查

### 内容检查
- [ ] 所有重要内容已迁移
- [ ] 文档内容准确完整
- [ ] 链接有效可用
- [ ] 格式统一规范

### 结构检查
- [ ] 文档层次清晰
- [ ] 导航链接正确
- [ ] 分类合理
- [ ] 易于查找

### 功能检查
- [ ] 文档可以正常访问
- [ ] 链接跳转正常
- [ ] 搜索功能正常
- [ ] 移动端适配

## 后续维护

### 文档更新流程
1. 代码变更时同步更新文档
2. 新功能添加时创建对应文档
3. 定期检查文档时效性
4. 收集用户反馈优化文档

### 文档质量保证
- 定期审查文档内容
- 更新过时信息
- 优化文档结构
- 提升用户体验

## 迁移完成后的工作

### 1. 更新项目README
```markdown
# QN Contest - 角色扮演AI系统

## 文档
- [📖 完整文档](docs/README.md) - 项目文档中心
- [🚀 快速开始](docs/getting-started/README.md) - 快速上手指南
- [🏗️ 系统架构](docs/architecture/README.md) - 架构设计文档
- [⭐ 功能特性](docs/features/README.md) - 功能模块详解
- [📚 API文档](docs/api/README.md) - API接口文档
```

### 2. 更新各模块README
- 后端模块：指向架构文档
- 前端模块：指向前端架构文档
- UI组件：指向UI组件文档

### 3. 添加文档贡献指南
- 文档编写规范
- 贡献流程说明
- 质量检查标准

## 总结

文档迁移完成后，项目将拥有：

- ✅ **统一的文档管理** - 所有文档集中在docs目录
- ✅ **清晰的层次结构** - 按功能分类组织文档
- ✅ **完整的导航系统** - 便于查找和访问
- ✅ **统一的格式规范** - 提升文档质量
- ✅ **持续维护机制** - 确保文档时效性

这将显著提升项目的可维护性和用户体验。
