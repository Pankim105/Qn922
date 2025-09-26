# 评估系统

QN Contest的智能评估系统是核心功能之一，通过AI模型对用户行为进行实时评估，并自动执行相应的游戏逻辑更新。

## 系统概述

评估系统基于AI模型的智能分析，将用户行为转化为结构化的评估数据，并自动执行游戏逻辑更新，包括骰子检定、任务管理、状态更新、情节管理等。

## 核心特性

### 🧠 智能评估
- **多维度评估**: 规则合规性、上下文一致性、收敛推进度
- **实时处理**: 每次AI回复都包含评估JSON
- **自动执行**: 评估结果自动转化为游戏逻辑更新

### 🎲 游戏机制集成
- **骰子检定**: 自动识别和执行骰子检定
- **任务系统**: 智能创建、更新、完成任务
- **学习挑战**: 寓教于乐的学习内容
- **状态管理**: 实时更新角色和世界状态

### 📊 数据追踪
- **完整记录**: 所有评估数据持久化存储
- **历史分析**: 支持评估历史查询和分析
- **性能监控**: 实时监控评估系统性能

## 评估流程

### 1. 用户行为输入
用户通过聊天界面输入行为描述，系统将行为传递给AI模型。

### 2. AI评估分析
AI模型基于以下维度对用户行为进行评估：

#### 评估维度
- **规则合规性 (ruleCompliance)**: 行为是否符合世界规则和逻辑 (0-1)
- **上下文一致性 (contextConsistency)**: 行为是否与当前故事上下文一致 (0-1)
- **收敛推进度 (convergenceProgress)**: 行为对故事收敛目标的贡献程度 (0-1)

#### 评估标准
- **0.8-1.0**: 优秀，完全符合预期，有助于故事推进（策略：ACCEPT）
- **0.6-0.8**: 良好，基本符合，大部分可接受（策略：ADJUST）
- **0.0-0.6**: 问题较大，需要修正或拒绝（策略：CORRECT）

### 3. 评估JSON生成
AI模型生成结构化的评估JSON，包含评估结果和游戏逻辑更新信息。

### 4. 游戏逻辑处理
系统解析评估JSON，自动执行相应的游戏逻辑更新。

## 评估JSON结构

### 基础评估字段
```json
{
  "ruleCompliance": 0.95,
  "contextConsistency": 0.90,
  "convergenceProgress": 0.70,
  "overallScore": 0.85,
  "strategy": "ACCEPT",
  "assessmentNotes": "行为评估说明",
  "suggestedActions": ["建议行动1", "建议行动2"],
  "convergenceHints": ["收敛提示1", "收敛提示2"]
}
```

### 游戏机制字段

#### 骰子检定 (diceRolls)
```json
"diceRolls": [
  {
    "diceType": 20,
    "modifier": 3,
    "context": "感知检定",
    "result": 15,
    "isSuccessful": true
  }
]
```

#### 学习挑战 (learningChallenges)
```json
"learningChallenges": [
  {
    "type": "MATH",
    "difficulty": "普通",
    "question": "计算2+2等于几？",
    "answer": "4",
    "isCorrect": true
  }
]
```

#### 状态更新 (stateUpdates)
```json
"stateUpdates": [
  {
    "type": "LOCATION",
    "value": "进入图书馆，书香气息扑面而来"
  },
  {
    "type": "INVENTORY",
    "value": "获得了魔法书x1，智力+5"
  }
]
```

#### 任务更新 (questUpdates)
```json
"questUpdates": {
  "created": [
    {
      "questId": "quest_new_001",
      "title": "新任务标题",
      "description": "任务描述",
      "rewards": {"exp": 50, "gold": 25}
    }
  ],
  "completed": [
    {
      "questId": "quest_001",
      "rewards": {"exp": 100, "gold": 50, "items": ["铁剑x1"]}
    }
  ],
  "progress": [
    {
      "questId": "quest_002",
      "progress": "2/5"
    }
  ],
  "expired": [
    {
      "questId": "quest_003",
      "reason": "剧情推进导致任务失效"
    }
  ]
}
```

#### 世界状态更新 (worldStateUpdates)
```json
"worldStateUpdates": {
  "currentLocation": "新位置",
  "environment": "环境变化",
  "npcs": [
    {
      "name": "NPC名称",
      "status": "状态变化"
    }
  ]
}
```

#### 技能状态更新 (skillsStateUpdates)
```json
"skillsStateUpdates": {
  "level": 2,
  "experience": 150,
  "gold": 75,
  "inventory": ["新物品"],
  "abilities": ["新技能"],
  "stats": {
    "力量": 12,
    "敏捷": 10,
    "智力": 15,
    "体质": 11
  }
}
```

#### 情节更新 (arcUpdates)
```json
"arcUpdates": {
  "currentArcName": "新情节名称",
  "currentArcStartRound": 5,
  "totalRounds": 10
}
```

#### 收敛状态更新 (convergenceStatusUpdates)
```json
"convergenceStatusUpdates": {
  "progress": 0.75,
  "nearestScenarioId": "story_convergence_2",
  "nearestScenarioTitle": "远古神庙",
  "distanceToNearest": 0.3,
  "scenarioProgress": {
    "story_convergence_1": 1.0,
    "story_convergence_2": 0.75,
    "main_convergence": 0.2
  },
  "activeHints": ["寻找神庙入口", "收集古代符文"]
}
```

