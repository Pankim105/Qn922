package com.qncontest.service;

import com.qncontest.service.interfaces.PromptBuilderInterface;
import com.qncontest.service.interfaces.MemoryManagerInterface;
import com.qncontest.service.prompt.PromptBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 角色扮演智能提示引擎 - 基于接口的重构版本
 * 作为提示词构建的统一入口，通过接口委托给具体的服务实现
 */
@Service
public class RoleplayPromptEngine {
    
    
    @Autowired
    private PromptBuilderInterface promptBuilder;
    
    @Autowired
    private MemoryManagerInterface memoryManager;
    
    /**
     * 角色扮演上下文 - 委托给PromptBuilder
     */
    public static class RoleplayContext extends PromptBuilder.RoleplayContext {
        public RoleplayContext(String worldType, String sessionId) {
            super(worldType, sessionId);
        }
    }
    
    /**
     * 构建分层角色扮演提示
     */
    public String buildLayeredPrompt(RoleplayContext context) {
        return promptBuilder.buildLayeredPrompt(context);
    }
    
    /**
     * 构建DM智能评估提示词
     */
    public String buildDMAwarePrompt(RoleplayContext context) {
        return promptBuilder.buildDMAwarePrompt(context);
    }
    
    /**
     * 构建简化的快速提示
     */
    public String buildQuickPrompt(String worldType, String message) {
        String roleDescription = switch (worldType) {
            case "fantasy_adventure" -> "奇幻世界的游戏主持人";
            case "educational" -> "寓教于乐的智慧导师";
            case "western_magic" -> "西方魔幻世界的贤者";
            case "martial_arts" -> "江湖中的前辈高人";
            case "japanese_school" -> "校园生活向导";
            default -> "智慧的向导";
        };

        return String.format("""
            你是%s。请用角色扮演的方式回应用户，保持世界观的一致性，
            提供生动的描述和有意义的互动。如果需要随机判定，使用骰子指令。

            ## 记忆管理
            如果你的回复中包含重要信息，请在回复末尾使用记忆标记：
            - 角色关系：[MEMORY:CHARACTER:角色名:关系变化]
            - 重要事件：[MEMORY:EVENT:事件描述]
            - 技能学习：[MEMORY:SKILL:技能名:学习情况]

            用户消息：%s
            """, roleDescription, message);
    }
    
    /**
     * 处理AI回复中的记忆标记
     */
    public void processMemoryMarkers(String sessionId, String aiResponse, String userAction) {
        memoryManager.processMemoryMarkers(sessionId, aiResponse, userAction);
    }
}
