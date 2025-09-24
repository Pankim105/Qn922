# 基于现有架构的灵活收敛故事系统实现方案

## 1. 现有架构分析

### 1.1 现有核心组件

#### ChatSession实体
现有ChatSession已包含完整的角色扮演字段：
- `worldType` - 世界类型
- `worldRules` - 世界规则（JSON）
- `godModeRules` - 上帝模式规则（JSON）
- `worldState` - 世界状态（JSON）
- `skillsState` - 技能状态（JSON）
- `storyCheckpoints` - 故事检查点（JSON）
- `stabilityAnchor` - 稳定性锚点（JSON）

#### WorldTemplate实体
现有WorldTemplate已包含：
- `worldId` - 世界ID
- `worldName` - 世界名称
- `description` - 世界描述
- `defaultRules` - 默认规则（JSON）
- `systemPromptTemplate` - 系统提示词模板
- `stabilityAnchors` - 稳定性锚点（JSON）
- `characterTemplates` - 角色模板（JSON）
- `locationTemplates` - 地点模板（JSON）
- `questTemplates` - 任务模板（JSON）

#### RoleplayWorldService
已实现功能：
- 世界状态管理
- 骰子检定系统
- 稳定性锚点管理
- 事件记录和版本控制
- 世界状态更新和校验

#### RoleplayPromptEngine
已实现功能：
- 分层提示词构建
- 世界模板集成
- 角色定义和状态管理
- 行为规则和技能指令

### 1.2 现有API接口
RoleplayController已提供：
- 世界模板管理
- 会话初始化
- 流式聊天
- 骰子检定
- 世界状态管理

## 2. 最小化修改实现方案

### 2.1 数据库修改

#### 在WorldTemplate表中添加收敛场景字段
```sql
ALTER TABLE world_templates ADD COLUMN convergence_scenarios JSON COMMENT '收敛场景集合';
ALTER TABLE world_templates ADD COLUMN dm_instructions TEXT COMMENT 'DM行为指令';
ALTER TABLE world_templates ADD COLUMN convergence_rules JSON COMMENT '收敛规则';
```

#### 扩展现有字段使用
- `storyCheckpoints` - 存储收敛状态和进度
- `worldState` - 扩展存储收敛进度信息
- `skillsState` - 扩展存储DM记忆和评估结果

### 2.2 服务层扩展

#### 扩展RoleplayWorldService
在现有RoleplayWorldService基础上添加：

```java
@Service
public class ConvergenceStoryService {

    @Autowired
    private RoleplayWorldService roleplayWorldService;

    @Autowired
    private WorldTemplateService worldTemplateService;

    /**
     * 评估用户行为并计算收敛状态
     */
    public ConvergenceResult assessUserAction(String sessionId, String userAction) {
        // 1. 获取当前会话状态
        ChatSession session = roleplayWorldService.getCurrentSession(sessionId);

        // 2. 评估行为合理性
        BehaviorAssessment assessment = assessBehavior(userAction, session);

        // 3. 计算收敛状态
        ConvergenceStatus convergenceStatus = calculateConvergence(session, userAction);

        // 4. 更新世界状态
        updateConvergenceState(sessionId, convergenceStatus, assessment);

        return new ConvergenceResult(assessment, convergenceStatus);
    }

    /**
     * 获取收敛引导提示
     */
    public String getConvergenceHint(String sessionId) {
        ConvergenceStatus status = getConvergenceStatus(sessionId);
        return generateConvergenceHint(status);
    }
}
```

#### 扩展RoleplayPromptEngine
在现有RoleplayPromptEngine基础上添加DM角色：

