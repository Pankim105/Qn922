package com.qncontest.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 世界状态实体 - 存储世界状态的历史快照
 */
@Entity
@Table(name = "world_states")
public class WorldState {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "session_id", nullable = false)
    private String sessionId;
    
    @Column(nullable = false)
    private Integer version;
    
    @Column(name = "current_location", columnDefinition = "JSON")
    private String currentLocation;
    
    @Column(columnDefinition = "JSON")
    private String characters;
    
    @Column(columnDefinition = "JSON")
    private String factions;
    
    @Column(columnDefinition = "JSON")
    private String inventory;
    
    @Column(name = "active_quests", columnDefinition = "JSON")
    private String activeQuests;
    
    @Column(name = "completed_quests", columnDefinition = "JSON")
    private String completedQuests;
    
    @Column(name = "event_history", columnDefinition = "JSON")
    private String eventHistory;
    
    @Column(nullable = false, length = 32)
    private String checksum;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    // 构造函数
    public WorldState() {
        this.createdAt = LocalDateTime.now();
    }
    
    public WorldState(String sessionId, Integer version) {
        this();
        this.sessionId = sessionId;
        this.version = version;
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
    
    public Integer getVersion() {
        return version;
    }
    
    public void setVersion(Integer version) {
        this.version = version;
    }
    
    public String getCurrentLocation() {
        return currentLocation;
    }
    
    public void setCurrentLocation(String currentLocation) {
        this.currentLocation = currentLocation;
    }
    
    public String getCharacters() {
        return characters;
    }
    
    public void setCharacters(String characters) {
        this.characters = characters;
    }
    
    public String getFactions() {
        return factions;
    }
    
    public void setFactions(String factions) {
        this.factions = factions;
    }
    
    public String getInventory() {
        return inventory;
    }
    
    public void setInventory(String inventory) {
        this.inventory = inventory;
    }
    
    public String getActiveQuests() {
        return activeQuests;
    }
    
    public void setActiveQuests(String activeQuests) {
        this.activeQuests = activeQuests;
    }
    
    public String getCompletedQuests() {
        return completedQuests;
    }
    
    public void setCompletedQuests(String completedQuests) {
        this.completedQuests = completedQuests;
    }
    
    public String getEventHistory() {
        return eventHistory;
    }
    
    public void setEventHistory(String eventHistory) {
        this.eventHistory = eventHistory;
    }
    
    public String getChecksum() {
        return checksum;
    }
    
    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}



