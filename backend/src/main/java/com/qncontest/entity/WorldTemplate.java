package com.qncontest.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 世界模板实体 - 存储5个预定义世界的规则和模板
 */
@Entity
@Table(name = "world_templates")
public class WorldTemplate {
    
    @Id
    @Column(name = "world_id")
    private String worldId;
    
    @Column(name = "world_name", nullable = false)
    private String worldName;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "default_rules", columnDefinition = "JSON", nullable = false)
    private String defaultRules;
    
    @Column(name = "system_prompt_template", columnDefinition = "TEXT", nullable = false)
    private String systemPromptTemplate;
    
    @Column(name = "stability_anchors", columnDefinition = "JSON")
    private String stabilityAnchors;
    
    @Column(name = "quest_templates", columnDefinition = "JSON")
    private String questTemplates;
    
    @Column(name = "character_templates", columnDefinition = "JSON")
    private String characterTemplates;
    
    @Column(name = "location_templates", columnDefinition = "JSON")
    private String locationTemplates;
    
    // 新增缺失的字段
    @Column(name = "convergence_scenarios", columnDefinition = "JSON")
    private String convergenceScenarios;
    
    @Column(name = "dm_instructions", columnDefinition = "TEXT")
    private String dmInstructions;
    
    @Column(name = "convergence_rules", columnDefinition = "JSON")
    private String convergenceRules;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // 构造函数
    public WorldTemplate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public WorldTemplate(String worldId, String worldName, String description) {
        this();
        this.worldId = worldId;
        this.worldName = worldName;
        this.description = description;
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getWorldId() {
        return worldId;
    }
    
    public void setWorldId(String worldId) {
        this.worldId = worldId;
    }
    
    public String getWorldName() {
        return worldName;
    }
    
    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getDefaultRules() {
        return defaultRules;
    }
    
    public void setDefaultRules(String defaultRules) {
        this.defaultRules = defaultRules;
    }
    
    public String getSystemPromptTemplate() {
        return systemPromptTemplate;
    }
    
    public void setSystemPromptTemplate(String systemPromptTemplate) {
        this.systemPromptTemplate = systemPromptTemplate;
    }
    
    public String getStabilityAnchors() {
        return stabilityAnchors;
    }
    
    public void setStabilityAnchors(String stabilityAnchors) {
        this.stabilityAnchors = stabilityAnchors;
    }
    
    public String getQuestTemplates() {
        return questTemplates;
    }
    
    public void setQuestTemplates(String questTemplates) {
        this.questTemplates = questTemplates;
    }
    
    public String getCharacterTemplates() {
        return characterTemplates;
    }
    
    public void setCharacterTemplates(String characterTemplates) {
        this.characterTemplates = characterTemplates;
    }
    
    public String getLocationTemplates() {
        return locationTemplates;
    }
    
    public void setLocationTemplates(String locationTemplates) {
        this.locationTemplates = locationTemplates;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    // 新增字段的 Getters and Setters
    public String getConvergenceScenarios() {
        return convergenceScenarios;
    }
    
    public void setConvergenceScenarios(String convergenceScenarios) {
        this.convergenceScenarios = convergenceScenarios;
    }
    
    public String getDmInstructions() {
        return dmInstructions;
    }
    
    public void setDmInstructions(String dmInstructions) {
        this.dmInstructions = dmInstructions;
    }
    
    public String getConvergenceRules() {
        return convergenceRules;
    }
    
    public void setConvergenceRules(String convergenceRules) {
        this.convergenceRules = convergenceRules;
    }
}



