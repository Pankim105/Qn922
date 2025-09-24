package com.qncontest.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qncontest.entity.*;
import com.qncontest.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 角色扮演世界服务 - 核心世界状态管理
 */
@Service
public class RoleplayWorldService {
    
    private static final Logger logger = LoggerFactory.getLogger(RoleplayWorldService.class);
    
    @Autowired
    private WorldStateRepository worldStateRepository;
    
    @Autowired
    private StabilityAnchorRepository stabilityAnchorRepository;
    
    @Autowired
    private WorldEventRepository worldEventRepository;
    
    @Autowired
    private DiceRollRepository diceRollRepository;
    
    @Autowired
    private ChatSessionService chatSessionService;
    
    @Autowired
    private WorldTemplateService worldTemplateService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SecureRandom random = new SecureRandom();
    
    /**
     * 初始化角色扮演会话
     */
    @Transactional
    public void initializeRoleplaySession(String sessionId, String worldType, String godModeRules, User user) {
        logger.info("初始化角色扮演会话: sessionId={}, worldType={}", sessionId, worldType);
        
        try {
            // 0. 检查是否已经初始化过
            ChatSession existingSession = chatSessionService.getOrCreateSession(sessionId, user);
            if (existingSession.getWorldType() != null && !existingSession.getWorldType().equals("general")) {
                logger.info("会话已初始化过，跳过重复初始化: sessionId={}, existingWorldType={}", 
                           sessionId, existingSession.getWorldType());
                return;
            }
            
            // 1. 获取世界模板
            String defaultRules = worldTemplateService.getDefaultRules(worldType);
            String stabilityAnchorsTemplate = worldTemplateService.getStabilityAnchors(worldType);
            
            // 2. 合并默认规则和上帝模式规则
            String mergedRules = mergeRules(defaultRules, godModeRules);
            
            // 3. 初始化世界状态
            String initialWorldState = createInitialWorldState(worldType, mergedRules);
            
            // 4. 初始化技能状态
            String initialSkillsState = createInitialSkillsState();
            
            // 5. 更新会话
            existingSession.setWorldType(worldType);
            existingSession.setWorldRules(mergedRules);
            existingSession.setGodModeRules(godModeRules);
            existingSession.setWorldState(initialWorldState);
            existingSession.setSkillsState(initialSkillsState);
            existingSession.setVersion(1);
            existingSession.setChecksum(calculateChecksum(initialWorldState));
            
            // 6. 初始化稳定性锚点
            initializeStabilityAnchors(sessionId, worldType, stabilityAnchorsTemplate, mergedRules);
            
            // 7. 记录初始化事件（使用安全的序列号生成）
            recordEventSafe(sessionId, WorldEvent.EventType.SYSTEM_EVENT, 
                           createEventData("session_initialized", Map.of(
                               "worldType", worldType,
                               "hasGodModeRules", godModeRules != null && !godModeRules.trim().isEmpty()
                           )));
            
            logger.info("角色扮演会话初始化完成: {}", sessionId);
            
        } catch (Exception e) {
            logger.error("初始化角色扮演会话失败: sessionId={}", sessionId, e);
            throw new RuntimeException("初始化角色扮演会话失败", e);
        }
    }
    
    /**
     * 执行骰子检定
     */
    @Transactional
    public DiceRoll rollDice(String sessionId, Integer diceType, Integer modifier, String context, Integer difficultyClass) {
        logger.debug("执行骰子检定: sessionId={}, diceType=d{}, modifier={}", sessionId, diceType, modifier);
        
        // 生成确定性随机数（基于会话和当前事件序号）
        int eventSequence = getNextEventSequence(sessionId);
        long seed = generateDeterministicSeed(sessionId, eventSequence);
        SecureRandom deterministicRandom = new SecureRandom();
        deterministicRandom.setSeed(seed);
        
        // 掷骰子
        int result = deterministicRandom.nextInt(diceType) + 1;
        
        // 创建骰子记录
        DiceRoll diceRoll = new DiceRoll(sessionId, diceType, modifier, result, context);
        if (difficultyClass != null) {
            diceRoll.setDifficultyClass(difficultyClass);
        }
        
        diceRoll = diceRollRepository.save(diceRoll);
        
        // 记录事件
        recordEvent(sessionId, WorldEvent.EventType.DICE_ROLL,
                   createEventData("dice_roll", Map.of(
                       "diceType", diceType,
                       "modifier", modifier != null ? modifier : 0,
                       "result", result,
                       "finalResult", diceRoll.getFinalResult(),
                       "context", context != null ? context : "",
                       "isSuccessful", diceRoll.getIsSuccessful() != null ? diceRoll.getIsSuccessful() : false
                   )), eventSequence);
        
        logger.info("骰子检定完成: sessionId={}, d{}+{} = {} (最终: {})", 
                   sessionId, diceType, modifier, result, diceRoll.getFinalResult());
        
        return diceRoll;
    }
    
