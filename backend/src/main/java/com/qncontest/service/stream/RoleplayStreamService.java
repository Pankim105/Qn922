package com.qncontest.service.stream;

import com.qncontest.dto.RoleplayRequest;
import com.qncontest.entity.ChatSession;
import com.qncontest.entity.User;
import com.qncontest.service.interfaces.ChatSessionManagerInterface;
import com.qncontest.service.interfaces.ResponseHandlerInterface;
import com.qncontest.service.interfaces.StreamChatServiceInterface;
import com.qncontest.service.interfaces.WorldStateManagerInterface;
import com.qncontest.service.RoleplayPromptEngine;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 角色扮演流式服务 - 实现StreamChatServiceInterface接口
 * 负责处理角色扮演相关的流式聊天
 */
@Service
public class RoleplayStreamService implements StreamChatServiceInterface {
    
    private static final Logger logger = LoggerFactory.getLogger(RoleplayStreamService.class);
    
    @Autowired
    private StreamingChatLanguageModel streamingChatLanguageModel;
    
    @Autowired
    private ChatSessionManagerInterface chatSessionService;
    
    @Autowired
    private WorldStateManagerInterface worldStateManager;
    
    @Autowired
    private RoleplayPromptEngine promptEngine;
    
    @Autowired
    private ResponseHandlerInterface responseHandler;
    
    /**
     * 处理角色扮演消息（内部方法，支持无SSE的场景）
     */
    public void processRoleplayMessage(RoleplayRequest request, User user, SseEmitter emitter) {
        try {
            // 处理会话
            ChatSession session = chatSessionService.getOrCreateSession(request.getSessionId(), user);
            chatSessionService.saveUserMessage(session, request.getMessage());
            
            // 初始化角色扮演会话（如果需要）
            if (request.getWorldType() != null && !request.getWorldType().isEmpty()) {
                worldStateManager.initializeRoleplaySession(
                    session.getSessionId(), 
                    request.getWorldType(), 
                    request.getGodModeRules(), 
                    user);
            }
            
            // 构建角色扮演历史
            List<ChatMessage> messages = buildRoleplayHistory(session, request);
            
            //打印发送给大模型的完整提示词
            logger.info("=== 发送给大模型的完整提示词 ===");
            for (int i = 0; i < messages.size(); i++) {
                ChatMessage msg = messages.get(i);
                String role = msg instanceof SystemMessage ? "SYSTEM" : 
                             msg instanceof UserMessage ? "USER" : "AI";
                String content = msg.toString();
                logger.info("消息 {} [{}]: {}", i + 1, role, content);
            }
            logger.info("=== 提示词结束 ===");
            
            // 创建响应处理器（使用带重试机制的处理器）
            CompletableFuture<String> responseFuture = new CompletableFuture<>();
            dev.langchain4j.model.StreamingResponseHandler<dev.langchain4j.data.message.AiMessage> handler = 
                responseHandler.createRetryableRoleplayHandler(emitter, session, request.getMessage(), responseFuture, streamingChatLanguageModel, messages);
            
            // 发送流式请求
            logger.info("开始调用大模型生成响应...");
            streamingChatLanguageModel.generate(messages, handler);
            
            // 等待响应完成，设置超时时间
            try {
                responseFuture.get(4, java.util.concurrent.TimeUnit.MINUTES);
            } catch (java.util.concurrent.TimeoutException e) {
                logger.error("角色扮演流式响应超时", e);
                if (emitter != null) {
                    emitter.completeWithError(new RuntimeException("响应超时，请重试"));
                }
                return;
            }
            
        } catch (Exception e) {
            logger.error("处理角色扮演消息失败", e);
            if (emitter != null) {
                emitter.completeWithError(e);
            }
        }
    }
    
    /**
     * 处理角色扮演流式聊天
     */
    public SseEmitter handleRoleplayStreamChat(RoleplayRequest request, User user) {
        SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时
        
        // 添加SSE连接状态监听
        emitter.onCompletion(() -> logger.info("SSE连接正常完成: sessionId={}", request.getSessionId()));
        emitter.onTimeout(() -> logger.warn("SSE连接超时: sessionId={}", request.getSessionId()));
        emitter.onError((ex) -> logger.error("SSE连接错误: sessionId={}, error={}", request.getSessionId(), ex.getMessage(), ex));
        
        // 异步处理，复用processRoleplayMessage方法
        CompletableFuture.runAsync(() -> {
            processRoleplayMessage(request, user, emitter);
        });
        
        return emitter;
    }
    
    /**
     * 构建角色扮演聊天历史
     */
    private List<ChatMessage> buildRoleplayHistory(ChatSession session, RoleplayRequest request) {
        List<ChatMessage> messages = new ArrayList<>();
        
        // 构建系统提示词
        RoleplayPromptEngine.RoleplayContext context = new RoleplayPromptEngine.RoleplayContext(
            request.getWorldType(), session.getSessionId());
        context.setCurrentMessage(request.getMessage());
        context.setWorldState(session.getWorldState());
        context.setSkillsState(session.getSkillsState());
        context.setGodModeRules(request.getGodModeRules());
        context.setSession(session);
        context.setTotalRounds(session.getTotalRounds());
        context.setCurrentArcStartRound(session.getCurrentArcStartRound());
        context.setCurrentArcName(session.getCurrentArcName());
        
        String systemPrompt = promptEngine.buildDMAwarePrompt(context);
        messages.add(new SystemMessage(systemPrompt));
        
        // 添加历史消息
        List<com.qncontest.entity.ChatMessage> historyMessages = 
            chatSessionService.getSessionHistory(session, 10);
        
        for (com.qncontest.entity.ChatMessage msg : historyMessages) {
            if (msg.getRole() == com.qncontest.entity.ChatMessage.MessageRole.USER) {
                messages.add(new UserMessage(msg.getContent()));
            } else {
                messages.add(new dev.langchain4j.data.message.AiMessage(msg.getContent()));
            }
        }
        
        // 添加当前用户消息
        messages.add(new UserMessage(request.getMessage()));
        
        return messages;
    }
    
    /**
     * 处理标准流式聊天
     * 注意：此实现委托给StreamChatService处理
     */
    @Override
    public SseEmitter handleStreamChat(com.qncontest.dto.ChatRequest request, User user) {
        // 这里应该委托给StreamChatService，但为了避免循环依赖，
        // 我们抛出一个异常，提示应该使用专门的StreamChatService
        throw new UnsupportedOperationException("标准聊天应该使用StreamChatService处理");
    }
}