```java
@Service
public class DMPromptBuilder {

    @Autowired
    private RoleplayPromptEngine existingPromptEngine;

    /**
     * 构建DM智能提示词
     */
    public String buildDMPrompt(RoleplayContext context, ConvergenceStatus convergenceStatus) {
        StringBuilder prompt = new StringBuilder();

        // 使用现有提示词构建
        String basePrompt = existingPromptEngine.buildLayeredPrompt(context);
        prompt.append(basePrompt);

        // 添加DM角色定义
        prompt.append("\n\n# 🎭 地下城主（DM）角色\n");
        prompt.append(getDMInstructions(context.getWorldType()));
        prompt.append("\n\n");

        // 添加收敛目标信息
        prompt.append("# 🎯 收敛目标\n");
        prompt.append(formatConvergenceScenarios(convergenceStatus));
        prompt.append("\n\n");

        // 添加DM行为准则
        prompt.append("# ⚖️ DM行为准则\n");
        prompt.append(getDMGuidelines());

        return prompt.toString();
    }
}
```

### 2.3 基于大模型的智能评估系统

### 2.3.1 评估模式切换设计

使用**模式切换**的思路来解决流式输出分割问题：

```markdown
## 🎲 DM决策原则
1. **用户完全自由**：接受任何合理的用户行为，不限制玩家选择
2. **智能评估**：基于世界规则评估用户行为的合理性
3. **动态调整**：根据评估结果调整场景发展和世界状态
4. **收敛引导**：在故事推进过程中，逐渐引导向收敛点
5. **一致性维护**：确保世界规则和故事逻辑的一致性

## 🧠 评估标准
- **合理性（0-1）**：行为是否符合世界物理规则和逻辑
- **一致性（0-1）**：行为是否与当前故事上下文一致
- **推进度（0-1）**：行为对故事收敛的贡献程度

## ⚖️ 响应策略
- **高分行为**：完全接受，正常推进故事
- **中分行为**：部分接受，调整影响程度
- **低分行为**：引导修正，建议替代方案

---

**重要**：在你的回复中，请使用以下格式：

**用户内容**（正常叙述，会显示给用户）...

**^&***（特殊切换标记）
```
{
  "ruleCompliance": 0.85,
  "contextConsistency": 0.92,
  "convergenceProgress": 0.78,
  "overallScore": 0.85,
  "strategy": "ACCEPT",
  "assessmentNotes": "这是一个合理的探索行为",
  "suggestedActions": ["继续探索", "与NPC对话"],
  "convergenceHints": ["正在接近重要情节节点"]
}
```
**^&***（结束标记）

**更多用户内容**（继续显示给用户）...

**规则**：
1. **^&*** 是切换标记，表示切换到评估模式
2. 评估JSON会被后端提取，不显示给用户
3. 评估完成后自动切换回用户内容模式
4. 可以在回复的任何位置插入评估块
```

### 2.3.2 评估数据结构

```java
public class DMAssessment {
    private double ruleCompliance;        // 规则合规性 (0-1)
    private double contextConsistency;    // 上下文一致性 (0-1)
    private double convergenceProgress;   // 收敛推进度 (0-1)
    private double overallScore;          // 综合评分 (0-1)
    private String assessmentNotes;       // 评估说明
    private List<String> suggestedActions; // 建议行动
    private List<String> convergenceHints; // 收敛提示
    private LocalDateTime assessedAt;     // 评估时间

    // 评估策略建议
    private AssessmentStrategy strategy; // ACCEPT, ADJUST, CORRECT

    public enum AssessmentStrategy {
        ACCEPT,    // 完全接受
        ADJUST,    // 部分调整
        CORRECT    // 引导修正
    }
}

public class ConvergenceStatus {
    private double progress;              // 收敛进度 (0-1)
    private String nearestScenarioId;     // 最近的收敛场景
    private String nearestScenarioTitle;  // 最近场景标题
    private double distanceToNearest;     // 到最近场景的距离
    private List<ScenarioProgress> scenarioProgress; // 所有场景进度
    private List<String> activeHints;     // 当前活跃的引导提示
}

public class ScenarioProgress {
    private String scenarioId;
    private String title;
    private double progress;              // 推进进度 (0-1)
    private boolean isActive;             // 是否激活
    private LocalDateTime lastUpdated;    // 最后更新时间
}
```