    /**
     * 更新世界状态
     */
    @Transactional
    public void updateWorldState(String sessionId, String newWorldState, String skillsState) {
        logger.debug("更新世界状态: sessionId={}", sessionId);
        
        try {
            ChatSession session = chatSessionService.getSessionWithMessages(sessionId);
            if (session == null) {
                throw new RuntimeException("会话不存在: " + sessionId);
            }
            
            // 乐观锁检查
            Integer currentVersion = session.getVersion();
            Integer newVersion = currentVersion + 1;
            
            // 更新会话状态
            session.setWorldState(newWorldState);
            if (skillsState != null) {
                session.setSkillsState(skillsState);
            }
            session.setVersion(newVersion);
            session.setChecksum(calculateChecksum(newWorldState));
            
            // 保存世界状态快照
            WorldState worldState = new WorldState(sessionId, newVersion);
            populateWorldStateFromJson(worldState, newWorldState);
            worldState.setChecksum(calculateChecksum(newWorldState));
            worldStateRepository.save(worldState);
            
            // 记录状态变更事件
            recordEvent(sessionId, WorldEvent.EventType.STATE_CHANGE,
                       createEventData("state_updated", Map.of(
                           "version", newVersion,
                           "hasSkillsUpdate", skillsState != null
                       )), getNextEventSequence(sessionId));
            
            logger.debug("世界状态更新完成: sessionId={}, version={}", sessionId, newVersion);
            
        } catch (Exception e) {
            logger.error("更新世界状态失败: sessionId={}", sessionId, e);
            throw new RuntimeException("更新世界状态失败", e);
        }
    }
    
    /**
     * 获取世界状态摘要（用于系统提示词）
     */
    public String getWorldStateSummary(String sessionId) {
        try {
            ChatSession session = chatSessionService.getSessionWithMessages(sessionId);
            if (session == null || session.getWorldState() == null) {
                return "{}";
            }
            
            JsonNode worldState = objectMapper.readTree(session.getWorldState());
            Map<String, Object> summary = new HashMap<>();
            
            // 提取关键信息
            if (worldState.has("currentLocation")) {
                summary.put("location", worldState.get("currentLocation"));
            }
            if (worldState.has("characters")) {
                summary.put("characters", worldState.get("characters"));
            }
            if (worldState.has("activeQuests")) {
                summary.put("activeQuests", worldState.get("activeQuests"));
            }
            
            return objectMapper.writeValueAsString(summary);
            
        } catch (Exception e) {
            logger.warn("获取世界状态摘要失败: sessionId={}", sessionId, e);
            return "{}";
        }
    }
    
    /**
     * 生成确定性种子
     */
    private long generateDeterministicSeed(String sessionId, int sequence) {
        String seedString = sessionId + "_" + sequence;
        return seedString.hashCode();
    }
    
    /**
     * 合并默认规则和上帝模式规则
     */
    private String mergeRules(String defaultRules, String godModeRules) {
        try {
            if (godModeRules == null || godModeRules.trim().isEmpty()) {
                return defaultRules;
            }
            
            JsonNode defaultNode = objectMapper.readTree(defaultRules);
            JsonNode godModeNode = objectMapper.readTree(godModeRules);
            
            // 简单合并：上帝模式规则覆盖默认规则
            Map<String, Object> merged = objectMapper.convertValue(defaultNode, Map.class);
            Map<String, Object> godModeMap = objectMapper.convertValue(godModeNode, Map.class);
            merged.putAll(godModeMap);
            
            return objectMapper.writeValueAsString(merged);
            
        } catch (Exception e) {
            logger.warn("合并规则失败，使用默认规则", e);
            return defaultRules;
        }
    }
    
