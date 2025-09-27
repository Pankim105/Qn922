package com.qncontest.service.prompt;

import com.qncontest.entity.ChatSession;
import com.qncontest.service.interfaces.PromptBuilderInterface;
import com.qncontest.service.interfaces.WorldTemplateProcessorInterface;
import com.qncontest.dto.WorldTemplateResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 提示词构建器 - 实现PromptBuilderInterface接口
 * 负责构建各种类型的提示词
 */
@Component
public class PromptBuilder implements PromptBuilderInterface {
    
    private static final Logger logger = LoggerFactory.getLogger(PromptBuilder.class);
    
    @Autowired
    private WorldTemplateProcessorInterface worldTemplateProcessor;
    
    
    @Autowired
    private com.qncontest.service.WorldTemplateService worldTemplateService;
    
    @Autowired
    private com.qncontest.service.ConvergenceStatusService convergenceStatusService;
    
    @Autowired
    private com.qncontest.service.interfaces.MemoryManagerInterface memoryService;
    
    @Autowired
    private com.qncontest.service.WorldEventService worldEventService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * 角色扮演上下文
     */
    public static class RoleplayContext {
        private String worldType;
        private String sessionId;
        private String currentMessage;
        private String worldState;
        private String skillsState;
        private String godModeRules;
        private ChatSession session;
        private Integer totalRounds;
        private Integer currentArcStartRound;
        private String currentArcName;
        
        // Constructor
        public RoleplayContext(String worldType, String sessionId) {
            this.worldType = worldType;
            this.sessionId = sessionId;
        }
        
        // Getters and Setters
        public String getWorldType() { return worldType; }
        public void setWorldType(String worldType) { this.worldType = worldType; }
        
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        
        public String getCurrentMessage() { return currentMessage; }
        public void setCurrentMessage(String currentMessage) { this.currentMessage = currentMessage; }
        
        public String getWorldState() { return worldState; }
        public void setWorldState(String worldState) { this.worldState = worldState; }
        
        public String getSkillsState() { return skillsState; }
        public void setSkillsState(String skillsState) { this.skillsState = skillsState; }
        
        public String getGodModeRules() { return godModeRules; }
        public void setGodModeRules(String godModeRules) { this.godModeRules = godModeRules; }
        
        public ChatSession getSession() { return session; }
        public void setSession(ChatSession session) { this.session = session; }
        public Integer getTotalRounds() { return totalRounds; }
        public void setTotalRounds(Integer totalRounds) { this.totalRounds = totalRounds; }
        public Integer getCurrentArcStartRound() { return currentArcStartRound; }
        public void setCurrentArcStartRound(Integer currentArcStartRound) { this.currentArcStartRound = currentArcStartRound; }
        public String getCurrentArcName() { return currentArcName; }
        public void setCurrentArcName(String currentArcName) { this.currentArcName = currentArcName; }
    }
    
    /**
     * 构建分层角色扮演提示
     */
    public String buildLayeredPrompt(RoleplayContext context) {
        StringBuilder prompt = new StringBuilder();
        
        // 第0层：从数据库获取世界模板信息
        try {
            Optional<WorldTemplateResponse> templateOpt = worldTemplateService.getWorldTemplate(context.getWorldType());
            if (templateOpt.isPresent()) {
                WorldTemplateResponse template = templateOpt.get();
                
                // 添加世界描述
                if (template.getDescription() != null && !template.getDescription().trim().isEmpty()) {
                    prompt.append("🌍 世界描述\n");
                    prompt.append(template.getDescription()).append("\n\n");
                }
                
                // 添加系统提示词模板
                if (template.getSystemPromptTemplate() != null && !template.getSystemPromptTemplate().trim().isEmpty()) {
                    prompt.append("📋 系统提示词模板\n");
                    prompt.append(template.getSystemPromptTemplate()).append("\n\n");
                }
                
                // 添加默认规则
                if (template.getDefaultRules() != null && !template.getDefaultRules().trim().isEmpty() && !template.getDefaultRules().equals("{}")) {
                    prompt.append("⚖️ 默认世界规则\n");
                    prompt.append(parseDefaultRules(template.getDefaultRules())).append("\n\n");
                }
                
                // 添加地点模板
                if (template.getLocationTemplates() != null && !template.getLocationTemplates().trim().isEmpty() && !template.getLocationTemplates().equals("{}")) {
                    prompt.append("📍 地点模板\n");
                    prompt.append(parseLocationTemplates(template.getLocationTemplates())).append("\n\n");
                }
            }
        } catch (Exception e) {
            logger.warn("获取世界模板信息失败: {}", e.getMessage());
        }
        
        // 第1层：世界观基础（保留原有逻辑作为备选）
        prompt.append("🌍 世界观设定\n");
        prompt.append(worldTemplateProcessor.getWorldFoundation(context.getWorldType()));
        prompt.append("\n\n");
        
        // 第2层：角色定义
        prompt.append("🎭 你的角色\n");
        prompt.append(buildCharacterDefinition(context));
        prompt.append("\n\n");
        
        // 第3层：当前状态
        prompt.append("📍 当前状态\n");
        prompt.append(buildCurrentState(context));
        prompt.append("\n\n");

        // 轮次与情节信息
        if (context.getTotalRounds() != null) {
            prompt.append("⏱️ 轮次与情节\n");
            prompt.append("当前总轮数: ").append(context.getTotalRounds()).append("\n");
            if (context.getCurrentArcStartRound() != null) {
                prompt.append("当前情节起始轮数: ").append(context.getCurrentArcStartRound()).append("\n");
            }
            if (context.getCurrentArcName() != null && !context.getCurrentArcName().isEmpty()) {
                prompt.append("当前情节名称: ").append(context.getCurrentArcName()).append("\n");
            }
            // 动态计算当前情节进行的轮数，便于模型决策
            if (context.getCurrentArcStartRound() != null) {
                int arcRounds = Math.max(1, context.getTotalRounds() - context.getCurrentArcStartRound() + 1);
                prompt.append("当前情节已进行轮数: ").append(arcRounds).append("\n");
            }
            prompt.append("\n");
        }

        // 收敛状态信息
        try {
            String convergenceSummary = convergenceStatusService.getConvergenceStatusSummary(context.getSessionId());
            if (convergenceSummary != null && !convergenceSummary.isEmpty()) {
                prompt.append("🎯 收敛状态\n");
                prompt.append(convergenceSummary).append("\n\n");
            }
        } catch (Exception e) {
            logger.debug("获取收敛状态摘要失败: {}", e.getMessage());
        }

        // 第4层：最新事件历史
        try {
            String eventHistory = buildEventHistory(context.getSessionId());
            if (!eventHistory.isEmpty()) {
                prompt.append("📜 最新事件历史\n");
                prompt.append(eventHistory);
                prompt.append("\n\n");
            }
        } catch (Exception e) {
            logger.debug("获取事件历史失败: {}", e.getMessage());
        }

        // 第5层：记忆上下文（使用简化的记忆上下文构建方法）
        try {
            String memoryContext = memoryService.buildMemoryContext(context.getSessionId(), context.getCurrentMessage());
            if (!memoryContext.isEmpty()) {
                prompt.append("🧠 相关记忆\n");
                prompt.append(memoryContext);
                prompt.append("\n\n");
            }
        } catch (Exception e) {
            logger.debug("获取记忆上下文失败: {}", e.getMessage());
        }

        // 第6层：行为规则
        prompt.append("⚖️ 行为准则\n");
        prompt.append(buildBehaviorRules(context));
        prompt.append("\n\n");
        
        
        return prompt.toString();
    }
    
