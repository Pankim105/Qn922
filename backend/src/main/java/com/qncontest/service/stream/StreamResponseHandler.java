package com.qncontest.service.stream;

import com.alibaba.dashscope.exception.ApiException;
import com.qncontest.entity.ChatSession;
import com.qncontest.entity.DMAssessment;
import com.qncontest.service.interfaces.ChatSessionManagerInterface;
import com.qncontest.service.interfaces.ResponseHandlerInterface;
import com.qncontest.service.RoleplayPromptEngine;
import com.qncontest.service.AssessmentExtractor;
import com.qncontest.service.AssessmentGameLogicProcessor;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * 流式响应处理器 - 实现ResponseHandlerInterface接口
 * 负责处理AI流式响应的回调逻辑
 */
@Component
public class StreamResponseHandler implements ResponseHandlerInterface {
    
    private static final Logger logger = LoggerFactory.getLogger(StreamResponseHandler.class);
    
    @Autowired
    private ChatSessionManagerInterface chatSessionService;
    
    
    @Autowired
    private RoleplayPromptEngine promptEngine;
    
    @Autowired
    private ApiErrorHandler apiErrorHandler;
    
    @Autowired
    private RetryableStreamResponseHandler retryableHandler;
    
    @Autowired
    private AssessmentExtractor assessmentExtractor;
    
