# 收敛机制

QN Contest的收敛机制是智能故事引导系统的核心，通过AI模型智能分析用户行为，引导故事向预设的结局方向发展，确保故事的连贯性和完整性。

## 系统概述

收敛机制基于智能算法，实时分析用户行为对故事发展的影响，计算到各个预设结局场景的距离，并提供动态的引导提示，确保无论用户如何探索，最终都能收敛到有意义的结局。

## 核心特性

### 🎯 智能收敛
- **多场景支持**: 支持多个预设的收敛场景
- **距离计算**: 实时计算到各场景的距离
- **动态引导**: 根据当前状态提供个性化引导

### 📊 进度跟踪
- **整体进度**: 跟踪故事向结局推进的整体进度
- **场景进度**: 跟踪各个收敛场景的完成进度
- **历史记录**: 记录收敛状态的变化历史

### 🧭 引导系统
- **智能提示**: 基于当前状态生成引导提示
- **多路径支持**: 支持多种到达结局的路径
- **自然引导**: 避免生硬的剧情强制

## 收敛原理

### 1. 自由探索
用户可以在游戏中进行任何想做的行为，系统不会强制限制用户的选择。

### 2. 智能分析
AI模型实时分析用户行为，评估其对故事发展的影响：
- 行为是否符合当前故事逻辑
- 行为是否推进了故事发展
- 行为是否接近某个收敛场景

### 3. 距离计算
系统计算当前故事状态到各个预设收敛场景的距离：
- **场景匹配度**: 当前状态与目标场景的匹配程度
- **路径可行性**: 从当前状态到达目标场景的可行性
- **时间因素**: 考虑故事发展的时间因素

### 4. 动态引导
基于距离计算结果，系统提供动态的引导提示：
- **微妙提示**: 在早期阶段提供微妙的引导
- **明确建议**: 在关键时刻提供明确的建议
- **紧急引导**: 在偏离过远时提供紧急引导

## 收敛状态管理

### 数据结构
```java
@Entity
public class ConvergenceStatus {
    private Long id;
    private String sessionId;
    private Double progress;                    // 整体收敛进度 (0-1)
    private String nearestScenarioId;          // 最近收敛场景ID
    private String nearestScenarioTitle;       // 最近收敛场景标题
    private Double distanceToNearest;          // 到最近场景的距离
    private String scenarioProgress;           // 各场景进度JSON
    private String activeHints;                // 活跃引导提示JSON
    private LocalDateTime lastUpdated;         // 最后更新时间
    private LocalDateTime createdAt;           // 创建时间
}
```

### 进度计算
```java
@Service
public class ConvergenceStatusService {
    
    public void updateProgress(String sessionId, double progress) {
        ConvergenceStatus status = getOrCreateConvergenceStatus(sessionId);
        status.updateProgress(progress);
        convergenceStatusRepository.save(status);
    }
    
    public void updateNearestScenario(String sessionId, String scenarioId, 
                                    String scenarioTitle, double distance) {
        ConvergenceStatus status = getOrCreateConvergenceStatus(sessionId);
        status.setNearestScenarioId(scenarioId);
        status.setNearestScenarioTitle(scenarioTitle);
        status.setDistanceToNearest(distance);
        status.setLastUpdated(LocalDateTime.now());
        convergenceStatusRepository.save(status);
    }
}
```

## 收敛场景定义

### 1. 场景类型
每个世界类型都有多个预设的收敛场景：

#### 异世界探险
- **主要结局**: 击败最终BOSS，拯救世界
- **备选结局**: 成为新的统治者
- **隐藏结局**: 发现世界的真相

#### 西方魔幻
- **主要结局**: 掌握终极魔法，成为大法师
- **备选结局**: 与龙族和解，建立新秩序
- **隐藏结局**: 发现魔法的起源

#### 东方武侠
- **主要结局**: 成为武林盟主，维护江湖正义
- **备选结局**: 隐居山林，追求武道极致
- **隐藏结局**: 发现武学的终极秘密

#### 日式校园
- **主要结局**: 毕业典礼，与朋友们告别
- **备选结局**: 转学离开，开始新生活
- **隐藏结局**: 发现学校的秘密

#### 寓教于乐
- **主要结局**: 完成所有学习挑战，成为学霸
- **备选结局**: 发现学习的乐趣，继续深造
- **隐藏结局**: 发现知识的终极奥秘

### 2. 场景属性
每个收敛场景都有以下属性：
- **场景ID**: 唯一标识符
- **场景标题**: 人类可读的标题
- **场景描述**: 详细的场景描述
- **触发条件**: 触发该场景的条件
- **权重**: 场景的重要性权重

## 距离计算算法