### 2.3.3 基于模式切换的流式评估截取器

```java
@Service
public class StreamingAssessmentExtractor {

    private static final String MODE_SWITCH_MARKER = "^&*";
    private static final int MAX_ASSESSMENT_SIZE = 5000; // 评估JSON最大长度

    private final ObjectMapper objectMapper = new ObjectMapper();
    private boolean isInAssessmentMode = false;  // 当前是否在评估模式
    private StringBuilder assessmentBuffer = new StringBuilder();
    private StringBuilder userContentBuffer = new StringBuilder();

    /**
     * 处理流式token，基于模式切换
     */
    public StreamTokenResult processToken(String token) {
        StreamTokenResult result = new StreamTokenResult();

        // 检测模式切换标记
        if (MODE_SWITCH_MARKER.equals(token.trim())) {
            // 切换模式
            if (isInAssessmentMode) {
                // 从评估模式切换到用户内容模式
                result.setAssessment(parseAndHandleAssessment());
                result.setShouldSendToClient(false);
                isInAssessmentMode = false;
                assessmentBuffer = new StringBuilder();
            } else {
                // 从用户内容模式切换到评估模式
                // 将之前累积的用户内容发送出去
                if (userContentBuffer.length() > 0) {
                    result.setContent(userContentBuffer.toString());
                    result.setShouldSendToClient(true);
                    userContentBuffer = new StringBuilder();
                }
                isInAssessmentMode = true;
            }
            return result;
        }

        // 根据当前模式处理token
        if (isInAssessmentMode) {
            // 评估模式：累积评估内容
            assessmentBuffer.append(token);

            // 检查缓冲区大小，防止内存溢出
            if (assessmentBuffer.length() > MAX_ASSESSMENT_SIZE) {
                logger.warn("评估内容过大，重置评估模式");
                isInAssessmentMode = false;
                assessmentBuffer = new StringBuilder();
                // 降级：将内容当作用户内容处理
                userContentBuffer.append("^&*").append(assessmentBuffer);
                result.setContent(userContentBuffer.toString());
                result.setShouldSendToClient(true);
                userContentBuffer = new StringBuilder();
                return result;
            }

            result.setShouldSendToClient(false);
            return result;

        } else {
            // 用户内容模式：累积普通内容
            userContentBuffer.append(token);
            result.setContent(token);
            result.setShouldSendToClient(true);
            return result;
        }
    }

    /**
     * 解析并处理评估结果
     */
    private DMAssessment parseAndHandleAssessment() {
        if (assessmentBuffer.length() == 0) {
            return null;
        }

        try {
            // 清理评估内容（移除可能的空白字符）
            String cleanJson = assessmentBuffer.toString().trim();
            DMAssessment assessment = objectMapper.readValue(cleanJson, DMAssessment.class);

            // 记录评估事件
            logger.debug("成功解析评估结果: ruleCompliance={}, contextConsistency={}, strategy={}",
                        assessment.getRuleCompliance(),
                        assessment.getContextConsistency(),
                        assessment.getStrategy());

            return assessment;

        } catch (Exception e) {
            logger.warn("解析评估结果失败: {}", assessmentBuffer.toString(), e);
            // 评估解析失败，记录错误但不影响用户体验
            logger.error("评估JSON解析失败，内容: {}", assessmentBuffer.toString());
            return null;
        }
    }

    /**
     * 获取累积的用户内容（用于批量发送）
     */
    public String getBufferedUserContent() {
        return userContentBuffer.toString();
    }

    /**
     * 强制重置所有状态
     */
    public void reset() {
        isInAssessmentMode = false;
        assessmentBuffer = new StringBuilder();
        userContentBuffer = new StringBuilder();
    }

    /**
     * 流式token处理结果
     */
    public static class StreamTokenResult {
        private String content;           // 要发送给客户端的内容
        private boolean shouldSendToClient; // 是否应该发送给客户端
        private DMAssessment assessment;  // 解析的评估结果

        // Getters and Setters
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public boolean isShouldSendToClient() { return shouldSendToClient; }
        public void setShouldSendToClient(boolean shouldSendToClient) {
            this.shouldSendToClient = shouldSendToClient;
        }

        public DMAssessment getAssessment() { return assessment; }
        public void setAssessment(DMAssessment assessment) {
            this.assessment = assessment;
        }
    }
}
```

