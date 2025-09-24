package com.qncontest.service;

import com.qncontest.entity.ChatMessage;
import com.qncontest.entity.ChatSession;
import com.qncontest.entity.User;
import com.qncontest.repository.ChatMessageRepository;
import com.qncontest.repository.ChatSessionRepository;
import com.qncontest.dto.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ChatSessionService {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatSessionService.class);
    
    @Autowired
    private ChatSessionRepository chatSessionRepository;
    
    @Autowired
    private ChatMessageRepository chatMessageRepository;
    
    /**
     * 获取用户的所有聊天会话
     */
    @Transactional(readOnly = true)
    public List<ChatResponse.SessionInfo> getUserSessions(User user) {
        logger.info("Fetching sessions for user: {} (ID: {})", user.getUsername(), user.getId());
        
        // 使用预加载消息的查询，避免N+1问题
        List<ChatSession> sessions = chatSessionRepository.findByUserWithMessages(user);
        logger.info("Found {} raw sessions from database", sessions.size());
        
        List<ChatResponse.SessionInfo> sessionInfos = sessions.stream()
                .map(session -> {
                    int messageCount = session.getMessages() != null ? session.getMessages().size() : 0;
                    logger.debug("Session {}: title='{}', messages={}", 
                        session.getSessionId(), session.getTitle(), messageCount);
                    return new ChatResponse.SessionInfo(
                        session.getSessionId(),
                        session.getTitle(),
                        session.getCreatedAt(),
                        session.getUpdatedAt(),
                        messageCount
                    );
                })
                .collect(Collectors.toList());
                
        logger.info("Returning {} session infos", sessionInfos.size());
        return sessionInfos;
    }
    
    /**
     * 获取或创建聊天会话
     */
    @Transactional
    public ChatSession getOrCreateSession(String sessionId, User user) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            // 创建新会话
            sessionId = generateSessionId();
        }
        
        Optional<ChatSession> existingSession = chatSessionRepository.findBySessionIdAndUser(sessionId, user);
        if (existingSession.isPresent()) {
            return existingSession.get();
        }
        
        // 创建新会话
        ChatSession newSession = new ChatSession(sessionId, "新对话", user);
        return chatSessionRepository.save(newSession);
    }
    
    /**
     * 获取会话的所有消息
     */
    @Transactional(readOnly = true)
    public List<ChatResponse.MessageInfo> getSessionMessages(String sessionId, User user) {
        Optional<ChatSession> sessionOpt = chatSessionRepository.findBySessionIdAndUser(sessionId, user);
        if (sessionOpt.isEmpty()) {
            return List.of();
        }
        
        ChatSession session = sessionOpt.get();
        List<ChatMessage> messages = chatMessageRepository.findByChatSessionOrderBySequenceNumberAsc(session);
        
        return messages.stream()
                .map(msg -> new ChatResponse.MessageInfo(
                    msg.getId(),
                    msg.getRole().name().toLowerCase(),
                    msg.getContent(),
                    msg.getSequenceNumber(),
                    msg.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }
    
    /**
     * 保存用户消息
     */
    @Transactional
    public ChatMessage saveUserMessage(ChatSession session, String content) {
        Integer nextSequenceNumber = getNextSequenceNumber(session);
        ChatMessage userMessage = new ChatMessage(session, ChatMessage.MessageRole.USER, content, nextSequenceNumber);
        return chatMessageRepository.save(userMessage);
    }
    
    /**
     * 保存AI消息
     */
    @Transactional
    public ChatMessage saveAiMessage(ChatSession session, String content) {
        Integer nextSequenceNumber = getNextSequenceNumber(session);
        ChatMessage aiMessage = new ChatMessage(session, ChatMessage.MessageRole.ASSISTANT, content, nextSequenceNumber);
        return chatMessageRepository.save(aiMessage);
    }
    
    /**
     * 更新会话标题
     */
    @Transactional
    public void updateSessionTitle(ChatSession session, String title) {
        if (title != null && !title.trim().isEmpty()) {
            session.setTitle(title.length() > 50 ? title.substring(0, 50) + "..." : title);
            chatSessionRepository.save(session);
        }
    }
    
    /**
     * 获取会话及其消息（避免懒加载异常）
     */
    @Transactional(readOnly = true)
    public ChatSession getSessionWithMessages(String sessionId) {
        return chatSessionRepository.findBySessionIdWithMessages(sessionId);
    }
    
    /**
     * 删除会话
     */
    @Transactional
    public boolean deleteSession(String sessionId, User user) {
        Optional<ChatSession> sessionOpt = chatSessionRepository.findBySessionIdAndUser(sessionId, user);
        if (sessionOpt.isEmpty()) {
            return false;
        }
        
        ChatSession session = sessionOpt.get();
        
        // 删除所有消息
        chatMessageRepository.deleteByChatSession(session);
        
        // 删除会话
        chatSessionRepository.delete(session);
        
        logger.info("Deleted chat session {} for user {}", sessionId, user.getUsername());
        return true;
    }
    
    /**
     * 获取会话的历史消息（用于AI上下文）
     */
    @Transactional(readOnly = true)
    public List<ChatMessage> getSessionHistory(ChatSession session, int maxMessages) {
        List<ChatMessage> allMessages = chatMessageRepository.findByChatSessionOrderBySequenceNumberAsc(session);
        
        // 如果消息数量超过限制，只取最近的消息
        if (allMessages.size() > maxMessages) {
            return allMessages.subList(allMessages.size() - maxMessages, allMessages.size());
        }
        
        return allMessages;
    }
    
    /**
     * 获取下一个序列号
     */
    private Integer getNextSequenceNumber(ChatSession session) {
        Integer maxSequenceNumber = chatMessageRepository.findMaxSequenceNumberBySession(session);
        return maxSequenceNumber + 1;
    }
    
    /**
     * 生成会话ID
     */
    private String generateSessionId() {
        return "session_" + System.currentTimeMillis() + "_" + 
               Long.toHexString(System.nanoTime()).substring(0, 8);
    }
}
