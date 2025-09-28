package com.qncontest.service.interfaces;

import com.qncontest.service.prompt.PromptBuilder;

/**
 * 提示词构建器接口
 * 定义提示词构建的标准行为
 */
public interface PromptBuilderInterface {
    
    /**
     * 构建分层角色扮演提示
     * @param context 角色扮演上下文
     * @return 构建的提示词
     */
    String buildLayeredPrompt(PromptBuilder.RoleplayContext context);
    
    /**
     * 构建DM智能评估提示词
     * @param context 角色扮演上下文
     * @return 构建的提示词
     */
    String buildDMAwarePrompt(PromptBuilder.RoleplayContext context);
    
}
