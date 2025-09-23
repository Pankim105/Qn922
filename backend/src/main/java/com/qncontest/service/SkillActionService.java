package com.qncontest.service;

import com.qncontest.entity.DiceRoll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 技能动作解析和执行服务
 * 负责解析AI回复中的技能指令并执行相应的游戏机制
 */
@Service
public class SkillActionService {
    
    private static final Logger logger = LoggerFactory.getLogger(SkillActionService.class);
    
    @Autowired
    private RoleplayWorldService roleplayWorldService;
    
    @Autowired
    private RoleplayMemoryService memoryService;
    
    /**
     * 技能动作基类
     */
    public abstract static class SkillAction {
        protected String type;
        protected String sessionId;
        
        public SkillAction(String type, String sessionId) {
            this.type = type;
            this.sessionId = sessionId;
        }
        
        public String getType() { return type; }
        public String getSessionId() { return sessionId; }
        
        public abstract String execute(SkillActionService service);
    }
    
    /**
     * 骰子动作
     */
    public static class DiceAction extends SkillAction {
        private String diceExpression;
        private String context;
        
        public DiceAction(String sessionId, String diceExpression, String context) {
            super("DICE", sessionId);
            this.diceExpression = diceExpression;
            this.context = context;
        }
        
        @Override
        public String execute(SkillActionService service) {
            return service.executeDiceRoll(this);
        }
        
        public String getDiceExpression() { return diceExpression; }
        public String getContext() { return context; }
    }
    
    /**
     * 任务动作
     */
    public static class QuestAction extends SkillAction {
        private String action; // CREATE, UPDATE, COMPLETE
        private String questId;
        private String questTitle;
        private String questDescription;
        
        public QuestAction(String sessionId, String action, String questId, String questTitle, String questDescription) {
            super("QUEST", sessionId);
            this.action = action;
            this.questId = questId;
            this.questTitle = questTitle;
            this.questDescription = questDescription;
        }
        
        @Override
        public String execute(SkillActionService service) {
            return service.executeQuestAction(this);
        }
        
        public String getAction() { return action; }
        public String getQuestId() { return questId; }
        public String getQuestTitle() { return questTitle; }
        public String getQuestDescription() { return questDescription; }
    }
    
    /**
     * 学习挑战动作
     */
    public static class ChallengeAction extends SkillAction {
        private String subject;
        private String difficulty;
        private String content;
        
        public ChallengeAction(String sessionId, String subject, String difficulty, String content) {
            super("CHALLENGE", sessionId);
            this.subject = subject;
            this.difficulty = difficulty;
            this.content = content;
        }
        
        @Override
        public String execute(SkillActionService service) {
            return service.executeLearningChallenge(this);
        }
        
        public String getSubject() { return subject; }
        public String getDifficulty() { return difficulty; }
        public String getContent() { return content; }
    }
    
    /**
     * 状态更新动作
     */
    public static class StateAction extends SkillAction {
        private String stateType;
        private String stateValue;
        
        public StateAction(String sessionId, String stateType, String stateValue) {
            super("STATE", sessionId);
            this.stateType = stateType;
            this.stateValue = stateValue;
        }
        
        @Override
        public String execute(SkillActionService service) {
            return service.executeStateUpdate(this);
        }
        
        public String getStateType() { return stateType; }
        public String getStateValue() { return stateValue; }
    }
    
    /**
     * 记忆动作
     */
    public static class MemoryAction extends SkillAction {
        private String memoryType;
        private String content;
        
        public MemoryAction(String sessionId, String memoryType, String content) {
            super("MEMORY", sessionId);
            this.memoryType = memoryType;
            this.content = content;
        }
        
        @Override
        public String execute(SkillActionService service) {
            return service.executeMemoryAction(this);
        }
        
        public String getMemoryType() { return memoryType; }
        public String getContent() { return content; }
    }
    
