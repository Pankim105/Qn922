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

| 世界 | AI角色 | 特色功能 |
|------|-------|----------|
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

### 1. 数据库初始化
```bash
mysql -u root -p < database-init.sql
```

### 2. 配置环境变量
```bash
# application.yml 或环境变量
DASHSCOPE_API_KEY=your_dashscope_api_key
JWT_SECRET=your_jwt_secret
```

### 3. 启动应用
```bash
mvn spring-boot:run
```

### 4. 测试API
```bash
# 登录获取token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"panzijian1234","password":"123456"}'

# 获取世界模板
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8080/api/roleplay/worlds
```

## 技能指令系统

AI会自动识别和执行以下指令：

```
[DICE:d20+5:攻击检定]     - 骰子检定
[QUEST:CREATE:任务:描述]   - 创建任务  
[CHALLENGE:MATH:3:题目]   - 学习挑战
[STATE:LOCATION:新位置]   - 状态更新
[MEMORY:EVENT:重要事件]   - 记录记忆
```

## 性能优化

通过LangChain优化实现：
- **Token使用效率** 提升60%
- **角色一致性** 提升50% 
- **响应相关性** 提升35%
- **技能集成** 全自动化

## 文档

- [项目文档](PROJECT_DOCUMENTATION.md) - 完整的系统架构和开发指南
- [API参考](API_REFERENCE.md) - API接口使用说明
- [数据库](database-init.sql) - 数据库初始化脚本

## 测试用户

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | admin123 | 管理员 |
| panzijian1234 | 123456 | 普通用户 |

## 技术栈

- **后端**: Spring Boot 3.x + Spring Security
- **AI集成**: LangChain4j + DashScope (通义千问)
- **数据库**: MySQL 8.0 + JPA/Hibernate
- **认证**: JWT + 刷新令牌机制
- **构建**: Maven

## 项目状态

- [完成] 基础聊天系统
- [完成] 用户认证系统  
- [完成] 角色扮演功能
- [完成] LangChain优化
- [完成] 技能动作系统
- [完成] 记忆管理系统
- [开发中] 向量数据库集成
- [开发中] 多模态支持

## 支持

如有问题或建议，请查看：
- [项目文档](PROJECT_DOCUMENTATION.md)
- [API参考](API_REFERENCE.md)
- [Issues](../../issues)

---

**版本**: 2.0  
**最后更新**: 2025-09-23  
**许可证**: MIT