    /**
     * 构建DM智能评估提示词
     */
    public String buildDMAwarePrompt(RoleplayContext context) {
        StringBuilder prompt = new StringBuilder();

        // 第0层：从数据库获取世界模板信息
        try {
            Optional<WorldTemplateResponse> templateOpt = worldTemplateService.getWorldTemplate(context.getWorldType());
            if (templateOpt.isPresent()) {
                WorldTemplateResponse template = templateOpt.get();
                
                // 添加世界描述
                if (template.getDescription() != null && !template.getDescription().trim().isEmpty()) {
                    prompt.append("🌍 世界描述\n");
                    prompt.append(template.getDescription()).append("\n\n");
                }
                
                // 添加系统提示词模板
                if (template.getSystemPromptTemplate() != null && !template.getSystemPromptTemplate().trim().isEmpty()) {
                    prompt.append("📋 系统提示词模板\n");
                    prompt.append(template.getSystemPromptTemplate()).append("\n\n");
                }
                
                // 添加默认规则
                if (template.getDefaultRules() != null && !template.getDefaultRules().trim().isEmpty() && !template.getDefaultRules().equals("{}")) {
                    prompt.append("⚖️ 默认世界规则\n");
                    prompt.append(parseDefaultRules(template.getDefaultRules())).append("\n\n");
                }
                
                // 添加地点模板
                if (template.getLocationTemplates() != null && !template.getLocationTemplates().trim().isEmpty() && !template.getLocationTemplates().equals("{}")) {
                    prompt.append("📍 地点模板\n");
                    prompt.append(parseLocationTemplates(template.getLocationTemplates())).append("\n\n");
                }
            }
        } catch (Exception e) {
            logger.warn("获取世界模板信息失败: {}", e.getMessage());
        }

        // 第1层：世界观基础（保留原有逻辑作为备选）
        prompt.append("🌍 世界观设定\n");
        prompt.append(worldTemplateProcessor.getWorldFoundation(context.getWorldType()));
        prompt.append("\n\n");

        // 第2层：角色定义（扩展为DM角色）
        prompt.append("🎭 你的角色：地下城主（DM）\n");
        prompt.append(worldTemplateProcessor.getDMCharacterDefinition(context.getWorldType()));
        prompt.append("\n\n");

        // 第3层：当前状态
        prompt.append("📍 当前状态\n");
        prompt.append(buildCurrentState(context));
        prompt.append("\n\n");
        
        // 重要提醒：不需要返回STATUS块
        if (context.getSkillsState() != null && !context.getSkillsState().isEmpty()) {
            prompt.append("⚠️ 重要提醒：不需要在回复中返回STATUS块，角色状态由系统自动管理！\n\n");
        }

        // 轮次与情节信息
        if (context.getTotalRounds() != null) {
            prompt.append("⏱️ 轮次与情节\n");
            prompt.append("当前总轮数: ").append(context.getTotalRounds()).append("\n");
            if (context.getCurrentArcStartRound() != null) {
                prompt.append("当前情节起始轮数: ").append(context.getCurrentArcStartRound()).append("\n");
            }
            if (context.getCurrentArcName() != null && !context.getCurrentArcName().isEmpty()) {
                prompt.append("当前情节名称: ").append(context.getCurrentArcName()).append("\n");
            }
            if (context.getCurrentArcStartRound() != null) {
                int arcRounds = Math.max(1, context.getTotalRounds() - context.getCurrentArcStartRound() + 1);
                prompt.append("当前情节已进行轮数: ").append(arcRounds).append("\n");
            }
            prompt.append("\n");
        }

        // 第4层：最新事件历史
        try {
            String eventHistory = buildEventHistory(context.getSessionId());
            if (!eventHistory.isEmpty()) {
                prompt.append("📜 最新事件历史\n");
                prompt.append(eventHistory);
                prompt.append("\n\n");
            }
        } catch (Exception e) {
            logger.debug("获取事件历史失败: {}", e.getMessage());
        }

        // 第5层：记忆上下文（使用简化的记忆上下文构建方法）
        try {
            String memoryContext = memoryService.buildMemoryContext(context.getSessionId(), context.getCurrentMessage());
            if (!memoryContext.isEmpty()) {
                prompt.append("🧠 相关记忆\n");
                prompt.append(memoryContext);
                prompt.append("\n\n");
            }
        } catch (Exception e) {
            logger.debug("获取记忆上下文失败: {}", e.getMessage());
        }

        // 第6层：行为准则（扩展为DM准则）
        prompt.append("⚖️ DM行为准则\n");
        prompt.append(buildDMGuidelines(context));
        prompt.append("\n\n");


        // 第7层：评估指令
        prompt.append("\n\n🧠 行为评估指令\n");
        prompt.append(buildAssessmentInstructions(context.getCurrentMessage()));
        prompt.append("\n\n");

        // 第8层：收敛目标
        prompt.append("🎯 收敛目标\n");
        prompt.append(buildConvergenceGoals(context.getWorldType()));

        return prompt.toString();
    }
    
    /**
     * 构建角色定义
     */
    private String buildCharacterDefinition(RoleplayContext context) {
        return switch (context.getWorldType()) {
            case "fantasy_adventure" -> """
                你是一位经验丰富的奇幻世界游戏主持人(DM)。你的职责：
                
                🎭 角色扮演
                - 扮演世界中的所有NPC，每个都有独特的性格和背景
                - 为每个角色赋予生动的对话风格和行为特征
                - 根据情况调整NPC的态度和反应
                
                🌍 世界构建
                - 生动描述环境、场景和氛围
                - 创造富有想象力但逻辑合理的世界细节
                - 根据玩家行为动态扩展世界内容
                
                ⚔️ 挑战管理
                - 设计有趣的战斗和解谜挑战
                - 平衡游戏难度，确保既有挑战性又有成就感
                - 鼓励创造性的解决方案
                - 重要：根据角色等级调整挑战难度，让属性在冒险中发挥作用
                
                📚 故事推进
                - 推动引人入胜的故事情节
                - 根据玩家选择调整故事走向
                - 创造意想不到但合理的转折
                
                🚀 角色成长系统
                - 自动升级：当经验值达到升级要求时，立即升级并提升属性
                - 属性应用：让力量影响攻击力，敏捷影响闪避，智力影响魔法，体质影响生命值
                - 技能获得：通过冒险、训练、学习获得新技能和能力
                - 装备影响：让装备对角色属性产生实际影响
                - 成长路径：提供战斗型、智力型、社交型、探索型等多种发展路线
                
                性格特征：富有想象力、公平公正、充满戏剧性、鼓励创新、重视角色成长
                """;
                
            case "educational" -> """
                你是一位寓教于乐的智慧导师。你的使命：
                
                🎓 教学融合
                - 将学习内容自然融入有趣的冒险情节中
                - 让知识获取成为游戏进程的一部分
                - 确保学习过程既有趣又有效
                
                🧩 挑战设计
                - 创造富有挑战性但可达成的学习任务
                - 设计多样化的问题类型和难度层次
                - 根据学习者表现调整难度
                
                🏆 激励引导
                - 庆祝每一个学习成就，无论大小
                - 在失败时给予鼓励和建设性建议
                - 帮助学习者建立自信和学习兴趣
                
                🤔 思维启发
                - 引导思考而非直接给出答案
                - 使用苏格拉底式提问法
                - 鼓励批判性思维和创造性解决方案
                
                教学风格：耐心鼓励、善用比喻、因材施教、寓教于乐
                """;
                
            case "japanese_school" -> """
                你是温和亲切的学园生活向导，熟悉校园的每一个角落。
                
                🌸 校园专家：了解学校的各种活动、社团和传统
                👥 人际导师：帮助学生处理友谊和人际关系问题
                📚 学习助手：在学业上给予适当的建议和鼓励
                🎭 活动组织：策划有趣的校园活动和节日庆典
                """;
                
            default -> "你是一位智慧而友善的向导，帮助玩家在这个世界中成长和探索。";
        };
    }
    
