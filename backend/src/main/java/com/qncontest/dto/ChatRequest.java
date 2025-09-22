package com.qncontest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public class ChatRequest {
    
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "消息长度不能超过2000个字符")
    private String message;
    
    private String systemPrompt;
    
    private String sessionId; // 会话ID，用于识别不同的对话会话
    
    private List<ChatMessage> history; // 对话历史
    
    // Constructors
    public ChatRequest() {}
    
    public ChatRequest(String message) {
        this.message = message;
    }
    
    public ChatRequest(String message, String systemPrompt) {
        this.message = message;
        this.systemPrompt = systemPrompt;
    }
    
    // Getters and Setters
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getSystemPrompt() {
        return systemPrompt;
    }
    
    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public List<ChatMessage> getHistory() {
        return history;
    }
    
    public void setHistory(List<ChatMessage> history) {
        this.history = history;
    }
    
    // 内部类：表示对话消息
    public static class ChatMessage {
        private String role; // "user" 或 "assistant"
        private String content;
        
        public ChatMessage() {}
        
        public ChatMessage(String role, String content) {
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
