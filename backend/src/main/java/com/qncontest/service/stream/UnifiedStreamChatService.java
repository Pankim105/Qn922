package com.qncontest.service.stream;

import com.qncontest.dto.ChatRequest;
import com.qncontest.dto.RoleplayRequest;
import com.qncontest.entity.User;
import com.qncontest.service.interfaces.StreamChatServiceInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 统一流式聊天服务 - 实现StreamChatServiceInterface接口
 * 负责根据请求类型分发到具体的服务实现
 */
@Service("unifiedStreamChatService")
public class UnifiedStreamChatService implements StreamChatServiceInterface {
    
    private static final Logger logger = LoggerFactory.getLogger(UnifiedStreamChatService.class);
    
    @Autowired
    private StreamChatService standardChatService;
    
    @Autowired
    private RoleplayStreamService roleplayStreamService;
    
    /**
     * 处理标准流式聊天
     */
    @Override
    public SseEmitter handleStreamChat(ChatRequest request, User user) {
        logger.info("统一服务处理标准流式聊天: sessionId={}, user={}", request.getSessionId(), user.getUsername());
        return standardChatService.handleStreamChat(request, user);
    }
    
    /**
     * 处理角色扮演流式聊天
     */
    @Override
    public SseEmitter handleRoleplayStreamChat(RoleplayRequest request, User user) {
        logger.info("统一服务处理角色扮演流式聊天: sessionId={}, worldType={}, user={}", 
                   request.getSessionId(), request.getWorldType(), user.getUsername());
        return roleplayStreamService.handleRoleplayStreamChat(request, user);
    }
}
