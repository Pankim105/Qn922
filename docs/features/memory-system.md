# 记忆系统

QN Contest的智能记忆管理系统，为角色扮演体验提供持续的记忆支持，确保AI能够保持故事连贯性和角色一致性。

## 系统概述

记忆系统基于现有的数据库结构（ChatSession、WorldEvent、WorldState）实现，将记忆数据存储在ChatSession的JSON字段中，并使用WorldEvent记录记忆事件，实现持久化存储和高效检索。

## 核心特性

### 🧠 智能记忆存储
- **自动识别**: 自动识别AI回复中的重要信息
- **重要性评估**: 智能评估记忆的重要性（0-1分值）
- **类型分类**: 支持8种记忆类型的自动分类
- **自动清理**: 智能清理低重要性记忆，保留最重要的15个

### 🔍 智能记忆检索
- **关键词匹配**: 基于关键词的相似性匹配
- **相关性排序**: 按相关性对检索结果排序
- **上下文感知**: 根据当前对话上下文检索相关记忆
- **可配置检索**: 支持自定义检索参数

### 🎯 大模型集成
- **自动提取**: AI回复中自动提取记忆标记
- **智能评估**: 自动评估和存储重要信息
- **上下文注入**: 记忆上下文自动注入到提示词中
- **无缝集成**: 与角色扮演系统无缝集成

## 记忆类型

| 类型 | 描述 | 示例 | 重要性权重 |
|------|------|------|-----------|
| CHARACTER | 角色相关 | 角色关系、性格变化 | 0.8 |
| EVENT | 重要事件 | 战斗、发现、成就 | 0.9 |
| RELATIONSHIP | 人际关系 | 友好、敌对、联盟 | 0.7 |
| WORLD_STATE | 世界状态 | 环境变化、地点变化 | 0.6 |
| EMOTION | 情绪状态 | 快乐、愤怒、恐惧 | 0.5 |
| SKILL | 技能学习 | 学会新技能、技能升级 | 0.8 |
| ITEM | 物品相关 | 获得物品、物品使用 | 0.6 |
| LOCATION | 地点相关 | 探索新地点、地点变化 | 0.5 |

## 记忆标记格式

### 1. 角色关系变化
```
[MEMORY:CHARACTER:精灵王:与法师建立了友好关系]
[MEMORY:CHARACTER:巨龙:被激怒，关系变为敌对]
```

### 2. 重要事件
```
[MEMORY:EVENT:成功铸造了魔法戒指]
[MEMORY:EVENT:击败了强大的恶魔领主]
```

### 3. 技能学习
```
[MEMORY:SKILL:火球术:学会了基础火球术攻击技能]
[MEMORY:SKILL:治疗术:掌握了中级治疗魔法]
```

### 4. 世界状态变化
```
[MEMORY:WORLD:魔法森林:黑暗力量被驱散]
[MEMORY:WORLD:村庄:获得了和平与繁荣]
```

### 5. 物品获得
```
[MEMORY:ITEM:神剑:获得了传说中的神剑]
[MEMORY:ITEM:魔法书:找到了古老的魔法典籍]
```

### 6. 地点探索
```
[MEMORY:LOCATION:神秘洞穴:发现了隐藏的宝藏洞穴]
[MEMORY:LOCATION:山顶神庙:到达了古老的神庙]
```

### 7. 情绪状态
```
[MEMORY:EMOTION:兴奋:发现了重要的线索]
[MEMORY:EMOTION:恐惧:面对强大的敌人感到恐惧]
```

### 8. 人际关系
```
[MEMORY:RELATIONSHIP:村长:建立了信任关系]
[MEMORY:RELATIONSHIP:盗贼:关系恶化，成为敌人]
```

## 技术实现

### 1. 记忆存储机制

#### 数据结构
```java
public class MemoryEntry {
    private String content;           // 记忆内容
    private String type;              // 记忆类型
    private double importance;        // 重要性评分 (0-1)
    private LocalDateTime createdAt;  // 创建时间
    private String context;           // 上下文信息
}
```