### 1. 状态匹配度
```java
public double calculateStateMatch(String currentState, String targetScenario) {
    // 基于关键词匹配计算状态相似度
    Set<String> currentKeywords = extractKeywords(currentState);
    Set<String> targetKeywords = extractKeywords(targetScenario);
    
    int intersection = Sets.intersection(currentKeywords, targetKeywords).size();
    int union = Sets.union(currentKeywords, targetKeywords).size();
    
    return (double) intersection / union;
}
```

### 2. 路径可行性
```java
public double calculatePathFeasibility(String currentState, String targetScenario) {
    // 基于故事逻辑计算路径可行性
    List<String> requiredSteps = getRequiredSteps(targetScenario);
    List<String> completedSteps = getCompletedSteps(currentState);
    
    int completedCount = 0;
    for (String step : requiredSteps) {
        if (completedSteps.contains(step)) {
            completedCount++;
        }
    }
    
    return (double) completedCount / requiredSteps.size();
}
```

### 3. 综合距离
```java
public double calculateDistance(String currentState, String targetScenario) {
    double stateMatch = calculateStateMatch(currentState, targetScenario);
    double pathFeasibility = calculatePathFeasibility(currentState, targetScenario);
    
    // 综合计算距离（越小越接近）
    return 1.0 - (stateMatch * 0.6 + pathFeasibility * 0.4);
}
```

## 引导提示生成

### 1. 提示类型
根据距离和进度，生成不同类型的引导提示：

#### 微妙提示 (距离 > 0.7)
- "你注意到远处有什么在闪烁"
- "空气中似乎有什么不寻常的气息"
- "你感觉有什么重要的事情即将发生"

#### 明确建议 (0.4 < 距离 ≤ 0.7)
- "建议你前往东边的森林，那里可能有重要线索"
- "与村长对话可能会获得有用的信息"
- "检查那个古老的石碑，它可能隐藏着秘密"

#### 紧急引导 (距离 ≤ 0.4)
- "时间紧迫！你必须立即前往神庙"
- "这是最后的机会，不要犹豫"
- "选择现在决定了你的命运"

### 2. 提示生成算法
```java
public List<String> generateHints(String sessionId) {
    ConvergenceStatus status = getConvergenceStatus(sessionId);
    double distance = status.getDistanceToNearest();
    
    List<String> hints = new ArrayList<>();
    
    if (distance > 0.7) {
        hints.addAll(generateSubtleHints(status));
    } else if (distance > 0.4) {
        hints.addAll(generateClearSuggestions(status));
    } else {
        hints.addAll(generateUrgentGuidance(status));
    }
    
    return hints;
}
```

## 收敛状态更新

### 1. 评估JSON集成
收敛状态更新通过评估JSON中的`convergenceStatusUpdates`字段进行：

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

### 2. 处理逻辑
```java
private void processConvergenceStatusUpdates(String sessionId, JsonNode assessment) {
    JsonNode convergenceUpdates = assessment.get("convergenceStatusUpdates");
    if (convergenceUpdates == null) return;
    
    // 更新整体进度
    if (convergenceUpdates.has("progress")) {
        double progress = convergenceUpdates.get("progress").asDouble();
        convergenceStatusService.updateProgress(sessionId, progress);
    }
    
    // 更新最近场景
    if (convergenceUpdates.has("nearestScenarioId")) {
        String scenarioId = convergenceUpdates.get("nearestScenarioId").asText();
        String scenarioTitle = convergenceUpdates.get("nearestScenarioTitle").asText();
        double distance = convergenceUpdates.get("distanceToNearest").asDouble();
        convergenceStatusService.updateNearestScenario(sessionId, scenarioId, scenarioTitle, distance);
    }
    
    // 更新场景进度
    if (convergenceUpdates.has("scenarioProgress")) {
        Map<String, Double> scenarioProgress = parseScenarioProgress(convergenceUpdates.get("scenarioProgress"));
        convergenceStatusService.updateScenarioProgress(sessionId, scenarioProgress);
    }
    
    // 更新活跃提示
    if (convergenceUpdates.has("activeHints")) {
        List<String> activeHints = parseActiveHints(convergenceUpdates.get("activeHints"));
        convergenceStatusService.updateActiveHints(sessionId, activeHints);
    }
}
```

## 提示词集成

### 1. 收敛状态摘要
在AI提示词中包含当前收敛状态信息：

