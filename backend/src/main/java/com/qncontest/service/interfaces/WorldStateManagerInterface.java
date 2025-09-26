package com.qncontest.service.interfaces;

import com.qncontest.entity.DiceRoll;
import com.qncontest.entity.User;

/**
 * 世界状态管理器接口
 * 定义世界状态管理的标准行为
 */
public interface WorldStateManagerInterface {
    
    /**
     * 初始化角色扮演会话
     * @param sessionId 会话ID
     * @param worldType 世界类型
     * @param godModeRules 上帝模式规则
     * @param user 用户信息
     */
    void initializeRoleplaySession(String sessionId, String worldType, String godModeRules, User user);
    
    /**
     * 执行骰子检定
     * @param sessionId 会话ID
     * @param diceType 骰子类型
     * @param modifier 修正值
     * @param context 检定上下文
     * @param difficultyClass 难度等级
     * @return 骰子检定结果
     */
    DiceRoll rollDice(String sessionId, Integer diceType, Integer modifier, String context, Integer difficultyClass);
    
    /**
     * 更新世界状态
     * @param sessionId 会话ID
     * @param newWorldState 新的世界状态
     * @param skillsState 技能状态
     */
    void updateWorldState(String sessionId, String newWorldState, String skillsState);
    
    /**
     * 获取世界状态摘要
     * @param sessionId 会话ID
     * @return 世界状态摘要
     */
    String getWorldStateSummary(String sessionId);
}
