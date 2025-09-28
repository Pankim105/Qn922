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
    private com.qncontest.service.ChatSessionService chatSessionService;
    
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
        try {
            // 获取实际的对话轮数（基于用户消息数量）
            int actualRounds = getActualConversationRounds(context.getSessionId());
            
            prompt.append("⏱️ 轮次与情节\n");
            prompt.append("当前总轮数: ").append(actualRounds).append(" (基于实际对话轮数)\n");
            
            if (context.getCurrentArcStartRound() != null) {
                prompt.append("当前情节起始轮数: ").append(context.getCurrentArcStartRound()).append("\n");
            }
            if (context.getCurrentArcName() != null && !context.getCurrentArcName().isEmpty()) {
                prompt.append("当前情节名称: ").append(context.getCurrentArcName()).append("\n");
            }
            // 动态计算当前情节进行的轮数，便于模型决策
            if (context.getCurrentArcStartRound() != null) {
                int arcRounds = Math.max(1, actualRounds - context.getCurrentArcStartRound() + 1);
                prompt.append("当前情节已进行轮数: ").append(arcRounds).append("\n");
            }
            prompt.append("\n");
        } catch (Exception e) {
            logger.debug("获取实际对话轮数失败: {}", e.getMessage());
            // 降级处理：使用原有的totalRounds
            if (context.getTotalRounds() != null) {
                prompt.append("⏱️ 轮次与情节\n");
                prompt.append("当前总轮数: ").append(context.getTotalRounds()).append(" (降级显示)\n");
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

        // 第4层：最新对话历史
        try {
            String conversationHistory = buildConversationHistory(context.getSessionId());
            if (!conversationHistory.isEmpty()) {
                prompt.append("💬 最新对话历史\n");
                prompt.append(conversationHistory);
                prompt.append("\n\n");
            }
        } catch (Exception e) {
            logger.debug("获取对话历史失败: {}", e.getMessage());
        }

        // 第5层：最新事件历史
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

        // 第6层：记忆上下文（使用简化的记忆上下文构建方法）
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
        try {
            // 获取实际的对话轮数（基于用户消息数量）
            int actualRounds = getActualConversationRounds(context.getSessionId());
            
            prompt.append("⏱️ 轮次与情节\n");
            prompt.append("当前总轮数: ").append(actualRounds).append(" (基于实际对话轮数)\n");
            
            if (context.getCurrentArcStartRound() != null) {
                prompt.append("当前情节起始轮数: ").append(context.getCurrentArcStartRound()).append("\n");
            }
            if (context.getCurrentArcName() != null && !context.getCurrentArcName().isEmpty()) {
                prompt.append("当前情节名称: ").append(context.getCurrentArcName()).append("\n");
            }
            if (context.getCurrentArcStartRound() != null) {
                int arcRounds = Math.max(1, actualRounds - context.getCurrentArcStartRound() + 1);
                prompt.append("当前情节已进行轮数: ").append(arcRounds).append("\n");
            }
            prompt.append("\n");
        } catch (Exception e) {
            logger.debug("获取实际对话轮数失败: {}", e.getMessage());
            // 降级处理：使用原有的totalRounds
            if (context.getTotalRounds() != null) {
                prompt.append("⏱️ 轮次与情节\n");
                prompt.append("当前总轮数: ").append(context.getTotalRounds()).append(" (降级显示)\n");
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
        }

        // 第4层：最新对话历史
        try {
            String conversationHistory = buildConversationHistory(context.getSessionId());
            if (!conversationHistory.isEmpty()) {
                prompt.append("💬 最新对话历史\n");
                prompt.append(conversationHistory);
                prompt.append("\n\n");
            }
        } catch (Exception e) {
            logger.debug("获取对话历史失败: {}", e.getMessage());
        }

        // 第5层：最新事件历史
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

        // 第6层：记忆上下文（使用简化的记忆上下文构建方法）
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
     * 构建角色定义 - 从数据库世界模板中读取
     */
    private String buildCharacterDefinition(RoleplayContext context) {
        try {
            Optional<WorldTemplateResponse> templateOpt = worldTemplateService.getWorldTemplate(context.getWorldType());
            if (templateOpt.isPresent()) {
                WorldTemplateResponse template = templateOpt.get();
                
                // 优先使用数据库中的系统提示词模板
                if (template.getSystemPromptTemplate() != null && !template.getSystemPromptTemplate().trim().isEmpty()) {
                    return template.getSystemPromptTemplate();
                }
                
                // 如果没有系统提示词模板，使用DM指令
                if (template.getDmInstructions() != null && !template.getDmInstructions().trim().isEmpty()) {
                    return template.getDmInstructions();
                }
            }
        } catch (Exception e) {
            logger.warn("获取世界模板角色定义失败: {}", e.getMessage());
        }
        
        // 降级处理：使用默认角色定义
        return "你是一位智慧而友善的向导，帮助玩家在这个世界中成长和探索。";
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
     * 构建行为准则 - 从数据库世界模板中读取
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
            
        // 从数据库获取世界特定规则
        String worldSpecificRules = getWorldSpecificRules(context.getWorldType());
        
        return commonRules + worldSpecificRules;
    }
    
    /**
     * 从数据库获取世界特定规则
     */
    private String getWorldSpecificRules(String worldType) {
        try {
            Optional<WorldTemplateResponse> templateOpt = worldTemplateService.getWorldTemplate(worldType);
            if (templateOpt.isPresent()) {
                WorldTemplateResponse template = templateOpt.get();
                
                // 使用数据库中的默认规则
                if (template.getDefaultRules() != null && !template.getDefaultRules().trim().isEmpty() && !template.getDefaultRules().equals("{}")) {
                    return "\n\n" + parseDefaultRules(template.getDefaultRules());
                }
            }
        } catch (Exception e) {
            logger.warn("获取世界特定规则失败: {}", e.getMessage());
        }
        
        return "";
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
            
            """;

        // 从数据库获取世界特定规则
        String worldSpecificRules = getWorldSpecificRules(context.getWorldType());
        
        // 添加通用的强制场景切换规则
        worldSpecificRules += "\n- 强制场景切换：在同一个场景中最多进行5轮对话，第5轮后必须强制切换场景或更新任务";

        return commonRules + worldSpecificRules;
    }
    

    /**
     * 构建评估指令
     */
    private String buildAssessmentInstructions(String userAction) {
        return String.format("""
            📝 评估任务
            请仔细评估玩家的以下行为："%s"

            评估维度：
            1. 规则合规性 (0-1)：行为是否符合世界规则和逻辑
            2. 上下文一致性 (0-1)：行为是否与当前故事上下文一致
            3. 收敛推进度 (0-1)：行为对故事收敛目标的贡献程度

            评估标准：
            - 0.8-1.0：优秀，完全符合预期，有助于故事推进（策略：ACCEPT）
            - 0.6-0.8：良好，基本符合，大部分可接受（策略：ADJUST）
            - 0.0-0.6：问题较大，需要修正或拒绝（策略：CORRECT）

            🚀 剧情推进要求（重要）
            - 必须推进剧情：无论评估结果如何，都要在回复中推进故事发展
            - 避免原地打转：不要重复描述相同场景，要引入新元素
            - 创造进展：每次回复都要有新的信息、事件或变化
            - 时间推进：让故事时间自然流动，避免时间停滞
            - 目标导向：始终朝着故事目标或下一个收敛点推进

            ⏰ 强制场景切换检查（关键）
            - 轮数统计：系统会自动统计当前场景的对话轮数
            - 第5轮强制切换：当达到第5轮对话时，必须强制进行以下操作之一：
              * 场景转换：移动到新地点、新环境
              * 任务更新：创建新任务、完成任务、任务进度更新
              * 事件触发：重要事件发生、新角色出现、环境变化
              * 时间跳跃：时间推进到下一阶段
            - 强制执行：这是硬性要求，不可违反，无论当前对话内容如何


             🎯 游戏逻辑整合到评估JSON
重要：所有游戏逻辑现在都通过评估JSON中的专门字段来处理，不再使用指令标记
            骰子系统 - diceRolls字段
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

            学习挑战系统 - learningChallenges字段（教育世界专用）
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

            状态更新系统 - stateUpdates字段
            当需要更新游戏状态时，在评估JSON中添加stateUpdates字段：
            ```json
            "stateUpdates": [
              {
                "type": "LOCATION",                // 状态类型：LOCATION, INVENTORY, RELATIONSHIP, EMOTION, SKILL
                "value": "进入图书馆，书香气息扑面而来"  // 状态变化描述
              }
            ]
            ```

            任务系统 - questUpdates字段
            任务管理通过questUpdates字段处理，包含四个子字段：
            - created: 新创建的任务
            - completed: 已完成的任务  
            - progress: 进度更新的任务
            - expired: 已过期的任务

            世界状态更新 - worldStateUpdates字段
            世界状态变化通过worldStateUpdates字段处理

            记忆更新 - memoryUpdates字段
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
            
            [场景描述]: [具体场景描述]; [角色动作]: [角色行为描述]; [NPC对话]: "[NPC对话内容]"; [环境变化]: [环境变化描述]; [声音效果]: [声音描述]; [角色内心独白]: "[角色心理活动]"; [NPC登场]: [NPC出现描述]; [环境氛围]: [氛围描述]
            [/DIALOGUE]
            
            [WORLD]
            世界状态信息，使用分号分隔的键值对格式：
            📍 当前位置: [具体位置]; 🌅 时间: [时间描述]; 🌤️ 天气: [天气状况]; 📚 环境: [环境描述]; 👥 NPC: [NPC状态描述]; ⚡ 特殊事件: [事件描述]
            [/WORLD]

            [QUESTS]
            任务信息，使用分号分隔的任务列表格式：
            1. [任务标题]: [任务描述]，进度[当前/目标]（奖励：[奖励描述]）; 2. [任务标题]: [任务描述]，进度[当前/目标]（奖励：[奖励描述]）; 3. [任务标题]: [任务描述]，进度[当前/目标]（奖励：[奖励描述]）
            [/QUESTS]

            [CHOICES]
            行动选择，必须使用分号(;)分隔每个选择项，格式为：数字. 标题 - 描述：
            1. [选择标题] - [选择描述]; 2. [选择标题] - [选择描述]; 3. [选择标题] - [选择描述]; 4. [选择标题] - [选择描述]; 5. 自由行动 - 描述你想进行的其他活动
            [/CHOICES]

            §{"ruleCompliance": [0-1], "contextConsistency": [0-1], "convergenceProgress": [0-1], "overallScore": [0-1], "strategy": "[ACCEPT|ADJUST|CORRECT]", "assessmentNotes": "[评估说明]", "suggestedActions": ["[建议1]", "[建议2]"], "convergenceHints": ["[提示1]", "[提示2]"], "questUpdates": {"created": [{"questId": "[任务ID]", "title": "[任务标题]", "description": "[任务描述]", "rewards": {"exp": [数值], "items": ["[物品]"]}}], "completed": [], "progress": [], "expired": []}, "worldStateUpdates": {"currentLocation": "[位置]", "environment": "[环境描述]", "npcs": [{"name": "[NPC名称]", "status": "[状态]"}]}, "memoryUpdates": [{"type": "[EVENT|CHARACTER|WORLD|SKILL]", "content": "[记忆内容]", "importance": [0-1]}], "arcUpdates": {"currentArcName": "[情节名称]", "currentArcStartRound": [轮数], "totalRounds": [总轮数]}, "convergenceStatusUpdates": {"progress": [0-1], "nearestScenarioId": "[场景ID]", "nearestScenarioTitle": "[场景标题]", "distanceToNearest": [0-1], "scenarioProgress": {"[场景ID]": [0-1]}, "activeHints": ["[提示1]", "[提示2]"]}}§
             
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
              - 使用分号分隔的结构化格式：标签名： 内容;  标签名：内容;  标签名： 内容
              - 支持多种分号：英文分号(;)、中文分号(；)、全角分号(；)
              - 支持markdown格式："引号对话"
              - 可以包含表情符号和动作描写
              - 内容应该生动有趣，便于前端渲染
              - 结构化标签说明：
               场景描述： - 描述当前环境和背景
                角色动作： - 描述玩家角色的行为
                NPC对话： - NPC的对话内容
                环境变化：- 环境状态的改变
                声音效果：- 听觉描述
                角色内心独白：- 角色的心理活动
                NPC登场：- NPC的出现和介绍
                环境氛围： - 整体氛围和感觉
              - 重要：必须使用分号分隔不同的对话模块，这是硬性格式要求
              - ⚠️ 格式要求：每个标签后必须紧跟冒号，内容要简洁明了
              - ⚠️ 格式示例：场景描述：[环境描述]; 角色动作：[角色行为]; NPC对话："[对话内容]"


            • WORLD块：
              - 使用分号分隔的键值对格式：键: 值; 键: 值; 键: 值
              - 支持表情符号作为键前缀：📍（位置）、🌅（时间）、🌤️（天气）、👥（NPC）、⚡（事件）
              - NPC格式：角色名（身份）：状态描述 | 其他NPC...
              - 环境描述要生动详细
              - 重要：使用分号(;)分隔每个键值对，便于前端解析
              - ⚠️ 格式要求：每个键后必须紧跟冒号，值要简洁明了
              - ⚠️ 格式示例：📍 当前位置: [位置描述]; 🌅 时间: [时间描述]; 🌤️ 天气: [天气状况]; 👥 NPC: [NPC状态]; ⚡ 特殊事件: [事件描述]

            • QUESTS块：使用分号(;)分隔任务项，格式：数字. 标题：描述，进度当前/目标（奖励：...）
              - 只显示当前活跃任务，已完成任务必须移除
              - 每个任务ID只能出现一次，不能重复
              - 与questUpdates保持完全同步
              - ⚠️ 格式要求：任务标题要简洁，描述要清晰，进度格式要准确
              - ⚠️ 格式示例：1. [任务标题]：[任务描述]，进度[当前/目标]（奖励：[奖励内容]）; 2. [任务标题]：[任务描述]，进度[当前/目标]（奖励：[奖励内容]）

            • CHOICES块：使用分号(;)分隔选择项，格式：数字. 标题 - 描述
              - 最后一个选择通常是"自由行动"选项
              - ⚠️ 格式要求：标题要简洁，描述要清晰说明选择的效果
              - ⚠️ 格式示例：1. [选择标题] - [选择描述]; 2. [选择标题] - [选择描述]; 3. [选择标题] - [选择描述]; 4. 自由行动 - [自由行动描述]

            • 评估JSON：用§包裹，包含评估分数、策略、建议等字段
            
            ⚠️ 前端渲染格式要求（重要）：
            - 所有标记块必须严格按照格式要求，确保前端能正确解析和渲染
            - DIALOGUE块：使用分号分隔不同模块，每个模块格式为"标签名：内容"
            - WORLD块：使用分号分隔键值对，格式为"表情符号 键名: 值"
            - QUESTS块：使用分号分隔任务项，格式为"数字. 标题：描述，进度当前/目标（奖励：...）"
            - CHOICES块：使用分号分隔选择项，格式为"数字. 标题 - 描述"
            - 评估JSON：用§包裹，必须是有效的JSON格式
            - ⚠️ 重要：格式示例中的[占位符]仅为说明格式结构，请根据实际剧情内容填充，不要直接复制示例内容
            
             任务评估要求：
            - 检查所有活跃任务状态：已完成、进度更新、已过期
            - QUESTS块与questUpdates必须完全同步
            - 每个任务ID只能出现一次，严禁重复

            
             任务更新格式：
            - questUpdates包含：created（新任务）、completed（完成任务）、progress（进度更新）、expired（过期任务）
            - rewards格式：{"exp": 数值, "items": ["物品名x数量"]}
            - items只包含本次获得的新物品，不重复已有物品

             世界状态更新：
            - worldStateUpdates包含：currentLocation、environment、npcs、worldEvents等


重要说明：
            - 任务奖励由后端自动处理，只需提供questUpdates数据
            - items字段只包含本次获得的新物品，不重复已有物品
            - 任务ID必须唯一，完成后从活跃列表移除
            
             其他更新字段：
            - arcUpdates：情节更新（currentArcName、currentArcStartRound）
            - convergenceStatusUpdates：收敛状态更新（progress、nearestScenarioId等）

             评估JSON要求：
            - 使用英文字段名：ruleCompliance、contextConsistency、convergenceProgress、overallScore、strategy等
            - strategy取值：ACCEPT、ADJUST、CORRECT
            - 用§包裹，放在回复最后
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
                    convergenceInfo.append("📖 故事收敛节点\n");
                    convergenceInfo.append("根据你的选择和行为，故事将向以下收敛点发展：\n");
                    convergenceInfo.append(parseConvergenceScenarios(convergenceScenarios));
                    convergenceInfo.append("\n\n");
                }

                // 添加收敛规则（从数据库获取）
                String convergenceRules = template.getConvergenceRules();
                if (convergenceRules != null && !convergenceRules.trim().isEmpty() && !convergenceRules.equals("{}")) {
                    convergenceInfo.append("⚖️ 收敛规则\n");
                    convergenceInfo.append(parseConvergenceRules(convergenceRules));
                    convergenceInfo.append("\n\n");
                }

                convergenceInfo.append("""
                     🎯 推进要求
                    - 持续进展：每次交互都要推进故事，避免重复或停滞
                    - 引入新元素：主动引入新角色、事件、地点或挑战
                    - 时间流动：让故事时间自然推进，创造紧迫感
                    - 目标明确：始终朝着明确的故事情节或结局推进
                    - 避免循环：不要在同一场景或情节中反复打转

                     ⏰ 强制场景切换规则（必须遵守）
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

             🎯 推进要求
            - 持续进展：每次交互都要推进故事，避免重复或停滞
            - 引入新元素：主动引入新角色、事件、地点或挑战
            - 时间流动：让故事时间自然推进，创造紧迫感
            - 目标明确：始终朝着明确的故事情节或结局推进
            - 避免循环：不要在同一场景或情节中反复打转

             ⏰ 强制场景切换规则（必须遵守）
            - 5轮限制：在同一个场景中最多进行5轮对话
            - 强制切换：第5轮后必须强制进行场景切换或任务更新
            - 切换方式：场景转换、任务更新、事件触发、时间跳跃
            - 不可违反：这是硬性规则，无论对话内容如何都必须执行

            享受自由探索的乐趣，同时感受故事的自然推进！
            """;
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
            
            // 直接发送原始事件数据JSON，而不是解析后的内容
            if (event.getEventData() != null && !event.getEventData().trim().isEmpty()) {
                // 验证JSON格式（但不使用解析结果，直接发送原始数据）
                try {
                    objectMapper.readTree(event.getEventData());
                    // JSON格式有效，直接发送原始数据
                    eventInfo.append("事件数据: ").append(event.getEventData());
                } catch (Exception jsonException) {
                    // JSON格式无效，仍然发送原始字符串
                    eventInfo.append("事件数据: ").append(event.getEventData());
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
    
    /**
     * 获取实际对话轮数（基于用户消息数量）
     */
    private int getActualConversationRounds(String sessionId) {
        try {
            // 获取会话信息
            com.qncontest.entity.ChatSession session = chatSessionService.getSessionById(sessionId);
            if (session == null) {
                logger.warn("会话不存在: sessionId={}", sessionId);
                return 0;
            }
            
            // 使用ChatSessionService获取用户消息数量
            // 这里我们直接使用totalRounds，因为它已经在saveUserMessage中正确计算
            Integer totalRounds = session.getTotalRounds();
            if (totalRounds == null) {
                return 0;
            }
            
            logger.debug("获取实际对话轮数: sessionId={}, totalRounds={}", sessionId, totalRounds);
            return totalRounds;
            
        } catch (Exception e) {
            logger.warn("获取实际对话轮数失败: sessionId={}", sessionId, e);
            return 0;
        }
    }
    
    /**
     * 构建对话历史
     */
    private String buildConversationHistory(String sessionId) {
        try {
            // 获取会话信息
            com.qncontest.entity.ChatSession session = chatSessionService.getSessionById(sessionId);
            if (session == null) {
                logger.warn("会话不存在: sessionId={}", sessionId);
                return "";
            }
            
            // 获取最近10轮对话历史
            List<com.qncontest.entity.ChatMessage> historyMessages = 
                chatSessionService.getSessionHistory(session, 10);
            
            if (historyMessages.isEmpty()) {
                return "";
            }
            
            StringBuilder history = new StringBuilder();
            for (com.qncontest.entity.ChatMessage msg : historyMessages) {
                if (msg.getRole() == com.qncontest.entity.ChatMessage.MessageRole.USER) {
                    history.append("用户: ").append(msg.getContent()).append("\n");
                } else {
                    history.append("助手: ").append(msg.getContent()).append("\n");
                }
            }
            
            return history.toString();
            
        } catch (Exception e) {
            logger.warn("构建对话历史失败: sessionId={}", sessionId, e);
            return "";
        }
    }
}
