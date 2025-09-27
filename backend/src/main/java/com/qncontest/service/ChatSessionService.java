package com.qncontest.service;

import com.qncontest.entity.ChatMessage;
import com.qncontest.entity.ChatSession;
import com.qncontest.entity.User;
import com.qncontest.repository.ChatMessageRepository;
import com.qncontest.repository.ChatSessionRepository;
import com.qncontest.dto.ChatResponse;
import com.qncontest.service.interfaces.ChatSessionManagerInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ChatSessionService implements ChatSessionManagerInterface {
    
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
     * 创建新的聊天会话
     */
    @Transactional
    public ChatSession createSession(User user, String worldType) {
        String sessionId;
        int guard = 0;
        do {
            sessionId = generateSessionId();
            guard++;
        } while (chatSessionRepository.existsById(sessionId) && guard < 5);
        
        logger.debug("创建新的角色扮演会话: sessionId={}, worldType={}, userId={}", 
                    sessionId, worldType, user.getId());
        
        ChatSession newSession = new ChatSession(sessionId, "新对话", user);
        newSession.setWorldType(worldType != null ? worldType : "general");
        
        return chatSessionRepository.save(newSession);
    }
    
    /**
     * 获取或创建聊天会话
     */
    @Transactional
    public ChatSession getOrCreateSession(String sessionId, User user) {
        // 未提供ID则生成全局唯一ID
        if (sessionId == null || sessionId.trim().isEmpty()) {
            String generated;
            int guard = 0;
            do {
                generated = generateSessionId();
                guard++;
            } while (chatSessionRepository.existsById(generated) && guard < 5);
            sessionId = generated;
            logger.debug("生成新的会话ID: {}", sessionId);
        }

        // 如果提供了ID，先全局查询是否已存在（避免不同用户下重复创建导致PK冲突）
        Optional<ChatSession> byId = chatSessionRepository.findById(sessionId);
        if (byId.isPresent()) {
            ChatSession found = byId.get();
            // 可选：如需强制归属检查，这里仅记录日志
            if (found.getUser() != null && user != null && !found.getUser().getId().equals(user.getId())) {
                logger.warn("会话ID已存在且归属不同用户: sessionId={}, ownerId={}, currentUserId={}",
                        sessionId, found.getUser().getId(), user.getId());
            }
            return found;
        }

        // 用户维度下也检查一次，若不存在则创建
        Optional<ChatSession> existingSession = chatSessionRepository.findBySessionIdAndUser(sessionId, user);
        if (existingSession.isPresent()) {
            return existingSession.get();
        }

        // 使用 try-catch 处理并发插入冲突
        try {
            ChatSession newSession = new ChatSession(sessionId, "新对话", user);
            return chatSessionRepository.save(newSession);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // 主键冲突，说明其他线程已经创建了该会话，重新查询
            logger.warn("检测到主键冲突，重新查询会话: sessionId={}, error={}", sessionId, e.getMessage());
            Optional<ChatSession> retrySession = chatSessionRepository.findById(sessionId);
            if (retrySession.isPresent()) {
                logger.info("成功获取到已存在的会话: sessionId={}", sessionId);
                return retrySession.get();
            }
            // 如果仍然找不到，抛出原始异常
            logger.error("主键冲突后仍无法找到会话: sessionId={}", sessionId);
            throw e;
        }
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
        // 每次用户请求视为一轮，对应会话总轮数+1
        Integer currentRounds = session.getTotalRounds() == null ? 0 : session.getTotalRounds();
        session.setTotalRounds(currentRounds + 1);
        
        // 首次设置情节起始轮数和情节名称
        if (session.getCurrentArcStartRound() == null) {
            // 设置为当前轮数（因为这是第一轮对话）
            session.setCurrentArcStartRound(session.getTotalRounds());
        }
        
        // 如果情节名称为空，设置默认名称
        if (session.getCurrentArcName() == null || session.getCurrentArcName().trim().isEmpty()) {
            session.setCurrentArcName("初始情节");
        }
        
        chatSessionRepository.save(session);
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
        return chatSessionRepository.findBySessionIdWithMessages(sessionId).orElse(null);
    }
    
    /**
     * 根据会话ID获取会话
     */
    @Transactional(readOnly = true)
    public ChatSession getSessionById(String sessionId) {
        return chatSessionRepository.findBySessionIdWithMessages(sessionId).orElse(null);
    }
    
    /**
     * 保存会话
     */
    @Transactional
    public ChatSession saveSession(ChatSession session) {
        logger.info("💾 保存会话到数据库: sessionId={}, version={}", session.getSessionId(), session.getVersion());
        ChatSession savedSession = chatSessionRepository.save(session);
        logger.info("✅ 会话保存完成: sessionId={}, 新version={}", savedSession.getSessionId(), savedSession.getVersion());
        return savedSession;
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
               Long.toHexString(Double.doubleToLongBits(Math.random())).substring(0, 8);
    }
}