### 2.3.4 基于模式切换的StreamAiService扩展

```java
@Service
public class EnhancedStreamAiService extends StreamAiService {

    @Autowired
    private StreamingAssessmentExtractor assessmentExtractor;

    @Autowired
    private DMAssessmentService dmAssessmentService;

    @Autowired
    private ConvergenceCalculator convergenceCalculator;

    /**
     * 扩展的角色扮演流式聊天，包含评估模式切换
     */
    @Override
    public SseEmitter handleRoleplayStreamChat(RoleplayRequest request, User user) {
        SseEmitter emitter = new SseEmitter(300000L);

        CompletableFuture.runAsync(() -> {
            try {
                // 保存安全上下文
                var authentication = SecurityContextHolder.getContext().getAuthentication();

                // 设置超时和错误处理
                emitter.onTimeout(() -> {
                    logger.warn("SSE connection timed out for user: {}", user.getUsername());
                    assessmentExtractor.reset(); // 重置评估状态
                    emitter.complete();
                });

                emitter.onError((error) -> {
                    logger.error("SSE connection error for user: {}", user.getUsername(), error);
                    assessmentExtractor.reset(); // 重置评估状态
                });

                // ... 其他现有代码保持不变 ...

                // 基于模式切换的流式响应处理器
                StreamingResponseHandler<AiMessage> handler = new StreamingResponseHandler<AiMessage>() {
                    @Override
                    public void onNext(String token) {
                        try {
                            SecurityContextHolder.getContext().setAuthentication(authentication);

                            // 使用评估模式切换器处理token
                            StreamingAssessmentExtractor.StreamTokenResult result =
                                assessmentExtractor.processToken(token);

                            // 处理评估结果（如果有）
                            if (result.getAssessment() != null) {
                                handleAssessmentResult(result.getAssessment(), session, request.getMessage());
                            }

                            // 如果有要发送给客户端的内容
                            if (result.isShouldSendToClient() && result.getContent() != null) {
                                String eventData = String.format("{\"content\":\"%s\"}",
                                    escapeJsonString(result.getContent()));
                                emitter.send(SseEmitter.event()
                                    .name("message")
                                    .data(eventData));
                            }

                        } catch (IOException e) {
                            logger.error("Error sending SSE data", e);
                            emitter.completeWithError(e);
                        }
                    }

                    @Override
                    public void onComplete(Response<AiMessage> response) {
                        try {
                            SecurityContextHolder.getContext().setAuthentication(authentication);

                            // 处理剩余的累积内容
                            String remainingContent = assessmentExtractor.getBufferedUserContent();
                            if (remainingContent != null && !remainingContent.isEmpty()) {
                                String eventData = String.format("{\"content\":\"%s\"}",
                                    escapeJsonString(remainingContent));
                                emitter.send(SseEmitter.event()
                                    .name("message")
                                    .data(eventData));
                            }

                            // 重置评估状态
                            assessmentExtractor.reset();

                            // ... 其他现有完成逻辑保持不变 ...

                        } catch (Exception e) {
                            logger.error("Error completing SSE stream", e);
                            emitter.completeWithError(e);
                        }
                    }

                    @Override
                    public void onError(Throwable error) {
                        logger.error("Error in AI response stream", error);
                        assessmentExtractor.reset(); // 重置评估状态
                        try {
                            emitter.send(SseEmitter.event()
                                .name("error")
                                .data("{\"error\":\"AI服务暂时不可用，请稍后重试\"}"));
                        } catch (IOException e) {
                            logger.error("Error sending error event", e);
                        }
                        emitter.completeWithError(error);
                    }
                };

                // ... 其余代码保持不变 ...

            } catch (Exception e) {
                logger.error("Error in roleplay stream chat processing", e);
                assessmentExtractor.reset(); // 重置评估状态
                try {
                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data("{\"error\":\"处理请求时发生错误\"}"));
                } catch (IOException ioException) {
                    logger.error("Error sending error event", ioException);
                }
                emitter.completeWithError(e);
            }
        }, executorService);

        return emitter;
    }

    /**
     * 处理评估结果
     */
    private void handleAssessmentResult(DMAssessment assessment, ChatSession session, String userAction) {
        try {
            // 计算收敛状态
            ConvergenceStatus convergenceStatus = convergenceCalculator.calculateConvergence(session, userAction);

            // 基于评估结果生成DM响应
            DMResponse dmResponse = dmAssessmentService.generateDMResponse(
                userAction, session, assessment, convergenceStatus);

            // 更新世界状态
            updateWorldState(session, dmResponse);

            // 记录评估事件
            recordAssessmentEvent(session.getSessionId(), assessment, convergenceStatus);

            logger.debug("成功处理评估结果: strategy={}, overallScore={}",
                        assessment.getStrategy(), assessment.getOverallScore());

        } catch (Exception e) {
            logger.error("处理评估结果失败", e);
            // 评估处理失败不影响用户体验，继续正常流程
        }
    }

    /**
     * 记录评估事件
     */
    private void recordAssessmentEvent(String sessionId, DMAssessment assessment, ConvergenceStatus status) {
        try {
            // 记录到世界事件系统
            String eventData = String.format(
                "{\"type\":\"dm_assessment\",\"assessment\":%s,\"convergence\":%s}",
                objectMapper.writeValueAsString(assessment),
                objectMapper.writeValueAsString(status)
            );

            // 这里可以调用RoleplayWorldService记录事件
            logger.info("记录DM评估事件: sessionId={}, strategy={}",
                       sessionId, assessment.getStrategy());

        } catch (Exception e) {
            logger.warn("记录评估事件失败", e);
        }
    }
}
```

