package com.qncontest.service;

import com.qncontest.dto.ChatRequest;
import com.qncontest.entity.User;
import com.qncontest.service.interfaces.StreamChatServiceInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 流式AI服务 - 基于接口的重构版本
 * 作为流式聊天服务的统一入口，通过接口委托给具体的服务实现
 */
@Service
public class StreamAiService {
    
    private static final Logger logger = LoggerFactory.getLogger(StreamAiService.class);
    
    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("unifiedStreamChatService")
    private StreamChatServiceInterface streamChatService;
    
    /**
     * 处理标准流式聊天
     */
    public SseEmitter handleStreamChat(ChatRequest request, User user) {
        logger.info("处理标准流式聊天: sessionId={}, user={}", request.getSessionId(), user.getUsername());
        return streamChatService.handleStreamChat(request, user);
    }
    
    /**
     * 处理角色扮演流式聊天
     */
    public SseEmitter handleRoleplayStreamChat(com.qncontest.dto.RoleplayRequest request, User user) {
        logger.info("处理角色扮演流式聊天: sessionId={}, worldType={}, user={}", 
                   request.getSessionId(), request.getWorldType(), user.getUsername());
        return streamChatService.handleRoleplayStreamChat(request, user);
    }
}
