package com.qncontest.controller;

import com.qncontest.dto.ChatResponse;
import com.qncontest.entity.User;
import com.qncontest.service.ChatSessionService;
import com.qncontest.service.UserDetailsServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chat/session")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ChatSessionController {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatSessionController.class);
    
    @Autowired
    private ChatSessionService chatSessionService;
    
    @Autowired
    private UserDetailsServiceImpl userDetailsService;
    
    /**
     * 获取用户的聊天会话列表
     */
    @GetMapping("/list")
    public ResponseEntity<ChatResponse> getUserSessions() {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                logger.warn("Unauthorized request to get sessions");
                return ResponseEntity.status(401)
                    .body(ChatResponse.error("用户未认证"));
            }
            
            logger.info("Getting sessions for user: {}", currentUser.getUsername());
            List<ChatResponse.SessionInfo> sessions = chatSessionService.getUserSessions(currentUser);
            ChatResponse.SessionListData data = new ChatResponse.SessionListData(sessions);
            
            logger.info("Found {} sessions for user: {}", sessions.size(), currentUser.getUsername());
            ChatResponse response = ChatResponse.success("获取会话列表成功", data);
            logger.debug("Response data: {}", response);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting user sessions", e);
            return ResponseEntity.status(500)
                .body(ChatResponse.error("获取会话列表失败"));
        }
    }
    
    /**
     * 获取指定会话的消息列表
     */
    @GetMapping("/{sessionId}/messages")
    public ResponseEntity<ChatResponse> getSessionMessages(@PathVariable String sessionId) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(401)
                    .body(ChatResponse.error("用户未认证"));
            }
            
            List<ChatResponse.MessageInfo> messages = chatSessionService.getSessionMessages(sessionId, currentUser);
            ChatResponse.MessageListData data = new ChatResponse.MessageListData(messages);
            
            return ResponseEntity.ok(ChatResponse.success("获取消息列表成功", data));
            
        } catch (Exception e) {
            logger.error("Error getting session messages for session: " + sessionId, e);
            return ResponseEntity.status(500)
                .body(ChatResponse.error("获取消息列表失败"));
        }
    }
    
    /**
     * 删除指定会话
     */
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<ChatResponse> deleteSession(@PathVariable String sessionId) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(401)
                    .body(ChatResponse.error("用户未认证"));
            }
            
            boolean deleted = chatSessionService.deleteSession(sessionId, currentUser);
            if (deleted) {
                return ResponseEntity.ok(ChatResponse.success("删除会话成功"));
            } else {
                return ResponseEntity.status(404)
                    .body(ChatResponse.error("会话不存在"));
            }
            
        } catch (Exception e) {
            logger.error("Error deleting session: " + sessionId, e);
            return ResponseEntity.status(500)
                .body(ChatResponse.error("删除会话失败"));
        }
    }
    
    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<ChatResponse> health() {
        return ResponseEntity.ok(ChatResponse.success("Chat service is running"));
    }
    
    /**
     * 获取当前认证用户
     */
    private User getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                String username = authentication.getName();
                return userDetailsService.findUserByUsername(username);
            }
            return null;
        } catch (Exception e) {
            logger.error("Error getting current user", e);
            return null;
        }
    }
}