### 2.3.5 评估后端处理服务

```java
@Service
public class DMAssessmentService {

    @Autowired
    private RoleplayWorldService roleplayWorldService;

    @Autowired
    private ConvergenceCalculator convergenceCalculator;

    /**
     * 基于大模型评估结果生成DM响应
     */
    public DMResponse generateDMResponse(String userAction, ChatSession session,
                                        DMAssessment assessment, ConvergenceStatus convergenceStatus) {

        DMResponse response = new DMResponse();

        // 根据评估策略决定响应方式
        switch (assessment.getStrategy()) {
            case ACCEPT:
                // 完全接受用户行为
                response.setResponseType(DMResponse.ResponseType.ACCEPT);
                response.setNarrative(generateAcceptingNarrative(userAction, session));
                response.setWorldStateChanges(generateWorldStateChanges(userAction, session, "full"));
                break;

            case ADJUST:
                // 部分接受，调整影响
                response.setResponseType(DMResponse.ResponseType.ADJUST);
                response.setNarrative(generateAdjustedNarrative(userAction, session, assessment));
                response.setWorldStateChanges(generateWorldStateChanges(userAction, session, "partial"));
                break;

            case CORRECT:
                // 引导修正
                response.setResponseType(DMResponse.ResponseType.CORRECT);
                response.setNarrative(generateCorrectingNarrative(userAction, session, assessment));
                response.setSuggestedAlternatives(assessment.getSuggestedActions());
                response.setConvergenceHints(convergenceStatus.getActiveHints());
                break;
        }

        return response;
    }

    /**
     * 更新世界状态
     */
    private void updateWorldState(ChatSession session, DMResponse response) {
        String currentState = session.getWorldState();
        String updatedState = mergeWorldStateChanges(currentState, response.getWorldStateChanges());

        // 更新收敛状态
        String currentCheckpoints = session.getStoryCheckpoints();
        String updatedCheckpoints = updateConvergenceInCheckpoints(currentCheckpoints, response.getConvergenceStatus());

        roleplayWorldService.updateWorldState(session.getSessionId(), updatedState, session.getSkillsState());
    }
}
```