#### 存储实现
```java
@Service
public class RoleplayMemoryService implements MemoryManagerInterface {
    
    public void storeMemory(String sessionId, String content, String memoryType, double importance) {
        // 获取当前记忆列表
        List<MemoryEntry> memories = getMemories(sessionId);
        
        // 创建新记忆
        MemoryEntry newMemory = new MemoryEntry();
        newMemory.setContent(content);
        newMemory.setType(memoryType);
        newMemory.setImportance(importance);
        newMemory.setCreatedAt(LocalDateTime.now());
        
        // 添加到记忆列表
        memories.add(newMemory);
        
        // 智能清理：保留最重要的15个记忆
        memories.sort((a, b) -> Double.compare(b.getImportance(), a.getImportance()));
        if (memories.size() > 15) {
            memories = memories.subList(0, 15);
        }
        
        // 保存到数据库
        saveMemoriesToSession(sessionId, memories);
        
        // 记录记忆事件
        recordMemoryEvent(sessionId, "MEMORY_STORED", content);
    }
}
```

### 2. 记忆检索机制

#### 相似性计算
```java
public List<MemoryEntry> retrieveRelevantMemories(String sessionId, String query, int maxResults) {
    List<MemoryEntry> memories = getMemories(sessionId);
    
    // 计算相似性分数
    List<ScoredMemory> scoredMemories = memories.stream()
        .map(memory -> {
            double score = calculateSimilarity(query, memory.getContent());
            return new ScoredMemory(memory, score);
        })
        .filter(scored -> scored.getScore() > 0.3) // 相似性阈值
        .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
        .limit(maxResults)
        .collect(Collectors.toList());
        
    return scoredMemories.stream()
        .map(ScoredMemory::getMemory)
        .collect(Collectors.toList());
}

private double calculateSimilarity(String query, String content) {
    // 使用简单的关键词匹配算法
    String[] queryWords = query.toLowerCase().split("\\s+");
    String[] contentWords = content.toLowerCase().split("\\s+");
    
    int matches = 0;
    for (String queryWord : queryWords) {
        for (String contentWord : contentWords) {
            if (contentWord.contains(queryWord) || queryWord.contains(contentWord)) {
                matches++;
                break;
            }
        }
    }
    
    return (double) matches / queryWords.length;
}
```

#### 上下文感知检索
```java
public String buildMemoryContext(String sessionId, String currentMessage) {
    // 从当前消息中提取关键词
    List<String> keywords = extractKeywords(currentMessage);
    
    // 检索相关记忆
    List<MemoryEntry> relevantMemories = new ArrayList<>();
    for (String keyword : keywords) {
        List<MemoryEntry> memories = retrieveRelevantMemories(sessionId, keyword, 3);
        relevantMemories.addAll(memories);
    }
    
    // 去重并排序
    relevantMemories = relevantMemories.stream()
        .distinct()
        .sorted((a, b) -> Double.compare(b.getImportance(), a.getImportance()))
        .limit(5)
        .collect(Collectors.toList());
    
    // 构建记忆上下文
    if (relevantMemories.isEmpty()) {
        return "";
    }
    
    StringBuilder context = new StringBuilder("\n## 🧠 相关记忆\n");
    for (MemoryEntry memory : relevantMemories) {
        context.append("- ").append(memory.getContent()).append("\n");
    }
    
    return context.toString();
}
```

### 3. 重要性评估机制

#### 自动评估
```java
public double assessMemoryImportance(String content, String userAction) {
    double importance = 0.5; // 基础重要性
    
    // 基于内容长度调整
    if (content.length() > 50) {
        importance += 0.1;
    }
    
    // 基于关键词调整
    String[] importantKeywords = {
        "重要", "关键", "发现", "获得", "学会", "击败", "成功", "失败"
    };
    
    for (String keyword : importantKeywords) {
        if (content.contains(keyword)) {
            importance += 0.1;
            break;
        }
    }
    
    // 基于记忆类型调整
    String memoryType = extractMemoryType(content);
    importance += getTypeImportanceWeight(memoryType);
    
    // 基于用户行为调整
    if (userAction != null && userAction.length() > 20) {
        importance += 0.1; // 复杂行为通常更重要
    }
    
    return Math.min(1.0, Math.max(0.0, importance));
}

private String extractMemoryType(String content) {
    // 从记忆标记中提取类型
    Pattern pattern = Pattern.compile("\\[MEMORY:([^:]+):");
    Matcher matcher = pattern.matcher(content);
    if (matcher.find()) {
        return matcher.group(1);
    }
    return "EVENT"; // 默认类型
}

private double getTypeImportanceWeight(String type) {
    Map<String, Double> weights = Map.of(
        "EVENT", 0.3,
        "CHARACTER", 0.2,
        "SKILL", 0.2,
        "RELATIONSHIP", 0.1,
        "ITEM", 0.1,
        "WORLD_STATE", 0.1,
        "LOCATION", 0.05,
        "EMOTION", 0.05
    );
    return weights.getOrDefault(type, 0.1);
}
```

