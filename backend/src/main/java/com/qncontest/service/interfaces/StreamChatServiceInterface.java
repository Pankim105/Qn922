package com.qncontest.service.interfaces;

import com.qncontest.dto.ChatRequest;
import com.qncontest.entity.User;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 流式聊天服务接口
 * 定义流式聊天服务的标准行为
 */
public interface StreamChatServiceInterface {
    
    /**
     * 处理标准流式聊天
     * @param request 聊天请求
     * @param user 用户信息
     * @return SSE发射器
     */
    SseEmitter handleStreamChat(ChatRequest request, User user);
    
    /**
     * 处理角色扮演流式聊天
     * @param request 角色扮演请求
     * @param user 用户信息
     * @return SSE发射器
     */
    SseEmitter handleRoleplayStreamChat(com.qncontest.dto.RoleplayRequest request, User user);
}
