# QN Contest API 参考文档

## 基础信息

- **Base URL**: `http://localhost:8080/api`
- **认证方式**: Bearer Token (JWT)
- **Content-Type**: `application/json`

## 认证接口

### 用户注册
```http
POST /auth/register
Content-Type: application/json

{
  "username": "string",
  "password": "string", 
  "email": "string"
}
```

### 用户登录
```http
POST /auth/login
Content-Type: application/json

{
  "username": "panzijian1234",
  "password": "123456"
}
```

**响应示例**:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "uuid-string",
  "tokenType": "Bearer",
  "expiresIn": 900000,
  "user": {
    "id": 1,
    "username": "panzijian1234",
    "email": "user@example.com",
    "role": "USER"
  }
}
```

### 刷新令牌
```http
POST /auth/refresh
Content-Type: application/json

{
  "refreshToken": "string"
}
```

### 用户登出
```http
POST /auth/logout
Content-Type: application/json

{
  "refreshToken": "string"
}
```

## 聊天接口

### 获取会话列表
```http
GET /chat/session/list
Authorization: Bearer {token}
```

**响应示例**:
```json
{
  "success": true,
  "message": "获取会话列表成功",
  "data": {
    "sessions": [
      {
        "sessionId": "session_123",
        "title": "奇幻冒险",
        "createdAt": "2025-09-23T12:00:00",
        "updatedAt": "2025-09-23T12:30:00",
        "messageCount": 5
      }
    ]
  }
}
```

### 获取会话消息
```http
GET /chat/session/{sessionId}/messages
Authorization: Bearer {token}
```

### 流式聊天
```http
POST /chat/stream
Authorization: Bearer {token}
Content-Type: application/json
Accept: text/event-stream

{
  "message": "你好，开始冒险吧！",
  "sessionId": "session_123",
  "systemPrompt": "你是游戏主持人",
  "history": [
    {
      "role": "user",
      "content": "我想探索森林"
    }
  ]
}
```

### 删除会话
```http
DELETE /chat/session/{sessionId}
Authorization: Bearer {token}
```

## 角色扮演接口

### 获取世界模板列表
```http
GET /roleplay/worlds
Authorization: Bearer {token}
```

**响应示例**:
```json
{
  "success": true,
  "message": "获取世界模板成功",
  "data": [
    {
      "worldId": "fantasy_adventure",
      "worldName": "异世界探险",
      "description": "经典的奇幻冒险世界，充满魔法、怪物和宝藏"
    }
  ]
}
```

### 获取世界模板详情
```http
GET /roleplay/worlds/{worldId}
Authorization: Bearer {token}
```

**可用的worldId**:
- `fantasy_adventure` - 异世界探险
- `western_magic` - 西方魔幻
- `martial_arts` - 东方武侠
- `japanese_school` - 日式校园
- `educational` - 寓教于乐

### 初始化角色扮演会话
```http
POST /roleplay/sessions/{sessionId}/initialize
Authorization: Bearer {token}
Content-Type: application/json

{
  "worldType": "fantasy_adventure",
  "godModeRules": "{}"
}
```

### 执行骰子检定
```http
POST /roleplay/sessions/{sessionId}/dice-roll
Authorization: Bearer {token}
Content-Type: application/json

{
  "diceType": 20,
  "modifier": 5,
  "context": "攻击检定",
  "difficultyClass": 15
}
```

**响应示例**:
```json
{
  "success": true,
  "message": "骰子检定完成",
  "data": {
    "id": 1,
    "sessionId": "session_123",
    "diceType": 20,
    "modifier": 5,
    "result": 15,
    "finalResult": 20,
    "context": "攻击检定",
    "isSuccessful": true,
    "difficultyClass": 15,
    "createdAt": "2025-09-23T12:00:00"
  }
}
```

### 获取世界状态
```http
GET /roleplay/sessions/{sessionId}/world-state
Authorization: Bearer {token}
```

### 更新世界状态
```http
POST /roleplay/sessions/{sessionId}/world-state
Authorization: Bearer {token}
Content-Type: application/json

{
  "worldState": "{\"currentLocation\":\"魔法森林\",\"characters\":{\"player\":{\"name\":\"勇者\",\"level\":5}}}",
  "skillsState": "{\"questLog\":{\"activeQuests\":[{\"id\":1,\"title\":\"寻找魔法水晶\"}]}}"
}
```

## 技能指令系统

在AI对话中，系统会自动识别和执行以下指令：

### 骰子指令
```
[DICE:d20+5:攻击检定]
[DICE:d6:伤害]
[DICE:d100:百分比检定]
```

### 任务指令
```
[QUEST:CREATE:寻找魔法剑:在古老的遗迹中寻找传说中的魔法剑]
[QUEST:UPDATE:quest_1:任务进度更新]
[QUEST:COMPLETE:quest_1:任务完成]
```

### 学习挑战指令
```
[CHALLENGE:MATH:3:计算 2x + 5 = 15 中x的值]
[CHALLENGE:HISTORY:古代:秦朝统一中国是在哪一年？]
[CHALLENGE:LANGUAGE:英语:翻译"Hello World"为中文]
```

### 状态更新指令
```
[STATE:LOCATION:魔法森林深处]
[STATE:INVENTORY:获得铁剑 +1]
[STATE:RELATIONSHIP:村长:友好度 +10]
[STATE:EMOTION:兴奋:发现了宝藏]
```

### 记忆指令
```
[MEMORY:EVENT:发现了古老的宝箱]
[MEMORY:CHARACTER:遇到了神秘的法师]
[MEMORY:WORLD:魔法森林的秘密通道]
```

## 快速开始

### 1. 获取访问令牌
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"panzijian1234","password":"123456"}'
```

### 2. 创建角色扮演会话
```bash
curl -X POST http://localhost:8080/api/roleplay/sessions/my_session/initialize \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"worldType":"fantasy_adventure","godModeRules":"{}"}'
```

### 3. 开始对话
```bash
curl -X POST http://localhost:8080/api/chat/stream \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{"message":"我想开始一场奇幻冒险！","sessionId":"my_session"}'
```

## 错误码

| 状态码 | 说明 |
|--------|------|
| 200 | 请求成功 |
| 400 | 请求参数错误 |
| 401 | 未认证或令牌无效 |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

## 注意事项

1. **令牌有效期**: 访问令牌15分钟，刷新令牌24小时
2. **请求频率**: 建议每秒不超过10次请求
3. **会话管理**: sessionId建议使用唯一标识符
4. **JSON格式**: 复杂状态数据以JSON字符串传递
5. **字符编码**: 支持UTF-8，中文内容正常显示

---

**更新时间**: 2025-09-23  
**API版本**: v1.0