    /**
     * 解析AI回复中的技能指令
     */
    public List<SkillAction> parseSkillActions(String aiResponse, String sessionId) {
        List<SkillAction> actions = new ArrayList<>();
        
        // 骰子指令：[DICE:d20+5:攻击检定]
        Pattern dicePattern = Pattern.compile("\\[DICE:(d\\d+(?:[+\\-]\\d+)?):([^\\]]+)\\]");
        Matcher diceMatcher = dicePattern.matcher(aiResponse);
        while (diceMatcher.find()) {
            actions.add(new DiceAction(sessionId, diceMatcher.group(1), diceMatcher.group(2)));
        }
        
        // 任务指令：[QUEST:CREATE:标题:描述] 或 [QUEST:UPDATE:ID:描述] 或 [QUEST:COMPLETE:ID]
        Pattern questPattern = Pattern.compile("\\[QUEST:(CREATE|UPDATE|COMPLETE):([^:]+)(?::([^\\]]+))?\\]");
        Matcher questMatcher = questPattern.matcher(aiResponse);
        while (questMatcher.find()) {
            String action = questMatcher.group(1);
            String param1 = questMatcher.group(2);
            String param2 = questMatcher.group(3);
            
            if ("CREATE".equals(action)) {
                actions.add(new QuestAction(sessionId, action, null, param1, param2));
            } else {
                actions.add(new QuestAction(sessionId, action, param1, null, param2));
            }
        }
        
        // 学习挑战：[CHALLENGE:MATH:3:计算2+2等于多少？]
        Pattern challengePattern = Pattern.compile("\\[CHALLENGE:(MATH|HISTORY|LANGUAGE):([^:]+):([^\\]]+)\\]");
        Matcher challengeMatcher = challengePattern.matcher(aiResponse);
        while (challengeMatcher.find()) {
            actions.add(new ChallengeAction(sessionId, challengeMatcher.group(1), challengeMatcher.group(2), challengeMatcher.group(3)));
        }
        
        // 状态更新：[STATE:LOCATION:新手村] 或 [STATE:INVENTORY:获得铁剑]
        Pattern statePattern = Pattern.compile("\\[STATE:(LOCATION|INVENTORY|RELATIONSHIP|EMOTION):([^\\]]+)\\]");
        Matcher stateMatcher = statePattern.matcher(aiResponse);
        while (stateMatcher.find()) {
            actions.add(new StateAction(sessionId, stateMatcher.group(1), stateMatcher.group(2)));
        }
        
        // 记忆指令：[MEMORY:EVENT:发现了古老的宝箱]
        Pattern memoryPattern = Pattern.compile("\\[MEMORY:(EVENT|CHARACTER|WORLD):([^\\]]+)\\]");
        Matcher memoryMatcher = memoryPattern.matcher(aiResponse);
        while (memoryMatcher.find()) {
            actions.add(new MemoryAction(sessionId, memoryMatcher.group(1), memoryMatcher.group(2)));
        }
        
        if (!actions.isEmpty()) {
            logger.info("解析到技能指令: sessionId={}, 指令数={}", sessionId, actions.size());
        }
        
        return actions;
    }
    
    /**
     * 执行技能动作并生成结果描述
     */
    public String executeSkillActions(List<SkillAction> actions) {
        if (actions.isEmpty()) {
            return "";
        }
        
        StringBuilder results = new StringBuilder();
        results.append("\n\n---\n**🎯 系统执行结果**\n");
        
        for (SkillAction action : actions) {
            try {
                String result = action.execute(this);
                if (!result.isEmpty()) {
                    results.append("• ").append(result).append("\n");
                }
            } catch (Exception e) {
                logger.error("执行技能动作失败: type={}, sessionId={}", action.getType(), action.getSessionId(), e);
                results.append("• ❌ ").append(action.getType()).append("执行失败\n");
            }
        }
        
        results.append("---\n");
        return results.toString();
    }
    
    /**
     * 执行骰子检定
     */
    public String executeDiceRoll(DiceAction action) {
        try {
            // 解析骰子表达式，例如 "d20+5"
            String[] parts = action.getDiceExpression().split("[+\\-]");
            String dicePart = parts[0]; // "d20"
            int modifier = 0;
            
            if (parts.length > 1) {
                String modifierStr = action.getDiceExpression().substring(dicePart.length());
                modifier = Integer.parseInt(modifierStr);
            }
            
            int diceType = Integer.parseInt(dicePart.substring(1)); // 去掉'd'
            
            DiceRoll result = roleplayWorldService.rollDice(
                action.getSessionId(),
                diceType,
                modifier,
                action.getContext(),
                15 // 默认难度
            );
            
            String successText = result.getIsSuccessful() ? "成功" : "失败";
            return String.format("🎲 %s: %d(骰值) + %d(修正) = %d (%s)", 
                               action.getContext(), result.getResult(), result.getModifier(), 
                               result.getFinalResult(), successText);
                               
        } catch (Exception e) {
            logger.error("骰子检定执行失败", e);
            return "🎲 骰子检定执行失败";
        }
    }
    
