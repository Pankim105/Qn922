package com.qncontest.service;

import com.qncontest.entity.ChatMessage;
import com.qncontest.entity.ChatSession;
import com.qncontest.repository.ChatMessageRepository;
import com.qncontest.repository.ChatSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 智能聊天服务 - 支持数据库持久化和智能上下文管理
 */
@Service
@Transactional
public class IntelligentChatService {
    
    private static final Logger log = LoggerFactory.getLogger(IntelligentChatService.class);
    
    // 智能上下文配置
    private static final int MAX_CONTEXT_MESSAGES = 20; // 最大上下文消息数
    private static final int MAX_CONTEXT_TOKENS = 8000; // 最大上下文token数（预留给回复）
    private static final int SUMMARY_TRIGGER_MESSAGES = 50; // 触发摘要的消息数
    
    @Autowired
    private ChatSessionRepository chatSessionRepository;
    
    @Autowired
    private ChatMessageRepository chatMessageRepository;
    
    @Autowired(required = false)
    private LangChainEnhancedService langChainEnhancedService;
    
    /**
     * 获取或创建聊天会话
     */
    public ChatSession getOrCreateSession(String userId, String sessionId) {
        Optional<ChatSession> existingSession = chatSessionRepository.findBySessionIdAndUserId(sessionId, userId);
        
        if (existingSession.isPresent()) {
            ChatSession session = existingSession.get();
            session.setUpdatedAt(LocalDateTime.now());
            return chatSessionRepository.save(session);
        } else {
            ChatSession newSession = new ChatSession(sessionId, userId);
            return chatSessionRepository.save(newSession);
        }
    }
    
    /**
     * 添加消息到会话
     */
    public ChatMessage addMessage(ChatSession session, String role, String content) {
        Integer nextSequence = chatMessageRepository.getMaxSequenceNumberBySession(session) + 1;
        
        ChatMessage message = new ChatMessage(session, role, content);
        message.setSequenceNumber(nextSequence);
        message.setTokens(estimateTokens(content)); // 简单估算token数
        
        ChatMessage savedMessage = chatMessageRepository.save(message);
        
        // 更新会话标题（如果是第一条用户消息）
        if ("user".equals(role) && session.getTitle() == null) {
            session.setTitle(generateTitle(content));
            chatSessionRepository.save(session);
        }
        
        // 检查是否需要摘要压缩
        checkAndTriggerSummary(session);
        
        log.debug("添加消息到会话 {}: {} - {}", session.getSessionId(), role, 
                content.substring(0, Math.min(50, content.length())));
        
        return savedMessage;
    }
    
