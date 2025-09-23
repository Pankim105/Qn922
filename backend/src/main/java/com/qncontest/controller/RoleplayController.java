package com.qncontest.controller;

import com.qncontest.dto.ChatResponse;
import com.qncontest.dto.RoleplayRequest;
import com.qncontest.dto.WorldTemplateResponse;
import com.qncontest.entity.DiceRoll;
import com.qncontest.entity.User;
import com.qncontest.service.RoleplayWorldService;
import com.qncontest.service.StreamAiService;
import com.qncontest.service.UserDetailsServiceImpl;
import com.qncontest.service.WorldTemplateService;
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

import java.util.List;

/**
 * 角色扮演控制器 - 处理角色扮演相关的API请求
 */
@RestController
@RequestMapping("/roleplay")
@CrossOrigin(origins = "*", maxAge = 3600)
public class RoleplayController {
    
    private static final Logger logger = LoggerFactory.getLogger(RoleplayController.class);
    
    @Autowired
    private WorldTemplateService worldTemplateService;
    
    @Autowired
    private RoleplayWorldService roleplayWorldService;
    
    @Autowired
    private StreamAiService streamAiService;
    
    @Autowired
    private UserDetailsServiceImpl userDetailsService;
    
    /**
     * 获取所有可用的世界模板
     */
    @GetMapping("/worlds")
    public ResponseEntity<ChatResponse> getWorldTemplates() {
        try {
            List<WorldTemplateResponse> templates = worldTemplateService.getAllWorldTemplates();
            return ResponseEntity.ok(ChatResponse.success("获取世界模板成功", templates));
            
        } catch (Exception e) {
            logger.error("获取世界模板失败", e);
            return ResponseEntity.status(500)
                .body(ChatResponse.error("获取世界模板失败"));
        }
    }
    
    /**
     * 获取指定世界模板的详细信息
     */
    @GetMapping("/worlds/{worldId}")
    public ResponseEntity<ChatResponse> getWorldTemplate(@PathVariable String worldId) {
        try {
            return worldTemplateService.getWorldTemplate(worldId)
                .map(template -> ResponseEntity.ok(ChatResponse.success("获取世界模板详情成功", template)))
                .orElse(ResponseEntity.status(404)
                    .body(ChatResponse.error("世界模板不存在")));
                    
        } catch (Exception e) {
            logger.error("获取世界模板详情失败: worldId={}", worldId, e);
            return ResponseEntity.status(500)
                .body(ChatResponse.error("获取世界模板详情失败"));
        }
    }
    