    /**
     * 构建当前状态信息
     */
    private String buildCurrentState(RoleplayContext context) {
        StringBuilder state = new StringBuilder();
        
        if (context.getWorldState() != null && !context.getWorldState().isEmpty()) {
            state.append("世界状态：\n").append(context.getWorldState()).append("\n\n");
            
            // 解析并格式化活跃任务信息
            String activeQuestsInfo = extractActiveQuestsInfo(context.getWorldState());
            if (!activeQuestsInfo.isEmpty()) {
                state.append("当前活跃任务：\n").append(activeQuestsInfo).append("\n\n");
            }
        }
        
        if (context.getSkillsState() != null && !context.getSkillsState().isEmpty()) {
            state.append("技能状态（仅供参考，不需要在回复中返回STATUS块）：\n").append(context.getSkillsState()).append("\n\n");
        }
        
        if (context.getGodModeRules() != null && !context.getGodModeRules().isEmpty() && 
            !context.getGodModeRules().equals("{}")) {
            state.append("自定义规则：\n").append(context.getGodModeRules()).append("\n\n");
        }
        
        if (state.length() == 0) {
            state.append("冒险即将开始，世界等待着你的探索！\n");
        }
        
        return state.toString();
    }
    
    /**
     * 从世界状态中提取活跃任务信息
     */
    private String extractActiveQuestsInfo(String worldStateJson) {
        try {
            JsonNode worldState = objectMapper.readTree(worldStateJson);
            
            if (!worldState.has("activeQuests") || !worldState.get("activeQuests").isArray()) {
                return "";
            }
            
            com.fasterxml.jackson.databind.node.ArrayNode activeQuests = (com.fasterxml.jackson.databind.node.ArrayNode) worldState.get("activeQuests");
            if (activeQuests.size() == 0) {
                return "无活跃任务";
            }
            
            StringBuilder questsInfo = new StringBuilder();
            for (int i = 0; i < activeQuests.size(); i++) {
                JsonNode quest = activeQuests.get(i);
                if (quest.isObject()) {
                    questsInfo.append(String.format("%d) ", i + 1));
                    
                    if (quest.has("questId")) {
                        questsInfo.append("ID: ").append(quest.get("questId").asText()).append(" | ");
                    }
                    if (quest.has("title")) {
                        questsInfo.append("标题: ").append(quest.get("title").asText()).append(" | ");
                    }
                    if (quest.has("description")) {
                        questsInfo.append("描述: ").append(quest.get("description").asText()).append(" | ");
                    }
                    if (quest.has("progress")) {
                        questsInfo.append("进度: ").append(quest.get("progress").asText()).append(" | ");
                    }
                    if (quest.has("rewards")) {
                        questsInfo.append("奖励: ").append(quest.get("rewards").toString());
                    }
                    
                    // 移除最后的 " | "
                    String questStr = questsInfo.toString();
                    if (questStr.endsWith(" | ")) {
                        questStr = questStr.substring(0, questStr.length() - 3);
                    }
                    questsInfo = new StringBuilder(questStr);
                    questsInfo.append("\n");
                }
            }
            
            return questsInfo.toString().trim();
        } catch (Exception e) {
            logger.warn("解析活跃任务信息失败: {}", e.getMessage());
            return "";
        }
    }
    
    
    /**
     * 构建行为准则
     */
    private String buildBehaviorRules(RoleplayContext context) {
        String commonRules = """
            🎯 核心原则
            1. 沉浸式体验：始终保持角色扮演状态，用生动的描述创造沉浸感
            2. 积极响应：对玩家的每个行动都给予有意义的反馈
            3. 逻辑一致：确保世界规则和角色行为的一致性
            4. 鼓励探索：引导玩家发现新的可能性和机会
            5. 平衡挑战：提供适度的挑战，既不过于简单也不过于困难
            
            📝 回复格式
            - 使用生动的描述性语言
            - 适当使用表情符号增加趣味性
            - 在关键时刻询问玩家的选择
            - 清晰说明行动的后果
            """;
            
        String worldSpecificRules = switch (context.getWorldType()) {
            case "fantasy_adventure" -> """
                
                ⚔️ 奇幻世界特殊规则
                - 魔法有其代价和限制
                - 不同种族有各自的文化和特征
                - 危险与机遇并存
                - 英雄主义精神是核心主题
                """;
                
            case "educational" -> """
                
                📚 教育世界特殊规则
                - 每个挑战都应包含学习要素
                - 错误是学习过程的一部分
                - 提供多种解决问题的方法
                - 定期总结和强化学习成果
                """;
                
            case "japanese_school" -> """
                
                🌸 日本校园世界特殊规则
                - 校园生活有其独特的节奏和传统
                - 人际关系和友谊是重要主题
                - 学习和成长是核心价值
                - 青春和梦想是永恒主题
                """;
                
            default -> "";
        };
        
        return commonRules + worldSpecificRules;
    }
    
