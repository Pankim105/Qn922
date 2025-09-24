# 提示词系统重构：从硬编码到数据库驱动

## 🎯 问题概述

原系统存在以下问题：
1. **硬编码提示词**：`RoleplayPromptEngine` 中的世界描述、DM指令和收敛目标都是硬编码的
2. **数据库字段缺失**：`WorldTemplate` 实体缺少数据库中存在的重要字段
3. **数据未利用**：数据库中丰富的世界模板数据（收敛场景、DM指令、收敛规则）没有被使用
4. **维护困难**：修改世界设定需要修改代码而不是数据库

## 🔧 解决方案

### 1. 完善 WorldTemplate 实体

**文件**: `backend/src/main/java/com/qncontest/entity/WorldTemplate.java`

**新增字段**:
```java
@Column(name = "convergence_scenarios", columnDefinition = "JSON")
private String convergenceScenarios;

@Column(name = "dm_instructions", columnDefinition = "TEXT")
private String dmInstructions;

@Column(name = "convergence_rules", columnDefinition = "JSON")
private String convergenceRules;
```

**作用**: 与数据库表结构保持一致，能够读取完整的世界模板数据。

### 2. 扩展 WorldTemplateService

**文件**: `backend/src/main/java/com/qncontest/service/WorldTemplateService.java`

**新增方法**:
- `getConvergenceScenarios(String worldType)` - 获取收敛场景
- `getDmInstructions(String worldType)` - 获取DM指令  
- `getConvergenceRules(String worldType)` - 获取收敛规则

**作用**: 提供访问新字段数据的服务方法。

### 3. 更新 WorldTemplateResponse DTO

**文件**: `backend/src/main/java/com/qncontest/dto/WorldTemplateResponse.java`

**新增字段**:
```java
private String convergenceScenarios;
private String dmInstructions;
private String convergenceRules;
```

**作用**: 确保API响应包含完整的世界模板信息。

### 4. 重构 RoleplayPromptEngine

**文件**: `backend/src/main/java/com/qncontest/service/RoleplayPromptEngine.java`

#### 4.1 动态DM角色定义

**修改前**: 完全硬编码的DM角色描述
**修改后**: 优先从数据库加载 `dm_instructions` 字段，回退到硬编码默认值

```java
private String buildDMCharacterDefinition(RoleplayContext context) {
    try {
        // 尝试从数据库获取DM指令
        String dmInstructions = worldTemplateService.getDmInstructions(context.getWorldType());
        if (dmInstructions != null && !dmInstructions.trim().isEmpty() && 
            !dmInstructions.equals("你是一个智能助手，可以帮助用户解答各种问题。请用友好、专业的语气回答。")) {
            return dmInstructions;
        }
    } catch (Exception e) {
        logger.warn("无法从数据库获取DM指令，使用默认指令: {}", context.getWorldType(), e);
    }
    // 回退到硬编码默认值...
}
```

#### 4.2 动态收敛目标构建

**修改前**: 简单的硬编码收敛目标
**修改后**: 从数据库解析收敛场景和规则

**新增功能**:
- `parseConvergenceScenarios()` - 解析收敛场景JSON
- `parseConvergenceRules()` - 解析收敛规则JSON

**收敛场景解析**:
- 主要结局 (`main_convergence`)
- 备选结局 (`alternative_convergence`) 
- 故事阶段 (`story_convergence_1` 到 `story_convergence_5`)

**收敛规则解析**:
- 收敛阈值 (`convergence_threshold`)
- 最大探索轮数 (`max_exploration_turns`)
- 故事完整度要求 (`story_completeness_required`)

## 📊 数据库数据示例

### 日式校园世界的收敛场景
```json
{
  "story_convergence_1": {
    "scenario_id": "school_transfer",
    "title": "转校生的到来",
    "description": "玩家作为转校生来到新学校，面对陌生的环境"
  },
  "main_convergence": {
    "scenario_id": "school_festival_success", 
    "title": "校园祭典的成功",
    "description": "成功举办校园祭典，成为学生会的核心成员，留下美好回忆"
  },
  "alternative_convergence": {
    "scenario_id": "youth_romance",
    "title": "青春恋爱物语", 
    "description": "发展一段美好的校园恋情，体验青涩的青春"
  }
}
```