#### 收敛状态计算
```java
public class ConvergenceCalculator {

    public ConvergenceStatus calculateConvergence(ChatSession session, String userAction) {
        ConvergenceStatus status = new ConvergenceStatus();

        // 获取当前世界模板的收敛场景
        WorldTemplate template = worldTemplateService.getWorldTemplate(session.getWorldType());
        List<ConvergenceScenario> scenarios = parseConvergenceScenarios(template.getConvergenceScenarios());

        // 计算到各收敛点的距离
        for (ConvergenceScenario scenario : scenarios) {
            double distance = calculateDistanceToConvergence(session, userAction, scenario);
            status.addScenarioDistance(scenario.getId(), distance);
        }

        // 判断是否触发收敛
        if (status.shouldTriggerConvergence()) {
            status.setTriggeredScenario(selectBestConvergenceScenario(status));
        }

        return status;
    }
}
```

### 2.4 前端接口扩展

#### 复用现有API
- 使用现有的 `/roleplay/chat/stream` 接口
- 扩展消息格式支持收敛信息
- 添加新的提示端点

#### 新的前端组件
```javascript
// 自由探索组件 - 复用现有聊天界面
const FreeExplorationChat = ({ sessionId, onMessage }) => {
    // 复用现有的流式聊天组件
    // 添加收敛进度显示
    // 添加DM提示功能
};

// 收敛进度组件
const ConvergenceProgress = ({ convergenceStatus }) => {
    return (
        <div className="convergence-progress">
            <h3>故事进度</h3>
            <ProgressBar progress={convergenceStatus.progress} />
            <div className="convergence-hints">
                {convergenceStatus.currentHints.map(hint => (
                    <div key={hint.id} className="hint">{hint.text}</div>
                ))}
            </div>
        </div>
    );
};
```

### 2.3.6 增强的DM提示词构建

