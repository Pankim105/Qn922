package com.qncontest.service;

import com.qncontest.service.interfaces.PromptBuilderInterface;
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
    
    
}
