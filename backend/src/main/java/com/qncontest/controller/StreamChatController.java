package com.qncontest.controller;

import com.qncontest.dto.ChatRequest;
import com.qncontest.dto.ChatResponse;
import com.qncontest.entity.User;
import com.qncontest.service.StreamAiService;
import com.qncontest.service.UserDetailsServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/chat")
@CrossOrigin(origins = "*", maxAge = 3600)
public class StreamChatController {
    
    private static final Logger logger = LoggerFactory.getLogger(StreamChatController.class);
    
    @Autowired
    private StreamAiService streamAiService;
    
    @Autowired
    private UserDetailsServiceImpl userDetailsService;
    
    /**
     * 流式聊天接口
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@Valid @RequestBody ChatRequest request, HttpServletResponse response) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户未认证");
            }

            logger.info("Received stream chat request from user: {}, session: {}",
                       currentUser.getUsername(), request.getSessionId());

            // 明确设置SSE相关头部
            response.setHeader("Cache-Control", "no-cache");
            response.setHeader("X-Accel-Buffering", "no");

            return streamAiService.handleStreamChat(request, currentUser);

        } catch (Exception e) {
            logger.error("Error in stream chat", e);
            throw e;
        }
    }
    
    /**
     * 验证token有效性接口
     */
    @GetMapping("/verify-token")
    public ResponseEntity<ChatResponse> verifyToken() {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(401)
                    .body(ChatResponse.error("Token无效"));
            }
            
            return ResponseEntity.ok(ChatResponse.success("Token有效", 
                new TokenInfo(currentUser.getUsername(), currentUser.getEmail())));
            
        } catch (Exception e) {
            logger.error("Error verifying token", e);
            return ResponseEntity.status(500)
                .body(ChatResponse.error("Token验证失败"));
        }
    }
    
    /**
     * 认证测试接口
     */
    @GetMapping("/auth-test")
    public ResponseEntity<ChatResponse> authTest() {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(401)
                    .body(ChatResponse.error("用户未认证"));
            }
            
            return ResponseEntity.ok(ChatResponse.success("认证成功", 
                new TokenInfo(currentUser.getUsername(), currentUser.getEmail())));
            
        } catch (Exception e) {
            logger.error("Error in auth test", e);
            return ResponseEntity.status(500)
                .body(ChatResponse.error("认证测试失败"));
        }
    }
    
    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<ChatResponse> health() {
        return ResponseEntity.ok(ChatResponse.success("Stream chat service is running"));
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
    
    /**
     * Token信息内部类
     */
    private static class TokenInfo {
        private String username;
        private String email;
        
        public TokenInfo(String username, String email) {
            this.username = username;
            this.email = email;
        }
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getEmail() {
            return email;
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
    }
}
