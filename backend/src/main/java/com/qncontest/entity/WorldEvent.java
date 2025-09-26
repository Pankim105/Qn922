package com.qncontest.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 世界事件实体 - 事件溯源，记录所有世界状态变更事件
 */
@Entity
@Table(name = "world_events")
public class WorldEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "session_id", nullable = false)
    private String sessionId;
    
    @Column(name = "event_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private EventType eventType;
    
    @Column(name = "event_data", columnDefinition = "JSON", nullable = false)
    private String eventData;
    
    @Column(nullable = false)
    private Integer sequence;
    
    @Column(nullable = false, length = 32)
    private String checksum;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    public enum EventType {
        USER_ACTION,      // 用户行动
        AI_RESPONSE,      // AI回复
        DICE_ROLL,        // 骰子检定
        QUEST_UPDATE,     // 任务更新
        STATE_CHANGE,     // 状态变更
        SKILL_USE,        // 技能使用
        LOCATION_CHANGE,  // 地点变更
        CHARACTER_UPDATE, // 角色更新
        SYSTEM_EVENT      // 系统事件
    }
    
    // 构造函数
    public WorldEvent() {
        this.timestamp = LocalDateTime.now();
    }
    
    public WorldEvent(String sessionId, EventType eventType, String eventData, Integer sequence) {
        this();
        this.sessionId = sessionId;
        this.eventType = eventType;
        this.eventData = eventData;
        this.sequence = sequence;
    }

    // 会话轮次与剧情情节（静态快照）
    @Column(name = "total_rounds")
    private Integer totalRounds;              // 记录事件发生时的会话总轮数

    @Column(name = "current_arc_start_round")
    private Integer currentArcStartRound;     // 记录事件发生时的当前情节起始轮数

    @Column(name = "current_arc_name")
    private String currentArcName;            // 记录事件发生时的当前情节名称
    
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
    
    public EventType getEventType() {
        return eventType;
    }
    
    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }
    
    public String getEventData() {
        return eventData;
    }
    
    public void setEventData(String eventData) {
        this.eventData = eventData;
    }
    
    public Integer getSequence() {
        return sequence;
    }
    
    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }
    
    public String getChecksum() {
        return checksum;
    }
    
    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getTotalRounds() {
        return totalRounds;
    }

    public void setTotalRounds(Integer totalRounds) {
        this.totalRounds = totalRounds;
    }

    public Integer getCurrentArcStartRound() {
        return currentArcStartRound;
    }

    public void setCurrentArcStartRound(Integer currentArcStartRound) {
        this.currentArcStartRound = currentArcStartRound;
    }

    public String getCurrentArcName() {
        return currentArcName;
    }

    public void setCurrentArcName(String currentArcName) {
        this.currentArcName = currentArcName;
    }
}