    /**
     * 初始化角色扮演会话
     */
    @PostMapping("/sessions/{sessionId}/initialize")
    public ResponseEntity<ChatResponse> initializeSession(
            @PathVariable String sessionId,
            @RequestBody InitializeSessionRequest request) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(401)
                    .body(ChatResponse.error("用户未认证"));
            }
            
            // 验证世界类型
            if (!worldTemplateService.isValidWorldType(request.getWorldType())) {
                return ResponseEntity.status(400)
                    .body(ChatResponse.error("无效的世界类型"));
            }
            
            // 初始化角色扮演会话
            roleplayWorldService.initializeRoleplaySession(
                sessionId, request.getWorldType(), request.getGodModeRules(), currentUser);
            
            return ResponseEntity.ok(ChatResponse.success("角色扮演会话初始化成功"));
            
        } catch (Exception e) {
            logger.error("初始化角色扮演会话失败: sessionId={}", sessionId, e);
            return ResponseEntity.status(500)
                .body(ChatResponse.error("初始化角色扮演会话失败"));
        }
    }
    
    /**
     * 角色扮演流式聊天（扩展原有聊天功能）
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter roleplayStreamChat(@Valid @RequestBody RoleplayRequest request, HttpServletResponse response) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户未认证");
            }
            
            logger.info("收到角色扮演流式聊天请求: user={}, session={}, world={}", 
                       currentUser.getUsername(), request.getSessionId(), request.getWorldType());
            
            // 设置SSE响应头
            response.setHeader("Cache-Control", "no-cache");
            response.setHeader("X-Accel-Buffering", "no");
            
            // 直接调用角色扮演流式聊天服务
            return streamAiService.handleRoleplayStreamChat(request, currentUser);
            
        } catch (Exception e) {
            logger.error("角色扮演流式聊天失败", e);
            throw e;
        }
    }
    
    /**
     * 执行骰子检定
     */
    @PostMapping("/sessions/{sessionId}/dice-roll")
    public ResponseEntity<ChatResponse> rollDice(
            @PathVariable String sessionId,
            @RequestBody DiceRollRequest request) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(401)
                    .body(ChatResponse.error("用户未认证"));
            }
            
            DiceRoll result = roleplayWorldService.rollDice(
                sessionId, 
                request.getDiceType(), 
                request.getModifier(), 
                request.getContext(),
                request.getDifficultyClass()
            );
            
            return ResponseEntity.ok(ChatResponse.success("骰子检定完成", result));
            
        } catch (Exception e) {
            logger.error("骰子检定失败: sessionId={}", sessionId, e);
            return ResponseEntity.status(500)
                .body(ChatResponse.error("骰子检定失败"));
        }
    }
    
    /**
     * 更新世界状态
     */
    @PostMapping("/sessions/{sessionId}/world-state")
    public ResponseEntity<ChatResponse> updateWorldState(
            @PathVariable String sessionId,
            @RequestBody UpdateWorldStateRequest request) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(401)
                    .body(ChatResponse.error("用户未认证"));
            }
            
            roleplayWorldService.updateWorldState(sessionId, request.getWorldState(), request.getSkillsState());
            
            return ResponseEntity.ok(ChatResponse.success("世界状态更新成功"));
            
        } catch (Exception e) {
            logger.error("更新世界状态失败: sessionId={}", sessionId, e);
            return ResponseEntity.status(500)
                .body(ChatResponse.error("更新世界状态失败"));
        }
    }
    
    /**
     * 获取世界状态摘要
     */
    @GetMapping("/sessions/{sessionId}/world-state")
    public ResponseEntity<ChatResponse> getWorldState(@PathVariable String sessionId) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(401)
                    .body(ChatResponse.error("用户未认证"));
            }
            
            String worldState = roleplayWorldService.getWorldStateSummary(sessionId);
            
            return ResponseEntity.ok(ChatResponse.success("获取世界状态成功", worldState));
            
        } catch (Exception e) {
            logger.error("获取世界状态失败: sessionId={}", sessionId, e);
            return ResponseEntity.status(500)
                .body(ChatResponse.error("获取世界状态失败"));
        }
    }
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<ChatResponse> health() {
        return ResponseEntity.ok(ChatResponse.success("Roleplay service is running"));
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
            logger.error("获取当前用户失败", e);
            return null;
        }
    }
    
    /**
     * 转换RoleplayRequest到基础ChatRequest（临时适配器）
     */
    private com.qncontest.dto.ChatRequest convertToBasicChatRequest(RoleplayRequest request) {
        com.qncontest.dto.ChatRequest chatRequest = new com.qncontest.dto.ChatRequest();
        chatRequest.setMessage(request.getMessage());
        chatRequest.setSessionId(request.getSessionId());
        chatRequest.setSystemPrompt(request.getSystemPrompt());
        
        // 转换历史消息
        if (request.getHistory() != null) {
            // TODO: 需要转换历史消息格式
        }
        
        return chatRequest;
    }
    
    // 内部DTO类
    public static class InitializeSessionRequest {
        private String worldType;
        private String godModeRules;
        
        // Getters and Setters
        public String getWorldType() {
            return worldType;
        }
        
        public void setWorldType(String worldType) {
            this.worldType = worldType;
        }
        
        public String getGodModeRules() {
            return godModeRules;
        }
        
        public void setGodModeRules(String godModeRules) {
            this.godModeRules = godModeRules;
        }
    }
    
    public static class DiceRollRequest {
        private Integer diceType;
        private Integer modifier = 0;
        private String context;
        private Integer difficultyClass;
        
        // Getters and Setters
        public Integer getDiceType() {
            return diceType;
        }
        
        public void setDiceType(Integer diceType) {
            this.diceType = diceType;
        }
        
        public Integer getModifier() {
            return modifier;
        }
        
        public void setModifier(Integer modifier) {
            this.modifier = modifier;
        }
        
        public String getContext() {
            return context;
        }
        
        public void setContext(String context) {
            this.context = context;
        }
        
        public Integer getDifficultyClass() {
            return difficultyClass;
        }
        
        public void setDifficultyClass(Integer difficultyClass) {
            this.difficultyClass = difficultyClass;
        }
    }
    
    public static class UpdateWorldStateRequest {
        private String worldState;
        private String skillsState;
        
        // Getters and Setters
        public String getWorldState() {
            return worldState;
        }
        
        public void setWorldState(String worldState) {
            this.worldState = worldState;
        }
        
        public String getSkillsState() {
            return skillsState;
        }
        
        public void setSkillsState(String skillsState) {
            this.skillsState = skillsState;
        }
    }
}