    /**
     * 获取智能上下文消息（避免token浪费）
     */
    public List<ChatMessage> getIntelligentContext(ChatSession session) {
        // 获取所有最近的消息
        List<ChatMessage> recentMessages = chatMessageRepository.findTopMessagesBySession(
            session, PageRequest.of(0, MAX_CONTEXT_MESSAGES * 2)); // 获取更多消息用于智能筛选
        
        Collections.reverse(recentMessages); // 按时间正序
        
        if (recentMessages.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 如果有LangChain4j服务，使用智能筛选
        if (langChainEnhancedService != null && langChainEnhancedService.isAvailable()) {
            try {
                // 使用最后一条用户消息作为查询上下文
                String lastUserMessage = recentMessages.stream()
                        .filter(msg -> "user".equals(msg.getRole()))
                        .reduce((first, second) -> second)
                        .map(ChatMessage::getContent)
                        .orElse("");
                
                if (!lastUserMessage.isEmpty()) {
                    List<ChatMessage> intelligentSelection = langChainEnhancedService.selectRelevantMessages(
                            lastUserMessage, recentMessages, MAX_CONTEXT_MESSAGES);
                    
                    log.debug("为会话 {} 使用LangChain智能筛选: {} -> {} 条消息", 
                            session.getSessionId(), recentMessages.size(), intelligentSelection.size());
                    
                    return intelligentSelection;
                }
            } catch (Exception e) {
                log.warn("LangChain智能筛选失败，回退到基础策略", e);
            }
        }
        
        // 基础策略: 按token限制裁剪
        List<ChatMessage> contextMessages = new ArrayList<>();
        int totalTokens = 0;
        
        for (ChatMessage message : recentMessages) {
            int messageTokens = message.getTokens() != null ? message.getTokens() : estimateTokens(message.getContent());
            
            if (totalTokens + messageTokens > MAX_CONTEXT_TOKENS && !contextMessages.isEmpty()) {
                break; // 达到token限制
            }
            
            contextMessages.add(message);
            totalTokens += messageTokens;
        }
        
        log.debug("为会话 {} 生成基础上下文: {} 条消息, {} tokens", 
                session.getSessionId(), contextMessages.size(), totalTokens);
        
        return contextMessages;
    }
    
    /**
     * 获取用户的所有会话列表
     */
    public List<ChatSession> getUserSessions(String userId) {
        return chatSessionRepository.findByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(userId);
    }
    
    /**
     * 获取会话的消息数量
     */
    public long getSessionMessageCount(ChatSession session) {
        return chatMessageRepository.countBySession(session);
    }
    
    /**
     * 获取会话的消息列表
     */
    public List<Map<String, Object>> getSessionMessages(String userId, String sessionId) {
        Optional<ChatSession> sessionOpt = chatSessionRepository.findBySessionIdAndUserId(sessionId, userId);
        if (sessionOpt.isEmpty()) {
            throw new RuntimeException("会话不存在或无权访问");
        }
        
        ChatSession session = sessionOpt.get();
        List<ChatMessage> messages = chatMessageRepository.findByChatSessionOrderBySequenceNumberAsc(session);
        
        return messages.stream()
            .map(msg -> {
                Map<String, Object> messageInfo = new HashMap<>();
                messageInfo.put("id", msg.getId());
                messageInfo.put("role", msg.getRole());
                messageInfo.put("content", msg.getContent());
                messageInfo.put("sequenceNumber", msg.getSequenceNumber());
                messageInfo.put("tokens", msg.getTokens());
                messageInfo.put("createdAt", msg.getCreatedAt().toString());
                return messageInfo;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * 删除会话
     */
    public void deleteSession(String userId, String sessionId) {
        Optional<ChatSession> session = chatSessionRepository.findBySessionIdAndUserId(sessionId, userId);
        if (session.isPresent()) {
            ChatSession chatSession = session.get();
            chatSession.setIsActive(false); // 软删除
            chatSessionRepository.save(chatSession);
            log.info("软删除会话: {}", sessionId);
        }
    }
    
    /**
     * 删除用户的所有会话
     */
    public void deleteAllUserSessions(String userId) {
        List<ChatSession> sessions = chatSessionRepository.findByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(userId);
        for (ChatSession session : sessions) {
            session.setIsActive(false);
        }
        chatSessionRepository.saveAll(sessions);
        log.info("软删除用户所有会话: {}", userId);
    }
    
    /**
     * 清理过期会话（定时任务）
     */
    public void cleanupExpiredSessions(int expireHours) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(expireHours);
        List<ChatSession> expiredSessions = chatSessionRepository.findInactiveSessionsBefore(cutoffTime);
        
        for (ChatSession session : expiredSessions) {
            session.setIsActive(false);
        }
        
        chatSessionRepository.saveAll(expiredSessions);
        log.info("清理了 {} 个过期会话", expiredSessions.size());
    }
    
    /**
     * 估算文本的token数量（简单实现）
     */
    private int estimateTokens(String text) {
        if (text == null) return 0;
        // 简单估算：中文字符约1.5个token，英文单词约1个token
        long chineseChars = text.chars().filter(ch -> ch >= 0x4E00 && ch <= 0x9FFF).count();
        long englishWords = text.split("\\s+").length;
        return (int)(chineseChars * 1.5 + englishWords);
    }
    
    /**
     * 生成会话标题
     */
    private String generateTitle(String firstMessage) {
        if (firstMessage.length() > 20) {
            return firstMessage.substring(0, 20) + "...";
        }
        return firstMessage;
    }
    
    /**
     * 检查并触发摘要压缩（高级功能）
     */
    private void checkAndTriggerSummary(ChatSession session) {
        try {
            // 使用数据库查询获取消息数量，避免LazyInitializationException
            long messageCount = chatMessageRepository.countBySession(session);
            
            if (messageCount > SUMMARY_TRIGGER_MESSAGES) {
                if (langChainEnhancedService != null && langChainEnhancedService.isAvailable()) {
                    try {
                        // 使用LangChain4j生成智能摘要
                        if (langChainEnhancedService.shouldCompressConversation(session)) {
                            log.info("会话 {} 触发LangChain智能压缩", session.getSessionId());
                            langChainEnhancedService.compressConversationHistory(session);
                        }
                    } catch (Exception e) {
                        log.error("LangChain摘要压缩失败", e);
                    }
                } else {
                    log.info("会话 {} 消息数过多({}条)，建议配置LangChain4j进行智能压缩", 
                            session.getSessionId(), messageCount);
                }
            }
        } catch (Exception e) {
            log.warn("检查摘要压缩触发条件时出错", e);
        }
    }
}
