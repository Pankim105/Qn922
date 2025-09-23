package com.qncontest.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 骰子记录实体 - 记录所有骰子检定结果
 */
@Entity
@Table(name = "dice_rolls")
public class DiceRoll {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "session_id", nullable = false)
    private String sessionId;
    
    @Column(name = "dice_type", nullable = false)
    private Integer diceType;
    
    @Column
    private Integer modifier = 0;
    
    @Column(nullable = false)
    private Integer result;
    
    @Column(length = 500)
    private String context;
    
    @Column(name = "is_successful")
    private Boolean isSuccessful;
    
    @Column(name = "difficulty_class")
    private Integer difficultyClass;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    // 构造函数
    public DiceRoll() {
        this.createdAt = LocalDateTime.now();
    }
    
    public DiceRoll(String sessionId, Integer diceType, Integer result) {
        this();
        this.sessionId = sessionId;
        this.diceType = diceType;
        this.result = result;
    }
    
    public DiceRoll(String sessionId, Integer diceType, Integer modifier, Integer result, String context) {
        this(sessionId, diceType, result);
        this.modifier = modifier;
        this.context = context;
    }
    
    /**
     * 计算最终结果（骰子结果 + 修正值）
     */
    public Integer getFinalResult() {
        return result + (modifier != null ? modifier : 0);
    }
    
    /**
     * 判断检定是否成功
     */
    public Boolean checkSuccess(Integer difficultyClass) {
        if (difficultyClass == null) return null;
        return getFinalResult() >= difficultyClass;
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
    
    public Integer getDiceType() {
        return diceType;
    }
    
    public void setDiceType(Integer diceType) {
        this.diceType = diceType;
    }
    
    public Integer getModifier() {
        return modifier;
    }
    
    public void setModifier(Integer modifier) {
        this.modifier = modifier;
    }
    
    public Integer getResult() {
        return result;
    }
    
    public void setResult(Integer result) {
        this.result = result;
    }
    
    public String getContext() {
        return context;
    }
    
    public void setContext(String context) {
        this.context = context;
    }
    
    public Boolean getIsSuccessful() {
        return isSuccessful;
    }
    
    public void setIsSuccessful(Boolean isSuccessful) {
        this.isSuccessful = isSuccessful;
    }
    
    public Integer getDifficultyClass() {
        return difficultyClass;
    }
    
    public void setDifficultyClass(Integer difficultyClass) {
        this.difficultyClass = difficultyClass;
        // 自动计算成功状态
        if (difficultyClass != null) {
            this.isSuccessful = checkSuccess(difficultyClass);
        }
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
