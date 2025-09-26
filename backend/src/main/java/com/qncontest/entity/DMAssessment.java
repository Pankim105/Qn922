package com.qncontest.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DM评估实体 - 存储大模型的智能评估结果
 */
@Entity
@Table(name = "dm_assessments")
public class DMAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Column(name = "rule_compliance", nullable = false)
    private Double ruleCompliance;        // 规则合规性 (0-1)

    @Column(name = "context_consistency", nullable = false)
    private Double contextConsistency;    // 上下文一致性 (0-1)

    @Column(name = "convergence_progress", nullable = false)
    private Double convergenceProgress;   // 收敛推进度 (0-1)

    @Column(name = "overall_score", nullable = false)
    private Double overallScore;          // 综合评分 (0-1)

    @Enumerated(EnumType.STRING)
    @Column(name = "strategy", nullable = false)
    private AssessmentStrategy strategy;  // 评估策略

    @Column(name = "assessment_notes", columnDefinition = "TEXT")
    private String assessmentNotes;       // 评估说明

    @Column(name = "suggested_actions", columnDefinition = "TEXT")
    @Convert(converter = StringListConverter.class)
    private List<String> suggestedActions;       // 建议行动列表

    @Column(name = "convergence_hints", columnDefinition = "TEXT")
    @Convert(converter = StringListConverter.class)
    private List<String> convergenceHints;       // 收敛提示列表

    @Column(name = "quest_updates", columnDefinition = "TEXT")
    @Convert(converter = JsonObjectConverter.class)
    private Object questUpdates;           // 任务更新信息（完成、进度、奖励等）

    @Column(name = "world_state_updates", columnDefinition = "TEXT")
    @Convert(converter = JsonObjectConverter.class)
    private Object worldStateUpdates;      // 世界状态更新

    @Column(name = "skills_state_updates", columnDefinition = "TEXT")
    @Convert(converter = JsonObjectConverter.class)
    private Object skillsStateUpdates;    // 技能状态更新

    @Column(name = "dice_rolls", columnDefinition = "TEXT")
    @Convert(converter = JsonObjectConverter.class)
    private Object diceRolls;             // 骰子检定结果

    @Column(name = "learning_challenges", columnDefinition = "TEXT")
    @Convert(converter = JsonObjectConverter.class)
    private Object learningChallenges;    // 学习挑战结果

    @Column(name = "state_updates", columnDefinition = "TEXT")
    @Convert(converter = JsonObjectConverter.class)
    private Object stateUpdates;          // 状态更新信息

    @Column(name = "arc_updates", columnDefinition = "TEXT")
    @Convert(converter = JsonObjectConverter.class)
    private Object arcUpdates;            // 情节更新信息（情节名称、起始轮数等）

    @Column(name = "convergence_status_updates", columnDefinition = "TEXT")
    @Convert(converter = JsonObjectConverter.class)
    private Object convergenceStatusUpdates; // 收敛状态更新信息

    @Column(name = "assessed_at", nullable = false)
    private LocalDateTime assessedAt;     // 评估时间

    @Column(name = "user_action", columnDefinition = "TEXT")
    private String userAction;            // 用户行为描述

    // 构造函数
    public DMAssessment() {
        this.assessedAt = LocalDateTime.now();
    }

    public DMAssessment(String sessionId, String userAction) {
        this();
        this.sessionId = sessionId;
        this.userAction = userAction;
    }

    // 评估策略枚举
    public enum AssessmentStrategy {
        ACCEPT,    // 完全接受
        ADJUST,    // 部分调整
        CORRECT    // 引导修正
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

    public Double getRuleCompliance() {
        return ruleCompliance;
    }

    public void setRuleCompliance(Double ruleCompliance) {
        this.ruleCompliance = ruleCompliance;
    }

    public Double getContextConsistency() {
        return contextConsistency;
    }

    public void setContextConsistency(Double contextConsistency) {
        this.contextConsistency = contextConsistency;
    }

    public Double getConvergenceProgress() {
        return convergenceProgress;
    }

    public void setConvergenceProgress(Double convergenceProgress) {
        this.convergenceProgress = convergenceProgress;
    }

    public Double getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(Double overallScore) {
        this.overallScore = overallScore;
    }

    public AssessmentStrategy getStrategy() {
        return strategy;
    }

    public void setStrategy(AssessmentStrategy strategy) {
        this.strategy = strategy;
    }

    public String getAssessmentNotes() {
        return assessmentNotes;
    }

    public void setAssessmentNotes(String assessmentNotes) {
        this.assessmentNotes = assessmentNotes;
    }

    public List<String> getSuggestedActions() {
        return suggestedActions;
    }

    public void setSuggestedActions(List<String> suggestedActions) {
        this.suggestedActions = suggestedActions;
    }

    public List<String> getConvergenceHints() {
        return convergenceHints;
    }

    public void setConvergenceHints(List<String> convergenceHints) {
        this.convergenceHints = convergenceHints;
    }

    public Object getQuestUpdates() {
        return questUpdates;
    }

    public void setQuestUpdates(Object questUpdates) {
        this.questUpdates = questUpdates;
    }

    public LocalDateTime getAssessedAt() {
        return assessedAt;
    }

    public void setAssessedAt(LocalDateTime assessedAt) {
        this.assessedAt = assessedAt;
    }

    public String getUserAction() {
        return userAction;
    }

    public void setUserAction(String userAction) {
        this.userAction = userAction;
    }

    public Object getWorldStateUpdates() {
        return worldStateUpdates;
    }

    public void setWorldStateUpdates(Object worldStateUpdates) {
        this.worldStateUpdates = worldStateUpdates;
    }

    public Object getSkillsStateUpdates() {
        return skillsStateUpdates;
    }

    public void setSkillsStateUpdates(Object skillsStateUpdates) {
        this.skillsStateUpdates = skillsStateUpdates;
    }

    public Object getDiceRolls() {
        return diceRolls;
    }

    public void setDiceRolls(Object diceRolls) {
        this.diceRolls = diceRolls;
    }

    public Object getLearningChallenges() {
        return learningChallenges;
    }

    public void setLearningChallenges(Object learningChallenges) {
        this.learningChallenges = learningChallenges;
    }

    public Object getStateUpdates() {
        return stateUpdates;
    }

    public void setStateUpdates(Object stateUpdates) {
        this.stateUpdates = stateUpdates;
    }

    public Object getArcUpdates() {
        return arcUpdates;
    }

    public void setArcUpdates(Object arcUpdates) {
        this.arcUpdates = arcUpdates;
    }

    public Object getConvergenceStatusUpdates() {
        return convergenceStatusUpdates;
    }

    public void setConvergenceStatusUpdates(Object convergenceStatusUpdates) {
        this.convergenceStatusUpdates = convergenceStatusUpdates;
    }

    /**
     * 判断是否为高分评估
     */
    public boolean isHighScore() {
        return overallScore != null && overallScore >= 0.8;
    }

    /**
     * 判断是否为中分评估
     */
    public boolean isMediumScore() {
        return overallScore != null && overallScore >= 0.6 && overallScore < 0.8;
    }

    /**
     * 判断是否为低分评估
     */
    public boolean isLowScore() {
        return overallScore != null && overallScore < 0.6;
    }
}