### DM指令示例
```text
作为校园生活的DM，你需要营造温暖、青春洋溢的氛围，鼓励玩家参与社团活动和人际交往，同时引导故事向成长和回忆的方向发展。
```

### 收敛规则示例
```json
{
  "convergence_threshold": 0.6,
  "max_exploration_turns": 25, 
  "story_completeness_required": 0.65
}
```

## 🎯 效果对比

### 修改前的提示词
```text
# 🎯 收敛目标
**世界类型**：日式校园
**主要目标**：现代日本校园生活，充满青春与友情
**收敛场景**：多个结局等待探索
**进度追踪**：系统会跟踪你的故事推进进度

无论你如何探索，故事都会自然地向某个结局收敛。
```

### 修改后的提示词
```text
# 🎯 收敛目标
**世界类型**：日式校园
**主要目标**：现代日本校园生活，充满青春与友情
**收敛场景**：多个结局等待探索
**进度追踪**：系统会跟踪你的故事推进进度

## 📖 故事收敛节点
根据你的选择和行为，故事将向以下收敛点发展：
- **主要结局**: 校园祭典的成功 - 成功举办校园祭典，成为学生会的核心成员，留下美好回忆
- **备选结局**: 青春恋爱物语 - 发展一段美好的校园恋情，体验青涩的青春
- **阶段1**: 转校生的到来 - 玩家作为转校生来到新学校，面对陌生的环境
- **阶段2**: 社团活动 - 参加社团活动，发展兴趣爱好，结识志同道合的朋友
- **阶段3**: 友情考验 - 面对朋友间的误会和考验，学会珍惜和维护友谊

## ⚖️ 收敛规则
- **收敛阈值**: 0.6 (故事收敛的触发条件)
- **最大探索轮数**: 25轮
- **故事完整度要求**: 65%

## 🎯 推进要求
- **持续进展**：每次交互都要推进故事，避免重复或停滞
- **引入新元素**：主动引入新角色、事件、地点或挑战
- **时间流动**：让故事时间自然推进，创造紧迫感
- **目标明确**：始终朝着明确的故事情节或结局推进
- **避免循环**：不要在同一场景或情节中反复打转

## ⏰ 强制场景切换规则（必须遵守）
- **3轮限制**：在同一个场景中最多进行3轮对话
- **强制切换**：第3轮后必须强制进行场景切换或任务更新
- **切换方式**：场景转换、任务更新、事件触发、时间跳跃
- **不可违反**：这是硬性规则，无论对话内容如何都必须执行

无论你如何探索，故事都会自然地向某个结局收敛。享受自由探索的乐趣！
```

## ✅ 优势

1. **数据驱动**: 世界设定现在完全由数据库驱动，便于管理和修改
2. **内容丰富**: 提示词现在包含详细的故事收敛节点和规则信息
3. **灵活性**: 可以通过修改数据库来调整世界设定，无需修改代码
4. **一致性**: 确保实体、DTO和服务层的数据结构完全一致
5. **向后兼容**: 如果数据库中没有数据，会回退到原有的硬编码逻辑
6. **可扩展**: 新的世界类型可以通过数据库配置添加，无需修改代码

## 🚀 部署说明

1. **数据库兼容**: 修改完全兼容现有数据库结构，无需迁移
2. **零停机**: 可以在不停机的情况下部署，系统会优雅降级
3. **逐步启用**: 可以逐个世界类型启用新功能，测试无误后全面推广

## 📝 总结

这次重构成功将硬编码的提示词系统转换为数据库驱动的动态系统。现在：

- ✅ 世界描述从数据库的 `description` 字段加载
- ✅ DM角色定义从数据库的 `dm_instructions` 字段加载  
- ✅ 收敛目标从数据库的 `convergence_scenarios` 和 `convergence_rules` 字段解析
- ✅ 所有字段都有合理的回退机制
- ✅ 系统现在能够充分利用数据库中丰富的世界模板数据

这为后续的世界扩展和内容管理奠定了坚实的基础。
