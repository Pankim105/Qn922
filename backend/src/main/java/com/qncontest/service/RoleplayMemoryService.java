package com.qncontest.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 角色扮演记忆管理服务
 * 负责智能管理角色记忆、世界状态和重要事件
 */
@Service
public class RoleplayMemoryService {
    
    private static final Logger logger = LoggerFactory.getLogger(RoleplayMemoryService.class);
    
    // 暂时使用内存存储，后续可以替换为持久化向量数据库
    private final Map<String, List<MemoryEntry>> sessionMemories = new HashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // 如果有配置向量数据库，可以注入这些组件
    // @Autowired(required = false)
    // private EmbeddingModel embeddingModel;
    
    // @Autowired(required = false) 
    // private EmbeddingStore<TextSegment> embeddingStore;
    
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
        
        MemoryEntry memory = new MemoryEntry(content, type, importance);
        
        sessionMemories.computeIfAbsent(sessionId, k -> new ArrayList<>()).add(memory);
        
        // 如果记忆太多，清理低重要性的记忆
        cleanupMemories(sessionId);
    }
    
    /**
     * 检索相关记忆
     */
    public List<MemoryEntry> retrieveRelevantMemories(String sessionId, String query, int maxResults) {
        List<MemoryEntry> memories = sessionMemories.getOrDefault(sessionId, new ArrayList<>());
        
        if (memories.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 简单的关键词匹配（后续可以用向量相似度）
        return memories.stream()
                .filter(memory -> isRelevant(memory.getContent(), query))
                .sorted((a, b) -> Double.compare(b.getImportance(), a.getImportance()))
                .limit(maxResults)
                .toList();
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
        
        String lowerEvent = event.toLowerCase();
        String lowerContext = context.toLowerCase();
        
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
     * 获取会话的完整记忆摘要
     */
    public String getMemorySummary(String sessionId) {
        List<MemoryEntry> memories = sessionMemories.getOrDefault(sessionId, new ArrayList<>());
        
        if (memories.isEmpty()) {
            return "暂无重要记忆";
        }
        
        // 按类型分组
        Map<String, List<MemoryEntry>> groupedMemories = new HashMap<>();
        for (MemoryEntry memory : memories) {
            groupedMemories.computeIfAbsent(memory.getType(), k -> new ArrayList<>()).add(memory);
        }
        
        StringBuilder summary = new StringBuilder();
        
        for (Map.Entry<String, List<MemoryEntry>> group : groupedMemories.entrySet()) {
            summary.append("### ").append(group.getKey()).append("\n");
            
            List<MemoryEntry> sortedMemories = group.getValue().stream()
                    .sorted((a, b) -> Double.compare(b.getImportance(), a.getImportance()))
                    .limit(3)
                    .toList();
                    
            for (MemoryEntry memory : sortedMemories) {
                summary.append("- ").append(memory.getContent()).append("\n");
            }
            summary.append("\n");
        }
        
        return summary.toString();
    }
    
    /**
     * 清理低重要性记忆
     */
    private void cleanupMemories(String sessionId) {
        List<MemoryEntry> memories = sessionMemories.get(sessionId);
        if (memories == null || memories.size() <= 50) {
            return; // 记忆数量还不多，不需要清理
        }
        
        // 保留重要性高的记忆
        memories.sort((a, b) -> Double.compare(b.getImportance(), a.getImportance()));
        
        // 只保留前30个最重要的记忆
        if (memories.size() > 30) {
            memories.subList(30, memories.size()).clear();
        }
        
        logger.info("清理会话记忆: sessionId={}, 保留记忆数={}", sessionId, memories.size());
    }
    
    /**
     * 简单的相关性判断
     */
    private boolean isRelevant(String content, String query) {
        if (query == null || query.trim().isEmpty()) {
            return true;
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
     * 更新角色关系
     */
    public void updateCharacterRelationship(String sessionId, String character, String relationship) {
        storeMemory(sessionId, 
                   String.format("与%s的关系: %s", character, relationship), 
                   "RELATIONSHIP", 0.8);
    }
    
    /**
     * 记录世界状态变化
     */
    public void recordWorldStateChange(String sessionId, String change, String reason) {
        storeMemory(sessionId, 
                   String.format("世界状态变化: %s (原因: %s)", change, reason), 
                   "WORLD_STATE", 0.7);
    }
}