```java
@Service
public class EnhancedDMPromptBuilder {

    @Autowired
    private RoleplayPromptEngine existingPromptEngine;

    @Autowired
    private WorldTemplateService worldTemplateService;

    /**
     * 构建包含评估指令的DM提示词
     */
    public String buildAssessmentEnabledPrompt(RoleplayContext context, String userAction) {
        StringBuilder prompt = new StringBuilder();

        // 使用现有提示词作为基础
        String basePrompt = existingPromptEngine.buildLayeredPrompt(context);
        prompt.append(basePrompt);
        prompt.append("\n\n");

        // 添加DM角色定义
        prompt.append("# 🎭 地下城主（DM）角色\n");
        prompt.append(getDMInstructions(context.getWorldType()));
        prompt.append("\n\n");

        // 添加收敛场景信息
        prompt.append("# 🎯 收敛目标\n");
        prompt.append(getConvergenceScenarios(context.getWorldType()));
        prompt.append("\n\n");

        // 添加评估指令
        prompt.append("# 🧠 行为评估指令\n");
        prompt.append(getAssessmentInstructions(userAction));
        prompt.append("\n\n");

        // 添加响应策略
        prompt.append("# ⚖️ DM响应策略\n");
        prompt.append(getResponseStrategyInstructions());

        return prompt.toString();
    }

    private String getDMInstructions(String worldType) {
        return """
            你是一个经验丰富的地下城主（Dungeon Master），负责：
            - 评估玩家的行为合理性和故事一致性
            - 动态生成合适的场景描述和NPC反应
            - 在适当时候引导故事向预设的收敛点发展
            - 维护世界状态的一致性和连贯性
            """;
    }

    private String getAssessmentInstructions(String userAction) {
        return String.format("""
            ## 📝 评估任务
            请仔细评估玩家的以下行为："%s"

            ### 评估维度：
            1. **规则合规性 (0-1)**：行为是否符合世界规则和逻辑
            2. **上下文一致性 (0-1)**：行为是否与当前故事上下文一致
            3. **收敛推进度 (0-1)**：行为对故事收敛目标的贡献程度

            ### 评估标准：
            - 0.8-1.0：优秀，完全符合预期
            - 0.6-0.8：良好，基本符合，大部分可接受
            - 0.4-0.6：一般，需要调整和引导
            - 0.0-0.4：问题较大，需要修正或拒绝

            ### 重要：在回复中包含JSON格式的评估结果
            """, userAction);
    }

    private String getResponseStrategyInstructions() {
        return """
            ## ⚖️ 响应策略
            根据综合评分（三个维度的平均值）选择响应策略：

            - **0.8-1.0 (ACCEPT)**：完全接受用户行为
              * 正常推进故事
              * 提供丰富描述
              * 记录行为结果

            - **0.6-0.8 (ADJUST)**：部分接受，调整影响
              * 接受核心行为
              * 调整不合理部分
              * 提供解释说明

            - **0.0-0.6 (CORRECT)**：引导修正
              * 指出问题所在
              * 提供替代建议
              * 给出收敛提示

            ## 🔍 评估输出格式
            在你的回复**任何位置**使用模式切换标记包含评估：

            **用户内容**（正常的故事叙述，会显示给用户）...

            **^&***（模式切换标记）
            ```
            {
              "ruleCompliance": 0.85,
              "contextConsistency": 0.92,
              "convergenceProgress": 0.78,
              "overallScore": 0.85,
              "strategy": "ACCEPT",
              "assessmentNotes": "这是一个合理的探索行为，有助于推进主线剧情",
              "suggestedActions": ["继续探索", "与NPC对话", "检查环境线索"],
              "convergenceHints": ["你的行为正在接近一个重要情节节点"]
            }
            ```
            **^&***（结束标记）

            **更多用户内容**（继续显示给用户）...

            **规则**：
            1. **^&*** 是模式切换标记，告诉系统切换到评估模式
            2. 评估JSON会被后端自动提取，不会在回复中显示给用户
            3. 评估块前后可以有任意用户内容
            4. 可以在回复的开头、中间或结尾插入评估块
            """;
    }

    private String getConvergenceScenarios(String worldType) {
        try {
            WorldTemplate template = worldTemplateService.getWorldTemplate(worldType);
            if (template.getConvergenceScenarios() != null) {
                return formatConvergenceScenarios(template.getConvergenceScenarios());
            }
        } catch (Exception e) {
            // 降级处理
        }

        // 默认收敛场景
        return """
            **主要收敛点**：
            - 故事结局：完成主要剧情线
            - 支线结局：完成重要支线任务
            - 开放结局：探索充分后的自然结局
            """;
    }
}
```

## 3. 实现步骤

### 3.1 数据库扩展
1. 在WorldTemplate表中添加收敛相关字段
2. 更新数据库初始化脚本，包含收敛场景数据

### 3.2 核心服务实现
1. 创建`StreamingAssessmentExtractor` - 流式评估截取器
2. 创建`DMAssessmentService` - 评估处理服务
3. 创建`ConvergenceCalculator` - 收敛状态计算器
4. 创建`EnhancedDMPromptBuilder` - 增强DM提示词构建器