## 处理流程

### 1. 评估JSON提取
```java
@Service
public class AssessmentExtractor {
    
    public String extractAssessmentJson(String fullResponse) {
        // 查找评估JSON标记
        String startMarker = "§{";
        String endMarker = "}§";
        
        int startIndex = fullResponse.indexOf(startMarker);
        int endIndex = fullResponse.lastIndexOf(endMarker);
        
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            return fullResponse.substring(startIndex + 1, endIndex + 1);
        }
        
        return null;
    }
}
```

### 2. 游戏逻辑处理
```java
@Service
public class AssessmentGameLogicProcessor {
    
    @Transactional
    public void processAssessmentGameLogic(String sessionId, String assessmentJson) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode assessment = mapper.readTree(assessmentJson);
            
            // 处理各种游戏逻辑
            processDiceRolls(sessionId, assessment);
            processLearningChallenges(sessionId, assessment);
            processStateUpdates(sessionId, assessment);
            processQuestUpdates(sessionId, assessment);
            processWorldStateUpdates(sessionId, assessment);
            processSkillsStateUpdates(sessionId, assessment);
            processArcUpdates(sessionId, assessment);
            processConvergenceStatusUpdates(sessionId, assessment);
            
        } catch (Exception e) {
            logger.error("处理评估游戏逻辑失败: {}", e.getMessage(), e);
        }
    }
}
```

### 3. 数据持久化
所有评估数据和处理结果都会持久化到数据库：

- **dm_assessments**: 存储完整的评估记录
- **dice_rolls**: 存储骰子检定结果
- **learning_challenges**: 存储学习挑战记录
- **world_events**: 存储世界事件记录
- **convergence_status**: 存储收敛状态信息

## 错误处理

### 1. JSON解析错误
- 捕获JSON解析异常
- 记录错误日志
- 继续处理其他字段

### 2. 数据库操作错误
- 使用事务确保数据一致性
- 记录操作失败日志
- 提供错误恢复机制

### 3. 业务逻辑错误
- 验证数据有效性
- 处理边界情况
- 提供默认值

## 性能优化

### 1. 异步处理
- 评估处理异步执行
- 避免阻塞主流程
- 提高响应速度

### 2. 批量操作
- 批量数据库操作
- 减少数据库连接开销
- 提高处理效率

### 3. 缓存策略
- 缓存常用数据
- 减少重复计算
- 提高系统性能

## 监控和日志

### 1. 处理日志
```java
logger.info("开始处理评估游戏逻辑: sessionId={}, assessmentLength={}", 
           sessionId, assessmentJson.length());

logger.debug("解析骰子检定: count={}", diceRolls.size());
logger.debug("解析任务更新: created={}, completed={}, progress={}, expired={}", 
           questUpdates.get("created").size(),
           questUpdates.get("completed").size(),
           questUpdates.get("progress").size(),
           questUpdates.get("expired").size());
```

### 2. 性能监控
- 处理时间统计
- 成功率监控
- 错误率跟踪

### 3. 业务指标
- 评估分布统计
- 游戏逻辑执行统计
- 用户行为分析

## 扩展性

### 1. 新游戏机制
- 支持添加新的游戏机制字段
- 提供扩展接口
- 保持向后兼容

### 2. 自定义评估
- 支持自定义评估维度
- 提供评估插件机制
- 支持第三方集成

### 3. 多模型支持
- 支持不同的AI模型
- 提供模型适配器
- 支持模型切换

## 使用示例

### 1. 基础评估
```json
{
  "ruleCompliance": 0.9,
  "contextConsistency": 0.85,
  "convergenceProgress": 0.6,
  "overallScore": 0.78,
  "strategy": "ACCEPT",
  "assessmentNotes": "用户行为符合游戏规则，有助于故事发展"
}
```

### 2. 复杂游戏逻辑
```json
{
  "ruleCompliance": 0.95,
  "contextConsistency": 0.90,
  "convergenceProgress": 0.80,
  "overallScore": 0.88,
  "strategy": "ACCEPT",
  "diceRolls": [
    {
      "diceType": 20,
      "modifier": 5,
      "context": "攻击检定",
      "result": 18,
      "isSuccessful": true
    }
  ],
  "questUpdates": {
    "completed": [
      {
        "questId": "quest_001",
        "rewards": {"exp": 100, "gold": 50}
      }
    ]
  },
  "skillsStateUpdates": {
    "level": 3,
    "experience": 250,
    "gold": 125
  }
}
```

## 最佳实践

### 1. 评估准确性
- 提供清晰的评估标准
- 定期校准评估模型
- 收集用户反馈

### 2. 性能优化
- 合理设置处理超时
- 优化数据库查询
- 使用适当的缓存策略

### 3. 错误处理
- 完善的异常处理机制
- 详细的错误日志记录
- 用户友好的错误提示

### 4. 数据安全
- 敏感数据加密存储
- 访问权限控制
- 审计日志记录

---

**评估系统是QN Contest的核心功能，通过智能评估和自动游戏逻辑处理，为用户提供流畅的角色扮演体验。**


