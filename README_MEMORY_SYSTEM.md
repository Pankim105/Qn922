# 角色扮演记忆系统

## 概述

角色扮演记忆系统是基于现有的数据库结构（ChatSession、WorldEvent、WorldState）实现的智能记忆管理服务。系统将记忆数据存储在ChatSession的JSON字段中，并使用WorldEvent记录记忆事件，实现持久化存储和高效检索。

## 架构设计

### 核心组件

1. **RoleplayMemoryService**: 记忆管理服务
2. **RoleplayPromptEngine**: 提示词引擎（集成记忆功能）
3. **RoleplayController**: 记忆管理API接口

### 数据存储策略

- **ChatSession.world_state**: 存储结构化的记忆数据（JSON格式）
- **WorldEvent**: 记录记忆相关的事件和操作
- **WorldState**: 存储记忆快照（可选）

## 功能特性

### 1. 记忆存储
- 支持多种记忆类型：CHARACTER、EVENT、RELATIONSHIP、WORLD_STATE、EMOTION、SKILL、ITEM、LOCATION
- 自动评估记忆重要性（0-1分值）
- 智能清理低重要性记忆（保留最重要15个）

### 2. 记忆检索
- 基于关键词的相似性匹配
- 支持相关性排序
- 可配置最大检索结果数量

### 3. 大模型集成
- AI回复中自动提取记忆标记
- 自动评估和存储重要信息
- 记忆上下文注入到提示词中

## API接口

### 存储记忆
```http
POST /roleplay/sessions/{sessionId}/memories
Content-Type: application/json

{
  "content": "学会了火球术",
  "type": "SKILL",
  "importance": 0.8
}
```

### 获取相关记忆
```http
GET /roleplay/sessions/{sessionId}/memories?query=魔法&maxResults=5
```

### 获取记忆摘要
```http
GET /roleplay/sessions/{sessionId}/memories/summary
```

### 记录重要事件
```http
POST /roleplay/sessions/{sessionId}/events/important
Content-Type: application/json

{
  "event": "击败了巨龙",
  "context": "在地下城深处的战斗"
}
```

### 处理AI回复中的记忆标记
```http
POST /roleplay/sessions/{sessionId}/memories/process
Content-Type: application/json

{
  "aiResponse": "你学会了火球术，这是一个强大的攻击技能。[MEMORY:SKILL:火球术:学会了基础火球术]",
  "userAction": "学习火球术"
}
```

## 使用方法

### 1. 基础使用

#### 存储记忆
```java
@Autowired
private RoleplayMemoryService memoryService;

// 存储重要事件
memoryService.recordImportantEvent(sessionId, "击败了巨龙", "史诗级战斗");

// 更新角色关系
memoryService.updateCharacterRelationship(sessionId, "精灵王", "建立了友好关系");

// 记录世界状态变化
memoryService.recordWorldStateChange(sessionId, "魔法森林恢复", "击败黑暗法师");
```

#### 检索记忆
```java
// 获取相关记忆
List<MemoryEntry> memories = memoryService.retrieveRelevantMemories(sessionId, "魔法", 5);

// 获取记忆摘要
String summary = memoryService.getMemorySummary(sessionId);
```

### 2. 大模型集成

#### 在提示词中添加记忆指令
```java
// 构建包含记忆上下文的提示词
RoleplayContext context = new RoleplayContext(worldType, sessionId);
context.setCurrentMessage("学习火球术");

String prompt = roleplayPromptEngine.buildLayeredPrompt(context);
```

#### 处理AI回复中的记忆标记
```java
// AI回复包含记忆标记
String aiResponse = "你学会了火球术。[MEMORY:SKILL:火球术:学会了基础火球术攻击技能]";

// 自动提取和存储记忆
roleplayPromptEngine.processMemoryMarkers(sessionId, aiResponse, "学习火球术");
```

### 3. 手动记忆管理

#### 存储自定义记忆
```java
memoryService.storeMemory(sessionId, "发现隐藏的宝藏", "ITEM", 0.9);
```

#### 更新记忆重要性
```java
// 重要性高的记忆会被优先保留
memoryService.storeMemory(sessionId, "获得了传说中的神剑", "ITEM", 0.95);
```

## 记忆类型

| 类型 | 描述 | 示例 |
|------|------|------|
| CHARACTER | 角色相关 | 角色关系、性格变化 |
| EVENT | 重要事件 | 战斗、发现、成就 |
| RELATIONSHIP | 人际关系 | 友好、敌对、联盟 |
| WORLD_STATE | 世界状态 | 环境变化、地点变化 |
| EMOTION | 情绪状态 | 快乐、愤怒、恐惧 |
| SKILL | 技能学习 | 学会新技能、技能升级 |
| ITEM | 物品相关 | 获得物品、物品使用 |
| LOCATION | 地点相关 | 探索新地点、地点变化 |

## 记忆标记格式

在AI回复中使用以下格式标记重要信息：

### 角色关系变化
```
[MEMORY:CHARACTER:精灵王:与法师建立了友好关系]
```

### 重要事件
```
[MEMORY:EVENT:成功铸造了魔法戒指]
```

### 技能学习
```
[MEMORY:SKILL:火球术:学会了基础火球术攻击技能]
```

### 世界状态变化
```
[MEMORY:WORLD:魔法森林:黑暗力量被驱散]
```

## 性能优化

### 1. 索引优化
- session_id + type 复合索引
- session_id + importance 索引
- timestamp 索引

### 2. 内存管理
- 自动清理低重要性记忆
- 限制每个类型的记忆数量（默认15个）
- 访问频率统计和缓存

### 3. 查询优化
- 支持分页查询
- 相关性排序
- 关键词匹配优化

## 扩展建议

### 1. 向量嵌入
- 集成向量数据库（可选）
- 语义相似性检索
- 记忆聚类和分类

### 2. 高级分析
- 记忆模式分析
- 情感轨迹追踪
- 故事一致性检查

### 3. 用户界面
- 记忆可视化界面
- 记忆搜索和过滤
- 记忆编辑和管理工具

## 故障排除

### 常见问题

1. **记忆不存储**
   - 检查sessionId是否存在
   - 验证记忆重要性阈值
   - 查看数据库连接状态

2. **检索失败**
   - 检查查询关键词
   - 验证数据库索引
   - 查看日志错误信息

3. **AI记忆标记不处理**
   - 检查标记格式是否正确
   - 验证AI回复解析逻辑
   - 查看处理日志

### 调试方法

```java
// 启用详细日志
logger.debug("存储记忆: sessionId={}, content={}", sessionId, content);

// 查看内存状态
String summary = memoryService.getMemorySummary(sessionId);
System.out.println(summary);

// 检查事件记录
List<WorldEvent> events = memoryService.getMemoryEvents(sessionId);
for (WorldEvent event : events) {
    System.out.println(event.getEventData());
}
```

## 总结

这个记忆系统充分利用了现有的数据库结构，通过JSON字段存储结构化记忆数据，使用WorldEvent记录记忆操作历史，实现了：

- ✅ 持久化存储
- ✅ 智能检索
- ✅ 大模型集成
- ✅ 自动清理
- ✅ 性能优化

系统设计灵活、可扩展，为角色扮演提供持续的记忆支持，使AI能够保持故事连贯性和角色一致性。