### 3.3 扩展现有服务
1. 扩展`StreamAiService`添加评估截取功能
2. 扩展`RoleplayWorldService`添加收敛状态管理
3. 扩展`RoleplayPromptEngine`支持DM评估模式

### 3.4 前端适配
1. 复用现有聊天界面，添加评估进度显示
2. 添加收敛进度可视化组件
3. 添加DM提示和建议显示

## 4. 工作流程

### 4.1 用户行为处理流程

```
用户输入 → 构建DM提示词 → 大模型生成回复
    ↓
流式输出 → 评估截取器检测标记 → 截取评估JSON
    ↓
后端处理评估 → 计算收敛状态 → 生成DM响应
    ↓
更新世界状态 → 继续转发其他内容 → 前端接收
```

### 4.2 模式切换时序图

```
Token流: [用户内容] [^&*] [评估JSON] [^&*] [更多用户内容]

处理过程:
1. 检测到 ^&* 标记 → 切换到评估模式
2. 累积评估JSON内容 → 直到下一个 ^&*
3. 解析评估结果 → 后端处理
4. 切换回用户内容模式 → 继续转发
```

**模式状态机**：

```
用户内容模式 ──^&*──→ 评估模式 ──^&*──→ 用户内容模式
    ↑                                        │
    └────────────────────────────────────────┘
         （循环处理）
```

**容错处理**：
- 如果评估JSON解析失败 → 记录错误但不影响用户体验
- 如果模式切换异常 → 自动重置到用户内容模式
- 如果评估内容过大 → 降级处理为用户内容

## 5. 优势分析

### 5.1 技术优势
- **零额外API调用**：评估在大模型的单次调用中完成
- **实时处理**：流式输出中实时截取和处理评估
- **智能评估**：充分利用大模型的理解和推理能力
- **完全隐藏**：评估信息对用户完全透明

### 5.2 架构优势
- **最小化修改**：只在流式处理链路中添加截取逻辑
- **向后兼容**：现有接口完全不变
- **容错性强**：评估失败时降级到普通回复模式
- **扩展灵活**：可以轻松添加新的评估维度

### 5.3 用户体验优势
- **完全自由**：用户可以进行任何想做的行为
- **智能引导**：基于大模型理解的个性化建议
- **自然收敛**：逐渐引导到预设故事节点
- **沉浸感强**：DM回复更加贴合用户行为

## 6. 总结

通过**模式切换**的流式评估截取机制，我们实现了：

1. **智能DM评估**：大模型作为DM进行智能行为评估
2. **零额外开销**：单次API调用完成评估和回复生成
3. **实时处理**：流式输出中实时模式切换和评估处理
4. **完全透明**：评估逻辑对用户完全隐藏
5. **灵活收敛**：根据评估结果智能引导故事发展
6. **极高容错性**：模式切换失败时自动降级，不影响用户体验

## 7. 核心创新点

### 7.1 模式切换机制
```
默认状态: 用户内容模式 ──^&*──→ 评估模式 ──^&*──→ 用户内容模式
```

- **单字符触发**：使用 `^&*` 作为模式切换标记，不会被分割
- **状态机管理**：清晰的模式切换逻辑，易于调试
- **智能缓冲**：分别管理用户内容和评估内容的缓冲

### 7.2 流式处理优势
- **分割安全**：单字符标记不会被流式输出分割
- **实时响应**：评估处理不阻塞用户内容转发
- **内存安全**：限制评估缓冲区大小，防止内存溢出
- **错误恢复**：异常时自动重置到安全状态

### 7.3 用户体验保证
- **完全自由**：用户行为不受任何限制
- **智能引导**：基于大模型理解的个性化评估和建议
- **无缝体验**：评估处理对用户完全透明
- **降级优雅**：评估失败时自动降级到普通回复模式

这种设计完美解决了流式输出分割问题，同时保持了系统的简洁性和可靠性，是一个**真正工程化**的解决方案！
