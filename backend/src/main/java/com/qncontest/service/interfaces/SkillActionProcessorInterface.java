package com.qncontest.service.interfaces;

import java.util.Map;

/**
 * 技能动作处理器接口
 * 定义技能动作处理的标准行为
 */
public interface SkillActionProcessorInterface {
    
    /**
     * 处理AI回复中的技能动作
     * @param sessionId 会话ID
     * @param aiResponse AI回复
     * @param userAction 用户行为
     */
    void processAiResponse(String sessionId, String aiResponse, String userAction);
    
    /**
     * 处理骰子检定
     * @param sessionId 会话ID
     * @param diceType 骰子类型
     * @param modifier 修正值
     * @param context 检定上下文
     * @return 检定结果
     */
    String processDiceRoll(String sessionId, Integer diceType, Integer modifier, String context);
    
    /**
     * 处理任务更新
     * @param sessionId 会话ID
     * @param questUpdates 任务更新信息
     */
    void processQuestUpdates(String sessionId, Map<String, Object> questUpdates);
}
