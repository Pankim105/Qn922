package com.qncontest.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 稳定性锚点实体 - 确保世界设定的一致性
 */
@Entity
@Table(name = "stability_anchors")
public class StabilityAnchor {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "session_id", nullable = false)
    private String sessionId;
    
    @Column(name = "anchor_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private AnchorType anchorType;
    
    @Column(name = "anchor_key", nullable = false)
    private String anchorKey;
    
    @Column(name = "anchor_value", columnDefinition = "TEXT", nullable = false)
    private String anchorValue;
    
    @Column(nullable = false)
    private Integer priority = 1;
    
    @Column(name = "is_immutable", nullable = false)
    private Boolean isImmutable = false;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    public enum AnchorType {
        WORLD_RULE,    // 世界规则
        CHARACTER,     // 角色相关
        LOCATION,      // 地点相关
        QUEST,         // 任务相关
        FACTION,       // 势力相关
        ITEM,          // 物品相关
        EVENT          // 事件相关
    }
    
    // 构造函数
    public StabilityAnchor() {
        this.createdAt = LocalDateTime.now();
    }
    
    public StabilityAnchor(String sessionId, AnchorType anchorType, String anchorKey, String anchorValue) {
        this();
        this.sessionId = sessionId;
        this.anchorType = anchorType;
        this.anchorKey = anchorKey;
        this.anchorValue = anchorValue;
    }
    
    public StabilityAnchor(String sessionId, AnchorType anchorType, String anchorKey, 
                          String anchorValue, Integer priority, Boolean isImmutable) {
        this(sessionId, anchorType, anchorKey, anchorValue);
        this.priority = priority;
        this.isImmutable = isImmutable;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public AnchorType getAnchorType() {
        return anchorType;
    }
    
    public void setAnchorType(AnchorType anchorType) {
        this.anchorType = anchorType;
    }
    
    public String getAnchorKey() {
        return anchorKey;
    }
    
    public void setAnchorKey(String anchorKey) {
        this.anchorKey = anchorKey;
    }
    
    public String getAnchorValue() {
        return anchorValue;
    }
    
    public void setAnchorValue(String anchorValue) {
        this.anchorValue = anchorValue;
    }
    
    public Integer getPriority() {
        return priority;
    }
    
    public void setPriority(Integer priority) {
        this.priority = priority;
    }
    
    public Boolean getIsImmutable() {
        return isImmutable;
    }
    
    public void setIsImmutable(Boolean isImmutable) {
        this.isImmutable = isImmutable;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}