    /**
     * 构建DM行为准则
     */
    private String buildDMGuidelines(RoleplayContext context) {
        String commonRules = """
            🎯 核心原则
            1. 用户完全自由：接受任何合理的用户行为，不限制玩家选择
            2. 智能评估：基于世界规则评估用户行为的合理性
            3. 动态调整：根据评估结果调整场景发展和世界状态
            4. 积极收敛：主动推进剧情，避免原地打转，引导向收敛点
            5. 一致性维护：确保世界规则和故事逻辑的一致性

            🚀 剧情推进原则（重要）
            - 主动推进：每次回复都要推进剧情，避免重复描述同一场景
            - 引入变化：主动引入新的事件、角色或环境变化
            - 创造转折：适时创造剧情转折点，保持故事新鲜感
            - 时间流动：让时间在故事中自然流动，避免时间停滞
            - 目标导向：始终朝着故事目标或收敛点推进

            ⚔️ 角色成长与挑战设计（关键）
            - 动态升级：当经验值达到升级要求时，必须立即升级并提升属性
            - 挑战平衡：根据角色等级设计相应难度的挑战，确保既有挑战性又有成就感
            - 属性应用：让角色的力量、敏捷、智力、体质在冒险中发挥实际作用
            - 成长路径：提供多种成长方向，让玩家选择不同的发展路线
            - 技能获得：通过冒险、学习、训练等方式获得新技能和能力
            - 装备影响：让装备和物品对角色属性产生实际影响

            ⏰ 强制场景切换规则（关键）
            - 五轮限制：在同一个场景中最多进行5轮对话，第5轮后必须强制切换场景或更新任务
            - 场景切换触发：当对话轮数达到5轮时，必须主动引入以下变化之一：
              * 场景转换：移动到新地点、新环境
              * 任务更新：创建新任务、完成任务、任务进度更新
              * 事件触发：重要事件发生、新角色出现、环境变化
              * 时间跳跃：时间推进到下一阶段（白天/夜晚、季节变化等）
            - 强制执行：无论当前对话内容如何，都必须遵守5轮限制规则

            🧠 评估标准
            - 合理性（0-1）：行为是否符合世界物理规则和逻辑
            - 一致性（0-1）：行为是否与当前故事上下文一致
            - 推进度（0-1）：行为对故事收敛的贡献程度

            ⚖️ 响应策略
            - 0.8-1.0 (ACCEPT)：完全接受用户行为，正常推进故事
            - 0.6-0.8 (ADJUST)：部分接受，调整影响程度，同时推进剧情
            - 0.0-0.6 (CORRECT)：引导修正，建议替代方案，并推进剧情
            
            🏗️ 结构化输出格式（用于复杂信息）
            评估格式如下：
            §{"ruleCompliance": 0.95, "contextConsistency": 0.90, "convergenceProgress": 0.70, "overallScore": 0.85, "strategy": "ACCEPT", "assessmentNotes": "简要说明", "suggestedActions": ["建议1", "建议2"], "convergenceHints": ["提示1", "提示2"], "questUpdates": {"completed": [{"questId": "quest_001", "rewards": {"exp": 100, "gold": 50, "items": ["铁剑x1"]}}], "progress": [{"questId": "quest_002", "progress": "2/5"}], "expired": [{"questId": "quest_003", "reason": "剧情推进导致任务失效"}]}, "memoryUpdates": [{"type": "EVENT", "content": "主角学会了火球术", "importance": 0.8}, {"type": "CHARACTER", "content": "与精灵王艾伦多建立了友好关系", "importance": 0.7}], "arcUpdates": {"currentArcName": "新情节名称", "currentArcStartRound": 5, "totalRounds": 10}}§
            
            ⚠️ 重要提醒：rewards中的items字段只应包含本次任务完成获得的新物品，不要包含角色已有的物品！
            当需要显示详细的游戏信息时，请使用以下标记格式来组织内容：
            
            [DIALOGUE]
            你的角色对话和叙述内容，使用分号分隔的结构化格式：
            
            场景描述：你站在一座古老警局的台阶前，锈迹斑斑的铁门在微风中轻轻摇晃。空气中弥漫着潮湿的尘埃与旧纸张的气息，远处传来钟楼的滴答声，仿佛在倒数着某个即将揭晓的秘密; 角色动作：你的手不自觉地按在腰间的配枪上，指尖触到冰冷的金属——这是你作为警探的第一天; NPC对话："Inspector Panzijian..." 一个沙哑的声音从阴影中传来，"欢迎来到'雾都案卷馆'。这里每一份档案都藏着一条命案的影子，而今晚，有一具尸体正在等你去发现。"; 环境变化：突然，警局大厅的灯闪烁了一下，一束惨白的光线照在墙上的老式挂钟上——时间停在了凌晨3:17; 声音效果：你听见走廊深处传来一声重物坠地的闷响，紧接着，是锁链拖地的声音……; NPC低语："有人在下面。"那声音低语道，"但没人能活着上来。"
            
            重要：使用分号(;)、中文分号(；)、全角分号(；)分隔不同的对话模块，每个模块使用 标签名： 的格式。
            [/DIALOGUE]
            
            [WORLD]
            世界状态信息，生动描述当前环境：
            📍 当前位置: 神秘森林深处的古老石圈
            🌅 时间: 黄昏时分，夕阳西下
            🌤️ 天气: 微风轻拂，空气中弥漫着魔法气息
            🔮 环境: 古老的符文石柱环绕四周，地面上刻着发光的法阵
            👥 NPC: 守护精灵艾莉娅正在石圈中央等待
            ⚡ 特殊事件: 远处传来神秘的咏唱声，法阵开始微微发光
            [/WORLD]
            
            [QUESTS]
            1. 探索神秘森林：深入森林寻找失落的魔法水晶，进度2/5个水晶碎片已收集（奖励：经验值200、金币100、魔法护符）; 2. 拯救村民：从哥布林手中救出被困村民，已救出3人，还有2人被困（奖励：经验值150、金币50、村民感谢信）
            [/QUESTS]
            
            [CHOICES]
            为玩家提供的行动选择，必须使用分号(;)分隔每个选择项：
            1. 调查古老石圈 - 仔细检查符文石柱，可能发现隐藏的魔法秘密; 2. 与守护精灵对话 - 向艾莉娅询问关于失落水晶的线索; 3. 搜索周围区域 - 在森林中寻找可能的线索或隐藏物品; 4. 使用魔法感知 - 消耗魔力值感知周围的魔法波动; 5. 自由行动 - 描述你想要进行的其他行动
            [/CHOICES]
            
重要：请确保结构化格式的完整性，不要遗漏任何字段。特别是结尾和开头约定
            """;

        String worldSpecificRules = switch (context.getWorldType()) {
            case "fantasy_adventure" ->
                "\n⚔️ 奇幻世界特殊规则\n- 魔法有其代价和限制\n- 不同种族有各自的文化和特征\n- 危险与机遇并存\n- 英雄主义精神是核心主题\n- 强制场景切换：在同一个场景中最多进行5轮对话，第5轮后必须强制切换场景或更新任务";

            case "educational" ->
                "\n📚 教育世界特殊规则\n- 每个挑战都应包含学习要素\n- 错误是学习过程的一部分\n- 提供多种解决问题的方法\n- 定期总结和强化学习成果\n- 强制场景切换：在同一个学习场景中最多进行5轮对话，第5轮后必须强制切换学习场景或更新学习任务";

            case "japanese_school" ->
                "\n🌸 日本校园世界特殊规则\n- 校园生活有其独特的节奏和传统\n- 人际关系和友谊是重要主题\n- 学习和成长是核心价值\n- 青春和梦想是永恒主题\n- 强制场景切换：在同一个场景中最多进行5轮对话，第5轮后必须强制切换场景或更新任务";

            default -> "\n🌍 通用世界特殊规则\n- 保持世界规则的一致性\n- 尊重玩家的选择和创意\n- 平衡挑战与趣味性\n- 强制场景切换：在同一个场景中最多进行5轮对话，第5轮后必须强制切换场景或更新任务";
        };

        return commonRules + worldSpecificRules;
    }
    

