package com.qncontest.service;

import com.qncontest.dto.ChatRequest.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 聊天会话管理服务
 * 管理用户的对话历史，支持会话过期清理
 */
@Service
public class ChatSessionService {
    
    private static final Logger log = LoggerFactory.getLogger(ChatSessionService.class);
    
    // 会话过期时间（30分钟）
    private static final long SESSION_TIMEOUT_MINUTES = 30;
    
    // 每个会话最大保留的对话记录数（避免过长的上下文）
    private static final int MAX_HISTORY_SIZE = 20;
    
    // 存储用户会话的对话历史
    // Key: userId:sessionId, Value: 会话信息
    private final Map<String, ChatSession> sessions = new ConcurrentHashMap<>();
    
    // 定时清理器
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "ChatSession-Cleanup");
        thread.setDaemon(true);
        return thread;
    });
    
    public ChatSessionService() {
        // 每5分钟清理一次过期会话
        cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredSessions, 5, 5, TimeUnit.MINUTES);
    }
    
    /**
     * 获取会话的对话历史
     */
    public List<ChatMessage> getSessionHistory(String userId, String sessionId) {
        String sessionKey = getSessionKey(userId, sessionId);
        ChatSession session = sessions.get(sessionKey);
        
        if (session == null || session.isExpired()) {
            return new ArrayList<>();
        }
        
        // 更新访问时间
        session.updateLastAccess();
        return new ArrayList<>(session.getHistory());
    }
    
    /**
     * 添加用户消息到会话历史
     */
    public void addUserMessage(String userId, String sessionId, String message) {
        addMessage(userId, sessionId, new ChatMessage("user", message));
    }
    
    /**
     * 添加助手消息到会话历史
     */
    public void addAssistantMessage(String userId, String sessionId, String message) {
        addMessage(userId, sessionId, new ChatMessage("assistant", message));
    }
    
    /**
     * 添加消息到会话历史
     */
    private void addMessage(String userId, String sessionId, ChatMessage message) {
        String sessionKey = getSessionKey(userId, sessionId);
        ChatSession session = sessions.computeIfAbsent(sessionKey, k -> new ChatSession());
        
        session.addMessage(message);
        session.updateLastAccess();
        
        log.debug("添加消息到会话 {}: {} - {}", sessionKey, message.getRole(), 
                message.getContent().substring(0, Math.min(50, message.getContent().length())));
    }
    
    /**
     * 清空指定会话的历史
     */
    public void clearSession(String userId, String sessionId) {
        String sessionKey = getSessionKey(userId, sessionId);
        sessions.remove(sessionKey);
        log.info("清空会话: {}", sessionKey);
    }
    
    /**
     * 清空用户的所有会话
     */
    public void clearUserSessions(String userId) {
        sessions.entrySet().removeIf(entry -> entry.getKey().startsWith(userId + ":"));
        log.info("清空用户所有会话: {}", userId);
    }
    
    /**
     * 获取会话统计信息
     */
    public Map<String, Object> getSessionStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSessions", sessions.size());
        stats.put("activeSessions", sessions.values().stream().mapToLong(s -> s.isExpired() ? 0 : 1).sum());
        return stats;
    }
    
    /**
     * 清理过期会话
     */
    private void cleanupExpiredSessions() {
        int initialSize = sessions.size();
        sessions.entrySet().removeIf(entry -> entry.getValue().isExpired());
        int cleanedUp = initialSize - sessions.size();
        
        if (cleanedUp > 0) {
            log.info("清理了 {} 个过期会话，当前活跃会话数: {}", cleanedUp, sessions.size());
        }
    }
    
    private String getSessionKey(String userId, String sessionId) {
        return userId + ":" + (sessionId != null ? sessionId : "default");
    }
    
    /**
     * 内部会话类
     */
    private static class ChatSession {
        private final List<ChatMessage> history = new ArrayList<>();
        private long lastAccessTime = System.currentTimeMillis();
        
        public List<ChatMessage> getHistory() {
            return history;
        }
        
        public void addMessage(ChatMessage message) {
            history.add(message);
            
            // 如果历史记录过长，移除最早的记录（但保留第一条系统消息）
            while (history.size() > MAX_HISTORY_SIZE) {
                // 找到第一个非系统消息并移除
                for (int i = 0; i < history.size(); i++) {
                    if (!"system".equals(history.get(i).getRole())) {
                        history.remove(i);
                        break;
                    }
                }
                // 如果全是系统消息，移除最后一个
                if (history.size() > MAX_HISTORY_SIZE) {
                    history.remove(history.size() - 1);
                }
            }
        }
        
        public void updateLastAccess() {
            this.lastAccessTime = System.currentTimeMillis();
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - lastAccessTime > TimeUnit.MINUTES.toMillis(SESSION_TIMEOUT_MINUTES);
        }
    }
}


