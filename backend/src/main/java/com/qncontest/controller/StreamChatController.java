package com.qncontest.controller;

import com.qncontest.dto.ChatRequest;
import com.qncontest.dto.ChatResponse;
import com.qncontest.service.StreamAiService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/chat")
@CrossOrigin(origins = "*", maxAge = 3600)
public class StreamChatController {
    
    @Autowired
    private StreamAiService streamAiService;
    
    /**
     * 流式对话接口
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("isAuthenticated()")
    public SseEmitter streamChat(@Valid @RequestBody ChatRequest chatRequest) {
        return streamAiService.streamChat(chatRequest);
    }
    
    /**
     * 简单对话接口（非流式，用于测试）
     */
    @PostMapping("/simple")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> simpleChat(@Valid @RequestBody ChatRequest chatRequest) {
        try {
            // 这里可以实现一个简单的非流式版本用于测试
            return ResponseEntity.ok(Map.of(
                "message", "流式对话功能已启用，请使用 /chat/stream 接口",
                "status", "success"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "对话失败",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "healthy",
            "service", "Stream Chat Service",
            "timestamp", System.currentTimeMillis()
        ));
    }
    
    /**
     * Token验证接口
     */
    @GetMapping("/verify-token")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> verifyToken() {
        return ResponseEntity.ok(Map.of(
            "status", "valid",
            "message", "Token is valid",
            "timestamp", System.currentTimeMillis()
        ));
    }
    
    /**
     * 简单的认证测试接口
     */
    @GetMapping("/auth-test")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> authTest() {
        return ResponseEntity.ok(Map.of(
            "authenticated", true,
            "message", "Authentication successful",
            "timestamp", System.currentTimeMillis()
        ));
    }
}
