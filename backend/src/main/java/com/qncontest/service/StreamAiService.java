package com.qncontest.service;

import com.qncontest.config.AiConfig;
import com.qncontest.dto.ChatRequest;
import com.qncontest.entity.ChatSession;
import com.qncontest.entity.DMAssessment;
import com.qncontest.entity.User;
import com.qncontest.repository.ChatSessionRepository;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
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
    private ChatSessionRepository chatSessionRepository;
    
    @Autowired
    private AiConfig.AiProperties aiProperties;
    
    @Autowired
    private RoleplayWorldService roleplayWorldService;
    
    @Autowired
    private RoleplayPromptEngine roleplayPromptEngine;
    
    
    
    @Autowired
    private RoleplayPromptEngine promptEngine;
    
    @Autowired
    private SkillActionService skillActionService;
    
    @Autowired
    private RoleplayMemoryService memoryService;

    @Autowired
    private AssessmentExtractor assessmentExtractor;

    private final ExecutorService executorService = Executors.newCachedThreadPool();
    
    @Autowired
    private ApplicationContext applicationContext;

    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
    
    /**
     * 在新事务中处理数据库操作，避免跨线程事务问题
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 30)
    public ChatSession processSessionInNewTransaction(String sessionId, User user, String message) {
        try {
            ChatSession session = chatSessionService.getOrCreateSession(sessionId, user);
            chatSessionService.saveUserMessage(session, message);
            return session;
        } catch (Exception e) {
            logger.error("处理会话事务失败: sessionId={}, user={}", sessionId, user.getUsername(), e);
            throw e;
        }
    }
    
    /**
     * 在新事务中保存AI消息
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 30)
    public void saveAiMessageInNewTransaction(ChatSession session, String content) {
        try {
            chatSessionService.saveAiMessage(session, content);
        } catch (Exception e) {
            logger.error("保存AI消息失败: sessionId={}", session.getSessionId(), e);
            throw e;
        }
    }
    
    /**
     * 在新事务中更新会话标题
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 30)
    public void updateSessionTitleInNewTransaction(ChatSession session, String title) {
        try {
            chatSessionService.updateSessionTitle(session, title);
        } catch (Exception e) {
            logger.error("更新会话标题失败: sessionId={}", session.getSessionId(), e);
            throw e;
        }
    }
    
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
                    // 清理安全上下文
                    SecurityContextHolder.clearContext();
                });
                
                emitter.onCompletion(() -> {
                    // logger.debug("SSE connection completed for user: {}", user.getUsername());
                    // 清理安全上下文
                    SecurityContextHolder.clearContext();
                });
                
                // 获取StreamAiService的代理对象以支持事务
                StreamAiService self = applicationContext.getBean(StreamAiService.class);
                
                // 在新事务中处理会话和用户消息
                ChatSession session = self.processSessionInNewTransaction(request.getSessionId(), user, request.getMessage());
                
                // 处理角色扮演世界初始化
                handleRoleplayInitialization(request, session, user);
                
                // 构建对话历史（包含角色扮演上下文）
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
                            
                            // 处理技能动作（仅在角色扮演模式下）
                            String finalResponse = fullResponse;
                            if (session.getWorldType() != null && !"general".equals(session.getWorldType())) {
                                try {
                                    // 解析和执行技能指令
                                    List<SkillActionService.SkillAction> skillActions = 
                                        skillActionService.parseSkillActions(fullResponse, session.getSessionId());
                                    
                                    if (!skillActions.isEmpty()) {
                                        // 执行技能动作
                                        String actionResults = skillActionService.executeSkillActions(skillActions);
                                        
                                        // 清理AI回复中的技能指令
                                        String cleanedResponse = skillActionService.cleanupSkillInstructions(fullResponse);
                                        
                                        // 合并清理后的回复和执行结果
                                        finalResponse = cleanedResponse + actionResults;
                                        
                                        // 记录重要事件到记忆系统
                                        for (SkillActionService.SkillAction action : skillActions) {
                                            if ("DICE".equals(action.getType()) || "QUEST".equals(action.getType())) {
                                                memoryService.recordImportantEvent(session.getSessionId(), 
                                                    "执行了" + action.getType() + "动作", fullResponse);
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    logger.error("处理技能动作失败: sessionId={}", session.getSessionId(), e);
                                }
                            }
                            
                            self.saveAiMessageInNewTransaction(session, finalResponse);
                            
                            // 如果是新会话的第一条消息，更新会话标题
                            // 避免懒加载异常，通过数据库查询获取消息数量
                            try {
                                ChatSession refreshedSession = chatSessionService.getSessionWithMessages(session.getSessionId());
                                if (refreshedSession != null && refreshedSession.getMessages().size() <= 2) { // 用户消息 + AI回复
                                    String title = request.getMessage().length() > 15 ? 
                                        request.getMessage().substring(0, 15) + "..." : 
                                        request.getMessage();
                                    self.updateSessionTitleInNewTransaction(session, title);
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
                        } catch (Exception e) {
                            // 包含 IllegalStateException: emitter 已完成 的场景，忽略
                            // logger.debug("Emitter already completed, skip sending error event");
                        }
                        try {
                            emitter.completeWithError(error);
                        } catch (Exception ignore) {
                        }
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
            } finally {
                // 确保清理安全上下文
                SecurityContextHolder.clearContext();
            }
        }, executorService);
        
        return emitter;
    }
    
    /**
     * 处理角色扮演流式聊天请求
     */
    public SseEmitter handleRoleplayStreamChat(com.qncontest.dto.RoleplayRequest request, User user) {
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
                    // 清理安全上下文
                    SecurityContextHolder.clearContext();
                });
                
                emitter.onCompletion(() -> {
                    // logger.debug("SSE connection completed for user: {}", user.getUsername());
                    // 清理安全上下文
                    SecurityContextHolder.clearContext();
                });
                
                // 获取StreamAiService的代理对象以支持事务
                StreamAiService self = applicationContext.getBean(StreamAiService.class);
                
                // 在新事务中处理会话和用户消息
                ChatSession session = self.processSessionInNewTransaction(request.getSessionId(), user, request.getMessage());
                
                // 处理角色扮演世界初始化
                handleRoleplayInitializationDirect(request, session, user);
                
                // 构建对话历史（包含角色扮演上下文）
                List<dev.langchain4j.data.message.ChatMessage> messages = buildRoleplayHistory(request, session);

                // 用于累积AI回复内容
                StringBuilder aiResponseBuilder = new StringBuilder();

                // DM评估相关变量
                boolean isDMAwareMode = request.getWorldType() != null && !"general".equals(request.getWorldType());
                if (isDMAwareMode) {
                    // logger.info("启用DM评估模式: sessionId={}, worldType={}", session.getSessionId(), request.getWorldType());
                }

                // 创建流式响应处理器
                StreamingResponseHandler<AiMessage> handler = new StreamingResponseHandler<AiMessage>() {
                    @Override
                    public void onNext(String token) {
                        try {
                            SecurityContextHolder.getContext().setAuthentication(authentication);

                            // logger.debug("📦 收到AI流式数据块: length={}, content={}", 
                            //            token.length(), token.length() > 50 ? token.substring(0, 50) + "..." : token);

                            // 直接转发所有内容到前端，不做任何截取处理
                            aiResponseBuilder.append(token);
                            String eventData = String.format("{\"content\":\"%s\"}", escapeJsonString(token));
                            emitter.send(SseEmitter.event().name("message").data(eventData));
                            
                            // logger.debug("📤 已发送数据块到前端，累积长度: {}", aiResponseBuilder.length());

                        } catch (IOException e) {
                            logger.error("❌ 发送SSE数据失败", e);
                            emitter.completeWithError(e);
                        }
                    }
                    
                    @Override
                    public void onComplete(Response<AiMessage> response) {
                        try {
                            SecurityContextHolder.getContext().setAuthentication(authentication);

                            // logger.info("🏁 AI流式响应完成: sessionId={}", session.getSessionId());
                            
                            String fullResponse = aiResponseBuilder.toString();
                            if (fullResponse.isEmpty() && response != null && response.content() != null) {
                                fullResponse = response.content().text();
                            }
                            
                            // logger.debug("📊 完整响应统计: length={}, isEmpty={}", 
                            //            fullResponse.length(), fullResponse.isEmpty());
                            // logger.debug("📝 响应内容预览: {}", 
                            //            fullResponse.length() > 200 ? fullResponse.substring(0, 200) + "..." : fullResponse);

                            // DM模式：从完整响应中提取评估内容
                            if (isDMAwareMode) {
                                // logger.info("🎯 DM模式完成处理，从完整响应中提取评估内容");

                                try {
                                    DMAssessment assessment = assessmentExtractor.extractAssessment(fullResponse);
                                    if (assessment != null) {
                                        // logger.info("✅ 提取到DM评估结果: strategy={}, score={}, notes={}",
                                        //            assessment.getStrategy(), assessment.getOverallScore(),
                                        //            assessment.getAssessmentNotes());
                                        applyAssessmentToWorld(session, assessment);
                                    } else {
                                        // logger.debug("❌ 未找到评估内容");
                                    }
                                } catch (Exception ex) {
                                    logger.error("❌ 提取或处理评估内容失败: sessionId={}", session.getSessionId(), ex);
                                }

                                // logger.info("🏁 DM评估模式处理完成");
                            }

                            // 处理技能动作
                            String finalResponse = fullResponse;
                            try {
                                List<SkillActionService.SkillAction> skillActions =
                                    skillActionService.parseSkillActions(fullResponse, session.getSessionId());

                                if (!skillActions.isEmpty()) {
                                    String actionResults = skillActionService.executeSkillActions(skillActions);
                                    String cleanedResponse = skillActionService.cleanupSkillInstructions(fullResponse);
                                    finalResponse = cleanedResponse + actionResults;

                                    for (SkillActionService.SkillAction action : skillActions) {
                                        if ("DICE".equals(action.getType()) || "QUEST".equals(action.getType())) {
                                            memoryService.recordImportantEvent(session.getSessionId(),
                                                "执行了" + action.getType() + "动作", fullResponse);
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                logger.error("处理技能动作失败: sessionId={}", session.getSessionId(), e);
                            }

                            self.saveAiMessageInNewTransaction(session, finalResponse);

                            // 更新会话标题
                            try {
                                ChatSession refreshedSession = chatSessionService.getSessionWithMessages(session.getSessionId());
                                if (refreshedSession != null && refreshedSession.getMessages().size() <= 2) {
                                    String title = request.getMessage().length() > 15 ?
                                        request.getMessage().substring(0, 15) + "..." :
                                        request.getMessage();
                                    self.updateSessionTitleInNewTransaction(session, title);
                                }
                            } catch (Exception e) {
                                logger.warn("Failed to update session title", e);
                            }

                            emitter.send(SseEmitter.event().name("complete").data("[DONE]"));
                            emitter.complete();

                        } catch (Exception e) {
                            logger.error("Error in AI response completion", e);
                            emitter.completeWithError(e);
                        }
                    }

                    @Override
                    public void onError(Throwable error) {
                        logger.error("❌ AI流式响应错误: sessionId={}, error={}", session.getSessionId(), error.getMessage());
                        // logger.debug("❌ 错误详情: ", error);
                        
                        // 检查是否是内容安全错误
                        boolean isContentSafetyError = error.getMessage() != null && 
                            (error.getMessage().contains("inappropriate content") || 
                             error.getMessage().contains("DataInspectionFailed"));
                        
                        // logger.debug("🔍 错误类型检查: isContentSafetyError={}, errorMessage={}", 
                        //            isContentSafetyError, error.getMessage());
                        
                        if (isContentSafetyError) {
                            logger.warn("🛡️ AI内容安全过滤器触发，尝试使用简化提示词重试: sessionId={}", session.getSessionId());
                            
                            // 使用简化的提示词重试
                            try {
                                String simplifiedPrompt = roleplayPromptEngine.buildQuickPrompt(request.getWorldType(), request.getMessage());
                                List<ChatMessage> simplifiedMessages = List.of(
                                    new SystemMessage(simplifiedPrompt),
                                    new UserMessage(request.getMessage())
                                );
                                
                                // logger.info("🔄 使用简化提示词重试: sessionId={}, promptLength={}", 
                                //            session.getSessionId(), simplifiedPrompt.length());
                                // logger.debug("📝 简化提示词内容: {}", simplifiedPrompt);
                                
                                // 打印简化模式的提示词
                                logger.info("=== 简化模式提示词 ===");
                                for (int i = 0; i < simplifiedMessages.size(); i++) {
                                    dev.langchain4j.data.message.ChatMessage msg = simplifiedMessages.get(i);
                                    String role = msg.getClass().getSimpleName();
                                    String content = msg.text();
                                    logger.info("简化消息 {} - 角色: {}, 长度: {}", i + 1, role, content.length());
                                    logger.info("简化内容: {}", content);
                                    logger.info("---");
                                }
                                logger.info("=== 简化模式提示词结束 ===");
                                
                                // 重新调用AI模型
                                streamingChatLanguageModel.generate(simplifiedMessages, new StreamingResponseHandler<AiMessage>() {
                                    @Override
                                    public void onNext(String token) {
                                        try {
                                            // logger.debug("🔄 简化模式收到数据块: length={}, content={}", 
                                            //            token.length(), token.length() > 30 ? token.substring(0, 30) + "..." : token);
                                            emitter.send(SseEmitter.event()
                                                .name("content")
                                                .data("{\"content\":\"" + token.replace("\"", "\\\"") + "\"}"));
                                        } catch (IOException e) {
                                            logger.error("❌ 简化模式发送数据块失败", e);
                                        }
                                    }

                                    @Override
                                    public void onComplete(Response<AiMessage> response) {
                                        try {
                                            SecurityContextHolder.getContext().setAuthentication(authentication);
                                            
                                            // logger.info("🏁 简化模式AI响应完成: sessionId={}", session.getSessionId());
                                            
                                            String fullResponse = aiResponseBuilder.toString();
                                            if (fullResponse.isEmpty() && response != null && response.content() != null) {
                                                fullResponse = response.content().text();
                                            }
                                            
                                            // logger.debug("📊 简化模式完整响应: length={}", fullResponse.length());
                                            
                                            self.saveAiMessageInNewTransaction(session, fullResponse);
                                            
                                            emitter.send(SseEmitter.event().name("complete").data("[DONE]"));
                                            emitter.complete();
                                            
                                            // logger.info("✅ 简化模式处理完成: sessionId={}", session.getSessionId());
                                        } catch (Exception e) {
                                            logger.error("❌ 简化模式AI响应完成失败: sessionId={}", session.getSessionId(), e);
                                            emitter.completeWithError(e);
                                        }
                                    }

                                    @Override
                                    public void onError(Throwable retryError) {
                                        logger.error("❌ 简化模式AI响应流错误: sessionId={}, error={}", 
                                                   session.getSessionId(), retryError.getMessage());
                                        try {
                                            emitter.send(SseEmitter.event()
                                                .name("error")
                                                .data("{\"error\":\"AI服务暂时不可用，请稍后重试\"}"));
                                        } catch (IOException e) {
                                            logger.error("❌ 简化模式发送错误事件失败", e);
                                        }
                                        emitter.completeWithError(retryError);
                                    }
                                });
                                
                                return; // 成功重试，直接返回
                                
                            } catch (Exception retryException) {
                                logger.error("Failed to retry with simplified prompt", retryException);
                            }
                        }
                        
                        // 如果重试失败或不是内容安全错误，发送错误消息
                        try {
                            emitter.send(SseEmitter.event()
                                .name("error")
                                .data("{\"error\":\"AI服务暂时不可用，请稍后重试\"}"));
                        } catch (IOException e) {
                            logger.error("Error sending error event", e);
                        }
                        emitter.completeWithError(error);

                        // DM模式：错误处理完成
                        if (isDMAwareMode) {
                            // logger.info("DM模式错误处理完成");
                        }
                    }
                };

                // 打印发送给大模型的提示词
                logger.info("=== 发送给大模型的提示词 ===");
                for (int i = 0; i < messages.size(); i++) {
                    dev.langchain4j.data.message.ChatMessage msg = messages.get(i);
                    String role = msg.getClass().getSimpleName();
                    String content = msg.text();
                    logger.info("消息 {} - 角色: {}, 长度: {}", i + 1, role, content.length());
                    logger.info("内容: {}", content);
                    logger.info("---");
                }
                logger.info("=== 提示词结束 ===");

                // 调用AI模型
                streamingChatLanguageModel.generate(messages, handler);

            } catch (Exception e) {
                logger.error("Error in roleplay stream chat processing", e);
                try {
                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data("{\"error\":\"处理请求时发生错误\"}"));
                } catch (IOException ioException) {
                    logger.error("Error sending error event", ioException);
                }
                emitter.completeWithError(e);
            } finally {
                // 确保清理安全上下文
                SecurityContextHolder.clearContext();
            }
        }, executorService);
        
        return emitter;
    }
    
    /**
     * 处理角色扮演世界初始化（直接版本）
     */
    private void handleRoleplayInitializationDirect(com.qncontest.dto.RoleplayRequest request, ChatSession session, User user) {
        String worldType = request.getWorldType();
        if (worldType != null && !"general".equals(worldType) && 
            (session.getWorldType() == null || "general".equals(session.getWorldType()))) {
            
            logger.info("初始化角色扮演世界: sessionId={}, worldType={}", session.getSessionId(), worldType);
            
            try {
                String godModeRules = request.getGodModeRules();
                roleplayWorldService.initializeRoleplaySession(session.getSessionId(), worldType, godModeRules, user);
            } catch (Exception e) {
                // 如果初始化失败（比如重复初始化），记录警告但不中断流程
                logger.warn("角色扮演世界初始化失败（可能已初始化过）: sessionId={}, worldType={}, error={}", 
                           session.getSessionId(), worldType, e.getMessage());
            }
        }
    }
    
    /**
     * 构建角色扮演聊天历史
     */
    private List<dev.langchain4j.data.message.ChatMessage> buildRoleplayHistory(com.qncontest.dto.RoleplayRequest request, ChatSession session) {
        List<dev.langchain4j.data.message.ChatMessage> messages = new ArrayList<>();
        
        // logger.info("🚀 开始构建角色扮演消息历史: sessionId={}, worldType={}, messageLength={}", 
        //            session.getSessionId(), request.getWorldType(), request.getMessage().length());
        
        // 添加系统消息（包含角色扮演上下文）
        String systemPrompt = buildRoleplaySystemPrompt(request, session);
        messages.add(new SystemMessage(systemPrompt));
        // logger.debug("📝 添加系统消息，长度: {}", systemPrompt.length());
        
        // 添加历史消息
        if (request.getHistory() != null && !request.getHistory().isEmpty()) {
            // logger.debug("📚 添加历史消息，数量: {}", request.getHistory().size());
            for (com.qncontest.dto.RoleplayRequest.ChatHistoryMessage historyMsg : request.getHistory()) {
                if ("user".equals(historyMsg.getRole())) {
                    messages.add(new UserMessage(historyMsg.getContent()));
                    // logger.debug("👤 添加用户历史消息，长度: {}", historyMsg.getContent().length());
                } else if ("assistant".equals(historyMsg.getRole())) {
                    messages.add(new AiMessage(historyMsg.getContent()));
                    // logger.debug("🤖 添加AI历史消息，长度: {}", historyMsg.getContent().length());
                }
            }
        } else {
            // logger.debug("📚 无历史消息");
        }
        
        // 添加当前用户消息
        messages.add(new UserMessage(request.getMessage()));
        // logger.debug("💬 添加当前用户消息，长度: {}", request.getMessage().length());
        // logger.debug("📊 总消息数量: {}", messages.size());
        
        return messages;
    }
    
    /**
     * 构建角色扮演系统提示词
     */
    private String buildRoleplaySystemPrompt(com.qncontest.dto.RoleplayRequest request, ChatSession session) {
        String worldType = request.getWorldType();
        if (worldType != null && !"general".equals(worldType)) {
            // 使用智能角色扮演提示引擎
            RoleplayPromptEngine.RoleplayContext context = new RoleplayPromptEngine.RoleplayContext(worldType, session.getSessionId());
            context.setCurrentMessage(request.getMessage());
            context.setWorldState(session.getWorldState());
            context.setSkillsState(session.getSkillsState());
            context.setGodModeRules(session.getGodModeRules());
            context.setSession(session);

            // 使用DM评估模式提示词
            String dmPrompt = promptEngine.buildDMAwarePrompt(context);
            // logger.debug("构建DM评估模式提示词，长度: {}", dmPrompt.length());
            return dmPrompt;
        } else {
            // 普通聊天模式
            String basePrompt = request.getSystemPrompt();
            if (basePrompt == null || basePrompt.trim().isEmpty()) {
                basePrompt = aiProperties.getDefaultSystemPrompt();
            }
            return basePrompt;
        }
    }
    
    /**
     * 处理角色扮演世界初始化
     */
    private void handleRoleplayInitialization(ChatRequest request, ChatSession session, User user) {
        // 检查是否为角色扮演模式且需要初始化
        String worldType = getWorldTypeFromRequest(request);
        if (worldType != null && !"general".equals(worldType) && 
            (session.getWorldType() == null || "general".equals(session.getWorldType()))) {
            
            // logger.info("初始化角色扮演世界: sessionId={}, worldType={}", session.getSessionId(), worldType);
            
            String godModeRules = getGodModeRulesFromRequest(request);
            roleplayWorldService.initializeRoleplaySession(session.getSessionId(), worldType, godModeRules, user);
        }
    }
    
    /**
     * 构建聊天历史
     */
    private List<dev.langchain4j.data.message.ChatMessage> buildChatHistory(ChatRequest request, ChatSession session) {
        List<dev.langchain4j.data.message.ChatMessage> messages = new ArrayList<>();
        
        // 添加系统消息（包含角色扮演上下文）
        String systemPrompt = buildSystemPrompt(request, session);
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
     * 构建系统提示词（包含角色扮演上下文）
     */
    private String buildSystemPrompt(ChatRequest request, ChatSession session) {
        // 检查是否为角色扮演模式
        String worldType = session.getWorldType();
        if (worldType != null && !"general".equals(worldType)) {
            // 使用智能角色扮演提示引擎
            RoleplayPromptEngine.RoleplayContext context = new RoleplayPromptEngine.RoleplayContext(worldType, session.getSessionId());
            context.setCurrentMessage(request.getMessage());
            context.setWorldState(session.getWorldState());
            context.setSkillsState(session.getSkillsState());
            context.setGodModeRules(session.getGodModeRules());
            context.setSession(session);
            
            return promptEngine.buildLayeredPrompt(context);
        } else {
            // 普通聊天模式
            String basePrompt = request.getSystemPrompt();
            if (basePrompt == null || basePrompt.trim().isEmpty()) {
                basePrompt = aiProperties.getDefaultSystemPrompt();
            }
            return basePrompt;
        }
    }
    
    /**
     * 从请求中获取世界类型
     */
    private String getWorldTypeFromRequest(ChatRequest request) {
        // 对于角色扮演请求，worldType应该从会话中获取
        // 这里返回null，让buildSystemPrompt从session中获取
        return null;
    }
    
    /**
     * 从请求中获取上帝模式规则
     */
    private String getGodModeRulesFromRequest(ChatRequest request) {
        // 对于角色扮演请求，godModeRules应该从会话中获取
        // 这里返回null，让buildSystemPrompt从session中获取
        return null;
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

    /**
     * 根据评估结果更新会话中的世界状态（任务移动/完成、进度与提示）
     */
    private void applyAssessmentToWorld(ChatSession session, com.qncontest.entity.DMAssessment assessment) {
        try {
            // 使用新的结构化字段
            com.fasterxml.jackson.databind.node.ArrayNode activeQuests = parseJsonArray(session.getActiveQuests());
            com.fasterxml.jackson.databind.node.ArrayNode completedQuests = parseJsonArray(session.getCompletedQuests());
            com.fasterxml.jackson.databind.node.ObjectNode characterStats = parseJsonObject(session.getCharacterStats());
            com.fasterxml.jackson.databind.node.ArrayNode assessmentHistory = parseJsonArray(session.getAssessmentHistory());
            
            // 记录当前评估到历史中
            com.fasterxml.jackson.databind.node.ObjectNode assessmentRecord = objectMapper.createObjectNode();
            assessmentRecord.put("assessmentId", assessment.getId());
            assessmentRecord.put("assessedAt", assessment.getAssessedAt().toString());
            assessmentRecord.put("overallScore", assessment.getOverallScore());
            assessmentRecord.put("strategy", assessment.getStrategy().toString());
            assessmentRecord.put("userAction", assessment.getUserAction());
            assessmentHistory.add(assessmentRecord);
            
            // 更新收敛进度
            double currentProgress = session.getConvergenceProgress() != null ? session.getConvergenceProgress() : 0.0;
            double newProgress = Math.min(1.0, currentProgress + (assessment.getConvergenceProgress() != null ? assessment.getConvergenceProgress() * 0.1 : 0.0));
            session.setConvergenceProgress(newProgress);

            // 处理任务更新
            if (assessment.getQuestUpdates() != null) {
                try {
                    String questUpdatesJson = objectMapper.writeValueAsString(assessment.getQuestUpdates());
                    com.fasterxml.jackson.databind.JsonNode questUpdates = objectMapper.readTree(questUpdatesJson);
                    if (questUpdates.isObject()) {
                        
                        // 处理新创建的任务
                        if (questUpdates.has("created") && questUpdates.get("created").isArray()) {
                            com.fasterxml.jackson.databind.node.ArrayNode createdQuests = (com.fasterxml.jackson.databind.node.ArrayNode) questUpdates.get("created");
                            for (com.fasterxml.jackson.databind.JsonNode newQuest : createdQuests) {
                                if (newQuest.has("questId") && newQuest.has("title")) {
                                    com.fasterxml.jackson.databind.node.ObjectNode questObj = objectMapper.createObjectNode();
                                    questObj.put("questId", newQuest.get("questId").asText());
                                    questObj.put("title", newQuest.get("title").asText());
                                    questObj.put("description", newQuest.has("description") ? newQuest.get("description").asText() : "");
                                    questObj.put("status", "ACTIVE");
                                    questObj.put("createdAt", java.time.LocalDateTime.now().toString());
                                    
                                    if (newQuest.has("rewards")) {
                                        questObj.set("rewards", newQuest.get("rewards"));
                                    }
                                    
                                    activeQuests.add(questObj);
                                    // logger.info("创建新任务: questId={}, title={}", questObj.get("questId").asText(), questObj.get("title").asText());
                                }
                            }
                        }
                        
                        // 处理已完成的任务
                        if (questUpdates.has("completed") && questUpdates.get("completed").isArray()) {
                            com.fasterxml.jackson.databind.node.ArrayNode completedQuestsFromUpdate = (com.fasterxml.jackson.databind.node.ArrayNode) questUpdates.get("completed");
                            for (com.fasterxml.jackson.databind.JsonNode quest : completedQuestsFromUpdate) {
                                if (quest.has("questId")) {
                                    String questId = quest.get("questId").asText();
                                    // 从活跃任务中移除
                                    for (int i = 0; i < activeQuests.size(); i++) {
                                        if (activeQuests.get(i).has("questId") && questId.equals(activeQuests.get(i).get("questId").asText())) {
                                            com.fasterxml.jackson.databind.JsonNode completedQuest = activeQuests.get(i);
                                            activeQuests.remove(i);
                                            
                                            // 添加完成时间戳和状态
                                            if (completedQuest.isObject()) {
                                                ((com.fasterxml.jackson.databind.node.ObjectNode) completedQuest)
                                                    .put("completedAt", java.time.LocalDateTime.now().toString())
                                                    .put("status", "COMPLETED");
                                                
                                                // 处理奖励
                                                if (quest.has("rewards")) {
                                                    applyQuestRewards(characterStats, quest.get("rewards"));
                                                    ((com.fasterxml.jackson.databind.node.ObjectNode) completedQuest).set("rewards", quest.get("rewards"));
                                                }
                                            }
                                            completedQuests.add(completedQuest);
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                        
                        // 处理进度更新的任务
                        if (questUpdates.has("progress") && questUpdates.get("progress").isArray()) {
                            com.fasterxml.jackson.databind.node.ArrayNode progressQuests = (com.fasterxml.jackson.databind.node.ArrayNode) questUpdates.get("progress");
                            for (com.fasterxml.jackson.databind.JsonNode progressQuest : progressQuests) {
                                if (progressQuest.has("questId") && progressQuest.has("progress")) {
                                    String questId = progressQuest.get("questId").asText();
                                    String progress = progressQuest.get("progress").asText();
                                    
                                    // 更新活跃任务中的进度
                                    for (int i = 0; i < activeQuests.size(); i++) {
                                        if (activeQuests.get(i).has("questId") && questId.equals(activeQuests.get(i).get("questId").asText())) {
                                            if (activeQuests.get(i).isObject()) {
                                                ((com.fasterxml.jackson.databind.node.ObjectNode) activeQuests.get(i)).put("progress", progress);
                                            }
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                        
                        // 处理过期的任务
                        if (questUpdates.has("expired") && questUpdates.get("expired").isArray()) {
                            com.fasterxml.jackson.databind.node.ArrayNode expiredQuests = (com.fasterxml.jackson.databind.node.ArrayNode) questUpdates.get("expired");
                            for (com.fasterxml.jackson.databind.JsonNode expiredQuest : expiredQuests) {
                                if (expiredQuest.has("questId")) {
                                    String questId = expiredQuest.get("questId").asText();
                                    String reason = expiredQuest.has("reason") ? expiredQuest.get("reason").asText() : "任务过期";
                                    
                                    // 从活跃任务中移除过期任务
                                    for (int i = 0; i < activeQuests.size(); i++) {
                                        if (activeQuests.get(i).has("questId") && questId.equals(activeQuests.get(i).get("questId").asText())) {
                                            com.fasterxml.jackson.databind.JsonNode expiredTask = activeQuests.get(i);
                                            activeQuests.remove(i);
                                            
                                            // 记录过期信息（可选：保存到已完成任务中作为历史记录）
                                            if (expiredTask.isObject()) {
                                                ((com.fasterxml.jackson.databind.node.ObjectNode) expiredTask)
                                                    .put("expiredAt", java.time.LocalDateTime.now().toString())
                                                    .put("status", "EXPIRED")
                                                    .put("expiredReason", reason);
                                                
                                                // 将过期任务移到已完成任务中（作为历史记录）
                                                completedQuests.add(expiredTask);
                                            }
                                            // logger.info("任务过期: questId={}, reason={}", questId, reason);
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.warn("处理questUpdates失败: {}", e.getMessage());
                }
            }

            // 更新会话状态
            session.setActiveQuests(objectMapper.writeValueAsString(activeQuests));
            session.setCompletedQuests(objectMapper.writeValueAsString(completedQuests));
            session.setCharacterStats(objectMapper.writeValueAsString(characterStats));
            session.setAssessmentHistory(objectMapper.writeValueAsString(assessmentHistory));
            session.setLastAssessmentId(assessment.getId());
            
            // 处理世界状态更新
            String worldStateJson = session.getWorldState();
            com.fasterxml.jackson.databind.node.ObjectNode worldObj = parseJsonObject(worldStateJson);
            
            if (assessment.getWorldStateUpdates() != null) {
                try {
                    com.fasterxml.jackson.databind.JsonNode worldUpdates;
                    if (assessment.getWorldStateUpdates() instanceof String) {
                        worldUpdates = objectMapper.readTree((String) assessment.getWorldStateUpdates());
                    } else {
                        worldUpdates = objectMapper.valueToTree(assessment.getWorldStateUpdates());
                    }
                    if (worldUpdates.isObject()) {
                        // 更新当前位置
                        if (worldUpdates.has("currentLocation")) {
                            worldObj.put("currentLocation", worldUpdates.get("currentLocation").asText());
                        }
                        
                        // 更新环境状态
                        if (worldUpdates.has("environment")) {
                            worldObj.put("environment", worldUpdates.get("environment").asText());
                        }
                        
                        // 更新NPC状态
                        if (worldUpdates.has("npcs") && worldUpdates.get("npcs").isArray()) {
                            worldObj.set("npcs", worldUpdates.get("npcs"));
                        }
                        
                        // 更新世界事件
                        if (worldUpdates.has("worldEvents") && worldUpdates.get("worldEvents").isArray()) {
                            worldObj.set("worldEvents", worldUpdates.get("worldEvents"));
                        }
                        
                        // 更新势力关系
                        if (worldUpdates.has("factions") && worldUpdates.get("factions").isObject()) {
                            worldObj.set("factions", worldUpdates.get("factions"));
                        }
                        
                        // 更新地点状态
                        if (worldUpdates.has("locations") && worldUpdates.get("locations").isObject()) {
                            worldObj.set("locations", worldUpdates.get("locations"));
                        }
                    }
                } catch (Exception e) {
                    logger.warn("处理worldStateUpdates失败: {}", e.getMessage());
                }
            }
            
            // 记录最近的评估建议与提示
            if (assessment.getSuggestedActions() != null) {
                try {
                    String actionsJson = objectMapper.writeValueAsString(assessment.getSuggestedActions());
                    com.fasterxml.jackson.databind.JsonNode actions = objectMapper.readTree(actionsJson);
                    if (actions.isArray()) worldObj.set("recentSuggestedActions", actions);
                } catch (Exception ignore) {}
            }
            if (assessment.getConvergenceHints() != null) {
                try {
                    String hintsJson = objectMapper.writeValueAsString(assessment.getConvergenceHints());
                    com.fasterxml.jackson.databind.JsonNode hints = objectMapper.readTree(hintsJson);
                    if (hints.isArray()) worldObj.set("recentConvergenceHints", hints);
                } catch (Exception ignore) {}
            }
            
            session.setWorldState(objectMapper.writeValueAsString(worldObj));
            
            // 处理技能状态更新
            if (assessment.getSkillsStateUpdates() != null) {
                try {
                    com.fasterxml.jackson.databind.JsonNode skillsUpdates;
                    if (assessment.getSkillsStateUpdates() instanceof String) {
                        skillsUpdates = objectMapper.readTree((String) assessment.getSkillsStateUpdates());
                    } else {
                        skillsUpdates = objectMapper.valueToTree(assessment.getSkillsStateUpdates());
                    }
                    if (skillsUpdates.isObject()) {
                        // 更新角色等级
                        if (skillsUpdates.has("level")) {
                            characterStats.put("level", skillsUpdates.get("level").asInt());
                        }
                        
                        // 更新经验值
                        if (skillsUpdates.has("experience")) {
                            characterStats.put("experience", skillsUpdates.get("experience").asInt());
                        }
                        
                        // 更新金币
                        if (skillsUpdates.has("gold")) {
                            characterStats.put("gold", skillsUpdates.get("gold").asInt());
                        }
                        
                        // 更新物品清单
                        if (skillsUpdates.has("inventory") && skillsUpdates.get("inventory").isArray()) {
                            characterStats.set("inventory", skillsUpdates.get("inventory"));
                        }
                        
                        // 更新技能/能力
                        if (skillsUpdates.has("abilities") && skillsUpdates.get("abilities").isArray()) {
                            characterStats.set("abilities", skillsUpdates.get("abilities"));
                        }
                        
                        // 更新属性
                        if (skillsUpdates.has("stats") && skillsUpdates.get("stats").isObject()) {
                            characterStats.set("stats", skillsUpdates.get("stats"));
                        }
                        
                        // 更新人际关系
                        if (skillsUpdates.has("relationships") && skillsUpdates.get("relationships").isObject()) {
                            characterStats.set("relationships", skillsUpdates.get("relationships"));
                        }
                    }
                } catch (Exception e) {
                    logger.warn("处理skillsStateUpdates失败: {}", e.getMessage());
                }
            }

            // 持久化到数据库
            chatSessionRepository.save(session);
        } catch (Exception e) {
            logger.error("applyAssessmentToWorld失败: sessionId={}", session.getSessionId(), e);
        }
    }

    /**
     * 解析JSON数组，如果解析失败返回空数组
     */
    private com.fasterxml.jackson.databind.node.ArrayNode parseJsonArray(String jsonString) {
        if (jsonString == null || jsonString.isBlank()) {
            return objectMapper.createArrayNode();
        }
        try {
            com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(jsonString);
            if (node.isArray()) {
                return (com.fasterxml.jackson.databind.node.ArrayNode) node;
            }
        } catch (Exception e) {
            logger.warn("解析JSON数组失败: {}", e.getMessage());
        }
        return objectMapper.createArrayNode();
    }
    
    /**
     * 解析JSON对象，如果解析失败返回空对象
     */
    private com.fasterxml.jackson.databind.node.ObjectNode parseJsonObject(String jsonString) {
        if (jsonString == null || jsonString.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(jsonString);
            if (node.isObject()) {
                return (com.fasterxml.jackson.databind.node.ObjectNode) node;
            }
        } catch (Exception e) {
            logger.warn("解析JSON对象失败: {}", e.getMessage());
        }
        return objectMapper.createObjectNode();
    }

    /**
     * 应用任务奖励到角色属性
     */
    private void applyQuestRewards(com.fasterxml.jackson.databind.node.ObjectNode characterStats, com.fasterxml.jackson.databind.JsonNode rewards) {
        try {
            // 经验奖励
            if (rewards.has("exp")) {
                int expGain = rewards.get("exp").asInt();
                int currentExp = characterStats.has("experience") ? characterStats.get("experience").asInt() : 0;
                int newExp = currentExp + expGain;
                characterStats.put("experience", newExp);
                
                // 简单的等级计算（每100经验升一级）
                int currentLevel = characterStats.has("level") ? characterStats.get("level").asInt() : 1;
                int newLevel = Math.max(currentLevel, (newExp / 100) + 1);
                if (newLevel > currentLevel) {
                    characterStats.put("level", newLevel);
                    logger.info("角色升级: {} -> {}", currentLevel, newLevel);
                }
            }
            
            // 金币奖励
            if (rewards.has("gold")) {
                int goldGain = rewards.get("gold").asInt();
                int currentGold = characterStats.has("gold") ? characterStats.get("gold").asInt() : 0;
                characterStats.put("gold", currentGold + goldGain);
            }
            
            // 物品奖励
            if (rewards.has("items") && rewards.get("items").isArray()) {
                com.fasterxml.jackson.databind.node.ArrayNode inventory = characterStats.has("inventory") && characterStats.get("inventory").isArray()
                    ? (com.fasterxml.jackson.databind.node.ArrayNode) characterStats.get("inventory")
                    : objectMapper.createArrayNode();
                
                for (com.fasterxml.jackson.databind.JsonNode item : rewards.get("items")) {
                    inventory.add(item.asText());
                }
                characterStats.set("inventory", inventory);
            }
            
            // 记录最后任务奖励
            characterStats.set("lastQuestRewards", rewards);
        } catch (Exception e) {
            logger.warn("应用任务奖励失败: {}", e.getMessage());
        }
    }
}

