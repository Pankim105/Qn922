package com.qncontest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 角色扮演聊天请求DTO
 */
public class RoleplayRequest {
    
    @NotBlank(message = "消息内容不能为空")
    private String message;
    
    @NotBlank(message = "会话ID不能为空")
    private String sessionId;
    
    @NotBlank(message = "世界类型不能为空")
    private String worldType;
    
    private String worldRules;
    private String godModeRules;
    private String worldState;
    private String skillsState;
    private List<ChatHistoryMessage> history;
    private String systemPrompt;
    
    // 技能相关
    private DiceRollRequest diceRoll;
    private QuestActionRequest questAction;
    private LearningChallengeRequest learningChallenge;
    
    public static class ChatHistoryMessage {
        private String role;
        private String content;
        
        // Constructors
        public ChatHistoryMessage() {}
        
        public ChatHistoryMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
        
        // Getters and Setters
        public String getRole() {
            return role;
        }
        
        public void setRole(String role) {
            this.role = role;
        }
        
        public String getContent() {
            return content;
        }
        
        public void setContent(String content) {
            this.content = content;
        }
    }
    
    public static class DiceRollRequest {
        @NotNull
        private Integer diceType;
        private Integer modifier = 0;
        private String context;
        private Integer difficultyClass;
        
        // Constructors
        public DiceRollRequest() {}
        
        public DiceRollRequest(Integer diceType, Integer modifier, String context) {
            this.diceType = diceType;
            this.modifier = modifier;
            this.context = context;
        }
        
        // Getters and Setters
        public Integer getDiceType() {
            return diceType;
        }
        
        public void setDiceType(Integer diceType) {
            this.diceType = diceType;
        }
        
        public Integer getModifier() {
            return modifier;
        }
        
        public void setModifier(Integer modifier) {
            this.modifier = modifier;
        }
        
        public String getContext() {
            return context;
        }
        
        public void setContext(String context) {
            this.context = context;
        }
        
        public Integer getDifficultyClass() {
            return difficultyClass;
        }
        
        public void setDifficultyClass(Integer difficultyClass) {
            this.difficultyClass = difficultyClass;
        }
    }
    
    public static class QuestActionRequest {
        private String action;
        private String questId;
        private String questTitle;
        private String questDescription;
        private String questStatus;
        
        // Getters and Setters
        public String getAction() {
            return action;
        }
        
        public void setAction(String action) {
            this.action = action;
        }
        
        public String getQuestId() {
            return questId;
        }
        
        public void setQuestId(String questId) {
            this.questId = questId;
        }
        
        public String getQuestTitle() {
            return questTitle;
        }
        
        public void setQuestTitle(String questTitle) {
            this.questTitle = questTitle;
        }
        
        public String getQuestDescription() {
            return questDescription;
        }
        
        public void setQuestDescription(String questDescription) {
            this.questDescription = questDescription;
        }
        
        public String getQuestStatus() {
            return questStatus;
        }
        
        public void setQuestStatus(String questStatus) {
            this.questStatus = questStatus;
        }
    }
    
    public static class LearningChallengeRequest {
        private String subject;
        private Integer level;
        private String question;
        private String userAnswer;
        private String challengeType;
        
        // Getters and Setters
        public String getSubject() {
            return subject;
        }
        
        public void setSubject(String subject) {
            this.subject = subject;
        }
        
        public Integer getLevel() {
            return level;
        }
        
        public void setLevel(Integer level) {
            this.level = level;
        }
        
        public String getQuestion() {
            return question;
        }
        
        public void setQuestion(String question) {
            this.question = question;
        }
        
        public String getUserAnswer() {
            return userAnswer;
        }
        
        public void setUserAnswer(String userAnswer) {
            this.userAnswer = userAnswer;
        }
        
        public String getChallengeType() {
            return challengeType;
        }
        
        public void setChallengeType(String challengeType) {
            this.challengeType = challengeType;
        }
    }
    
    // Main class Getters and Setters
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getWorldType() {
        return worldType;
    }
    
    public void setWorldType(String worldType) {
        this.worldType = worldType;
    }
    
    public String getWorldRules() {
        return worldRules;
    }
    
    public void setWorldRules(String worldRules) {
        this.worldRules = worldRules;
    }
    
    public String getGodModeRules() {
        return godModeRules;
    }
    
    public void setGodModeRules(String godModeRules) {
        this.godModeRules = godModeRules;
    }
    
    public String getWorldState() {
        return worldState;
    }
    
    public void setWorldState(String worldState) {
        this.worldState = worldState;
    }
    
    public String getSkillsState() {
        return skillsState;
    }
    
    public void setSkillsState(String skillsState) {
        this.skillsState = skillsState;
    }
    
    public List<ChatHistoryMessage> getHistory() {
        return history;
    }
    
    public void setHistory(List<ChatHistoryMessage> history) {
        this.history = history;
    }
    
    public String getSystemPrompt() {
        return systemPrompt;
    }
    
    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }
    
    public DiceRollRequest getDiceRoll() {
        return diceRoll;
    }
    
    public void setDiceRoll(DiceRollRequest diceRoll) {
        this.diceRoll = diceRoll;
    }
    
    public QuestActionRequest getQuestAction() {
        return questAction;
    }
    
    public void setQuestAction(QuestActionRequest questAction) {
        this.questAction = questAction;
    }
    
    public LearningChallengeRequest getLearningChallenge() {
        return learningChallenge;
    }
    
    public void setLearningChallenge(LearningChallengeRequest learningChallenge) {
        this.learningChallenge = learningChallenge;
    }
}



