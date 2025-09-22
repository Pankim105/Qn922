package com.qncontest.service;

import com.qncontest.config.AiConfig;
import com.qncontest.dto.ChatRequest;
import com.qncontest.entity.ChatSession;
import com.qncontest.entity.User;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class StreamAiService {
    
    private static final Logger logger = LoggerFactory.getLogger(StreamAiService.class);
    
    @Autowired
    private StreamingChatLanguageModel streamingChatLanguageModel;
    
    @Autowired
    private ChatSessionService chatSessionService;
    
    @Autowired
    private AiConfig.AiProperties aiProperties;
    
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    
    /**
     * 处理流式聊天请求
     */
    public SseEmitter handleStreamChat(ChatRequest request, User user) {
        SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时
        
        CompletableFuture.runAsync(() -> {
            try {
                // 保存安全上下文
                var authentication = SecurityContextHolder.getContext().getAuthentication();
                
                // 设置超时和错误处理
                emitter.onTimeout(() -> {
                    logger.warn("SSE connection timed out for user: {}", user.getUsername());
                    emitter.complete();
                });
                
                emitter.onError((error) -> {
                    logger.error("SSE connection error for user: {}", user.getUsername(), error);
                });
                
                emitter.onCompletion(() -> {
                    logger.debug("SSE connection completed for user: {}", user.getUsername());
                });
                
                // 获取或创建会话
                ChatSession session = chatSessionService.getOrCreateSession(request.getSessionId(), user);
                
                // 保存用户消息
                chatSessionService.saveUserMessage(session, request.getMessage());
                
                // 构建对话历史
                List<dev.langchain4j.data.message.ChatMessage> messages = buildChatHistory(request, session);
                
                // 用于累积AI回复内容
                StringBuilder aiResponseBuilder = new StringBuilder();
                
                // 创建流式响应处理器
                StreamingResponseHandler<AiMessage> handler = new StreamingResponseHandler<AiMessage>() {
                    @Override
                    public void onNext(String token) {
                        try {
                            // 在异步任务中恢复安全上下文
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            
                            aiResponseBuilder.append(token);
                            
                            // 发送流式数据
                            String eventData = String.format("{\"content\":\"%s\"}",
                                escapeJsonString(token));
                            emitter.send(SseEmitter.event()
                                .name("message")
                                .data(eventData));
                                
                        } catch (IOException e) {
                            logger.error("Error sending SSE data", e);
                            emitter.completeWithError(e);
                        }
                    }
                    
                    @Override
                    public void onComplete(Response<AiMessage> response) {
                        try {
                            // 在异步任务中恢复安全上下文
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            
                            // 保存AI回复
                            String fullResponse = aiResponseBuilder.toString();
                            if (fullResponse.isEmpty() && response != null && response.content() != null) {
                                fullResponse = response.content().text();
                            }
                            
                            chatSessionService.saveAiMessage(session, fullResponse);
                            
                            // 如果是新会话的第一条消息，更新会话标题
                            // 避免懒加载异常，通过数据库查询获取消息数量
                            try {
                                ChatSession refreshedSession = chatSessionService.getSessionWithMessages(session.getSessionId());
                                if (refreshedSession != null && refreshedSession.getMessages().size() <= 2) { // 用户消息 + AI回复
                                    String title = request.getMessage().length() > 15 ? 
                                        request.getMessage().substring(0, 15) + "..." : 
                                        request.getMessage();
                                    chatSessionService.updateSessionTitle(session, title);
                                }
                            } catch (Exception e) {
                                logger.warn("Failed to update session title", e);
                            }
                            
                            // 发送完成信号
                            emitter.send(SseEmitter.event()
                                .name("complete")
                                .data("[DONE]"));
                            emitter.complete();
                            
                        } catch (Exception e) {
                            logger.error("Error completing SSE stream", e);
                            emitter.completeWithError(e);
                        }
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        logger.error("Error in AI response stream", error);
                        try {
                            emitter.send(SseEmitter.event()
                                .name("error")
                                .data("{\"error\":\"AI服务暂时不可用，请稍后重试\"}"));
                        } catch (IOException e) {
                            logger.error("Error sending error event", e);
                        }
                        emitter.completeWithError(error);
                    }
                };
                
                // 调用AI模型
                streamingChatLanguageModel.generate(messages, handler);
                
            } catch (Exception e) {
                logger.error("Error in stream chat processing", e);
                try {
                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data("{\"error\":\"处理请求时发生错误\"}"));
                } catch (IOException ioException) {
                    logger.error("Error sending error event", ioException);
                }
                emitter.completeWithError(e);
            }
        }, executorService);
        
        return emitter;
    }
    
    /**
     * 构建聊天历史
     */
    private List<dev.langchain4j.data.message.ChatMessage> buildChatHistory(ChatRequest request, ChatSession session) {
        List<dev.langchain4j.data.message.ChatMessage> messages = new ArrayList<>();
        
        // 添加系统消息
        String systemPrompt = request.getSystemPrompt();
        if (systemPrompt == null || systemPrompt.trim().isEmpty()) {
            systemPrompt = aiProperties.getDefaultSystemPrompt();
        }
        messages.add(new SystemMessage(systemPrompt));
        
        // 添加历史消息（来自前端传递的history）
        if (request.getHistory() != null && !request.getHistory().isEmpty()) {
            for (ChatRequest.ChatHistoryMessage historyMsg : request.getHistory()) {
                if ("user".equals(historyMsg.getRole())) {
                    messages.add(new UserMessage(historyMsg.getContent()));
                } else if ("assistant".equals(historyMsg.getRole())) {
                    messages.add(new AiMessage(historyMsg.getContent()));
                }
            }
        }
        
        // 添加当前用户消息
        messages.add(new UserMessage(request.getMessage()));
        
        return messages;
    }
    
    /**
     * 转义JSON字符串
     */
    private String escapeJsonString(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}