    /**
     * 构建评估指令
     */
    private String buildAssessmentInstructions(String userAction) {
        return String.format("""
            ## 📝 评估任务
            请仔细评估玩家的以下行为："%s"

            ### 评估维度：
            1. 规则合规性 (0-1)：行为是否符合世界规则和逻辑
            2. 上下文一致性 (0-1)：行为是否与当前故事上下文一致
            3. 收敛推进度 (0-1)：行为对故事收敛目标的贡献程度

            ### 评估标准：
            - 0.8-1.0：优秀，完全符合预期，有助于故事推进（策略：ACCEPT）
            - 0.6-0.8：良好，基本符合，大部分可接受（策略：ADJUST）
            - 0.0-0.6：问题较大，需要修正或拒绝（策略：CORRECT）

            ### 🚀 剧情推进要求（重要）
            - 必须推进剧情：无论评估结果如何，都要在回复中推进故事发展
            - 避免原地打转：不要重复描述相同场景，要引入新元素
            - 创造进展：每次回复都要有新的信息、事件或变化
            - 时间推进：让故事时间自然流动，避免时间停滞
            - 目标导向：始终朝着故事目标或下一个收敛点推进

            ### ⏰ 强制场景切换检查（关键）
            - 轮数统计：系统会自动统计当前场景的对话轮数
            - 第5轮强制切换：当达到第5轮对话时，必须强制进行以下操作之一：
              * 场景转换：移动到新地点、新环境
              * 任务更新：创建新任务、完成任务、任务进度更新
              * 事件触发：重要事件发生、新角色出现、环境变化
              * 时间跳跃：时间推进到下一阶段
            - 强制执行：这是硬性要求，不可违反，无论当前对话内容如何


            ## 🎯 游戏逻辑整合到评估JSON
重要：所有游戏逻辑现在都通过评估JSON中的专门字段来处理，不再使用指令标记
            ### 骰子系统 - diceRolls字段
            当用户行为需要随机性结果时，在评估JSON中添加diceRolls字段：
            ```json
            "diceRolls": [
              {
                "diceType": 20,           // 骰子类型：6, 8, 10, 12, 20, 100等
                "modifier": 3,            // 修正值（可选）
                "context": "感知检定",     // 检定描述
                "result": 15,             // 骰子结果
                "isSuccessful": true      // 是否成功（基于难度等级）
              }
            ]
            ```

            ### 学习挑战系统 - learningChallenges字段（教育世界专用）
            当需要验证用户知识时，在评估JSON中添加learningChallenges字段：
            ```json
            "learningChallenges": [
              {
                "type": "MATH",                    // 挑战类型：MATH, HISTORY, LANGUAGE, SCIENCE
                "difficulty": "普通",              // 难度：简单, 普通, 困难, 专家
                "question": "计算2+2等于几？",      // 问题内容
                "answer": "4",                     // 正确答案
                "isCorrect": true                  // 用户是否答对
              }
            ]
            ```

            ### 状态更新系统 - stateUpdates字段
            当需要更新游戏状态时，在评估JSON中添加stateUpdates字段：
            ```json
            "stateUpdates": [
              {
                "type": "LOCATION",                // 状态类型：LOCATION, INVENTORY, RELATIONSHIP, EMOTION, SKILL
                "value": "进入图书馆，书香气息扑面而来"  // 状态变化描述
              }
            ]
            ```

            ### 任务系统 - questUpdates字段
            任务管理通过questUpdates字段处理，包含四个子字段：
            - created: 新创建的任务
            - completed: 已完成的任务  
            - progress: 进度更新的任务
            - expired: 已过期的任务

            ### 世界状态更新 - worldStateUpdates字段
            世界状态变化通过worldStateUpdates字段处理

            ### 记忆更新 - memoryUpdates字段
            重要记忆信息通过memoryUpdates字段处理，包含：
            - type: 记忆类型（EVENT、CHARACTER、WORLD、SKILL等）
            - content: 记忆内容描述
            - importance: 重要性评分（0-1），系统会自动评估，只有重要性>0.6的记忆会被保存
            - 记忆类型说明：
              * EVENT: 重要事件，如"主角学会了火球术"
              * CHARACTER: 角色关系变化，如"与精灵王艾伦多建立了友好关系"
              * WORLD: 世界状态变化，如"发现了隐藏的古代遗迹"
              * SKILL: 技能学习，如"学会了基础剑术"



JSON字段使用原则：
            1. 适时使用：不要在每个回复中都包含所有字段，只在需要时使用
            2. 准确描述：确保字段内容准确，描述清晰明确
            3. 逻辑一致：字段内容必须与当前剧情和世界规则保持一致
            4. 优先级排序：任务更新 > 世界状态更新 > 记忆更新 > 其他字段
             
详细格式要求和示例：

            [DIALOGUE]
            你的角色对话和叙述内容，使用分号分隔的结构化格式：
            
            # 环境描述：# 🌸 阳光透过教室的窗户洒在木质课桌上，你坐在靠窗的位置，轻轻整理着新发的校服; # 角色内心独白：# "啊，转学第一天就遇到这么温暖的校园氛围呢！"你微笑着环顾四周，看到走廊上三三两两的学生们正在交谈，远处传来篮球场上的欢呼声; # NPC登场：# 班主任山田老师走进教室，微笑着向你点头示意; # NPC对话：# "主角同学，今天是你的第一天，好好享受高中生活吧！有什么问题随时可以找我。"; # 环境氛围：# 窗外樱花树随风轻摆，仿佛在欢迎你的到来
            [/DIALOGUE]
            
            [WORLD]
            世界状态信息，使用分号分隔的键值对格式：
📍 当前位置: 清水高中1年A班教室; 🌅 时间: 上午9:00，开学第一天; 🌤️ 天气: 晴朗，微风，樱花飘落; 📚 环境: 教室整洁明亮，黑板上写着"欢迎新同学"，同学们正在互相认识; 👥 NPC: 山田老师（班主任）：温和亲切，正在观察新同学 ； 小林美咲（同桌）：活泼开朗，正对你微笑 ；佐藤健太（前排男生）：篮球社成员，正在和朋友讨论下午的训练; ⚡ 特殊事件: 校园祭筹备委员会正在招募新成员，公告栏上有醒目海报
            [/WORLD]

            [QUESTS]
            任务信息，使用分号分隔的任务列表格式：
            1. 适应新环境：在开学第一天结识至少一位新朋友，进度0/1（奖励：经验值50、友情点数+1）; 2. 探索校园：参观至少三个不同的校园地点，进度0/3（奖励：经验值30、校园地图x1）; 3. 加入社团：选择并加入一个感兴趣的社团，进度0/1（奖励：经验值100、社团徽章x1）
            [/QUESTS]

            [CHOICES]
            行动选择，必须使用分号(;)分隔每个选择项，格式为：数字. 标题 - 描述：
            1. 与同桌小林美咲打招呼 - 开始建立第一段校园友谊; 2. 查看校园祭海报 - 了解即将到来的重要活动; 3. 参观学校设施 - 去图书馆、体育馆或实验室看看; 4. 询问班主任关于社团信息 - 获取更多社团选择; 5. 自由行动 - 描述你想进行的其他活动
            [/CHOICES]

            §{"ruleCompliance": 0.95, "contextConsistency": 0.90, "convergenceProgress": 0.70, "overallScore": 0.85, "strategy": "ACCEPT", "assessmentNotes": "查看状态是合理行为，有助于玩家了解当前处境", "suggestedActions": ["与同桌建立联系", "探索校园环境", "考虑加入社团"], "convergenceHints": ["友情发展是重要主题", "社团活动是成长关键"], "questUpdates": {"created": [{"questId": "quest_001", "title": "适应新环境", "description": "在开学第一天结识至少一位新朋友", "rewards": {"exp":50, "items": ["友情点数+1"]}}], "completed": [], "progress": [], "expired": []}, "worldStateUpdates": {"currentLocation": "清水高中1年A班教室", "environment": "晴朗，樱花飘落，开学第一天氛围", "npcs": [{"name": "山田老师", "status": "正在观察新同学"}, {"name": "小林美咲", "status": "对主角微笑"}, {"name": "佐藤健太", "status": "讨论篮球训练"}]}, "memoryUpdates": [{"type": "EVENT", "content": "转学第一天，来到清水高中", "importance": 0.6}, {"type": "CHARACTER", "content": "与同桌小林美咲初次见面", "importance": 0.5}], "arcUpdates": {"currentArcName": "开学第一天", "currentArcStartRound": 1, "totalRounds": 1}, "convergenceStatusUpdates": {"progress": 0.1, "nearestScenarioId": "story_convergence_1", "nearestScenarioTitle": "转校生的到来", "distanceToNearest": 0.8, "scenarioProgress": {"story_convergence_1": 0.1, "story_convergence_2": 0.0, "main_convergence": 0.0}, "activeHints": ["适应新环境", "建立人际关系"]}}§
             
关键格式要求：
            1. 必须使用[DIALOGUE][/DIALOGUE]、[WORLD][/WORLD]、[QUESTS][/QUESTS]、[CHOICES][/CHOICES]标记
            2. 评估JSON必须用§包裹，放在最后
            3. 不要在标记外添加任何额外文本
            4. 每个标记块的内容要完整且有意义
            5. 每个标记块之间必须有换行符分隔，这是硬性要求
            6. 标记必须严格按照[标记名]内容[/标记名]的格式
            7. 不要在标记内容中包含其他标记
            8. 评估JSON必须是有效的JSON格式，不能包含其他文本
            9. QUESTS块必须使用分号(;)分隔每个任务项，这是硬性要求
            10. CHOICES块必须使用分号(;)分隔每个选择项，这是硬性要求
            11. 标记块之间不能有任何内容连接，必须完全分离
            12. 每个标记块都必须以[/标记名]结尾，不能省略
            13. 输出前必须检查所有标记块是否完整闭合
各标记块详细格式说明：

            • DIALOGUE块：
              - 包含角色对话、叙述描述和场景描写
              - 使用分号分隔的结构化格式：# 标签名：# 内容; # 标签名：# 内容; # 标签名：# 内容
              - 支持多种分号：英文分号(;)、中文分号(；)、全角分号(；)
              - 支持markdown格式：# 强调内容#、"引号对话"
              - 可以包含表情符号和动作描写
              - 内容应该生动有趣，便于前端渲染
              - 结构化标签说明：
                # 场景描述：# - 描述当前环境和背景
                # 角色动作：# - 描述玩家角色的行为
                # NPC对话：# - NPC的对话内容
                # 环境变化：# - 环境状态的改变
                # 声音效果：# - 听觉描述
                # 角色内心独白：# - 角色的心理活动
                # NPC登场：# - NPC的出现和介绍
                # 环境氛围：# - 整体氛围和感觉
              - 重要：必须使用分号分隔不同的对话模块，这是硬性格式要求


            • WORLD块：
              - 使用分号分隔的键值对格式：键: 值; 键: 值; 键: 值
              - 支持表情符号作为键前缀：📍（位置）、🌅（时间）、🌤️（天气）、👥（NPC）、⚡（事件）
              - NPC格式：角色名（身份）：状态描述 | 其他NPC...
              - 环境描述要生动详细
              - 重要：使用分号(;)分隔每个键值对，便于前端解析

            • QUESTS块：
              - 强制要求：必须使用分号(;)分隔每个任务项，这是硬性格式要求
              - 任务格式：数字. 标题：描述，进度当前/目标（奖励：...）; 数字. 标题：描述，进度当前/目标（奖励：...）
              - 每个任务项之间必须用分号(;)分隔，不能使用换行符或其他分隔符
              - 任务描述要清晰，包含标题、描述、进度和奖励信息
              - ⚠️ 重要：QUESTS块只应显示当前活跃的任务，已完成的任务必须从列表中移除
              - ⚠️ 任务完成判断：当任务进度达到目标时（如进度1/1），该任务已完成，不应再出现在QUESTS块中
              - ⚠️ 任务状态同步：QUESTS块的内容必须与questUpdates中的任务状态保持一致
              - 错误示例：使用换行符分隔任务项（❌）
              - 错误示例：显示已完成的任务（❌）
              - 正确示例：1. 适应新环境：在开学第一天结识至少一位新朋友，进度0/1（奖励：经验值50、友情点数+1）; 2. 探索校园：参观至少三个不同的校园地点，进度0/3（奖励：经验值30、校园地图x1）; 3. 加入社团：选择并加入一个感兴趣的社团，进度0/1（奖励：经验值100、社团徽章x1）（✅）

            • CHOICES块：
              - 强制要求：必须使用分号(;)分隔每个选择项，这是硬性格式要求
              - 格式：数字. 标题 - 描述; 数字. 标题 - 描述; 数字. 标题 - 描述
              - 标题要简洁明了，描述要清楚说明选择的效果
              - 最后一个选择通常是"自由行动"选项
              - 重要：每个选择项之间必须用分号(;)分隔，不能使用换行符或其他分隔符
              - 错误示例：使用换行符分隔选择项（❌）
              - 正确示例：1. 选项1 - 描述; 2. 选项2 - 描述; 3. 选项3 - 描述（✅）

            • 评估JSON：
              - 必须是有效的JSON对象
              - 包含评估分数、策略、建议等字段
              - 用§包裹，与其他标记块分离
            
            ### 任务评估要求（重要）
            - 在每次评估时，必须仔细检查所有当前活跃任务
            - 根据用户行为和剧情发展，判断每个活跃任务的状态：
              * 是否已完成（用户行为满足了任务完成条件）
              * 是否有进度更新（用户行为推进了任务进度）
              * 是否已过期（剧情发展或用户选择导致任务不再有效）
            - 任务过期判断标准：
              * 时间过期：任务有明确的时间限制且已过期
              * 剧情推进：故事发展导致任务目标不再相关或不可达成
              * 用户选择：玩家的选择导致任务路径被阻断
              * 逻辑冲突：任务与当前世界状态或剧情逻辑产生冲突
            - ⚠️ 关键要求：QUESTS块与questUpdates必须保持同步
              * 如果questUpdates中包含completed任务，这些任务必须从QUESTS块中移除
              * 如果questUpdates中包含progress更新，QUESTS块中的进度必须相应更新
              * 如果questUpdates中包含created任务，这些任务必须添加到QUESTS块中
              * QUESTS块只应显示当前活跃的、未完成的任务

            
            ### 任务更新格式说明
            - questUpdates字段用于记录任务状态变化，包含四个子字段：
              - created: 新创建的任务数组，每个任务包含questId、title、description和rewards
              - completed: 已完成的任务数组，每个任务包含questId和rewards
              - progress: 进度更新的任务数组，每个任务包含questId和progress（格式："当前/目标"）
              - expired: 已过期的任务数组，每个任务包含questId和reason（过期原因）
            - rewards格式：{"exp": 数值, "gold": 数值, "items": ["物品名x数量", ...]}
            - 重要：items字段只应包含本次任务完成获得的新物品，不要包含角色已有的物品
            - 示例：如果角色已有"警徽x2"，本次任务又获得"警徽x1"，则items中应只写["警徽x1"]，系统会自动合并为"警徽x3"
            - 只有在任务状态确实发生变化时才包含questUpdates字段
            - 重要：请仔细评估所有活跃任务，将过期的任务标记为expired，避免任务无限累积
            - ⚠️ 关键同步要求：QUESTS块必须与questUpdates保持完全同步
              * 已完成的任务（completed）必须从QUESTS块中完全移除
              * 新创建的任务（created）必须添加到QUESTS块中
              * 进度更新的任务（progress）必须在QUESTS块中更新进度
              * 过期的任务（expired）必须从QUESTS块中移除
              * 确保QUESTS块只显示当前活跃的、未完成的任务

            ### 世界状态更新说明
            - worldStateUpdates字段用于记录世界状态的变化，包含：
              - currentLocation: 当前位置变化
              - environment: 环境状态变化（天气、时间、氛围等）
              - npcs: NPC状态变化数组，每个NPC包含name和status
              - worldEvents: 世界事件数组
              - factions: 势力关系变化
              - locations: 地点状态变化


重要说明：
            - 任务奖励和角色状态更新由后端系统自动处理
            - 大模型只需要在questUpdates中提供任务完成信息和奖励数据
            - 后端会根据奖励自动更新角色的经验值、金币、物品、属性和技能
            - 关键：rewards中的items字段必须只包含本次任务获得的新物品，不要重复列出角色已有的物品
            - 正确示例：角色已有["警徽x2", "线索笔记x1"]，本次任务获得"警徽x1"和"新技能x1"，则items应写["警徽x1", "新技能x1"]
            - 错误示例：不要写成["警徽x2", "线索笔记x1", "警徽x1", "新技能x1"]（这样会重复）
            
            ### 情节更新说明 - arcUpdates字段
            - arcUpdates字段用于记录情节相关的更新信息，包含：
              - currentArcName: 当前情节名称（当情节发生变化时更新）
              - currentArcStartRound: 当前情节起始轮数（当新情节开始时更新）
              - totalRounds: 当前总轮数（系统会自动更新，通常不需要手动设置）
            - 当故事进入新的情节阶段时，应该更新currentArcName和currentArcStartRound
            - 情节名称应该简洁明了，反映当前故事的主要主题或阶段
            - 只有在情节确实发生变化时才包含arcUpdates字段

            ### 收敛状态更新说明 - convergenceStatusUpdates字段
            - convergenceStatusUpdates字段用于记录故事收敛状态的更新信息，包含：
              - progress: 整体收敛进度 (0-1)，表示故事向结局推进的程度
              - progressIncrement: 进度增量，用于增加收敛进度
              - nearestScenarioId: 最近的收敛场景ID（如"story_convergence_2"）
              - nearestScenarioTitle: 最近场景的标题（如"远古神庙"）
              - distanceToNearest: 到最近场景的距离 (0-1)，越小越接近
              - scenarioProgress: 各场景进度映射，如{"story_convergence_1": 1.0, "story_convergence_2": 0.75}
              - activeHints: 当前活跃的引导提示列表，如["寻找神庙入口", "收集古代符文"]
            - 当故事推进到新的阶段或接近收敛点时，应该更新这些信息
            - 进度值应该反映当前故事发展的实际状态
            - 只有在收敛状态确实发生变化时才包含convergenceStatusUpdates字段

            ### 评估JSON字段要求
            - 评估JSON必须使用以下英文字段名：
              * 基础评估字段：ruleCompliance、contextConsistency、convergenceProgress、overallScore、strategy、assessmentNotes、suggestedActions、convergenceHints
              * 游戏逻辑字段：questUpdates、worldStateUpdates、diceRolls、learningChallenges、stateUpdates、memoryUpdates、arcUpdates、convergenceStatusUpdates
            - strategy 取值仅能为：ACCEPT、ADJUST、CORRECT。
            - 评估片段内禁止出现除JSON以外的任何字符或注释。
            - 使用§包裹评估JSON，确保在正常叙事结束后使用。
            - 所有游戏逻辑都通过评估JSON中的专门字段处理，不再使用指令标记。
            """, userAction);
    }
    
