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
import com.qncontest.service.VoiceInstructionParser;
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

    @Autowired
    private VoiceInstructionParser voiceInstructionParser;

    
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

            // 检查是否为语音输入，如果是则进行语音指令解析
            if ("voice".equals(request.getInputType())) {
                logger.info("检测到语音输入，进行指令解析: sessionId={}, worldType={}", 
                           request.getSessionId(), request.getWorldType());
                
                String originalMessage = request.getMessage();
                String parsedMessage = voiceInstructionParser.parseVoiceInstruction(
                    originalMessage, 
                    request.getWorldType(), 
                    request.getSessionId()
                );
                
                // 验证解析结果
                if (voiceInstructionParser.isValidVoiceInstruction(parsedMessage)) {
                    request.setMessage(parsedMessage);
                    logger.info("语音指令解析成功: 原始='{}', 解析后='{}'", originalMessage, parsedMessage);
                } else {
                    logger.warn("语音指令解析失败，使用原始文本: '{}'", originalMessage);
                }
            }

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
     * 初始化角色数据
     */
    @PostMapping("/sessions/{sessionId}/character")
    public ResponseEntity<ChatResponse> initializeCharacter(
            @PathVariable String sessionId,
            @RequestBody CharacterInitializationRequest request) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(401)
                    .body(ChatResponse.error("用户未认证"));
            }
            
            // 验证技能数量限制
            if (request.getSkills() == null || request.getSkills().isEmpty()) {
                return ResponseEntity.status(400)
                    .body(ChatResponse.error("请至少选择一个技能"));
            }
            
            if (request.getSkills().size() > 2) {
                return ResponseEntity.status(400)
                    .body(ChatResponse.error("最多只能选择两个技能"));
            }
            
            // 初始化角色数据到会话中
            roleplayWorldService.initializeCharacter(
                sessionId, 
                request.getCharacterName(),
                request.getProfession(),
                request.getSkills(),
                request.getBackground()
            );
            
            return ResponseEntity.ok(ChatResponse.success("角色初始化成功"));
            
        } catch (Exception e) {
            logger.error("角色初始化失败: sessionId={}", sessionId, e);
            return ResponseEntity.status(500)
                .body(ChatResponse.error("角色初始化失败"));
        }
    }

    /**
     * 修复角色属性（包括生命值、魔力值和所有属性）
     */
    @PostMapping("/sessions/{sessionId}/fix-character-attributes")
    public ResponseEntity<ChatResponse> fixCharacterAttributes(@PathVariable String sessionId) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(401)
                    .body(ChatResponse.error("用户未认证"));
            }
            
            // 验证会话所有权
            ChatSession session = chatSessionService.getSessionById(sessionId);
            if (session == null) {
                return ResponseEntity.status(404)
                    .body(ChatResponse.error("会话不存在"));
            }
            
            if (!session.getUser().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(403)
                    .body(ChatResponse.error("无权访问此会话"));
            }
            
            // 修复角色属性
            roleplayWorldService.fixCharacterAttributes(sessionId);
            
            return ResponseEntity.ok(ChatResponse.success("角色属性修复完成"));
            
        } catch (Exception e) {
            logger.error("修复角色属性失败: sessionId={}", sessionId, e);
            return ResponseEntity.status(500)
                .body(ChatResponse.error("修复角色属性失败"));
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

    public static class CharacterInitializationRequest {
        private String characterName;
        private String profession;
        private java.util.List<String> skills;
        private String background;

        // Getters and Setters
        public String getCharacterName() {
            return characterName;
        }

        public void setCharacterName(String characterName) {
            this.characterName = characterName;
        }

        public String getProfession() {
            return profession;
        }

        public void setProfession(String profession) {
            this.profession = profession;
        }

        public java.util.List<String> getSkills() {
            return skills;
        }

        public void setSkills(java.util.List<String> skills) {
            this.skills = skills;
        }

        public String getBackground() {
            return background;
        }

        public void setBackground(String background) {
            this.background = background;
        }
    }

    /**
     * 测试接口：模拟大模型流式输出
     */
    @GetMapping(value = "/test/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter testStreamResponse() {
        SseEmitter emitter = new SseEmitter(60000L); // 60秒超时
        
        // 模拟的大模型响应内容
        String fullResponse = """
            [DIALOGUE]
            场景描述：你站在一座古老警局的台阶前，锈迹斑斑的铁门在微风中轻轻摇晃。空气中弥漫着潮湿的尘埃与旧纸张的气息，远处传来钟楼的滴答声，仿佛在倒数着某个即将揭晓的秘密; 角色动作：你的手不自觉地按在腰间的配枪上，指尖触到冰冷的金属——这是你作为警探的第一天; NPC对话："Inspector Panzijian..." 一个沙哑的声音从阴影中传来，"欢迎来到'雾都案卷馆'。这里每一份档案都藏着一条命案的影子，而今晚，有一具尸体正在等你去发现。"; 环境变化：突然，警局大厅的灯闪烁了一下，一束惨白的光线照在墙上的老式挂钟上——时间停在了凌晨3:17; 声音效果：你听见走廊深处传来一声重物坠地的闷响，紧接着，是锁链拖地的声音……; NPC低语："有人在下面。"那声音低语道，"但没人能活着上来。"
            [/DIALOGUE]

            [WORLD]
            📍 当前位置: 雾都警局·档案馆入口; 🌅 时间: 凌晨3:17，夜色浓稠如墨; 🌤️ 天气: 雾气弥漫，街灯昏黄，雨丝悄然飘落; 🔦 环境: 木质楼梯吱呀作响，墙上挂着褪色的通缉令与泛黄的照片；角落里堆满积灰的案卷箱，最深处隐约可见一道铁门半开；👥 NPC: 无名低语者（神秘人，身份不明）：仅以声音出现，语气似哀求又似警告 ； 警局守夜人老陈（已失踪）：最后被目击于地下室；⚡ 特殊事件: 档案馆中央的铜铃无故自响三声，同时所有灯光短暂熄灭，重新亮起时，墙上一幅照片悄然更换——上面是你自己的脸，却戴着镣铐
            [/WORLD]

            [QUESTS]
            1. 探查雾都警局档案馆：调查警局内异常现象，寻找失踪守夜人老陈的下落，进度0/1（奖励：经验值100、警徽x1、线索卡「血字日记」）; 2. 解读铜铃异象：查明铜铃为何在无人触碰时自行响起，进度0/1（奖励：经验值80、灵觉感知+1）
            [/QUESTS]

            [CHOICES]
            1. 进入档案馆深处 - 沿着吱呀作响的楼梯向下，直面黑暗中的未知； 2. 检查墙上的新照片 - 仔细观察那张"戴镣铐的你"的照片，寻找隐藏线索； 3. 搜寻守夜人遗留物品 - 在大厅角落翻找可能属于老陈的遗物； 4. 使用法医工具检测空气 - 用随身携带的便携式化学试纸测试空气中是否有毒素或血迹残留； 5. 自由行动 - 描述你想要进行的其他行动
            [/CHOICES]

            §{"ruleCompliance": 0.95, "contextConsistency": 0.92, "convergenceProgress": 0.65, "overallScore": 0.85, "strategy": "ACCEPT", "assessmentNotes": "玩家启动角色扮演行为符合侦探世界设定，且推动剧情进入核心谜题阶段", "suggestedActions": ["优先检查照片中的细节", "注意铜铃的规律性响动"], "convergenceHints": ["照片是关键线索，暗示身份错位", "铜铃三响，对应三个死者"], "questUpdates": {"created": [{"questId": "quest_001", "title": "探查雾都警局档案馆", "description": "调查警局内异常现象，寻找失踪守夜人老陈的下落", "rewards": {"exp": 100, "items": ["警徽x1", "线索卡「血字日记」"]}}, {"questId": "quest_002", "title": "解读铜铃异象", "description": "查明铜铃为何在无人触碰时自行响起", "rewards": {"exp": 80, "attributes": {"perception": 1}}}], "completed": [], "progress": [], "expired": []}, "worldStateUpdates": {"currentLocation": "雾都警局·档案馆入口", "environment": "夜雾笼罩，灯光忽明忽暗，空气中弥漫着铁锈与旧纸张的气味", "npcs": [{"name": "无名低语者", "status": "声音来自阴影，未现身"}, {"name": "老陈", "status": "失踪，最后一次目击于地下室"}]}, "diceRolls": [{"diceType": 20, "modifier": 3, "context": "观察力检定", "result": 16, "isSuccessful": true}], "learningChallenges": [{"type": "LOGIC", "difficulty": "普通", "question": "如果三个人都说自己没杀人，但其中两人说谎，谁是凶手？", "answer": "说真话的那个人不是凶手", "isCorrect": false}], "stateUpdates": [{"type": "LOCATION", "value": "进入雾都警局档案馆，气氛压抑，灯光闪烁"}, {"type": "INVENTORY", "value": "获得便携式化学试纸x1，智力+2"}], "memoryUpdates": [{"type": "EVENT", "content": "主角首次进入雾都警局档案馆，遭遇神秘低语者", "importance": 0.85}, {"type": "CHARACTER", "content": "与无名低语者建立初步联系，对方似乎知晓主角过去", "importance": 0.75}], "arcUpdates": {"currentArcName": "血铃迷局", "currentArcStartRound": 1, "totalRounds": 45}, "convergenceStatusUpdates": {"progress": 0.15, "nearestScenarioId": "story_convergence_3", "nearestScenarioTitle": "三响之墓", "distanceToNearest": 0.85, "scenarioProgress": {"story_convergence_1": 0.0, "story_convergence_2": 0.1, "story_convergence_3": 0.15, "main_convergence": 0.05}, "activeHints": ["照片中的镣铐有特殊标记", "铜铃三响，对应三具尸体"]}}§
            """;

        // 异步发送流式数据
        new Thread(() -> {
            try {
                // 将完整响应分成小块进行流式发送
                String[] chunks = fullResponse.split("(?<=\\n)");
                
                for (int i = 0; i < chunks.length; i++) {
                    String chunk = chunks[i];
                    
                    // 模拟网络延迟
                    Thread.sleep(50 + (int)(Math.random() * 100));
                    
                    // 发送数据块
                    emitter.send(SseEmitter.event()
                        .name("message")
                        .data(chunk));
                    
                    // 每发送几个块就发送一次心跳
                    if (i % 10 == 0) {
                        emitter.send(SseEmitter.event()
                            .name("heartbeat")
                            .data("ping"));
                    }
                }
                
                // 发送完成信号
                emitter.send(SseEmitter.event()
                    .name("complete")
                    .data(""));
                
                // 完成流
                emitter.complete();
                
                logger.info("测试流式响应发送完成");
                
            } catch (Exception e) {
                logger.error("测试流式响应发送失败", e);
                emitter.completeWithError(e);
            }
        }).start();
        
        return emitter;
    }
}

