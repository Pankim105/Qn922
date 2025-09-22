package com.qncontest.controller;

import com.qncontest.entity.ChatSession;
import com.qncontest.service.ChatSessionService;
import com.qncontest.service.IntelligentChatService;
import com.qncontest.service.LangChainEnhancedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 聊天会话管理控制器
 */
@RestController
@RequestMapping("/chat/session")
@CrossOrigin(origins = "*", maxAge = 3600)
@PreAuthorize("isAuthenticated()")
public class ChatSessionController {
    
    @Autowired
    private ChatSessionService chatSessionService;
    
    @Autowired
    private IntelligentChatService intelligentChatService;
    
    @Autowired(required = false)
    private LangChainEnhancedService langChainEnhancedService;
    
    /**
     * 获取用户的所有会话列表
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getUserSessions() {
        String currentUser = getCurrentUsername();
        if (currentUser == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户认证失败"));
        }
        
        List<ChatSession> sessions = intelligentChatService.getUserSessions(currentUser);
        
        // 转换为前端需要的格式
        List<Map<String, Object>> sessionList = sessions.stream()
            .map(session -> {
                Map<String, Object> sessionInfo = new HashMap<>();
                sessionInfo.put("sessionId", session.getSessionId());
                sessionInfo.put("title", session.getTitle() != null ? session.getTitle() : "新对话");
                sessionInfo.put("createdAt", session.getCreatedAt().toString());
                sessionInfo.put("updatedAt", session.getUpdatedAt().toString());
                // 使用数据库查询获取消息数量，避免LazyInitializationException
                sessionInfo.put("messageCount", intelligentChatService.getSessionMessageCount(session));
                return sessionInfo;
            })
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(Map.of(
            "sessions", sessionList,
            "total", sessionList.size()
        ));
    }
    
    /**
     * 获取指定会话的消息列表
     */
    @GetMapping("/{sessionId}/messages")
    public ResponseEntity<Map<String, Object>> getSessionMessages(@PathVariable String sessionId) {
        String currentUser = getCurrentUsername();
        if (currentUser == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户认证失败"));
        }
        
        try {
            List<Map<String, Object>> messages = intelligentChatService.getSessionMessages(currentUser, sessionId);
            return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "messages", messages
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "获取会话消息失败: " + e.getMessage()));
        }
    }
    
    /**
     * 清空指定会话的历史
     */
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Map<String, Object>> clearSession(@PathVariable String sessionId) {
        String currentUser = getCurrentUsername();
        if (currentUser == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户认证失败"));
        }
        
        intelligentChatService.deleteSession(currentUser, sessionId);
        
        return ResponseEntity.ok(Map.of(
            "message", "会话历史已清空",
            "sessionId", sessionId
        ));
    }
    
    /**
     * 清空用户的所有会话
     */
    @DeleteMapping("/all")
    public ResponseEntity<Map<String, Object>> clearAllSessions() {
        String currentUser = getCurrentUsername();
        if (currentUser == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户认证失败"));
        }
        
        intelligentChatService.deleteAllUserSessions(currentUser);
        
        return ResponseEntity.ok(Map.of(
            "message", "所有会话历史已清空",
            "user", currentUser
        ));
    }
    
    /**
     * 获取会话摘要
     */
    @GetMapping("/{sessionId}/summary")
    public ResponseEntity<Map<String, Object>> getSessionSummary(@PathVariable String sessionId) {
        String currentUser = getCurrentUsername();
        if (currentUser == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户认证失败"));
        }
        
        try {
            ChatSession session = intelligentChatService.getOrCreateSession(currentUser, sessionId);
            
            String summary;
            if (langChainEnhancedService != null && langChainEnhancedService.isAvailable()) {
                summary = langChainEnhancedService.generateConversationSummary(session);
            } else {
                summary = "LangChain4j未配置，无法生成智能摘要";
            }
            
            return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "summary", summary,
                "messageCount", session.getMessages().size(),
                "generatedBy", langChainEnhancedService != null && langChainEnhancedService.isAvailable() ? "LangChain4j" : "Simple"
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "获取摘要失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取LangChain4j状态
     */
    @GetMapping("/langchain/status")
    public ResponseEntity<Map<String, Object>> getLangChainStatus() {
        boolean isAvailable = langChainEnhancedService != null && langChainEnhancedService.isAvailable();
        
        Map<String, Object> status = new HashMap<>();
        status.put("available", isAvailable);
        status.put("service", isAvailable ? "LangChain4j Enhanced" : "Basic");
        status.put("features", isAvailable ? 
            List.of("智能摘要", "上下文筛选", "重要性分析", "对话压缩") : 
            List.of("基础对话", "简单历史"));
        
        return ResponseEntity.ok(status);
    }
    
    /**
     * 获取会话统计信息（管理员用）
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getSessionStats() {
        Map<String, Object> stats = chatSessionService.getSessionStats();
        stats.put("langchain4j_available", langChainEnhancedService != null && langChainEnhancedService.isAvailable());
        return ResponseEntity.ok(stats);
    }
    
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : null;
    }
}