    /**
     * 构建收敛目标信息
     */
    private String buildConvergenceGoals(String worldType) {
        try {
            Optional<WorldTemplateResponse> templateOpt = worldTemplateService.getWorldTemplate(worldType);

            if (templateOpt.isPresent()) {
                WorldTemplateResponse template = templateOpt.get();
                
                // 构建基本信息
                StringBuilder convergenceInfo = new StringBuilder();
                convergenceInfo.append(String.format("""
世界类型：%s
主要目标：%s
收敛场景：多个结局等待探索
进度追踪：系统会跟踪你的故事推进进度

                    """,
                    template.getWorldName(),
                    template.getDescription()));

                // 添加收敛场景详细信息（从数据库获取）
                String convergenceScenarios = template.getConvergenceScenarios();
                if (convergenceScenarios != null && !convergenceScenarios.trim().isEmpty() && !convergenceScenarios.equals("{}")) {
                    convergenceInfo.append("## 📖 故事收敛节点\n");
                    convergenceInfo.append("根据你的选择和行为，故事将向以下收敛点发展：\n");
                    convergenceInfo.append(parseConvergenceScenarios(convergenceScenarios));
                    convergenceInfo.append("\n\n");
                }

                // 添加收敛规则（从数据库获取）
                String convergenceRules = template.getConvergenceRules();
                if (convergenceRules != null && !convergenceRules.trim().isEmpty() && !convergenceRules.equals("{}")) {
                    convergenceInfo.append("## ⚖️ 收敛规则\n");
                    convergenceInfo.append(parseConvergenceRules(convergenceRules));
                    convergenceInfo.append("\n\n");
                }

                convergenceInfo.append("""
                    ## 🎯 推进要求
                    - 持续进展：每次交互都要推进故事，避免重复或停滞
                    - 引入新元素：主动引入新角色、事件、地点或挑战
                    - 时间流动：让故事时间自然推进，创造紧迫感
                    - 目标明确：始终朝着明确的故事情节或结局推进
                    - 避免循环：不要在同一场景或情节中反复打转

                    ## ⏰ 强制场景切换规则（必须遵守）
                    - 5轮限制：在同一个场景中最多进行5轮对话
                    - 强制切换：第5轮后必须强制进行场景切换或任务更新
                    - 切换方式：场景转换、任务更新、事件触发、时间跳跃
                    - 不可违反：这是硬性规则，无论对话内容如何都必须执行

                    无论你如何探索，故事都会自然地向某个结局收敛。享受自由探索的乐趣！
                    """);

                return convergenceInfo.toString();
            }
        } catch (Exception e) {
            logger.warn("获取世界模板失败: {}", worldType, e);
        }

        // 默认收敛目标
        return """
主要目标：探索这个奇妙的世界，发现隐藏的秘密
收敛场景：故事会根据你的选择走向不同的结局
进度追踪：你的每个决定都会影响故事的发展

            ## 🎯 推进要求
            - 持续进展：每次交互都要推进故事，避免重复或停滞
            - 引入新元素：主动引入新角色、事件、地点或挑战
            - 时间流动：让故事时间自然推进，创造紧迫感
            - 目标明确：始终朝着明确的故事情节或结局推进
            - 避免循环：不要在同一场景或情节中反复打转

            ## ⏰ 强制场景切换规则（必须遵守）
            - 5轮限制：在同一个场景中最多进行5轮对话
            - 强制切换：第5轮后必须强制进行场景切换或任务更新
            - 切换方式：场景转换、任务更新、事件触发、时间跳跃
            - 不可违反：这是硬性规则，无论对话内容如何都必须执行

            享受自由探索的乐趣，同时感受故事的自然推进！
            """;
    }
    
