package com.qncontest.service.stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.qncontest.entity.ChatSession;
import com.qncontest.service.interfaces.ChatSessionManagerInterface;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.output.Response;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * 智能流式处理器 - 解决JSON结构被破坏的问题
 */
public class SmartStreamHandler {
    private static final Logger logger = LoggerFactory.getLogger(SmartStreamHandler.class);
    
    private final ChatSessionManagerInterface chatSessionService;
    
    public SmartStreamHandler(ChatSessionManagerInterface chatSessionService) {
        this.chatSessionService = chatSessionService;
    }
    
    /**
     * 创建智能角色扮演响应处理器
     */
    public StreamingResponseHandler<AiMessage> createSmartRoleplayHandler(
            SseEmitter emitter,
            ChatSession session,
            String userMessage,
            CompletableFuture<String> responseFuture) {
        
        return new StreamingResponseHandler<AiMessage>() {
            private final StringBuilder fullResponse = new StringBuilder();
            private final StringBuilder buffer = new StringBuilder();
            private volatile boolean isCompleted = false;
            private boolean inAssessmentJson = false;
            
            @Override
            public void onNext(String token) {
                if (isCompleted) {
                    logger.warn("尝试在已完成的emitter上发送token，忽略");
                    return;
                }
                
                try {
                    fullResponse.append(token);
                    buffer.append(token);
                    
                    // 只在TRACE级别记录token，减少日志噪音
                    if (logger.isTraceEnabled()) {
                        logger.trace("收到大模型token: {}", token);
                    }
                    
                    // 智能处理流式输出
                    processTokenStream(token);
                    
                } catch (IOException e) {
                    logger.error("发送SSE消息失败", e);
                    handleError(e);
                }
            }
            
            /**
             * 智能处理token流，避免破坏JSON结构
             */
            private void processTokenStream(String token) throws IOException {
                // 检查是否进入评估JSON区域
                if (token.contains("§{") && !inAssessmentJson) {
                    inAssessmentJson = true;
                    logger.debug("进入评估JSON区域");
                }
                
                // 检查是否离开评估JSON区域
                if (inAssessmentJson && token.contains("}§")) {
                    inAssessmentJson = false;
                    logger.debug("离开评估JSON区域");
                }
                
                // 如果在评估JSON区域内，累积完整JSON后再发送
                if (inAssessmentJson) {
                    // 检查是否包含完整的评估JSON
                    String currentBuffer = buffer.toString();
                    if (currentBuffer.contains("§{") && currentBuffer.contains("}§")) {
                        // 找到完整的评估JSON，发送累积的内容
                        sendAccumulatedContent();
                        buffer.setLength(0);
                    }
                    return;
                }
                
                // 检查是否在结构化标记内
                if (token.contains("*DIALOGUE:") || token.contains("*WORLD:") || 
                    token.contains("*QUESTS") || token.contains("*CHOICES:") || 
                    token.contains("*STATUS:") || token.contains("*ASSESSMENT:")) {
                    
                    // 在结构化标记内，检查是否有完整的标记块
                    String currentBuffer = buffer.toString();
                    if (isCompleteStructuredBlock(currentBuffer)) {
                        sendAccumulatedContent();
                        buffer.setLength(0);
                    }
                    return;
                }
                
                // 普通内容，直接发送
                sendTokenAsJson(token);
            }
            
            /**
             * 检查是否是完整的结构化块
             */
            private boolean isCompleteStructuredBlock(String content) {
                // 检查是否有完整的标记块（以*/结尾）
                return content.contains("*/") && 
                       (content.contains("*DIALOGUE:") || content.contains("*WORLD:") || 
                        content.contains("*QUESTS") || content.contains("*CHOICES:") || 
                        content.contains("*STATUS:") || content.contains("*ASSESSMENT:"));
            }
            
            /**
             * 发送累积的内容
             */
            private void sendAccumulatedContent() throws IOException {
                String content = buffer.toString();
                if (!content.trim().isEmpty()) {
                    sendTokenAsJson(content);
                }
            }
            
            /**
             * 将token作为JSON发送
             */
            private void sendTokenAsJson(String token) throws IOException {
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
            }
            
            @Override
            public void onComplete(Response<AiMessage> response) {
                if (isCompleted) {
                    logger.warn("尝试在已完成的emitter上调用onComplete，忽略");
                    return;
                }
                
                try {
                    String fullText = fullResponse.toString();
                    
                    // 发送剩余的缓冲内容
                    if (buffer.length() > 0) {
                        sendAccumulatedContent();
                    }
                    
                    // 打印完整的大模型响应
                    // logger.info("=== 大模型完整响应 ===");
                    // logger.info("响应内容: {}", fullText);
                    // logger.info("=== 响应结束 ===");
                    
                    responseFuture.complete(fullText);
                    
                    // 保存AI消息
                    chatSessionService.saveAiMessage(session, fullText);
                    
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
                logger.error("角色扮演流式响应处理出错", error);
                handleError(error);
            }
            
            private void handleError(Throwable error) {
                if (!isCompleted) {
                    try {
                        emitter.send(SseEmitter.event()
                                .name("error")
                                .data("{\"error\":\"" + error.getMessage() + "\"}"));
                        emitter.completeWithError(error);
                    } catch (IOException e) {
                        logger.error("发送错误信息失败", e);
                    }
                    isCompleted = true;
                }
            }
            
            private void processAssessmentGameLogic(String sessionId, String fullText) {
                // 这里应该调用实际的游戏逻辑处理方法
                // 暂时留空，等待后续实现
                logger.debug("处理评估JSON游戏逻辑: sessionId={}, textLength={}", sessionId, fullText.length());
            }
        };
    }
}