```java
public String getConvergenceStatusSummary(String sessionId) {
    Optional<ConvergenceStatus> statusOpt = convergenceStatusRepository.findBySessionId(sessionId);
    if (statusOpt.isEmpty()) {
        return "";
    }
    
    ConvergenceStatus status = statusOpt.get();
    StringBuilder summary = new StringBuilder();
    summary.append(String.format("- **整体收敛进度**: %.1f%%\n", status.getProgress() * 100));
    
    if (status.getNearestScenarioId() != null) {
        summary.append(String.format("- **最近收敛场景**: %s (%s), 距离: %.2f\n",
                status.getNearestScenarioTitle(), status.getNearestScenarioId(), status.getDistanceToNearest()));
    }
    
    if (status.getScenarioProgress() != null && !status.getScenarioProgress().isEmpty()) {
        try {
            Map<String, Double> scenarioProgress = objectMapper.readValue(status.getScenarioProgress(), Map.class);
            String progressDetails = scenarioProgress.entrySet().stream()
                    .map(entry -> String.format("%s: %.1f%%", entry.getKey(), entry.getValue() * 100))
                    .collect(Collectors.joining(", "));
            summary.append(String.format("- **各场景进度**: %s\n", progressDetails));
        } catch (Exception e) {
            logger.warn("解析场景进度JSON失败: {}", e.getMessage());
        }
    }
    
    if (status.getActiveHints() != null && !status.getActiveHints().isEmpty()) {
        try {
            List<String> activeHints = objectMapper.readValue(status.getActiveHints(), List.class);
            summary.append(String.format("- **活跃引导提示**: %s\n", String.join(", ", activeHints)));
        } catch (Exception e) {
            logger.warn("解析活跃提示JSON失败: {}", e.getMessage());
        }
    }
    
    return summary.toString();
}
```

### 2. 提示词模板
```java
public String buildPrompt(RoleplayContext context) {
    StringBuilder prompt = new StringBuilder();
    
    // 基础提示词
    prompt.append(getBasePrompt(context));
    
    // 收敛状态信息
    String convergenceSummary = convergenceStatusService.getConvergenceStatusSummary(context.getSessionId());
    if (!convergenceSummary.isEmpty()) {
        prompt.append("\n## 当前收敛状态\n");
        prompt.append(convergenceSummary);
    }
    
    // 收敛引导要求
    prompt.append("\n## 收敛引导要求\n");
    prompt.append("请根据当前收敛状态，在回复中提供适当的引导提示，确保故事向预设结局发展。");
    
    return prompt.toString();
}
```

## 性能优化

### 1. 缓存策略
- 缓存收敛状态计算结果
- 缓存场景距离计算结果
- 缓存引导提示生成结果

### 2. 异步处理
- 收敛状态更新异步执行
- 距离计算异步执行
- 引导提示生成异步执行

### 3. 批量操作
- 批量更新收敛状态
- 批量计算场景距离
- 批量生成引导提示

## 监控和日志

### 1. 收敛监控
```java
logger.info("收敛状态更新: sessionId={}, progress={}, nearestScenario={}, distance={}", 
           sessionId, progress, nearestScenario, distance);

logger.debug("场景进度更新: sessionId={}, scenarioProgress={}", sessionId, scenarioProgress);
logger.debug("引导提示更新: sessionId={}, activeHints={}", sessionId, activeHints);
```

### 2. 性能指标
- 收敛状态更新延迟
- 距离计算时间
- 引导提示生成时间

### 3. 业务指标
- 收敛成功率
- 平均收敛时间
- 用户满意度

## 扩展性

### 1. 新场景支持
- 支持动态添加新的收敛场景
- 提供场景配置接口
- 支持场景权重调整

### 2. 自定义算法
- 支持自定义距离计算算法
- 提供算法插件机制
- 支持A/B测试

### 3. 多模型支持
- 支持不同的收敛算法
- 提供模型适配器
- 支持模型切换

## 使用示例

### 1. 基础收敛
```json
{
  "progress": 0.3,
  "nearestScenarioId": "story_convergence_1",
  "nearestScenarioTitle": "初次冒险",
  "distanceToNearest": 0.8,
  "activeHints": ["探索周围环境", "与NPC对话"]
}
```

### 2. 高级收敛
```json
{
  "progress": 0.85,
  "nearestScenarioId": "story_convergence_3",
  "nearestScenarioTitle": "最终决战",
  "distanceToNearest": 0.2,
  "scenarioProgress": {
    "story_convergence_1": 1.0,
    "story_convergence_2": 0.9,
    "story_convergence_3": 0.85
  },
  "activeHints": ["准备最终战斗", "收集关键道具"]
}
```

## 最佳实践

### 1. 收敛平衡
- 避免过度引导，保持用户自主性
- 提供多种收敛路径
- 平衡自由度和故事完整性

### 2. 提示质量
- 提供自然、有趣的引导提示
- 避免生硬的剧情强制
- 根据用户偏好调整提示风格

### 3. 性能优化
- 合理设置计算频率
- 优化算法复杂度
- 使用适当的缓存策略

### 4. 用户体验
- 提供收敛状态可视化
- 支持收敛历史查看
- 提供收敛设置选项

---

**收敛机制是QN Contest的核心创新，通过智能算法确保故事的有意义发展，为用户提供既自由又连贯的角色扮演体验。**