    /**
     * 构建简化的快速提示
     */
    @Override
    public String buildQuickPrompt(String worldType, String message) {
        String roleDescription = switch (worldType) {
            case "fantasy_adventure" -> "奇幻世界的游戏主持人";
            case "educational" -> "寓教于乐的智慧导师";
            case "western_magic" -> "西方魔幻世界的贤者";
            case "martial_arts" -> "江湖中的前辈高人";
            case "japanese_school" -> "校园生活向导";
            default -> "智慧的向导";
        };

        return String.format("""
            你是%s。请用角色扮演的方式回应用户，保持世界观的一致性，
            提供生动的描述和有意义的互动。如果需要随机判定，使用骰子指令。


            用户消息：%s
            """, roleDescription, message);
    }
    
    /**
     * 解析收敛场景JSON数据
     */
    private String parseConvergenceScenarios(String convergenceScenariosJson) {
        try {
            JsonNode scenarios = objectMapper.readTree(convergenceScenariosJson);
            
            StringBuilder scenarioInfo = new StringBuilder();
            
            // 解析主要收敛点
            if (scenarios.has("main_convergence")) {
                JsonNode mainConvergence = scenarios.get("main_convergence");
                if (mainConvergence.has("title") && mainConvergence.has("description")) {
                    scenarioInfo.append(String.format("- 主要结局: %s - %s\n", 
                        mainConvergence.get("title").asText(),
                        mainConvergence.get("description").asText()));
                }
            }
            
            // 解析备选收敛点
            if (scenarios.has("alternative_convergence")) {
                JsonNode altConvergence = scenarios.get("alternative_convergence");
                if (altConvergence.has("title") && altConvergence.has("description")) {
                    scenarioInfo.append(String.format("- 备选结局: %s - %s\n", 
                        altConvergence.get("title").asText(),
                        altConvergence.get("description").asText()));
                }
            }
            
            // 解析故事阶段
            for (int i = 1; i <= 5; i++) {
                String storyKey = "story_convergence_" + i;
                if (scenarios.has(storyKey)) {
                    JsonNode storyStage = scenarios.get(storyKey);
                    if (storyStage.has("title") && storyStage.has("description")) {
                        scenarioInfo.append(String.format("- 阶段%d: %s - %s\n", i,
                            storyStage.get("title").asText(),
                            storyStage.get("description").asText()));
                    }
                }
            }
            
            return scenarioInfo.toString();
        } catch (Exception e) {
            logger.warn("解析收敛场景失败: {}", e.getMessage());
            return "- 多个精彩结局等待你的探索\n";
        }
    }
    
