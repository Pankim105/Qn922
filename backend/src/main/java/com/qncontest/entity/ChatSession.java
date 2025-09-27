package com.qncontest.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chat_sessions")
public class ChatSession {
    
    @Id
    private String sessionId;
    
    @Column(nullable = false)
    private String title;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, referencedColumnName = "id")
    private User user;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "chatSession", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ChatMessage> messages = new ArrayList<>();
    
    // 角色扮演世界相关字段
    @Column(name = "world_type")
    private String worldType = "general";
    
    @Column(name = "world_rules", columnDefinition = "JSON")
    private String worldRules;
    
    @Column(name = "god_mode_rules", columnDefinition = "JSON")
    private String godModeRules;
    
    @Column(name = "world_state", columnDefinition = "JSON")
    private String worldState;
    
    @Column(name = "skills_state", columnDefinition = "JSON")
    private String skillsState;
    
    @Column(name = "story_checkpoints", columnDefinition = "JSON")
    private String storyCheckpoints;
    
    @Column(name = "stability_anchor", columnDefinition = "JSON")
    private String stabilityAnchor;
    
    // 新增：评估相关字段
    @Column(name = "assessment_history", columnDefinition = "JSON")
    private String assessmentHistory;        // 评估历史记录
    
    @Column(name = "last_assessment_id")
    private Long lastAssessmentId;           // 最后一次评估ID
    
    @Column(name = "convergence_progress")
    private Double convergenceProgress;      // 当前收敛进度
    
    @Column(name = "active_quests", columnDefinition = "JSON")
    private String activeQuests;             // 活跃任务列表
    
    @Column(name = "completed_quests", columnDefinition = "JSON")
    private String completedQuests;          // 已完成任务列表
    
    @Column
    private Integer version = 1;
    
    @Column(length = 32)
    private String checksum;

    // 对话轮次与剧情情节跟踪
    @Column(name = "total_rounds")
    private Integer totalRounds = 0;          // 当前会话累计总轮数（每次用户请求+1）

    @Column(name = "current_arc_start_round")
    private Integer currentArcStartRound;     // 当前情节起始轮数

    @Column(name = "current_arc_name")
    private String currentArcName;            // 当前情节名称
    
    // 构造函数
    public ChatSession() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public ChatSession(String sessionId, String title, User user) {
        this();
        this.sessionId = sessionId;
        this.title = title;
        this.user = user;
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
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
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
    
    public List<ChatMessage> getMessages() {
        return messages;
    }
    
    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
    }
    
    public int getMessageCount() {
        return messages.size();
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    // 角色扮演世界相关的Getters and Setters
    public String getWorldType() {
        return worldType;
    }
    
    public void setWorldType(String worldType) {
        this.worldType = worldType;
    }
    
    public String getWorldRules() {
        return worldRules;
    }
    
    public void setWorldRules(String worldRules) {
        this.worldRules = worldRules;
    }
    
    public String getGodModeRules() {
        return godModeRules;
    }
    
    public void setGodModeRules(String godModeRules) {
        this.godModeRules = godModeRules;
    }
    
    public String getWorldState() {
        return worldState;
    }
    
    public void setWorldState(String worldState) {
        this.worldState = worldState;
    }
    
    public String getSkillsState() {
        return skillsState;
    }
    
    public void setSkillsState(String skillsState) {
        this.skillsState = skillsState;
    }
    
    public String getStoryCheckpoints() {
        return storyCheckpoints;
    }
    
    public void setStoryCheckpoints(String storyCheckpoints) {
        this.storyCheckpoints = storyCheckpoints;
    }
    
    public String getStabilityAnchor() {
        return stabilityAnchor;
    }
    
    public void setStabilityAnchor(String stabilityAnchor) {
        this.stabilityAnchor = stabilityAnchor;
    }
    
    public Integer getVersion() {
        return version;
    }
    
    public void setVersion(Integer version) {
        this.version = version;
    }
    
    public String getChecksum() {
        return checksum;
    }
    
    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }
    
    // 新增字段的 Getters and Setters
    public String getAssessmentHistory() {
        return assessmentHistory;
    }
    
    public void setAssessmentHistory(String assessmentHistory) {
        this.assessmentHistory = assessmentHistory;
    }
    
    public Long getLastAssessmentId() {
        return lastAssessmentId;
    }
    
    public void setLastAssessmentId(Long lastAssessmentId) {
        this.lastAssessmentId = lastAssessmentId;
    }
    
    public Double getConvergenceProgress() {
        return convergenceProgress;
    }
    
    public void setConvergenceProgress(Double convergenceProgress) {
        this.convergenceProgress = convergenceProgress;
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
    

    // 轮次/情节字段的 Getters/Setters
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
