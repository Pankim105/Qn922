package com.qncontest.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false, referencedColumnName = "sessionId")
    private ChatSession chatSession;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MessageRole role;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    public enum MessageRole {
        USER, ASSISTANT
    }
    
    // 构造函数
    public ChatMessage() {
        this.createdAt = LocalDateTime.now();
    }
    
    public ChatMessage(ChatSession chatSession, MessageRole role, String content, Integer sequenceNumber) {
        this();
        this.chatSession = chatSession;
        this.role = role;
        this.content = content;
        this.sequenceNumber = sequenceNumber;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public ChatSession getChatSession() {
        return chatSession;
    }
    
    public void setChatSession(ChatSession chatSession) {
        this.chatSession = chatSession;
    }
    
    public MessageRole getRole() {
        return role;
    }
    
    public void setRole(MessageRole role) {
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
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