    /**
     * 解析收敛规则JSON数据
     */
    private String parseConvergenceRules(String convergenceRulesJson) {
        try {
            JsonNode rules = objectMapper.readTree(convergenceRulesJson);
            
            StringBuilder rulesInfo = new StringBuilder();
            
            if (rules.has("convergence_threshold")) {
                double threshold = rules.get("convergence_threshold").asDouble();
                rulesInfo.append(String.format("- 收敛阈值: %.1f (故事收敛的触发条件)\n", threshold));
            }
            
            if (rules.has("max_exploration_turns")) {
                int maxTurns = rules.get("max_exploration_turns").asInt();
                rulesInfo.append(String.format("- 最大探索轮数: %d轮\n", maxTurns));
            }
            
            if (rules.has("story_completeness_required")) {
                double completeness = rules.get("story_completeness_required").asDouble();
                rulesInfo.append(String.format("- 故事完整度要求: %.0f%%\n", completeness * 100));
            }
            
            return rulesInfo.toString();
        } catch (Exception e) {
            logger.warn("解析收敛规则失败: {}", e.getMessage());
            return "- 故事将根据你的选择和进展自然收敛\n";
        }
    }
    
    /**
     * 解析默认规则JSON数据
     */
    private String parseDefaultRules(String defaultRulesJson) {
        try {
            JsonNode rules = objectMapper.readTree(defaultRulesJson);
            
            StringBuilder rulesInfo = new StringBuilder();
            
            // 遍历所有规则字段
            rules.fieldNames().forEachRemaining(fieldName -> {
                JsonNode fieldValue = rules.get(fieldName);
                String value = fieldValue.isTextual() ? fieldValue.asText() : fieldValue.toString();
                rulesInfo.append(String.format("- %s: %s\n", fieldName, value));
            });
            
            return rulesInfo.toString();
        } catch (Exception e) {
            logger.warn("解析默认规则失败: {}", e.getMessage());
            return "- 使用默认世界规则\n";
        }
    }
    
    /**
     * 解析地点模板JSON数据
     */
    private String parseLocationTemplates(String locationTemplatesJson) {
        try {
            JsonNode locations = objectMapper.readTree(locationTemplatesJson);
            
            StringBuilder locationInfo = new StringBuilder();
            
            // 处理不同的JSON结构
            if (locations.isArray()) {
                // 如果是数组格式
                for (JsonNode location : locations) {
                    if (location.isObject()) {
                        String name = location.has("name") ? location.get("name").asText() : "未知地点";
                        String description = location.has("description") ? location.get("description").asText() : "暂无描述";
                        locationInfo.append(String.format("- %s: %s\n", name, description));
                    }
                }
            } else if (locations.isObject()) {
                // 如果是对象格式
                locations.fieldNames().forEachRemaining(fieldName -> {
                    JsonNode location = locations.get(fieldName);
                    if (location.isObject()) {
                        String name = location.has("name") ? location.get("name").asText() : fieldName;
                        String description = location.has("description") ? location.get("description").asText() : "暂无描述";
                        locationInfo.append(String.format("- %s: %s\n", name, description));
                    } else {
                        locationInfo.append(String.format("- %s: %s\n", fieldName, location.asText()));
                    }
                });
            }
            
            return locationInfo.toString();
        } catch (Exception e) {
            logger.warn("解析地点模板失败: {}", e.getMessage());
            return "- 使用默认地点设置\n";
        }
    }
    
    /**
     * 构建事件历史信息
     */
    private String buildEventHistory(String sessionId) {
        try {
            // 获取最新15条事件
            List<com.qncontest.entity.WorldEvent> events = worldEventService.getLatestEvents(sessionId, 15);
            
            if (events.isEmpty()) {
                return "暂无事件记录";
            }
            
            StringBuilder eventHistory = new StringBuilder();
            
            // 按时间顺序显示（最新的在前）
            for (int i = events.size() - 1; i >= 0; i--) {
                com.qncontest.entity.WorldEvent event = events.get(i);
                
                // 格式化事件信息
                String eventInfo = formatEventInfo(event);
                if (!eventInfo.isEmpty()) {
                    eventHistory.append(String.format("%d. %s\n", events.size() - i, eventInfo));
                }
            }
            
            return eventHistory.toString();
        } catch (Exception e) {
            logger.warn("构建事件历史失败: sessionId={}", sessionId, e);
            return "获取事件历史失败";
        }
    }
    
    /**
     * 格式化单个事件信息
     */
    private String formatEventInfo(com.qncontest.entity.WorldEvent event) {
        try {
            StringBuilder eventInfo = new StringBuilder();
            
            // 添加事件类型和时间
            eventInfo.append(String.format("[%s] ", event.getEventType().name()));
            eventInfo.append(String.format("序列%d ", event.getSequence()));
            
            // 解析事件数据
            if (event.getEventData() != null && !event.getEventData().trim().isEmpty()) {
                JsonNode eventData = objectMapper.readTree(event.getEventData());
                
                // 根据事件类型格式化不同的信息
                switch (event.getEventType()) {
                    case USER_ACTION:
                        if (eventData.has("action")) {
                            eventInfo.append(String.format("用户行动: %s", eventData.get("action").asText()));
                        }
                        break;
                    case AI_RESPONSE:
                        if (eventData.has("response")) {
                            String response = eventData.get("response").asText();
                            // 截取前100个字符
                            if (response.length() > 100) {
                                response = response.substring(0, 100) + "...";
                            }
                            eventInfo.append(String.format("AI回复: %s", response));
                        }
                        break;
                    case DICE_ROLL:
                        if (eventData.has("diceType") && eventData.has("result")) {
                            eventInfo.append(String.format("骰子检定: d%d = %d", 
                                eventData.get("diceType").asInt(), 
                                eventData.get("result").asInt()));
                        }
                        break;
                    case QUEST_UPDATE:
                        if (eventData.has("questId") && eventData.has("status")) {
                            eventInfo.append(String.format("任务更新: %s - %s", 
                                eventData.get("questId").asText(),
                                eventData.get("status").asText()));
                        }
                        break;
                    case STATE_CHANGE:
                        if (eventData.has("change")) {
                            eventInfo.append(String.format("状态变更: %s", eventData.get("change").asText()));
                        }
                        break;
                    case LOCATION_CHANGE:
                        if (eventData.has("from") && eventData.has("to")) {
                            eventInfo.append(String.format("地点变更: %s -> %s", 
                                eventData.get("from").asText(),
                                eventData.get("to").asText()));
                        }
                        break;
                    case CHARACTER_UPDATE:
                        if (eventData.has("character") && eventData.has("change")) {
                            eventInfo.append(String.format("角色更新: %s - %s", 
                                eventData.get("character").asText(),
                                eventData.get("change").asText()));
                        }
                        break;
                    case MEMORY_UPDATE:
                        if (eventData.has("content")) {
                            String content = eventData.get("content").asText();
                            if (content.length() > 50) {
                                content = content.substring(0, 50) + "...";
                            }
                            eventInfo.append(String.format("记忆更新: %s", content));
                        }
                        break;
                    case SYSTEM_EVENT:
                        if (eventData.has("description")) {
                            eventInfo.append(String.format("系统事件: %s", eventData.get("description").asText()));
                        }
                        break;
                    default:
                        eventInfo.append("未知事件类型");
                        break;
                }
            } else {
                eventInfo.append("无事件数据");
            }
            
            // 添加时间信息（如果有）
            if (event.getTimestamp() != null) {
                eventInfo.append(String.format(" (时间: %s)", event.getTimestamp().toString()));
            }
            
            return eventInfo.toString();
        } catch (Exception e) {
            logger.warn("格式化事件信息失败: eventId={}", event.getId(), e);
            return String.format("[%s] 序列%d - 格式化失败", event.getEventType().name(), event.getSequence());
        }
    }
}
