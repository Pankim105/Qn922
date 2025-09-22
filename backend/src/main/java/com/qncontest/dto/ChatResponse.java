package com.qncontest.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ChatResponse {
    
    private boolean success;
    private String message;
    private Object data;
    
    // 构造函数
    public ChatResponse() {}
    
    public ChatResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    public ChatResponse(boolean success, String message, Object data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }
    
    // 静态工厂方法
    public static ChatResponse success(String message) {
        return new ChatResponse(true, message);
    }
    
    public static ChatResponse success(String message, Object data) {
        return new ChatResponse(true, message, data);
    }
    
    public static ChatResponse error(String message) {
        return new ChatResponse(false, message);
    }
    
    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public Object getData() {
        return data;
    }
    
    public void setData(Object data) {
        this.data = data;
    }
    
    // 内部类用于会话列表响应
    public static class SessionListData {
        private List<SessionInfo> sessions;
        
        public SessionListData(List<SessionInfo> sessions) {
            this.sessions = sessions;
        }
        
        public List<SessionInfo> getSessions() {
            return sessions;
        }
        
        public void setSessions(List<SessionInfo> sessions) {
            this.sessions = sessions;
        }
    }
    
    // 内部类用于会话信息
    public static class SessionInfo {
        private String sessionId;
        private String title;
        private String createdAt;
        private String updatedAt;
        private int messageCount;
        
        public SessionInfo() {}
        
        public SessionInfo(String sessionId, String title, LocalDateTime createdAt, 
                          LocalDateTime updatedAt, int messageCount) {
            this.sessionId = sessionId;
            this.title = title;
            this.createdAt = createdAt.toString();
            this.updatedAt = updatedAt.toString();
            this.messageCount = messageCount;
        }
        
        // Getters and Setters
        public String getSessionId() {
            return sessionId;
        }
        
        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }
        
        public String getTitle() {
            return title;
        }
        
        public void setTitle(String title) {
            this.title = title;
        }
        
        public String getCreatedAt() {
            return createdAt;
        }
        
        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }
        
        public String getUpdatedAt() {
            return updatedAt;
        }
        
        public void setUpdatedAt(String updatedAt) {
            this.updatedAt = updatedAt;
        }
        
        public int getMessageCount() {
            return messageCount;
        }
        
        public void setMessageCount(int messageCount) {
            this.messageCount = messageCount;
        }
    }
    
    // 内部类用于消息列表响应
    public static class MessageListData {
        private List<MessageInfo> messages;
        
        public MessageListData(List<MessageInfo> messages) {
            this.messages = messages;
        }
        
        public List<MessageInfo> getMessages() {
            return messages;
        }
        
        public void setMessages(List<MessageInfo> messages) {
            this.messages = messages;
        }
    }
    
    // 内部类用于消息信息
    public static class MessageInfo {
        private Long id;
        private String role;
        private String content;
        private Integer sequenceNumber;
        private String createdAt;
        
        public MessageInfo() {}
        
        public MessageInfo(Long id, String role, String content, Integer sequenceNumber, LocalDateTime createdAt) {
            this.id = id;
            this.role = role;
            this.content = content;
            this.sequenceNumber = sequenceNumber;
            this.createdAt = createdAt.toString();
        }
        
        // Getters and Setters
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
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
        
        public Integer getSequenceNumber() {
            return sequenceNumber;
        }
        
        public void setSequenceNumber(Integer sequenceNumber) {
            this.sequenceNumber = sequenceNumber;
        }
        
        public String getCreatedAt() {
            return createdAt;
        }
        
        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }
    }
}
