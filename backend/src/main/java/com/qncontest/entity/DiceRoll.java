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

    @Column(name = "roll_id", nullable = false)
    private String rollId;

    @Column(name = "dice_type", nullable = false)
    private Integer diceType;
    
    @Column(name = "num_dice", nullable = false)
    private Integer numDice = 1;

    @Column
    private Integer modifier = 0;

    @Column(nullable = false)
    private Integer result;

    @Column(name = "final_result", nullable = false)
    private Integer finalResult;

    @Column(length = 500)
    private String reason;

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
        this.rollId = generateRollId();
    }

    public DiceRoll(String sessionId, Integer diceType, Integer result) {
        this();
        this.sessionId = sessionId;
        this.diceType = diceType;
        this.result = result;
        this.finalResult = result + (modifier != null ? modifier : 0);
    }

    public DiceRoll(String sessionId, Integer diceType, Integer modifier, Integer result, String context) {
        this(sessionId, diceType, result);
        this.modifier = modifier;
        this.context = context;
        this.reason = context;
        this.finalResult = result + (modifier != null ? modifier : 0);
    }

    /**
     * 生成唯一的roll_id
     */
    private String generateRollId() {
        return "roll_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
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

    public String getRollId() {
        return rollId;
    }

    public void setRollId(String rollId) {
        this.rollId = rollId;
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

    public Integer getNumDice() {
        return numDice;
    }

    public void setNumDice(Integer numDice) {
        this.numDice = numDice;
    }

    public Integer getFinalResult() {
        return finalResult;
    }

    public void setFinalResult(Integer finalResult) {
        this.finalResult = finalResult;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