    @Autowired
    private AssessmentGameLogicProcessor assessmentGameLogicProcessor;
    
    
    /**
     * 创建标准聊天响应处理器
     */
    public StreamingResponseHandler<AiMessage> createStandardChatHandler(
            SseEmitter emitter, 
            ChatSession session, 
            String userMessage,
            CompletableFuture<String> responseFuture) {
        
        return new StreamingResponseHandler<AiMessage>() {
            private final StringBuilder fullResponse = new StringBuilder();
            private volatile boolean isCompleted = false;
            
            @Override
            public void onNext(String token) {
                if (isCompleted) {
                    logger.warn("尝试在已完成的emitter上发送token，忽略");
                    return;
                }
                try {
                    fullResponse.append(token);
                    // 只在TRACE级别记录token，减少日志噪音
                    if (logger.isTraceEnabled()) {
                        logger.trace("收到大模型token: {}", token);
                    }
                    // 发送JSON格式的数据，改进转义处理
                    String escapedToken = token
                            .replace("\\", "\\\\")  // 先转义反斜杠
                            .replace("\"", "\\\"")  // 转义双引号
                            .replace("\n", "\\n")   // 转义换行符
                            .replace("\r", "\\r")   // 转义回车符
                            .replace("\t", "\\t");  // 转义制表符
                    
                    String jsonData = "{\"content\":\"" + escapedToken + "\"}";
                    emitter.send(SseEmitter.event()
                            .name("message")
                            .data(jsonData));
                } catch (IOException e) {
                    logger.error("发送SSE消息失败", e);
                    handleError(e);
                }
            }
            
            @Override
            public void onComplete(Response<AiMessage> response) {
                if (isCompleted) {
                    logger.warn("尝试在已完成的emitter上调用onComplete，忽略");
                    return;
                }
                try {
                    String fullText = fullResponse.toString();
                    // 打印完整的大模型响应
                    // logger.info("=== 大模型完整响应 ===");
                    // logger.info("响应内容: {}", fullText);
                    // logger.info("=== 响应结束 ===");
                    
                    responseFuture.complete(fullText);
                    
                    // 保存AI消息
                    chatSessionService.saveAiMessage(session, fullText);
                    
                    // 处理记忆标记
                    logger.info("开始处理记忆标记和指令解析...");
                    promptEngine.processMemoryMarkers(session.getSessionId(), fullText, userMessage);
                    logger.info("记忆标记和指令解析完成");
                    
                    emitter.send(SseEmitter.event()
                            .name("complete")
                            .data("{\"status\":\"completed\"}"));
                    emitter.complete();
                    isCompleted = true;
                    
                } catch (Exception e) {
                    logger.error("完成流式响应处理失败", e);
                    handleError(e);
                }
            }
            
            @Override
            public void onError(Throwable error) {
                logger.error("流式响应错误", error);
                logger.error("错误详情: {}", error.getMessage(), error);
                
                // 如果是DashScope API错误，进行详细分析
                if (error instanceof ApiException) {
                    apiErrorHandler.analyzeApiError((ApiException) error);
                }
                
                responseFuture.completeExceptionally(error);
                handleError(error);
            }
            
            private void handleError(Throwable error) {
                if (isCompleted) {
                    logger.warn("尝试在已完成的emitter上处理错误，忽略");
                    return;
                }
                try {
                    // 获取用户友好的错误消息
                    String userFriendlyMessage = apiErrorHandler.getUserFriendlyMessage(error);
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("{\"error\":\"" + userFriendlyMessage.replace("\"", "\\\"") + "\"}"));
                } catch (IOException e) {
                    logger.error("发送错误事件失败", e);
                }
                try {
                    emitter.completeWithError(error);
                } catch (Exception e) {
                    logger.error("完成emitter失败", e);
                }
                isCompleted = true;
            }
        };
    }
    
    /**
     * 创建角色扮演响应处理器
     */
    public StreamingResponseHandler<AiMessage> createRoleplayHandler(
            SseEmitter emitter,
            ChatSession session,
            String userMessage,
            CompletableFuture<String> responseFuture) {
        
        return new StreamingResponseHandler<AiMessage>() {
            private final StringBuilder fullResponse = new StringBuilder();
            private volatile boolean isCompleted = false;
            
            @Override
            public void onNext(String token) {
                if (isCompleted) {
                    logger.warn("尝试在已完成的emitter上发送token，忽略");
                    return;
                }
                try {
                    fullResponse.append(token);
                    // 只在TRACE级别记录token，减少日志噪音
                    if (logger.isTraceEnabled()) {
                        logger.trace("收到大模型token: {}", token);
                    }
                    // 发送JSON格式的数据，改进转义处理
                    String escapedToken = token
                            .replace("\\", "\\\\")  // 先转义反斜杠
                            .replace("\"", "\\\"")  // 转义双引号
                            .replace("\n", "\\n")   // 转义换行符
                            .replace("\r", "\\r")   // 转义回车符
                            .replace("\t", "\\t");  // 转义制表符
                    
                    String jsonData = "{\"content\":\"" + escapedToken + "\"}";
                    emitter.send(SseEmitter.event()
                            .name("message")
                            .data(jsonData));
                } catch (IOException e) {
                    logger.error("发送SSE消息失败", e);
                    handleError(e);
                }
            }
            
            @Override
            public void onComplete(Response<AiMessage> response) {
                if (isCompleted) {
                    logger.warn("尝试在已完成的emitter上调用onComplete，忽略");
                    return;
                }
                try {
                    String fullText = fullResponse.toString();
                    // 打印完整的大模型响应
                    // logger.info("=== 大模型完整响应 ===");
                    // logger.info("响应内容: {}", fullText);
                    // logger.info("=== 响应结束 ===");
                    
                    responseFuture.complete(fullText);
                    
                    // 保存AI消息
                    chatSessionService.saveAiMessage(session, fullText);
                    
                    // 处理记忆标记
                    logger.info("开始处理角色扮演记忆标记...");
                    promptEngine.processMemoryMarkers(session.getSessionId(), fullText, userMessage);
                    logger.info("角色扮演记忆标记处理完成");
                    
                    // 处理评估JSON中的游戏逻辑
                    logger.info("🎮 开始处理评估JSON中的游戏逻辑...");
                    logger.info("🎮 会话ID: {}, 响应长度: {}", session.getSessionId(), fullText.length());
                    processAssessmentGameLogic(session.getSessionId(), fullText);
                    logger.info("🎮 评估JSON游戏逻辑处理完成");
                    
                    emitter.send(SseEmitter.event()
                            .name("complete")
                            .data("{\"status\":\"completed\"}"));
                    emitter.complete();
                    isCompleted = true;
                    
                } catch (Exception e) {
                    logger.error("完成角色扮演流式响应处理失败", e);
                    handleError(e);
                }
            }
            
            @Override
            public void onError(Throwable error) {
                logger.error("角色扮演流式响应错误", error);
                logger.error("错误详情: {}", error.getMessage(), error);
                
                // 如果是DashScope API错误，进行详细分析
                if (error instanceof ApiException) {
                    apiErrorHandler.analyzeApiError((ApiException) error);
                }
                
                responseFuture.completeExceptionally(error);
                handleError(error);
            }
            
            private void handleError(Throwable error) {
                if (isCompleted) {
                    logger.warn("尝试在已完成的emitter上处理错误，忽略");
                    return;
                }
                try {
                    // 获取用户友好的错误消息
                    String userFriendlyMessage = apiErrorHandler.getUserFriendlyMessage(error);
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("{\"error\":\"" + userFriendlyMessage.replace("\"", "\\\"") + "\"}"));
                } catch (IOException e) {
                    logger.error("发送错误事件失败", e);
                }
                try {
                    emitter.completeWithError(error);
                } catch (Exception e) {
                    logger.error("完成emitter失败", e);
                }
                isCompleted = true;
            }
        };
    }
    
    /**
     * 创建带重试机制的角色扮演响应处理器
     */
    public StreamingResponseHandler<AiMessage> createRetryableRoleplayHandler(
            SseEmitter emitter,
            ChatSession session,
            String userMessage,
            CompletableFuture<String> responseFuture,
            dev.langchain4j.model.chat.StreamingChatLanguageModel streamingChatLanguageModel,
            java.util.List<dev.langchain4j.data.message.ChatMessage> messages) {
        
        return retryableHandler.createRetryableRoleplayHandler(
            emitter, session, userMessage, responseFuture, streamingChatLanguageModel, messages);
    }
    
    /**
     * 创建带重试机制的标准聊天响应处理器
     */
    public StreamingResponseHandler<AiMessage> createRetryableStandardHandler(
            SseEmitter emitter,
            ChatSession session,
            String userMessage,
            CompletableFuture<String> responseFuture,
            dev.langchain4j.model.chat.StreamingChatLanguageModel streamingChatLanguageModel,
            java.util.List<dev.langchain4j.data.message.ChatMessage> messages) {
        
        return retryableHandler.createRetryableStandardHandler(
            emitter, session, userMessage, responseFuture, streamingChatLanguageModel, messages);
    }
    
    /**
     * 处理评估JSON中的游戏逻辑
     */
    private void processAssessmentGameLogic(String sessionId, String aiResponse) {
        try {
            logger.info("🔍 开始检查AI响应中的评估JSON: sessionId={}, 响应长度={}", sessionId, aiResponse.length());
            
            // 检查是否包含评估JSON
            if (!assessmentExtractor.containsAssessment(aiResponse)) {
                logger.info("ℹ️ AI响应中未包含评估JSON，跳过游戏逻辑处理");
                return;
            }
            
            logger.info("✅ 检测到评估JSON，开始提取...");
            
            // 提取评估结果
            DMAssessment assessment = assessmentExtractor.extractAssessmentEntity(aiResponse);
            if (assessment == null) {
                logger.warn("⚠️ 提取评估结果失败");
                return;
            }
            
            logger.info("✅ 成功提取评估结果: strategy={}, score={}, assessmentId={}", 
                       assessment.getStrategy(), assessment.getOverallScore(), assessment.getId());
            
            // 处理评估JSON中的游戏逻辑
            logger.info("🎯 开始处理评估JSON中的游戏逻辑...");
            assessmentGameLogicProcessor.processAssessmentGameLogic(sessionId, assessment);
            logger.info("🎯 评估JSON游戏逻辑处理完成");
            
        } catch (Exception e) {
            logger.error("❌ 处理评估JSON游戏逻辑失败: sessionId={}", sessionId, e);
        }
    }
}