    /**
     * 创建初始世界状态
     */
    private String createInitialWorldState(String worldType, String rules) {
        Map<String, Object> initialState = new HashMap<>();
        initialState.put("worldType", worldType);
        initialState.put("initialized", true);
        initialState.put("currentLocation", getInitialLocation(worldType));
        initialState.put("characters", Map.of("protagonist", getInitialCharacter(worldType)));
        initialState.put("activeQuests", Map.of());
        initialState.put("completedQuests", Map.of());
        initialState.put("inventory", Map.of());
        initialState.put("factions", Map.of());
        
        try {
            return objectMapper.writeValueAsString(initialState);
        } catch (JsonProcessingException e) {
            logger.error("创建初始世界状态失败", e);
            return "{}";
        }
    }
    
    /**
     * 创建初始技能状态
     */
    private String createInitialSkillsState() {
        Map<String, Object> skillsState = new HashMap<>();
        skillsState.put("questLog", Map.of("quests", Map.of()));
        skillsState.put("diceHistory", Map.of("rolls", List.of()));
        skillsState.put("learningProgress", Map.of(
            "math", Map.of("level", 1, "score", 0),
            "history", Map.of("level", 1, "score", 0),
            "language", Map.of("level", 1, "score", 0)
        ));
        
        try {
            return objectMapper.writeValueAsString(skillsState);
        } catch (JsonProcessingException e) {
            logger.error("创建初始技能状态失败", e);
            return "{}";
        }
    }
    
    /**
     * 初始化稳定性锚点
     */
    private void initializeStabilityAnchors(String sessionId, String worldType, String template, String rules) {
        try {
            // 添加不可变的世界规则锚点
            stabilityAnchorRepository.save(new StabilityAnchor(
                sessionId, StabilityAnchor.AnchorType.WORLD_RULE, 
                "world_type", worldType, 10, true));
            
            stabilityAnchorRepository.save(new StabilityAnchor(
                sessionId, StabilityAnchor.AnchorType.WORLD_RULE,
                "basic_rules", rules, 9, true));
            
            // 根据世界类型添加特定锚点
            addWorldSpecificAnchors(sessionId, worldType);
            
        } catch (Exception e) {
            logger.warn("初始化稳定性锚点失败: sessionId={}", sessionId, e);
        }
    }
    
    /**
     * 添加世界特定的锚点
     */
    private void addWorldSpecificAnchors(String sessionId, String worldType) {
        switch (worldType) {
            case "isekai":
                stabilityAnchorRepository.save(new StabilityAnchor(
                    sessionId, StabilityAnchor.AnchorType.WORLD_RULE,
                    "magic_system", "mana_based", 8, true));
                break;
            case "western_fantasy":
                stabilityAnchorRepository.save(new StabilityAnchor(
                    sessionId, StabilityAnchor.AnchorType.WORLD_RULE,
                    "dnd_rules", "basic_dnd", 8, true));
                break;
            case "wuxia_history":
                stabilityAnchorRepository.save(new StabilityAnchor(
                    sessionId, StabilityAnchor.AnchorType.WORLD_RULE,
                    "historical_era", "ming_dynasty", 8, true));
                break;
            case "jp_school":
                stabilityAnchorRepository.save(new StabilityAnchor(
                    sessionId, StabilityAnchor.AnchorType.WORLD_RULE,
                    "school_setting", "japanese_high_school", 8, true));
                break;
            case "edutainment":
                stabilityAnchorRepository.save(new StabilityAnchor(
                    sessionId, StabilityAnchor.AnchorType.WORLD_RULE,
                    "educational_focus", "age_appropriate", 8, true));
                break;
        }
    }
    
