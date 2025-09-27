package com.qncontest.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qncontest.entity.ChatSession;
import com.qncontest.entity.ChatMessage;
import com.qncontest.entity.WorldEvent;
import com.qncontest.repository.ChatSessionRepository;
import com.qncontest.repository.ChatMessageRepository;
import com.qncontest.repository.WorldEventRepository;
import com.qncontest.service.interfaces.MemoryManagerInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 角色扮演记忆管理服务
 * 负责智能管理角色记忆、世界状态和重要事件
 * 基于现有的ChatSession、WorldEvent、WorldState表结构
 */
@Service
public class RoleplayMemoryService implements MemoryManagerInterface {

    private static final Logger logger = LoggerFactory.getLogger(RoleplayMemoryService.class);

    private final ObjectMapper objectMapper;

    @Autowired
    public RoleplayMemoryService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Autowired
    private ChatSessionRepository chatSessionRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private WorldEventRepository worldEventRepository;
    
    /**
     * 角色记忆结构
     */
    public static class CharacterMemory {
        private String characterName;
        private String personality;
        private List<String> coreTraits;
        private Map<String, Object> relationships;
        private List<String> importantEvents;
        private Map<String, Integer> emotionalState;
        
        // Constructors
        public CharacterMemory() {
            this.coreTraits = new ArrayList<>();
            this.relationships = new HashMap<>();
            this.importantEvents = new ArrayList<>();
            this.emotionalState = new HashMap<>();
        }
        
        // Getters and Setters
        public String getCharacterName() { return characterName; }
        public void setCharacterName(String characterName) { this.characterName = characterName; }
        
        public String getPersonality() { return personality; }
        public void setPersonality(String personality) { this.personality = personality; }
        
        public List<String> getCoreTraits() { return coreTraits; }
        public void setCoreTraits(List<String> coreTraits) { this.coreTraits = coreTraits; }
        
        public Map<String, Object> getRelationships() { return relationships; }
        public void setRelationships(Map<String, Object> relationships) { this.relationships = relationships; }
        
        public List<String> getImportantEvents() { return importantEvents; }
        public void setImportantEvents(List<String> importantEvents) { this.importantEvents = importantEvents; }
        
        public Map<String, Integer> getEmotionalState() { return emotionalState; }
        public void setEmotionalState(Map<String, Integer> emotionalState) { this.emotionalState = emotionalState; }
    }
    
    /**
     * 记忆条目
     */
    public static class MemoryEntry {
        private String content;
        private String type; // CHARACTER, EVENT, RELATIONSHIP, WORLD_STATE
        private double importance; // 0.0 - 1.0
        private long timestamp;
        private Map<String, Object> metadata;
        
        public MemoryEntry(String content, String type, double importance) {
            this.content = content;
            this.type = type;
            this.importance = importance;
            this.timestamp = System.currentTimeMillis();
            this.metadata = new HashMap<>();
        }
        
        // Getters and Setters
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public double getImportance() { return importance; }
        public void setImportance(double importance) { this.importance = importance; }
        
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }
    
