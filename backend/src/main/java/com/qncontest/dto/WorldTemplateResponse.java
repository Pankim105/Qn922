package com.qncontest.dto;

import com.qncontest.entity.WorldTemplate;
import java.time.LocalDateTime;

/**
 * 世界模板响应DTO
 */
public class WorldTemplateResponse {
    
    private String worldId;
    private String worldName;
    private String description;
    private String defaultRules;
    private String systemPromptTemplate;
    private String stabilityAnchors;
    private String questTemplates;
    private String characterTemplates;
    private String locationTemplates;
    private String convergenceScenarios;
    private String dmInstructions;
    private String convergenceRules;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 构造函数
    public WorldTemplateResponse() {}
    
    public WorldTemplateResponse(WorldTemplate template) {
        this.worldId = template.getWorldId();
        this.worldName = template.getWorldName();
        this.description = template.getDescription();
        this.defaultRules = template.getDefaultRules();
        this.systemPromptTemplate = template.getSystemPromptTemplate();
        this.stabilityAnchors = template.getStabilityAnchors();
        this.questTemplates = template.getQuestTemplates();
        this.characterTemplates = template.getCharacterTemplates();
        this.locationTemplates = template.getLocationTemplates();
        this.convergenceScenarios = template.getConvergenceScenarios();
        this.dmInstructions = template.getDmInstructions();
        this.convergenceRules = template.getConvergenceRules();
        this.createdAt = template.getCreatedAt();
        this.updatedAt = template.getUpdatedAt();
    }
    
    /**
     * 简化版构造函数，只包含基本信息（用于列表显示）
     */
    public static WorldTemplateResponse simple(WorldTemplate template) {
        WorldTemplateResponse response = new WorldTemplateResponse();
        response.worldId = template.getWorldId();
        response.worldName = template.getWorldName();
        response.description = template.getDescription();
        return response;
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



