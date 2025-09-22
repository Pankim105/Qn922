package com.qncontest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public class ChatRequest {
    
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 4000, message = "消息内容不能超过4000个字符")
    private String message;
    
    private String sessionId;
    
    private List<ChatHistoryMessage> history;
    
    @Size(max = 1000, message = "系统提示不能超过1000个字符")
    private String systemPrompt;
    
    // 构造函数
    public ChatRequest() {}
    
    public ChatRequest(String message, String sessionId) {
        this.message = message;
        this.sessionId = sessionId;
    }
    
    // Getters and Setters
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
    
    // 内部类用于表示历史消息
    public static class ChatHistoryMessage {
        private String role; // "user" 或 "assistant"
        private String content;
        
        public ChatHistoryMessage() {}
        
        public ChatHistoryMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
        
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
}