    /**
     * 存储重要记忆
     */
    public void storeMemory(String sessionId, String content, String type, double importance) {
        logger.debug("存储记忆: sessionId={}, type={}, importance={}", sessionId, type, importance);

        try {
            // 1. 记录到WorldEvent中作为记忆事件
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("content", content);
            eventData.put("type", type);
            eventData.put("importance", importance);
            eventData.put("timestamp", LocalDateTime.now());

            WorldEvent memoryEvent = new WorldEvent();
            memoryEvent.setSessionId(sessionId);
            memoryEvent.setEventType(WorldEvent.EventType.SYSTEM_EVENT);
            memoryEvent.setEventData(objectMapper.writeValueAsString(eventData));
            memoryEvent.setSequence(getNextEventSequence(sessionId));
            memoryEvent.setChecksum(generateChecksum(eventData));
            // 记录当前会话情节快照
            chatSessionRepository.findById(sessionId).ifPresent(cs -> {
                memoryEvent.setTotalRounds(cs.getTotalRounds());
                memoryEvent.setCurrentArcStartRound(cs.getCurrentArcStartRound());
                memoryEvent.setCurrentArcName(cs.getCurrentArcName());
            });

            worldEventRepository.save(memoryEvent);

            // 2. 更新ChatSession中的记忆数据
            Optional<ChatSession> sessionOpt = chatSessionRepository.findById(sessionId);
            if (sessionOpt.isPresent()) {
                ChatSession session = sessionOpt.get();

                // 获取现有的记忆数据
                Map<String, Object> memories = parseMemoriesFromSession(session);
                memories.computeIfAbsent(type, k -> new ArrayList<Map<String, Object>>());

                // 添加新记忆
                Map<String, Object> newMemory = new HashMap<>();
                newMemory.put("content", content);
                newMemory.put("importance", importance);
                newMemory.put("timestamp", LocalDateTime.now());

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> memoryList = (List<Map<String, Object>>) memories.get(type);
                memoryList.add(newMemory);

                // 清理低重要性记忆
                cleanupMemories(memories, type);

                // 保存更新后的记忆数据
                session.setWorldState(objectMapper.writeValueAsString(memories));
                chatSessionRepository.save(session);
            }

            logger.info("记忆存储成功: sessionId={}, type={}, content={}", sessionId, type, content);
        } catch (Exception e) {
            logger.error("存储记忆失败: sessionId={}, type={}", sessionId, type, e);
        }
    }
    
