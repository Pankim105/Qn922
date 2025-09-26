package com.qncontest.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qncontest.entity.ChatSession;
import com.qncontest.entity.WorldEvent;
import com.qncontest.entity.WorldState;
import com.qncontest.repository.ChatSessionRepository;
import com.qncontest.repository.WorldEventRepository;
import com.qncontest.repository.WorldStateRepository;
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
public class RoleplayMemoryService {

    private static final Logger logger = LoggerFactory.getLogger(RoleplayMemoryService.class);

    private final ObjectMapper objectMapper;

    @Autowired
    public RoleplayMemoryService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Autowired
    private ChatSessionRepository chatSessionRepository;

    @Autowired
    private WorldEventRepository worldEventRepository;

    @Autowired
    private WorldStateRepository worldStateRepository;
    
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
