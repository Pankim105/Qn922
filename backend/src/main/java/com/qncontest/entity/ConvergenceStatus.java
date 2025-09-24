package com.qncontest.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 收敛状态实体 - 存储故事收敛进度和状态
 */
@Entity
@Table(name = "convergence_status")
public class ConvergenceStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Column(name = "progress", nullable = false)
    private Double progress;              // 整体收敛进度 (0-1)

    @Column(name = "nearest_scenario_id")
    private String nearestScenarioId;     // 最近的收敛场景ID

    @Column(name = "nearest_scenario_title")
    private String nearestScenarioTitle;  // 最近场景标题

    @Column(name = "distance_to_nearest")
    private Double distanceToNearest;     // 到最近场景的距离

    @Column(name = "scenario_progress", columnDefinition = "JSON")
    private String scenarioProgress;       // 所有场景进度JSON

    @Column(name = "active_hints", columnDefinition = "JSON")
    private String activeHints;            // 当前活跃的引导提示

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;    // 最后更新时间

    // 构造函数
    public ConvergenceStatus() {
        this.lastUpdated = LocalDateTime.now();
        this.progress = 0.0;
    }

    public ConvergenceStatus(String sessionId) {
        this();
        this.sessionId = sessionId;
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

    public Double getProgress() {
        return progress;
    }

    public void setProgress(Double progress) {
        this.progress = progress;
    }

    public String getNearestScenarioId() {
        return nearestScenarioId;
    }

    public void setNearestScenarioId(String nearestScenarioId) {
        this.nearestScenarioId = nearestScenarioId;
    }

    public String getNearestScenarioTitle() {
        return nearestScenarioTitle;
    }

    public void setNearestScenarioTitle(String nearestScenarioTitle) {
        this.nearestScenarioTitle = nearestScenarioTitle;
    }

    public Double getDistanceToNearest() {
        return distanceToNearest;
    }

    public void setDistanceToNearest(Double distanceToNearest) {
        this.distanceToNearest = distanceToNearest;
    }

    public String getScenarioProgress() {
        return scenarioProgress;
    }

    public void setScenarioProgress(String scenarioProgress) {
        this.scenarioProgress = scenarioProgress;
    }

    public String getActiveHints() {
        return activeHints;
    }

    public void setActiveHints(String activeHints) {
        this.activeHints = activeHints;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    /**
     * 判断是否接近收敛（进度 > 0.7）
     */
    public boolean isApproachingConvergence() {
        return progress != null && progress > 0.7;
    }

    /**
     * 判断是否处于收敛阶段（进度 > 0.5）
     */
    public boolean isInConvergencePhase() {
        return progress != null && progress > 0.5;
    }

    /**
     * 判断是否刚刚开始（进度 < 0.3）
     */
    public boolean isEarlyStage() {
        return progress != null && progress < 0.3;
    }

    /**
     * 更新进度并记录时间
     */
    public void updateProgress(double newProgress) {
        this.progress = newProgress;
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * 增加进度
     */
    public void addProgress(double increment) {
        this.progress = Math.min(1.0, (this.progress != null ? this.progress : 0.0) + increment);
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * 获取进度百分比字符串
     */
    public String getProgressPercentage() {
        if (progress == null) return "0%";
        return String.format("%.0f%%", progress * 100);
    }
}