    /**
     * 记录世界事件
     */
    private void recordEvent(String sessionId, WorldEvent.EventType eventType, String eventData, int sequence) {
        try {
            WorldEvent event = new WorldEvent(sessionId, eventType, eventData, sequence);
            event.setChecksum(DigestUtils.md5DigestAsHex(eventData.getBytes()));
            worldEventRepository.save(event);
        } catch (Exception e) {
            logger.warn("记录世界事件失败: sessionId={}, eventType={}", sessionId, eventType, e);
        }
    }
    
    /**
     * 安全地记录世界事件（自动处理序列号冲突）
     */
    private void recordEventSafe(String sessionId, WorldEvent.EventType eventType, String eventData) {
        try {
            int sequence = getNextEventSequence(sessionId);
            WorldEvent event = new WorldEvent(sessionId, eventType, eventData, sequence);
            event.setChecksum(DigestUtils.md5DigestAsHex(eventData.getBytes()));
            worldEventRepository.save(event);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            if (e.getMessage().contains("UK_session_sequence")) {
                logger.warn("事件序列号冲突，跳过重复事件记录: sessionId={}, eventType={}", sessionId, eventType);
            } else {
                logger.warn("记录世界事件失败: sessionId={}, eventType={}", sessionId, eventType, e);
            }
        } catch (Exception e) {
            logger.warn("记录世界事件失败: sessionId={}, eventType={}", sessionId, eventType, e);
        }
    }
    
    /**
     * 获取下一个事件序号
     */
    private int getNextEventSequence(String sessionId) {
        return worldEventRepository.findMaxSequenceBySessionId(sessionId).orElse(0) + 1;
    }
    
    /**
     * 创建事件数据
     */
    private String createEventData(String action, Map<String, Object> data) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("action", action);
            eventData.put("data", data);
            eventData.put("timestamp", System.currentTimeMillis());
            return objectMapper.writeValueAsString(eventData);
        } catch (JsonProcessingException e) {
            logger.warn("创建事件数据失败", e);
            return "{}";
        }
    }
    
    /**
     * 计算状态校验和
     */
    private String calculateChecksum(String content) {
        return DigestUtils.md5DigestAsHex(content.getBytes());
    }
    
    /**
     * 从JSON填充WorldState对象
     */
    private void populateWorldStateFromJson(WorldState worldState, String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node.has("currentLocation")) {
                worldState.setCurrentLocation(node.get("currentLocation").toString());
            }
            if (node.has("characters")) {
                worldState.setCharacters(node.get("characters").toString());
            }
            if (node.has("factions")) {
                worldState.setFactions(node.get("factions").toString());
            }
            if (node.has("inventory")) {
                worldState.setInventory(node.get("inventory").toString());
            }
            if (node.has("activeQuests")) {
                worldState.setActiveQuests(node.get("activeQuests").toString());
            }
            if (node.has("completedQuests")) {
                worldState.setCompletedQuests(node.get("completedQuests").toString());
            }
        } catch (Exception e) {
            logger.warn("填充WorldState失败", e);
        }
    }
    
    /**
     * 获取初始地点
     */
    private String getInitialLocation(String worldType) {
        switch (worldType) {
            case "isekai": return "新手村";
            case "western_fantasy": return "冒险者酒馆";
            case "wuxia_history": return "江湖客栈";
            case "jp_school": return "1年A班教室";
            case "edutainment": return "虚拟教室";
            default: return "起始地点";
        }
    }
    
    /**
     * 获取初始角色
     */
    private Map<String, Object> getInitialCharacter(String worldType) {
        Map<String, Object> character = new HashMap<>();
        character.put("name", "主角");
        character.put("level", 1);
        
        switch (worldType) {
            case "isekai":
                character.put("class", "未定");
                character.put("hp", 100);
                character.put("mp", 50);
                break;
            case "western_fantasy":
                character.put("race", "人类");
                character.put("class", "冒险者");
                break;
            case "wuxia_history":
                character.put("sect", "未定");
                character.put("martial_level", "初学");
                break;
            case "jp_school":
                character.put("grade", "高一");
                character.put("club", "未加入");
                break;
            case "edutainment":
                character.put("grade", "学生");
                character.put("subjects", List.of("数学"));
                break;
        }
        
        return character;
    }
}

