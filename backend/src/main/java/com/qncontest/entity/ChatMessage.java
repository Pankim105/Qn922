package com.qncontest.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 聊天消息实体
 */
@Entity
@Table(name = "chat_messages")
public class ChatMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", referencedColumnName = "id")
    private ChatSession chatSession;
    
    @Column(name = "role", nullable = false)
    private String role; // "user", "assistant", "system"
    
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;
    
    @Column(name = "tokens")
    private Integer tokens; // 消息的token数量
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "sequence_number")
    private Integer sequenceNumber; // 消息在会话中的序号
    
    // 构造函数
    public ChatMessage() {}
    
    public ChatMessage(ChatSession chatSession, String role, String content) {
        this.chatSession = chatSession;
        this.role = role;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public ChatSession getChatSession() { return chatSession; }
    public void setChatSession(ChatSession chatSession) { this.chatSession = chatSession; }
    
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public Integer getTokens() { return tokens; }
    public void setTokens(Integer tokens) { this.tokens = tokens; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public Integer getSequenceNumber() { return sequenceNumber; }
    public void setSequenceNumber(Integer sequenceNumber) { this.sequenceNumber = sequenceNumber; }
}