    /**
     * 检索相关记忆
     */
    public List<MemoryEntry> retrieveRelevantMemories(String sessionId, String query, int maxResults) {
        try {
            // 从ChatSession中获取记忆数据
            Optional<ChatSession> sessionOpt = chatSessionRepository.findById(sessionId);
            if (!sessionOpt.isPresent()) {
                return new ArrayList<>();
            }

            ChatSession session = sessionOpt.get();
            Map<String, Object> memories = parseMemoriesFromSession(session);

            // 合并所有类型的记忆，兼容多种数据结构
            List<Object> allMemories = new ArrayList<>();
            for (Object memoryValue : memories.values()) {
                if (memoryValue instanceof List) {
                    List<?> list = (List<?>) memoryValue;
                    allMemories.addAll(list);
                } else if (memoryValue instanceof Map) {
                    allMemories.add(memoryValue);
                } else if (memoryValue != null) {
                    // 兜底：字符串或其它类型
                    allMemories.add(memoryValue);
                }
            }

            if (allMemories.isEmpty()) {
                return new ArrayList<>();
            }

            // 简单的关键词匹配（后续可以用向量相似度）
            return allMemories.stream()
                    .filter(memoryObj -> {
                        String contentStr;
                        if (memoryObj instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> mapObj = (Map<String, Object>) memoryObj;
                            Object content = mapObj.get("content");
                            contentStr = content == null ? "" : content.toString();
                        } else {
                            contentStr = memoryObj.toString();
                        }
                        return isRelevant(contentStr, query);
                    })
                    .map(this::convertToMemoryEntryFromAny)
                    .sorted((a, b) -> Double.compare(b.getImportance(), a.getImportance()))
                    .limit(maxResults)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("检索记忆失败: sessionId={}", sessionId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 评估记忆重要性
     */
    public double assessMemoryImportance(String event, String context) {
        // 简化的重要性评估算法
        double importance = 0.5; // 基础重要性
        
        // 关键词权重
        String[] highImportanceKeywords = {
            "死亡", "获得", "失去", "发现", "秘密", "任务", "完成", 
            "等级", "技能", "宝藏", "boss", "重要", "关键"
        };
        
        String[] mediumImportanceKeywords = {
            "遇到", "对话", "购买", "移动", "战斗", "学习", "挑战"
        };
        
        String lowerEvent = event == null ? "" : event.toLowerCase();
        String lowerContext = context == null ? "" : context.toLowerCase();
        
        for (String keyword : highImportanceKeywords) {
            if (lowerEvent.contains(keyword) || lowerContext.contains(keyword)) {
                importance += 0.3;
            }
        }
        
        for (String keyword : mediumImportanceKeywords) {
            if (lowerEvent.contains(keyword) || lowerContext.contains(keyword)) {
                importance += 0.1;
            }
        }
        
        return Math.min(importance, 1.0);
    }
    
    /**
     * 构建角色记忆上下文
     */
    public String buildMemoryContext(String sessionId, String currentSituation) {
        List<MemoryEntry> relevantMemories = retrieveRelevantMemories(sessionId, currentSituation, 5);

        if (relevantMemories.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder();
        context.append("## 相关记忆\n");

        for (MemoryEntry memory : relevantMemories) {
            context.append("- **").append(memory.getType()).append("**: ")
                   .append(memory.getContent()).append("\n");
        }

        return context.toString();
    }

    /**
     * 构建简化的第四层记忆上下文
     * 基于ChatSession的worldState、skillsState、最近消息历史和WorldEvent记录
     */
    public String buildSimplifiedMemoryContext(String sessionId, String currentMessage) {
        try {
            StringBuilder context = new StringBuilder();
            
            // 1. 获取ChatSession信息
            Optional<ChatSession> sessionOpt = chatSessionRepository.findById(sessionId);
            if (!sessionOpt.isPresent()) {
                return "";
            }
            
            ChatSession session = sessionOpt.get();
            
            // 2. 构建世界状态上下文
            String worldStateContext = buildWorldStateContext(session);
            if (!worldStateContext.isEmpty()) {
                context.append("## 🌍 世界状态记忆\n");
                context.append(worldStateContext).append("\n\n");
            }
            
            // 3. 构建角色状态上下文
            String skillsStateContext = buildSkillsStateContext(session);
            if (!skillsStateContext.isEmpty()) {
                context.append("## 🎭 角色状态记忆\n");
                context.append(skillsStateContext).append("\n\n");
            }
            
            // 4. 构建最近消息历史上下文
            String recentMessagesContext = buildRecentMessagesContext(sessionId, currentMessage);
            if (!recentMessagesContext.isEmpty()) {
                context.append("## 💬 最近对话记忆\n");
                context.append(recentMessagesContext).append("\n\n");
            }
            
            // 5. 构建重要事件上下文
            String importantEventsContext = buildImportantEventsContext(sessionId, currentMessage);
            if (!importantEventsContext.isEmpty()) {
                context.append("## 📅 重要事件记忆\n");
                context.append(importantEventsContext).append("\n\n");
            }
            
            return context.toString();
            
        } catch (Exception e) {
            logger.error("构建简化记忆上下文失败: sessionId={}", sessionId, e);
            return "";
        }
    }
    
    /**
     * 构建世界状态上下文
     */
    private String buildWorldStateContext(ChatSession session) {
        try {
            String worldState = session.getWorldState();
            if (worldState == null || worldState.trim().isEmpty() || worldState.equals("{}")) {
                return "";
            }
            
            // 解析世界状态JSON，提取关键信息
            JsonNode worldStateJson = objectMapper.readTree(worldState);
            StringBuilder context = new StringBuilder();
            
            // 提取位置信息
            if (worldStateJson.has("currentLocation")) {
                context.append("当前位置: ").append(worldStateJson.get("currentLocation").asText()).append("\n");
            }
            
            // 提取环境信息
            if (worldStateJson.has("environment")) {
                context.append("环境状态: ").append(worldStateJson.get("environment").asText()).append("\n");
            }
            
            // 提取活跃任务信息
            if (worldStateJson.has("activeQuests") && worldStateJson.get("activeQuests").isArray()) {
                JsonNode activeQuests = worldStateJson.get("activeQuests");
                if (activeQuests.size() > 0) {
                    context.append("当前任务: ");
                    for (int i = 0; i < Math.min(activeQuests.size(), 3); i++) {
                        JsonNode quest = activeQuests.get(i);
                        if (quest.has("title")) {
                            context.append(quest.get("title").asText());
                            if (i < Math.min(activeQuests.size(), 3) - 1) {
                                context.append(", ");
                            }
                        }
                    }
                    context.append("\n");
                }
            }
            
            // 提取NPC信息
            if (worldStateJson.has("npcs") && worldStateJson.get("npcs").isArray()) {
                JsonNode npcs = worldStateJson.get("npcs");
                if (npcs.size() > 0) {
                    context.append("重要NPC: ");
                    for (int i = 0; i < Math.min(npcs.size(), 3); i++) {
                        JsonNode npc = npcs.get(i);
                        if (npc.has("name")) {
                            context.append(npc.get("name").asText());
                            if (i < Math.min(npcs.size(), 3) - 1) {
                                context.append(", ");
                            }
                        }
                    }
                    context.append("\n");
                }
            }
            
            return context.toString();
            
        } catch (Exception e) {
            logger.warn("解析世界状态失败: sessionId={}", session.getSessionId(), e);
            return "";
        }
    }
    
    /**
     * 构建角色状态上下文
     */
    private String buildSkillsStateContext(ChatSession session) {
        try {
            String skillsState = session.getSkillsState();
            if (skillsState == null || skillsState.trim().isEmpty() || skillsState.equals("{}")) {
                return "";
            }
            
            // 解析角色状态JSON，提取关键信息
            JsonNode skillsStateJson = objectMapper.readTree(skillsState);
            StringBuilder context = new StringBuilder();
            
            // 提取角色基本信息
            if (skillsStateJson.has("level")) {
                context.append("角色等级: ").append(skillsStateJson.get("level").asText()).append("\n");
            }
            
            if (skillsStateJson.has("experience")) {
                context.append("经验值: ").append(skillsStateJson.get("experience").asText()).append("\n");
            }
            
            if (skillsStateJson.has("health")) {
                context.append("生命值: ").append(skillsStateJson.get("health").asText()).append("\n");
            }
            
            // 提取属性信息
            if (skillsStateJson.has("attributes")) {
                JsonNode attributes = skillsStateJson.get("attributes");
                if (attributes.has("strength")) {
                    context.append("力量: ").append(attributes.get("strength").asText()).append(" ");
                }
                if (attributes.has("intelligence")) {
                    context.append("智力: ").append(attributes.get("intelligence").asText()).append(" ");
                }
                if (attributes.has("agility")) {
                    context.append("敏捷: ").append(attributes.get("agility").asText()).append(" ");
                }
                if (attributes.has("constitution")) {
                    context.append("体质: ").append(attributes.get("constitution").asText());
                }
                context.append("\n");
            }
            
            // 提取技能信息
            if (skillsStateJson.has("skills") && skillsStateJson.get("skills").isArray()) {
                JsonNode skills = skillsStateJson.get("skills");
                if (skills.size() > 0) {
                    context.append("主要技能: ");
                    for (int i = 0; i < Math.min(skills.size(), 3); i++) {
                        JsonNode skill = skills.get(i);
                        if (skill.has("name")) {
                            context.append(skill.get("name").asText());
                            if (i < Math.min(skills.size(), 3) - 1) {
                                context.append(", ");
                            }
                        }
                    }
                    context.append("\n");
                }
            }
            
            return context.toString();
            
        } catch (Exception e) {
            logger.warn("解析角色状态失败: sessionId={}", session.getSessionId(), e);
            return "";
        }
    }
    
    /**
     * 构建最近消息历史上下文
     */
    private String buildRecentMessagesContext(String sessionId, String currentMessage) {
        try {
            // 获取最近的消息历史（最多5条）
            List<ChatMessage> recentMessages = chatMessageRepository.findByChatSessionOrderBySequenceNumberAsc(
                chatSessionRepository.findById(sessionId).orElse(null)
            );
            
            if (recentMessages == null || recentMessages.isEmpty()) {
                return "";
            }
            
            // 取最近的几条消息
            List<ChatMessage> lastMessages = recentMessages.stream()
                .skip(Math.max(0, recentMessages.size() - 5))
                .collect(Collectors.toList());
            
            StringBuilder context = new StringBuilder();
            
            for (ChatMessage message : lastMessages) {
                String role = message.getRole() == ChatMessage.MessageRole.USER ? "玩家" : "AI";
                String content = message.getContent();
                
                // 截取过长的消息
                if (content.length() > 100) {
                    content = content.substring(0, 100) + "...";
                }
                
                context.append("- ").append(role).append(": ").append(content).append("\n");
            }
            
            return context.toString();
            
        } catch (Exception e) {
            logger.warn("获取最近消息历史失败: sessionId={}", sessionId, e);
            return "";
        }
    }
    
    /**
     * 构建重要事件上下文
     */
    private String buildImportantEventsContext(String sessionId, String currentMessage) {
        try {
            // 获取最近的重要事件（包含多种事件类型，最多10条）
            List<WorldEvent> recentEvents = worldEventRepository.findBySessionIdOrderByTimestampDesc(sessionId);
            
            if (recentEvents == null || recentEvents.isEmpty()) {
                return "";
            }
            
            // 过滤出重要的事件类型，并按时间排序
            List<WorldEvent> importantEvents = recentEvents.stream()
                .filter(event -> isImportantEventType(event.getEventType()))
                .limit(10) // 增加事件数量
                .collect(Collectors.toList());
            
            if (importantEvents.isEmpty()) {
                return "";
            }
            
            StringBuilder context = new StringBuilder();
            
            for (WorldEvent event : importantEvents) {
                try {
                    String eventDescription = buildEventDescription(event);
                    if (!eventDescription.isEmpty()) {
                        context.append("- ").append(eventDescription).append("\n");
                    }
                } catch (Exception e) {
                    logger.debug("解析事件数据失败: eventId={}", event.getId(), e);
                }
            }
            
            return context.toString();
            
        } catch (Exception e) {
            logger.warn("获取重要事件失败: sessionId={}", sessionId, e);
            return "";
        }
    }
    
    /**
     * 判断是否为重要的事件类型
     */
    private boolean isImportantEventType(WorldEvent.EventType eventType) {
        return eventType == WorldEvent.EventType.DICE_ROLL ||
               eventType == WorldEvent.EventType.QUEST_UPDATE ||
               eventType == WorldEvent.EventType.STATE_CHANGE ||
               eventType == WorldEvent.EventType.CHARACTER_UPDATE ||
               eventType == WorldEvent.EventType.SKILL_USE ||
               eventType == WorldEvent.EventType.LOCATION_CHANGE ||
               eventType == WorldEvent.EventType.SYSTEM_EVENT;
    }
    
    /**
     * 构建事件描述
     */
    private String buildEventDescription(WorldEvent event) {
        try {
            JsonNode eventData = objectMapper.readTree(event.getEventData());
            StringBuilder description = new StringBuilder();
            
            // 根据事件类型构建不同的描述
            switch (event.getEventType()) {
                case DICE_ROLL:
                    if (eventData.has("diceType") && eventData.has("result")) {
                        String diceType = eventData.get("diceType").asText();
                        int result = eventData.get("result").asInt();
                        String context = eventData.has("context") ? eventData.get("context").asText() : "检定";
                        boolean isSuccessful = eventData.has("isSuccessful") ? eventData.get("isSuccessful").asBoolean() : false;
                        description.append("骰子检定: ").append(context).append(" (")
                                  .append(diceType).append("=").append(result)
                                  .append(isSuccessful ? ", 成功" : ", 失败").append(")");
                    }
                    break;
                    
                case QUEST_UPDATE:
                    if (eventData.has("type")) {
                        String type = eventData.get("type").asText();
                        description.append("任务更新: ").append(type);
                        if (eventData.has("questId")) {
                            description.append(" (任务ID: ").append(eventData.get("questId").asText()).append(")");
                        }
                    }
                    break;
                    
                case STATE_CHANGE:
                    if (eventData.has("change")) {
                        String change = eventData.get("change").asText();
                        description.append("状态变化: ").append(change);
                    }
                    break;
                    
                case CHARACTER_UPDATE:
                    if (eventData.has("type")) {
                        String type = eventData.get("type").asText();
                        description.append("角色更新: ").append(type);
                        if (eventData.has("oldLevel") && eventData.has("newLevel")) {
                            description.append(" (等级: ").append(eventData.get("oldLevel").asInt())
                                      .append(" -> ").append(eventData.get("newLevel").asInt()).append(")");
                        }
                    }
                    break;
                    
                case SKILL_USE:
                    if (eventData.has("skillName")) {
                        String skillName = eventData.get("skillName").asText();
                        description.append("技能使用: ").append(skillName);
                    }
                    break;
                    
                case LOCATION_CHANGE:
                    if (eventData.has("from") && eventData.has("to")) {
                        String from = eventData.get("from").asText();
                        String to = eventData.get("to").asText();
                        description.append("位置变化: ").append(from).append(" -> ").append(to);
                    }
                    break;
                    
                case SYSTEM_EVENT:
                    if (eventData.has("content")) {
                        String content = eventData.get("content").asText();
                        String type = eventData.has("type") ? eventData.get("type").asText() : "系统事件";
                        description.append(type).append(": ").append(content);
                    }
                    break;
                    
                default:
                    // 对于其他类型，尝试提取通用信息
                    if (eventData.has("content")) {
                        description.append("事件: ").append(eventData.get("content").asText());
                    } else if (eventData.has("type")) {
                        description.append("事件: ").append(eventData.get("type").asText());
                    }
                    break;
            }
            
            // 截取过长的描述
            String result = description.toString();
            if (result.length() > 100) {
                result = result.substring(0, 100) + "...";
            }
            
            return result;
            
        } catch (Exception e) {
            logger.debug("构建事件描述失败: eventId={}", event.getId(), e);
            return "";
        }
    }

    /**
     * 获取下一个事件序列号
     */
    private Integer getNextEventSequence(String sessionId) {
        Optional<WorldEvent> lastEvent = worldEventRepository.findTopBySessionIdOrderBySequenceDesc(sessionId);
        return lastEvent.map(event -> event.getSequence() + 1).orElse(1);
    }

    /**
     * 从ChatSession中解析记忆数据
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseMemoriesFromSession(ChatSession session) {
        try {
            String worldState = session.getWorldState();
            if (worldState == null || worldState.trim().isEmpty()) {
                return new HashMap<>();
            }
            return objectMapper.readValue(worldState, Map.class);
        } catch (Exception e) {
            logger.warn("解析记忆数据失败，使用空记忆: sessionId={}", session.getSessionId());
            return new HashMap<>();
        }
    }

    /**
     * 将Map转换为MemoryEntry
     */
    private MemoryEntry convertToMemoryEntry(Map<String, Object> memoryMap) {
        // 安全地获取content，处理不同的数据类型
        Object contentObj = memoryMap.get("content");
        String content = contentObj instanceof String ? (String) contentObj : 
                        contentObj != null ? contentObj.toString() : "";
        
        // 安全地获取type
        Object typeObj = memoryMap.get("type");
        String type = typeObj instanceof String ? (String) typeObj : 
                     typeObj != null ? typeObj.toString() : "UNKNOWN";
        
        MemoryEntry entry = new MemoryEntry(
            content,
            type,
            ((Number) memoryMap.getOrDefault("importance", 0.5)).doubleValue()
        );

        if (memoryMap.containsKey("timestamp")) {
            try {
                Object timestamp = memoryMap.get("timestamp");
                if (timestamp instanceof String) {
                    // 尝试作为字符串解析
                    entry.setTimestamp(LocalDateTime.parse((String) timestamp).toEpochSecond(java.time.ZoneOffset.UTC) * 1000);
                } else if (timestamp instanceof LocalDateTime) {
                    // 直接作为 LocalDateTime 转换
                    entry.setTimestamp(((LocalDateTime) timestamp).toEpochSecond(java.time.ZoneOffset.UTC) * 1000);
                } else if (timestamp instanceof Long) {
                    // 作为毫秒时间戳
                    entry.setTimestamp((Long) timestamp);
                } else {
                    entry.setTimestamp(System.currentTimeMillis());
                }
            } catch (Exception e) {
                entry.setTimestamp(System.currentTimeMillis());
            }
        }

        if (memoryMap.containsKey("metadata")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) memoryMap.get("metadata");
            entry.setMetadata(metadata);
        }

        return entry;
    }

    /**
     * 将任意对象转换为MemoryEntry，兼容字符串/Map等
     */
    @SuppressWarnings("unchecked")
    private MemoryEntry convertToMemoryEntryFromAny(Object obj) {
        if (obj instanceof Map) {
            return convertToMemoryEntry((Map<String, Object>) obj);
        }
        // 兜底字符串
        String content = obj == null ? "" : obj.toString();
        MemoryEntry entry = new MemoryEntry(content, "TEXT", 0.5);
        entry.setTimestamp(System.currentTimeMillis());
        return entry;
    }

    /**
     * 清理低重要性记忆
     */
    private void cleanupMemories(Map<String, Object> memories, String type) {
        if (!memories.containsKey(type)) {
            return;
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> memoryList = (List<Map<String, Object>>) memories.get(type);
        if (memoryList == null || memoryList.size() <= 20) { // 降低清理阈值
            return;
        }

        // 按重要性排序，保留最重要的记忆
        memoryList.sort((a, b) -> Double.compare(
            ((Number) b.getOrDefault("importance", 0.5)).doubleValue(),
            ((Number) a.getOrDefault("importance", 0.5)).doubleValue()
        ));

        // 只保留前15个最重要的记忆
        if (memoryList.size() > 15) {
            memoryList = memoryList.subList(0, 15);
            memories.put(type, memoryList);
        }

        logger.debug("清理记忆完成: type={}, 保留数量={}", type, memoryList.size());
    }
    
    /**
     * 获取会话的完整记忆摘要
     */
    public String getMemorySummary(String sessionId) {
        try {
            Optional<ChatSession> sessionOpt = chatSessionRepository.findById(sessionId);
            if (!sessionOpt.isPresent()) {
                return "暂无重要记忆";
            }

            ChatSession session = sessionOpt.get();
            Map<String, Object> memories = parseMemoriesFromSession(session);

            if (memories.isEmpty()) {
                return "暂无重要记忆";
            }

            // 按类型分组
            Map<String, List<Map<String, Object>>> groupedMemories = new HashMap<>();
            for (String type : memories.keySet()) {
                Object memoryList = memories.get(type);
                if (memoryList instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> typedMemoryList = (List<Map<String, Object>>) memoryList;
                    groupedMemories.put(type, typedMemoryList);
                }
            }

            StringBuilder summary = new StringBuilder();

            for (Map.Entry<String, List<Map<String, Object>>> group : groupedMemories.entrySet()) {
                summary.append("### ").append(group.getKey()).append("\n");

                List<Map<String, Object>> sortedMemories = group.getValue().stream()
                        .sorted((a, b) -> Double.compare(
                            ((Number) b.getOrDefault("importance", 0.5)).doubleValue(),
                            ((Number) a.getOrDefault("importance", 0.5)).doubleValue()
                        ))
                        .limit(3)
                        .collect(Collectors.toList());

                for (Map<String, Object> memory : sortedMemories) {
                    summary.append("- ").append(memory.get("content")).append("\n");
                }
                summary.append("\n");
            }

            return summary.toString();
        } catch (Exception e) {
            logger.error("获取记忆摘要失败: sessionId={}", sessionId, e);
            return "获取记忆摘要时发生错误";
        }
    }
    
    /**
     * 简单的相关性判断
     */
    private boolean isRelevant(String content, String query) {
        if (query == null || query.trim().isEmpty()) {
            return true;
        }

        if (content == null || content.trim().isEmpty()) {
            return false;
        }

        String lowerContent = content.toLowerCase();
        String lowerQuery = query.toLowerCase();

        // 简单的关键词匹配
        String[] queryWords = lowerQuery.split("\\s+");
        for (String word : queryWords) {
            if (word.length() > 2 && lowerContent.contains(word)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 更新角色关系（使用WorldEvent记录）
     */
    public void updateCharacterRelationship(String sessionId, String character, String relationship) {
        String content = String.format("与%s的关系: %s", character, relationship);
        double importance = 0.8;
        storeMemory(sessionId, content, "CHARACTER", importance);

        // 同时记录到WorldEvent
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("character", character);
            eventData.put("relationship", relationship);
            eventData.put("type", "RELATIONSHIP_UPDATE");

            WorldEvent relationshipEvent = new WorldEvent();
            relationshipEvent.setSessionId(sessionId);
            relationshipEvent.setEventType(WorldEvent.EventType.CHARACTER_UPDATE);
            relationshipEvent.setEventData(objectMapper.writeValueAsString(eventData));
            relationshipEvent.setSequence(getNextEventSequence(sessionId));
            relationshipEvent.setChecksum(generateChecksum(eventData));
            chatSessionRepository.findById(sessionId).ifPresent(cs -> {
                relationshipEvent.setTotalRounds(cs.getTotalRounds());
                relationshipEvent.setCurrentArcStartRound(cs.getCurrentArcStartRound());
                relationshipEvent.setCurrentArcName(cs.getCurrentArcName());
            });

            worldEventRepository.save(relationshipEvent);
        } catch (Exception e) {
            logger.error("记录角色关系事件失败: sessionId={}", sessionId, e);
        }
    }

    /**
     * 记录世界状态变化（使用WorldEvent记录）
     */
    public void recordWorldStateChange(String sessionId, String change, String reason) {
        String content = String.format("世界状态变化: %s (原因: %s)", change, reason);
        double importance = 0.7;
        storeMemory(sessionId, content, "WORLD_STATE", importance);

        // 同时记录到WorldEvent
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("change", change);
            eventData.put("reason", reason);
            eventData.put("type", "WORLD_STATE_CHANGE");

            WorldEvent stateEvent = new WorldEvent();
            stateEvent.setSessionId(sessionId);
            stateEvent.setEventType(WorldEvent.EventType.STATE_CHANGE);
            stateEvent.setEventData(objectMapper.writeValueAsString(eventData));
            stateEvent.setSequence(getNextEventSequence(sessionId));
            stateEvent.setChecksum(generateChecksum(eventData));
            chatSessionRepository.findById(sessionId).ifPresent(cs -> {
                stateEvent.setTotalRounds(cs.getTotalRounds());
                stateEvent.setCurrentArcStartRound(cs.getCurrentArcStartRound());
                stateEvent.setCurrentArcName(cs.getCurrentArcName());
            });

            worldEventRepository.save(stateEvent);
        } catch (Exception e) {
            logger.error("记录世界状态事件失败: sessionId={}", sessionId, e);
        }
    }

    /**
     * 获取会话的所有记忆事件
     */
    public List<WorldEvent> getMemoryEvents(String sessionId) {
        return worldEventRepository.findBySessionIdAndEventTypeOrderByTimestampDesc(sessionId, WorldEvent.EventType.SYSTEM_EVENT);
    }

    /**
     * 大模型更新记忆接口
     * 当大模型生成回复时，可以调用此方法来更新记忆
     */
    public void updateMemoriesFromAI(String sessionId, String aiResponse, String userAction) {
        try {
            // 解析AI回复中的记忆相关内容
            List<String> newMemories = extractMemoriesFromResponse(aiResponse);

            for (String memory : newMemories) {
                // 评估记忆重要性
                double importance = assessMemoryImportance(memory, userAction);
                if (importance > 0.6) { // 只存储重要性较高的记忆
                    storeMemory(sessionId, memory, "AI_GENERATED", importance);
                }
            }

            logger.info("AI记忆更新完成: sessionId={}, 记忆数量={}", sessionId, newMemories.size());
        } catch (Exception e) {
            logger.error("AI记忆更新失败: sessionId={}", sessionId, e);
        }
    }

    /**
     * 从AI回复中提取记忆内容
     */
    private List<String> extractMemoriesFromResponse(String response) {
        List<String> memories = new ArrayList<>();

        // 这里可以实现更复杂的解析逻辑
        // 例如：查找特定标记、分析句子结构等
        String[] sentences = response.split("[。.!！？]");
        for (String sentence : sentences) {
            if (sentence.length() > 20 && sentence.length() < 200) {
                // 简单的筛选：长度适中的句子可能包含重要信息
                memories.add(sentence.trim());
            }
        }

        return memories.stream().limit(5).collect(Collectors.toList()); // 限制数量
    }
    
    /**
     * 记录重要事件
     */
    public void recordImportantEvent(String sessionId, String event, String context) {
        double importance = assessMemoryImportance(event, context);

        if (importance > 0.6) { // 只记录重要性较高的事件
            storeMemory(sessionId, event, "EVENT", importance);
            logger.info("记录重要事件: sessionId={}, importance={}, event={}",
                       sessionId, importance, event);
        }
    }

    /**
     * 生成事件数据的校验和
     */
    private String generateChecksum(Map<String, Object> eventData) {
        try {
            String dataString = objectMapper.writeValueAsString(eventData);
            return DigestUtils.md5DigestAsHex(dataString.getBytes()).toUpperCase();
        } catch (Exception e) {
            logger.warn("生成校验和失败，使用默认值", e);
            return "DEFAULT";
        }
    }
    
}