### 4. 记忆标记处理

#### 自动提取
```java
public void processMemoryMarkers(String sessionId, String aiResponse, String userAction) {
    // 使用正则表达式提取记忆标记
    Pattern pattern = Pattern.compile("\\[MEMORY:([^:]+):([^\\]]+)\\]");
    Matcher matcher = pattern.matcher(aiResponse);
    
    while (matcher.find()) {
        String type = matcher.group(1);
        String content = matcher.group(2);
        
        // 评估重要性
        double importance = assessMemoryImportance(content, userAction);
        
        // 存储记忆
        storeMemory(sessionId, content, type, importance);
        
        logger.debug("提取并存储记忆: type={}, content={}, importance={}", 
                    type, content, importance);
    }
}
```

#### 记忆清理
```java
public void cleanupMemories(String sessionId) {
    List<MemoryEntry> memories = getMemories(sessionId);
    
    // 按重要性排序
    memories.sort((a, b) -> Double.compare(b.getImportance(), a.getImportance()));
    
    // 保留最重要的15个记忆
    if (memories.size() > 15) {
        List<MemoryEntry> toKeep = memories.subList(0, 15);
        List<MemoryEntry> toRemove = memories.subList(15, memories.size());
        
        // 保存保留的记忆
        saveMemoriesToSession(sessionId, toKeep);
        
        // 记录清理事件
        for (MemoryEntry memory : toRemove) {
            recordMemoryEvent(sessionId, "MEMORY_CLEANUP", memory.getContent());
        }
        
        logger.info("清理记忆: sessionId={}, 保留={}, 清理={}", 
                   sessionId, toKeep.size(), toRemove.size());
    }
}
```

## API接口

### 1. 存储记忆
```http
POST /api/roleplay/sessions/{sessionId}/memories
Content-Type: application/json

{
  "content": "学会了火球术",
  "type": "SKILL",
  "importance": 0.8
}
```

### 2. 获取相关记忆
```http
GET /api/roleplay/sessions/{sessionId}/memories?query=魔法&maxResults=5
```

### 3. 获取记忆摘要
```http
GET /api/roleplay/sessions/{sessionId}/memories/summary
```

### 4. 记录重要事件
```http
POST /api/roleplay/sessions/{sessionId}/events/important
Content-Type: application/json

{
  "event": "击败了巨龙",
  "context": "在地下城深处的战斗"
}
```

### 5. 处理AI回复中的记忆标记
```http
POST /api/roleplay/sessions/{sessionId}/memories/process
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

## 性能优化

### 1. 索引优化
- **复合索引**: session_id + type 复合索引
- **重要性索引**: session_id + importance 索引
- **时间索引**: timestamp 索引

### 2. 内存管理
- **自动清理**: 自动清理低重要性记忆
- **数量限制**: 限制每个类型的记忆数量（默认15个）
- **访问频率**: 统计和缓存访问频率

### 3. 查询优化
- **分页查询**: 支持分页查询
- **相关性排序**: 相关性排序优化
- **关键词匹配**: 关键词匹配优化

## 扩展建议

### 1. 向量嵌入
- **向量数据库**: 集成向量数据库（可选）
- **语义相似性**: 语义相似性检索
- **记忆聚类**: 记忆聚类和分类

### 2. 高级分析
- **记忆模式**: 记忆模式分析
- **情感轨迹**: 情感轨迹追踪
- **故事一致性**: 故事一致性检查

### 3. 用户界面
- **记忆可视化**: 记忆可视化界面
- **记忆搜索**: 记忆搜索和过滤
- **记忆编辑**: 记忆编辑和管理工具

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

记忆系统充分利用了现有的数据库结构，通过JSON字段存储结构化记忆数据，使用WorldEvent记录记忆操作历史，实现了：

- ✅ **持久化存储**: 记忆数据持久化保存
- ✅ **智能检索**: 基于关键词的智能记忆检索
- ✅ **大模型集成**: 与AI系统无缝集成
- ✅ **自动清理**: 智能记忆清理和优化
- ✅ **性能优化**: 高效的存储和检索性能

系统设计灵活、可扩展，为角色扮演提供持续的记忆支持，使AI能够保持故事连贯性和角色一致性。通过智能的重要性评估和自动清理机制，系统能够高效管理记忆数据，确保最重要的信息得到保留和利用。
