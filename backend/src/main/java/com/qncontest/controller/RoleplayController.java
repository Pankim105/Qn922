package com.qncontest.controller;

import com.qncontest.dto.ChatResponse;
import com.qncontest.dto.RoleplayRequest;
import com.qncontest.dto.WorldTemplateResponse;
import com.qncontest.entity.ChatSession;
import com.qncontest.entity.DiceRoll;
import com.qncontest.entity.User;
import com.qncontest.service.ChatSessionService;
import com.qncontest.service.RoleplayMemoryService;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Autowired
    private RoleplayMemoryService roleplayMemoryService;
    
    @Autowired
    private ChatSessionService chatSessionService;

    
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
     * 创建新的角色扮演会话
     */
    @PostMapping("/sessions")
    public ResponseEntity<ChatResponse> createSession(@RequestBody CreateSessionRequest request) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(401)
                    .body(ChatResponse.error("用户未认证"));
            }
            
            // 验证世界类型
            if (!worldTemplateService.isValidWorldType(request.getWorldId())) {
                return ResponseEntity.status(400)
                    .body(ChatResponse.error("无效的世界类型"));
            }
            
            // 创建新的聊天会话（后端生成会话ID）
            ChatSession session = chatSessionService.createSession(currentUser, request.getWorldId());
            
            // 初始化角色扮演会话
            roleplayWorldService.initializeRoleplaySession(
                session.getSessionId(), 
                request.getWorldId(), 
                request.getGodModeRules(), 
                currentUser);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("sessionId", session.getSessionId());
            responseData.put("worldId", request.getWorldId());
            responseData.put("createdAt", session.getCreatedAt());
            
            return ResponseEntity.ok(ChatResponse.success("角色扮演会话创建成功", responseData));
            
        } catch (Exception e) {
            logger.error("创建角色扮演会话失败", e);
            return ResponseEntity.status(500)
                .body(ChatResponse.error("创建角色扮演会话失败"));
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
            
            // logger.info("收到角色扮演流式聊天请求: user={}, session={}, world={}, message长度={}",
            //            currentUser.getUsername(), request.getSessionId(), request.getWorldType(), request.getMessage().length());

            // 检查是否启用DM评估模式
            boolean isDMAwareMode = request.getWorldType() != null && !"general".equals(request.getWorldType());
            if (isDMAwareMode) {
                // logger.info("启用DM评估模式: worldType={}, sessionId={}", request.getWorldType(), request.getSessionId());
            } else {
                // logger.info("使用普通角色扮演模式: sessionId={}", request.getSessionId());
            }

            // 设置SSE响应头
            response.setHeader("Cache-Control", "no-cache");
            response.setHeader("X-Accel-Buffering", "no");

            // 直接调用角色扮演流式聊天服务，传递用户信息避免在异步线程中查询数据库
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
     * 获取会话消息历史
     */
    @GetMapping("/sessions/{sessionId}/messages")
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
            logger.error("获取会话消息失败: sessionId={}", sessionId, e);
            return ResponseEntity.status(500)
                .body(ChatResponse.error("获取会话消息失败"));
        }
    }
    
    /**
     * 获取会话技能状态
     */
    @GetMapping("/sessions/{sessionId}/skills")
    public ResponseEntity<ChatResponse> getSessionSkills(@PathVariable String sessionId) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(401)
                    .body(ChatResponse.error("用户未认证"));
            }
            
            ChatSession session = chatSessionService.getSessionById(sessionId);
            if (session == null) {
                return ResponseEntity.status(404)
                    .body(ChatResponse.error("会话不存在"));
            }
            
            return ResponseEntity.ok(ChatResponse.success("获取技能状态成功", session.getSkillsState()));
            
        } catch (Exception e) {
            logger.error("获取技能状态失败: sessionId={}", sessionId, e);
            return ResponseEntity.status(500)
                .body(ChatResponse.error("获取技能状态失败"));
        }
    }
    
    /**
     * 获取会话状态（包括世界状态和技能状态）
     */
    @GetMapping("/sessions/{sessionId}/state")
    public ResponseEntity<ChatResponse> getSessionState(@PathVariable String sessionId) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(401)
                    .body(ChatResponse.error("用户未认证"));
            }
            
            ChatSession session = chatSessionService.getSessionById(sessionId);
            if (session == null) {
                return ResponseEntity.status(404)
                    .body(ChatResponse.error("会话不存在"));
            }
            
            Map<String, Object> sessionState = new HashMap<>();
            sessionState.put("worldState", session.getWorldState());
            sessionState.put("skillsState", session.getSkillsState());
            sessionState.put("totalRounds", session.getTotalRounds());
            sessionState.put("currentArcName", session.getCurrentArcName());
            sessionState.put("currentArcStartRound", session.getCurrentArcStartRound());
            
            return ResponseEntity.ok(ChatResponse.success("获取会话状态成功", sessionState));
            
        } catch (Exception e) {
            logger.error("获取会话状态失败: sessionId={}", sessionId, e);
            return ResponseEntity.status(500)
                .body(ChatResponse.error("获取会话状态失败"));
        }
    }
    
    /**
     * 存储记忆
     */
    @PostMapping("/sessions/{sessionId}/memories")
    public ResponseEntity<ChatResponse> storeMemory(
            @PathVariable String sessionId,
            @RequestBody MemoryRequest request) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(401)
                    .body(ChatResponse.error("用户未认证"));
            }

            double importance = request.getImportance() != null ? request.getImportance() : 0.5;
            roleplayMemoryService.storeMemory(sessionId, request.getContent(), request.getType(), importance);

            return ResponseEntity.ok(ChatResponse.success("记忆存储成功"));

        } catch (Exception e) {
            logger.error("存储记忆失败: sessionId={}", sessionId, e);
            return ResponseEntity.status(500)
                .body(ChatResponse.error("存储记忆失败"));
        }
    }

    /**
     * 获取相关记忆
     */
    @GetMapping("/sessions/{sessionId}/memories")
    public ResponseEntity<ChatResponse> getMemories(
            @PathVariable String sessionId,
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int maxResults) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(401)
                    .body(ChatResponse.error("用户未认证"));
            }

            List<RoleplayMemoryService.MemoryEntry> memories =
                roleplayMemoryService.retrieveRelevantMemories(sessionId, query, maxResults);

            return ResponseEntity.ok(ChatResponse.success("获取记忆成功", memories));

        } catch (Exception e) {
            logger.error("获取记忆失败: sessionId={}", sessionId, e);
            return ResponseEntity.status(500)
                .body(ChatResponse.error("获取记忆失败"));
        }
    }

    /**
     * 获取记忆摘要
     */
    @GetMapping("/sessions/{sessionId}/memories/summary")
    public ResponseEntity<ChatResponse> getMemorySummary(@PathVariable String sessionId) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(401)
                    .body(ChatResponse.error("用户未认证"));
            }

            String summary = roleplayMemoryService.getMemorySummary(sessionId);

            return ResponseEntity.ok(ChatResponse.success("获取记忆摘要成功", summary));

        } catch (Exception e) {
            logger.error("获取记忆摘要失败: sessionId={}", sessionId, e);
            return ResponseEntity.status(500)
                .body(ChatResponse.error("获取记忆摘要失败"));
        }
    }

    /**
     * 记录重要事件
     */
    @PostMapping("/sessions/{sessionId}/events/important")
    public ResponseEntity<ChatResponse> recordImportantEvent(
            @PathVariable String sessionId,
            @RequestBody RecordEventRequest request) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(401)
                    .body(ChatResponse.error("用户未认证"));
            }

            roleplayMemoryService.recordImportantEvent(sessionId, request.getEvent(), request.getContext());

            return ResponseEntity.ok(ChatResponse.success("重要事件记录成功"));

        } catch (Exception e) {
            logger.error("记录重要事件失败: sessionId={}", sessionId, e);
            return ResponseEntity.status(500)
                .body(ChatResponse.error("记录重要事件失败"));
        }
    }

    /**
     * 更新角色关系
     */
    @PostMapping("/sessions/{sessionId}/relationships")
    public ResponseEntity<ChatResponse> updateRelationship(
            @PathVariable String sessionId,
            @RequestBody UpdateRelationshipRequest request) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(401)
                    .body(ChatResponse.error("用户未认证"));
            }

            roleplayMemoryService.updateCharacterRelationship(sessionId, request.getCharacter(), request.getRelationship());

            return ResponseEntity.ok(ChatResponse.success("角色关系更新成功"));

        } catch (Exception e) {
            logger.error("更新角色关系失败: sessionId={}", sessionId, e);
            return ResponseEntity.status(500)
                .body(ChatResponse.error("更新角色关系失败"));
        }
    }

    /**
     * 处理AI回复中的记忆标记
     */
    @PostMapping("/sessions/{sessionId}/memories/process")
    public ResponseEntity<ChatResponse> processMemoryMarkers(
            @PathVariable String sessionId,
            @RequestBody ProcessMemoryRequest request) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(401)
                    .body(ChatResponse.error("用户未认证"));
            }

            roleplayMemoryService.updateMemoriesFromAI(sessionId, request.getAiResponse(), request.getUserAction());

            return ResponseEntity.ok(ChatResponse.success("记忆处理完成"));

        } catch (Exception e) {
            logger.error("处理记忆标记失败: sessionId={}", sessionId, e);
            return ResponseEntity.status(500)
                .body(ChatResponse.error("处理记忆标记失败"));
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
            if (authentication == null || !authentication.isAuthenticated()) {
                return null;
            }

            Object principal = authentication.getPrincipal();
            if (principal instanceof User) {
                return (User) principal;
            }

            String username = authentication.getName();
            if (username == null) {
                return null;
            }
            // 回退到按用户名查询（尽量避免在SSE请求中频繁触发数据库读取）
            return userDetailsService.findUserByUsername(username);
        } catch (Exception e) {
            logger.error("获取当前用户失败", e);
            return null;
        }
    }
    
    
    // 内部DTO类
    public static class CreateSessionRequest {
        private String worldId;
        private String godModeRules;
        
        // Getters and Setters
        public String getWorldId() {
            return worldId;
        }
        
        public void setWorldId(String worldId) {
            this.worldId = worldId;
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

    public static class MemoryRequest {
        private String content;
        private String type;
        private Double importance;

        // Getters and Setters
        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Double getImportance() {
            return importance;
        }

        public void setImportance(Double importance) {
            this.importance = importance;
        }
    }

    public static class ProcessMemoryRequest {
        private String aiResponse;
        private String userAction;

        // Getters and Setters
        public String getAiResponse() {
            return aiResponse;
        }

        public void setAiResponse(String aiResponse) {
            this.aiResponse = aiResponse;
        }

        public String getUserAction() {
            return userAction;
        }

        public void setUserAction(String userAction) {
            this.userAction = userAction;
        }
    }

    public static class RecordEventRequest {
        private String event;
        private String context;

        // Getters and Setters
        public String getEvent() {
            return event;
        }

        public void setEvent(String event) {
            this.event = event;
        }

        public String getContext() {
            return context;
        }

        public void setContext(String context) {
            this.context = context;
        }
    }

    public static class UpdateRelationshipRequest {
        private String character;
        private String relationship;

        // Getters and Setters
        public String getCharacter() {
            return character;
        }

        public void setCharacter(String character) {
            this.character = character;
        }

        public String getRelationship() {
            return relationship;
        }

        public void setRelationship(String relationship) {
            this.relationship = relationship;
        }
    }
}