    /**
     * 执行任务动作
     */
    public String executeQuestAction(QuestAction action) {
        switch (action.getAction()) {
            case "CREATE":
                // 这里可以调用任务系统创建新任务
                memoryService.storeMemory(action.getSessionId(), 
                                        "创建任务: " + action.getQuestTitle(), 
                                        "QUEST", 0.8);
                return String.format("📋 创建任务: %s", action.getQuestTitle());
                
            case "UPDATE":
                memoryService.storeMemory(action.getSessionId(), 
                                        "更新任务: " + action.getQuestId() + " - " + action.getQuestDescription(), 
                                        "QUEST", 0.7);
                return String.format("📋 更新任务: %s", action.getQuestId());
                
            case "COMPLETE":
                memoryService.storeMemory(action.getSessionId(), 
                                        "完成任务: " + action.getQuestId(), 
                                        "QUEST", 0.9);
                return String.format("🏆 完成任务: %s", action.getQuestId());
                
            default:
                return "📋 未知任务操作";
        }
    }
    
    /**
     * 执行学习挑战
     */
    public String executeLearningChallenge(ChallengeAction action) {
        // 记录学习挑战
        memoryService.storeMemory(action.getSessionId(), 
                                String.format("学习挑战[%s]: %s", action.getSubject(), action.getContent()), 
                                "CHALLENGE", 0.8);
        
        return String.format("🎯 %s挑战(难度%s): %s", 
                           action.getSubject(), action.getDifficulty(), action.getContent());
    }
    
    /**
     * 执行状态更新
     */
    public String executeStateUpdate(StateAction action) {
        String stateDescription = switch (action.getStateType()) {
            case "LOCATION" -> {
                memoryService.recordWorldStateChange(action.getSessionId(), 
                                                   "位置变更: " + action.getStateValue(), 
                                                   "玩家移动");
                yield "📍 位置更新: " + action.getStateValue();
            }
            case "INVENTORY" -> {
                memoryService.recordWorldStateChange(action.getSessionId(), 
                                                   "物品变更: " + action.getStateValue(), 
                                                   "物品获得/失去");
                yield "🎒 物品变更: " + action.getStateValue();
            }
            case "RELATIONSHIP" -> {
                memoryService.updateCharacterRelationship(action.getSessionId(), 
                                                        "角色", action.getStateValue());
                yield "👥 关系变化: " + action.getStateValue();
            }
            case "EMOTION" -> {
                memoryService.storeMemory(action.getSessionId(), 
                                        "情绪变化: " + action.getStateValue(), 
                                        "EMOTION", 0.6);
                yield "😊 情绪状态: " + action.getStateValue();
            }
            default -> "❓ 未知状态更新: " + action.getStateValue();
        };
        
        return stateDescription;
    }
    
    /**
     * 执行记忆动作
     */
    public String executeMemoryAction(MemoryAction action) {
        double importance = switch (action.getMemoryType()) {
            case "EVENT" -> 0.8;
            case "CHARACTER" -> 0.7;
            case "WORLD" -> 0.6;
            default -> 0.5;
        };
        
        memoryService.storeMemory(action.getSessionId(), action.getContent(), 
                                action.getMemoryType(), importance);
        
        return String.format("🧠 记录%s记忆: %s", action.getMemoryType(), action.getContent());
    }
    
    /**
     * 清理AI回复中的技能指令（移除已处理的指令）
     */
    public String cleanupSkillInstructions(String aiResponse) {
        String cleaned = aiResponse;
        
        // 移除所有技能指令
        cleaned = cleaned.replaceAll("\\[DICE:[^\\]]+\\]", "");
        cleaned = cleaned.replaceAll("\\[QUEST:[^\\]]+\\]", "");
        cleaned = cleaned.replaceAll("\\[CHALLENGE:[^\\]]+\\]", "");
        cleaned = cleaned.replaceAll("\\[STATE:[^\\]]+\\]", "");
        cleaned = cleaned.replaceAll("\\[MEMORY:[^\\]]+\\]", "");
        
        // 清理多余的空行
        cleaned = cleaned.replaceAll("\\n{3,}", "\n\n");
        cleaned = cleaned.trim();
        
        return cleaned;
    }
}
