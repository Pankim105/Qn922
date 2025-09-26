# API文档总览

QN Contest提供完整的RESTful API接口，支持用户认证、聊天会话、角色扮演等核心功能。

## API基础信息

- **Base URL**: `http://localhost:8080/api`
- **认证方式**: Bearer Token (JWT)
- **Content-Type**: `application/json`
- **字符编码**: UTF-8

## 接口分类

### 🔐 认证接口
- [用户注册](authentication.md#用户注册)
- [用户登录](authentication.md#用户登录)
- [刷新令牌](authentication.md#刷新令牌)
- [用户登出](authentication.md#用户登出)

### 💬 聊天接口
- [获取会话列表](chat.md#获取会话列表)
- [获取会话消息](chat.md#获取会话消息)
- [流式聊天](chat.md#流式聊天)
- [删除会话](chat.md#删除会话)

### 🎭 角色扮演接口
- [获取世界模板](roleplay.md#获取世界模板)
- [初始化会话](roleplay.md#初始化会话)
- [骰子检定](roleplay.md#骰子检定)
- [世界状态管理](roleplay.md#世界状态管理)

### 📚 API参考
- [完整API参考](reference.md)
- [错误码说明](reference.md#错误码)
- [数据模型](reference.md#数据模型)

## 快速开始

### 1. 获取访问令牌
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"panzijian1234","password":"123456"}'
```

### 2. 使用令牌访问API
```bash
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8080/api/roleplay/worlds
```

## 认证机制

### JWT令牌
- **访问令牌**: 15分钟有效期
- **刷新令牌**: 24小时有效期
- **令牌格式**: `Bearer <token>`

### 请求头
```http
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json
```

## 响应格式

### 成功响应
```json
{
  "success": true,
  "message": "操作成功",
  "data": {
    // 响应数据
  }
}
```

### 错误响应
```json
{
  "success": false,
  "message": "错误描述",
  "error": "ERROR_CODE",
  "timestamp": "2025-01-27T10:00:00Z"
}
```

## 流式响应

### SSE格式
```http
Content-Type: text/event-stream
Cache-Control: no-cache
Connection: keep-alive
```

### 事件格式
```
event: message
data: {"content":"AI回复内容"}

event: complete
data: {"status":"success"}
```

## 错误处理

### HTTP状态码
| 状态码 | 说明 |
|--------|------|
| 200 | 请求成功 |
| 400 | 请求参数错误 |
| 401 | 未认证或令牌无效 |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

### 错误码
| 错误码 | 说明 |
|--------|------|
| INVALID_TOKEN | 令牌无效 |
| TOKEN_EXPIRED | 令牌过期 |
| INSUFFICIENT_PERMISSIONS | 权限不足 |
| RESOURCE_NOT_FOUND | 资源不存在 |
| VALIDATION_ERROR | 参数验证失败 |

## 限流策略

### 请求限制
- **认证接口**: 每分钟10次
- **聊天接口**: 每分钟60次
- **角色扮演接口**: 每分钟30次

### 限流响应
```json
{
  "success": false,
  "message": "请求过于频繁，请稍后重试",
  "error": "RATE_LIMIT_EXCEEDED",
  "retryAfter": 60
}
```

## 测试工具

### Postman集合
- 导入Postman集合文件
- 配置环境变量
- 运行API测试

### curl示例
```bash
# 登录
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"panzijian1234","password":"123456"}'

# 获取世界模板
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8080/api/roleplay/worlds

# 流式聊天
curl -X POST http://localhost:8080/api/chat/stream \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{"message":"你好","sessionId":"test_session"}'
```

## 开发指南

### 1. 环境配置
- 设置API Base URL
- 配置认证令牌
- 设置请求超时

### 2. 错误处理
- 检查HTTP状态码
- 处理业务错误码
- 实现重试机制

### 3. 流式处理
- 处理SSE事件
- 管理连接状态
- 处理连接断开

## 版本管理

### API版本
- **当前版本**: v1.0
- **版本标识**: 通过URL路径标识
- **向后兼容**: 保持向后兼容性

### 版本更新
- 新功能通过新版本发布
- 旧版本继续支持
- 提供迁移指南

## 监控和日志

### 请求日志
- 记录所有API请求
- 包含请求参数和响应
- 支持日志查询和分析

### 性能监控
- 响应时间统计
- 错误率监控
- 并发量统计

## 安全考虑

### 数据安全
- 敏感数据加密传输
- 输入验证和过滤
- SQL注入防护

### 访问控制
- JWT令牌认证
- 角色权限控制
- API访问限制

## 获取帮助

- 📖 查看[详细API文档](reference.md)
- 🐛 报告问题到[项目Issues](https://github.com/your-repo/issues)
- 💬 加入社区讨论

---

**开始使用QN Contest API，构建您的角色扮演应用！** 🎭
