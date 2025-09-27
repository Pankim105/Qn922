package com.qncontest.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 技能动作服务 - 已简化，所有指令处理已移至评估JSON
 * 
 * 注意：此服务已不再处理指令标记，所有游戏逻辑现在通过评估JSON中的专门字段处理：
 * - diceRolls: 骰子检定
 * - questUpdates: 任务更新
 * - learningChallenges: 学习挑战
 * - stateUpdates: 状态更新
 * - memoryUpdates: 记忆更新
 * - worldStateUpdates: 世界状态更新
 */
@Service
public class SkillActionService {
    
    private static final Logger logger = LoggerFactory.getLogger(SkillActionService.class);
    
    @Autowired
    private RoleplayMemoryService memoryService;
    
    @Autowired
    private RoleplayWorldService roleplayWorldService;
    
    /**
     * 清理AI回复中的指令标记（移除已处理的指令）
     * 注意：现在主要用于清理可能残留的旧格式指令
     */
    public String cleanupSkillInstructions(String aiResponse) {
        String cleaned = aiResponse;
        
        // 移除所有旧格式的指令标记
        cleaned = cleaned.replaceAll("\\[DICE:[^\\]]+\\]", "");
        cleaned = cleaned.replaceAll("\\[QUEST:[^\\]]+\\]", "");
        cleaned = cleaned.replaceAll("\\[CHALLENGE:[^\\]]+\\]", "");
        cleaned = cleaned.replaceAll("\\[STATE:[^\\]]+\\]", "");
        cleaned = cleaned.replaceAll("\\[CHARACTER:[^\\]]+\\]", "");
        cleaned = cleaned.replaceAll("\\[WORLD:[^\\]]+\\]", "");
        
        // 清理多余的空行
        cleaned = cleaned.replaceAll("\\n{3,}", "\n\n");
        cleaned = cleaned.trim();
        
        return cleaned;
    }
    
    /**
     * 检查AI回复中是否包含旧格式的指令标记
     * 用于日志记录和调试
     */
    public boolean containsLegacyInstructions(String aiResponse) {
        return aiResponse.contains("[DICE:") || 
               aiResponse.contains("[QUEST:") || 
               aiResponse.contains("[CHALLENGE:") || 
               aiResponse.contains("[STATE:") || 
               aiResponse.contains("[CHARACTER:") || 
               aiResponse.contains("[WORLD:");
    }
    
    /**
     * 记录检测到的旧格式指令（用于调试和迁移）
     */
    public void logLegacyInstructions(String sessionId, String aiResponse) {
        if (containsLegacyInstructions(aiResponse)) {
            logger.warn("⚠️ 检测到旧格式指令标记，建议使用评估JSON格式: sessionId={}", sessionId);
            // 可以在这里添加更详细的日志记录
        }
    }
}