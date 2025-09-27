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
    
    // 语音输入相关字段
    private String inputType; // 'text' 或 'voice'
    private String originalVoiceData; // 原始语音数据（可选）
    
    
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
    
    public String getInputType() {
        return inputType;
    }
    
    public void setInputType(String inputType) {
        this.inputType = inputType;
    }
    
    public String getOriginalVoiceData() {
        return originalVoiceData;
    }
    
    public void setOriginalVoiceData(String originalVoiceData) {
        this.originalVoiceData = originalVoiceData;
    }
    
}



