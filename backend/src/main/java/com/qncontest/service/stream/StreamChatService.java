package com.qncontest.service.stream;

import com.qncontest.dto.ChatRequest;
import com.qncontest.entity.ChatSession;
import com.qncontest.entity.User;
import com.qncontest.service.interfaces.ChatSessionManagerInterface;
import com.qncontest.service.interfaces.ResponseHandlerInterface;
import com.qncontest.service.interfaces.StreamChatServiceInterface;
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
 * 流式聊天服务 - 实现StreamChatServiceInterface接口
 * 负责处理标准聊天功能
 */
@Service
public class StreamChatService implements StreamChatServiceInterface {
    
    private static final Logger logger = LoggerFactory.getLogger(StreamChatService.class);
    
    @Autowired
    private StreamingChatLanguageModel streamingChatLanguageModel;
    
    @Autowired
    private ChatSessionManagerInterface chatSessionService;
    
    @Autowired
    private ResponseHandlerInterface responseHandler;
    
    /**
     * 处理标准流式聊天
     */
    public SseEmitter handleStreamChat(ChatRequest request, User user) {
        SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时
        
        // 添加SSE连接状态监听
        emitter.onCompletion(() -> logger.info("SSE连接正常完成（标准聊天）: sessionId={}", request.getSessionId()));
        emitter.onTimeout(() -> logger.warn("SSE连接超时（标准聊天）: sessionId={}", request.getSessionId()));
        emitter.onError((ex) -> logger.error("SSE连接错误（标准聊天）: sessionId={}, error={}", request.getSessionId(), ex.getMessage(), ex));
        
        CompletableFuture.runAsync(() -> {
            try {
                // 处理会话
                ChatSession session = chatSessionService.getOrCreateSession(request.getSessionId(), user);
                chatSessionService.saveUserMessage(session, request.getMessage());
                
                // 构建聊天历史
                List<ChatMessage> messages = buildChatHistory(session, request.getMessage());
                
                // 打印发送给大模型的完整提示词
                // logger.info("=== 发送给大模型的完整提示词（标准聊天） ===");
                // for (int i = 0; i < messages.size(); i++) {
                //     ChatMessage msg = messages.get(i);
                //     String role = msg instanceof dev.langchain4j.data.message.SystemMessage ? "SYSTEM" : 
                //                  msg instanceof dev.langchain4j.data.message.UserMessage ? "USER" : "AI";
                //     String content = msg.toString();
                //     logger.info("消息 {} [{}]: {}", i + 1, role, content);
                // }
                // logger.info("=== 提示词结束 ===");
                
                // 创建响应处理器（使用带重试机制的处理器）
                CompletableFuture<String> responseFuture = new CompletableFuture<>();
                dev.langchain4j.model.StreamingResponseHandler<dev.langchain4j.data.message.AiMessage> handler = 
                    responseHandler.createRetryableStandardHandler(emitter, session, request.getMessage(), responseFuture, streamingChatLanguageModel, messages);
                
                // 发送流式请求
                logger.info("开始调用大模型生成响应（标准聊天）...");
                streamingChatLanguageModel.generate(messages, handler);
                
                // 等待响应完成，设置超时时间
                try {
                    responseFuture.get(4, java.util.concurrent.TimeUnit.MINUTES);
                } catch (java.util.concurrent.TimeoutException e) {
                    logger.error("标准聊天流式响应超时", e);
                    emitter.completeWithError(new RuntimeException("响应超时，请重试"));
                    return;
                }
                
            } catch (Exception e) {
                logger.error("处理流式聊天失败", e);
                emitter.completeWithError(e);
            }
        });
        
        return emitter;
    }
    
    /**
     * 构建聊天历史
     */
    private List<ChatMessage> buildChatHistory(ChatSession session, String currentMessage) {
        List<ChatMessage> messages = new ArrayList<>();
        
        // 添加系统消息
        messages.add(new SystemMessage("你是一个智能助手，可以帮助用户解答各种问题。请用友好、专业的语气回答。"));
        
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
        messages.add(new UserMessage(currentMessage));
        
        return messages;
    }
    
    /**
     * 处理角色扮演流式聊天
     * 注意：此实现委托给RoleplayStreamService处理
     */
    @Override
    public SseEmitter handleRoleplayStreamChat(com.qncontest.dto.RoleplayRequest request, User user) {
        // 这里应该委托给RoleplayStreamService，但为了避免循环依赖，
        // 我们抛出一个异常，提示应该使用专门的RoleplayStreamService
        throw new UnsupportedOperationException("角色扮演聊天应该使用RoleplayStreamService处理");
    }
}
