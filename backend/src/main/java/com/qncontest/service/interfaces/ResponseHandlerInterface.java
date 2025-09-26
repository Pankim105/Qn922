package com.qncontest.service.interfaces;

import com.qncontest.entity.ChatSession;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 响应处理器接口
 * 定义流式响应处理的标准行为
 */
public interface ResponseHandlerInterface {
    
    /**
     * 创建标准聊天响应处理器
     * @param emitter SSE发射器
     * @param session 聊天会话
     * @param userMessage 用户消息
     * @param responseFuture 响应Future
     * @return 流式响应处理器
     */
    StreamingResponseHandler<AiMessage> createStandardChatHandler(
            SseEmitter emitter, 
            ChatSession session, 
            String userMessage,
            CompletableFuture<String> responseFuture);
    
    /**
     * 创建角色扮演响应处理器
     * @param emitter SSE发射器
     * @param session 聊天会话
     * @param userMessage 用户消息
     * @param responseFuture 响应Future
     * @return 流式响应处理器
     */
    StreamingResponseHandler<AiMessage> createRoleplayHandler(
            SseEmitter emitter,
            ChatSession session,
            String userMessage,
            CompletableFuture<String> responseFuture);
    
    /**
     * 创建带重试机制的角色扮演响应处理器
     * @param emitter SSE发射器
     * @param session 聊天会话
     * @param userMessage 用户消息
     * @param responseFuture 响应Future
     * @param streamingChatLanguageModel 流式聊天模型
     * @param messages 消息列表
     * @return 流式响应处理器
     */
    StreamingResponseHandler<AiMessage> createRetryableRoleplayHandler(
            SseEmitter emitter,
            ChatSession session,
            String userMessage,
            CompletableFuture<String> responseFuture,
            StreamingChatLanguageModel streamingChatLanguageModel,
            List<ChatMessage> messages);
    
    /**
     * 创建带重试机制的标准聊天响应处理器
     * @param emitter SSE发射器
     * @param session 聊天会话
     * @param userMessage 用户消息
     * @param responseFuture 响应Future
     * @param streamingChatLanguageModel 流式聊天模型
     * @param messages 消息列表
     * @return 流式响应处理器
     */
    StreamingResponseHandler<AiMessage> createRetryableStandardHandler(
            SseEmitter emitter,
            ChatSession session,
            String userMessage,
            CompletableFuture<String> responseFuture,
            StreamingChatLanguageModel streamingChatLanguageModel,
            List<ChatMessage> messages);
}
