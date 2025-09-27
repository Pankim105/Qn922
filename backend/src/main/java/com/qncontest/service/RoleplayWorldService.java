package com.qncontest.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qncontest.entity.ChatSession;
import com.qncontest.entity.DiceRoll;
import com.qncontest.entity.User;
import com.qncontest.entity.WorldEvent;
import com.qncontest.entity.WorldState;
import com.qncontest.repository.DiceRollRepository;
import com.qncontest.repository.WorldEventRepository;
import com.qncontest.repository.WorldStateRepository;
import com.qncontest.service.interfaces.WorldStateManagerInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

/**
 * 角色扮演世界管理服务
 * 实现WorldStateManagerInterface接口，负责管理角色扮演世界的状态、事件和骰子检定
 */
@Service
public class RoleplayWorldService implements WorldStateManagerInterface {
    
    private static final Logger logger = LoggerFactory.getLogger(RoleplayWorldService.class);
    
    @Autowired
    private WorldStateRepository worldStateRepository;
    
    @Autowired
    private WorldEventRepository worldEventRepository;
    
    @Autowired
    private DiceRollRepository diceRollRepository;

    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private ChatSessionService chatSessionService;

    private final Random random = new Random();
    
    /**
     * 初始化角色扮演会话
     * @param sessionId 会话ID
     * @param worldType 世界类型
     * @param godModeRules 上帝模式规则
     * @param user 用户信息
     */
    @Override
    public void initializeRoleplaySession(String sessionId, String worldType, String godModeRules, User user) {
        logger.info("初始化角色扮演会话: sessionId={}, worldType={}, userId={}", 
                   sessionId, worldType, user.getId());
        
        try {
            // 检查是否已经存在世界状态
            Optional<WorldState> existingState = worldStateRepository.findTop1BySessionIdOrderByVersionDescCreatedAtDesc(sessionId);
            if (existingState.isPresent()) {
                logger.info("会话 {} 已存在世界状态，跳过初始化", sessionId);
                return;
            }
            
            // 创建初始世界状态
            WorldState initialState = createInitialWorldState(sessionId, worldType, godModeRules);
            worldStateRepository.save(initialState);
            
            // 记录初始化事件
            recordWorldEvent(sessionId, WorldEvent.EventType.SYSTEM_EVENT, 
                           createInitializationEventData(worldType, godModeRules, user));
            
            logger.info("角色扮演会话初始化完成: sessionId={}", sessionId);
            
        } catch (Exception e) {
            logger.error("初始化角色扮演会话失败: sessionId={}", sessionId, e);
            throw new RuntimeException("初始化角色扮演会话失败", e);
        }
    }
    
    /**
     * 执行骰子检定
     * @param sessionId 会话ID
     * @param diceType 骰子类型
     * @param modifier 修正值
     * @param context 检定上下文
     * @param difficultyClass 难度等级
     * @return 骰子检定结果
     */
    @Override
    public DiceRoll rollDice(String sessionId, Integer diceType, Integer modifier, String context, Integer difficultyClass) {
        logger.info("执行骰子检定: sessionId={}, diceType={}, modifier={}, context={}, dc={}", 
                   sessionId, diceType, modifier, context, difficultyClass);
        
        try {
            // 生成骰子结果
            int result = rollDiceResult(diceType);
            int finalResult = result + (modifier != null ? modifier : 0);
            
            // 创建骰子记录
            DiceRoll diceRoll = new DiceRoll();
            diceRoll.setSessionId(sessionId);
            diceRoll.setDiceType(diceType);
            diceRoll.setModifier(modifier != null ? modifier : 0);
            diceRoll.setResult(result);
            diceRoll.setFinalResult(finalResult);
            diceRoll.setContext(context);
            diceRoll.setDifficultyClass(difficultyClass);
            diceRoll.setCreatedAt(LocalDateTime.now());
            
            // 判断是否成功
            if (difficultyClass != null) {
                diceRoll.setIsSuccessful(finalResult >= difficultyClass);
            }
            
            // 保存骰子记录
            diceRollRepository.save(diceRoll);
            
            // 记录骰子事件
            recordWorldEvent(sessionId, WorldEvent.EventType.DICE_ROLL, 
                           createDiceRollEventData(diceRoll));
            
            logger.info("骰子检定完成: sessionId={}, result={}, finalResult={}, success={}", 
                       sessionId, result, finalResult, diceRoll.getIsSuccessful());
            
            return diceRoll;
            
        } catch (Exception e) {
            logger.error("执行骰子检定失败: sessionId={}", sessionId, e);
            throw new RuntimeException("执行骰子检定失败", e);
        }
    }
    
    /**
     * 更新世界状态
     * @param sessionId 会话ID
     * @param newWorldState 新的世界状态
     * @param skillsState 技能状态
     */
    @Override
    public void updateWorldState(String sessionId, String newWorldState, String skillsState) {
        logger.info("更新世界状态: sessionId={}", sessionId);
        
        try {
            // 获取当前会话
            ChatSession session = chatSessionService.getSessionWithMessages(sessionId);
            if (session == null) {
                logger.warn("会话 {} 不存在，无法更新世界状态", sessionId);
                return;
            }
            
            boolean hasUpdates = false;
            
            // 更新世界状态
            if (newWorldState != null && !newWorldState.trim().isEmpty()) {
                logger.info("更新会话世界状态: sessionId={}", sessionId);
                session.setWorldState(newWorldState);
                hasUpdates = true;
            }
            
            // 更新技能状态
            if (skillsState != null && !skillsState.trim().isEmpty()) {
                logger.info("更新会话技能状态: sessionId={}", sessionId);
                session.setSkillsState(skillsState);
                hasUpdates = true;
            }
            
            if (hasUpdates) {
                // 更新版本号和校验和
                session.setVersion(session.getVersion() + 1);
                session.setChecksum(calculateSessionChecksum(session));
                
                // 保存会话
                chatSessionService.saveSession(session);
                
                // 记录状态变更事件
                recordWorldEvent(sessionId, WorldEvent.EventType.STATE_CHANGE, 
                               createStateChangeEventData(session.getVersion() - 1, session.getVersion(), newWorldState, skillsState));
                
                logger.info("世界状态更新完成: sessionId={}, version={}", sessionId, session.getVersion());
            } else {
                logger.info("无状态更新，跳过: sessionId={}", sessionId);
            }
            
        } catch (Exception e) {
            logger.error("更新世界状态失败: sessionId={}", sessionId, e);
            throw new RuntimeException("更新世界状态失败", e);
        }
    }
    
    /**
     * 初始化角色数据
     * @param sessionId 会话ID
     * @param characterName 角色名称
     * @param profession 职业
     * @param skills 技能列表
     * @param background 背景故事
     */
    public void initializeCharacter(String sessionId, String characterName, String profession, 
                                  java.util.List<String> skills, String background) {
        logger.info("初始化角色数据: sessionId={}, characterName={}, profession={}", 
                   sessionId, characterName, profession);
        
        try {
            // 获取当前会话
            ChatSession session = chatSessionService.getSessionWithMessages(sessionId);
            if (session == null) {
                logger.warn("会话 {} 不存在，无法初始化角色", sessionId);
                throw new RuntimeException("会话不存在");
            }
            
            // 创建角色数据JSON
            Map<String, Object> characterData = new HashMap<>();
            characterData.put("name", characterName);
            characterData.put("profession", profession);
            characterData.put("skills", skills);
            characterData.put("background", background);
            characterData.put("level", 1);
            characterData.put("experience", 0);
            characterData.put("createdAt", LocalDateTime.now().toString());
            
            // 创建技能状态JSON
            Map<String, Object> skillsState = new HashMap<>();
            skillsState.put("characterName", characterName);
            skillsState.put("profession", profession);
            skillsState.put("selectedSkills", skills);
            skillsState.put("skillLevels", createInitialSkillLevels(skills));
            skillsState.put("attributes", createInitialAttributes(profession));
            
            // 添加基础游戏状态
            skillsState.put("level", 1);
            skillsState.put("experience", 0);
            skillsState.put("gold", 0);
            skillsState.put("inventory", new ArrayList<>());
            skillsState.put("abilities", new ArrayList<>());
            
            // 添加生命值和魔力值
            skillsState.put("生命值", "100/100");
            skillsState.put("魔力值", "50/50");
            
            // 添加中文属性映射（兼容现有数据格式）
            Map<String, Object> chineseStats = new HashMap<>();
            Map<String, Integer> attributes = createInitialAttributes(profession);
            chineseStats.put("体质", attributes.get("constitution"));
            chineseStats.put("力量", attributes.get("strength"));
            chineseStats.put("敏捷", attributes.get("dexterity"));
            chineseStats.put("智力", attributes.get("intelligence"));
            chineseStats.put("智慧", attributes.get("wisdom"));
            chineseStats.put("魅力", attributes.get("charisma"));
            skillsState.put("stats", chineseStats);
            
            // 创建世界状态JSON
            Map<String, Object> worldState = new HashMap<>();
            worldState.put("character", characterData);
            worldState.put("currentLocation", "起始地点");
            worldState.put("inventory", new HashMap<>());
            worldState.put("relationships", new HashMap<>());
            worldState.put("quests", new HashMap<>());
            worldState.put("lastUpdated", LocalDateTime.now().toString());
            
            // 更新会话状态
            session.setSkillsState(objectMapper.writeValueAsString(skillsState));
            session.setWorldState(objectMapper.writeValueAsString(worldState));
            session.setVersion(session.getVersion() + 1);
            session.setChecksum(calculateSessionChecksum(session));
            
            // 保存会话
            chatSessionService.saveSession(session);
            
            // 记录角色初始化事件
            recordWorldEvent(sessionId, WorldEvent.EventType.CHARACTER_UPDATE, 
                           createCharacterInitializationEventData(characterName, profession, skills, background));
            
            logger.info("角色数据初始化完成: sessionId={}, characterName={}", sessionId, characterName);
            
        } catch (Exception e) {
            logger.error("初始化角色数据失败: sessionId={}", sessionId, e);
            throw new RuntimeException("初始化角色数据失败", e);
        }
    }

    /**
     * 修复角色属性（包括生命值、魔力值和所有属性）
     */
    public void fixCharacterAttributes(String sessionId) {
        logger.info("修复角色属性: sessionId={}", sessionId);
        
        try {
            // 获取当前会话
            ChatSession session = chatSessionService.getSessionWithMessages(sessionId);
            if (session == null) {
                logger.warn("会话 {} 不存在，无法修复角色属性", sessionId);
                throw new RuntimeException("会话不存在");
            }
            
            String currentSkillsState = session.getSkillsState();
            if (currentSkillsState == null || currentSkillsState.isEmpty()) {
                logger.warn("技能状态为空，无法修复角色属性: sessionId={}", sessionId);
                return;
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> skillsState = (Map<String, Object>) objectMapper.readValue(currentSkillsState, Map.class);
            
            boolean needsUpdate = false;
            
            // 1. 修复生命值和魔力值
            if (!skillsState.containsKey("生命值")) {
                skillsState.put("生命值", "100/100");
                needsUpdate = true;
                logger.info("添加缺失的生命值: sessionId={}", sessionId);
            }
            
            if (!skillsState.containsKey("魔力值")) {
                skillsState.put("魔力值", "50/50");
                needsUpdate = true;
                logger.info("添加缺失的魔力值: sessionId={}", sessionId);
            }
            
            // 2. 修复attributes字段（英文属性）
            Object attributesObj = skillsState.get("attributes");
            Map<String, Object> attributes;
            if (attributesObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> existingAttributes = (Map<String, Object>) attributesObj;
                attributes = new HashMap<>(existingAttributes);
            } else {
                attributes = new HashMap<>();
                needsUpdate = true;
            }
            
            // 3. 确保所有英文属性都存在
            String[] englishStatNames = {"strength", "dexterity", "intelligence", "constitution", "wisdom", "charisma"};
            for (String statName : englishStatNames) {
                if (!attributes.containsKey(statName)) {
                    attributes.put(statName, 10); // 默认值10
                    needsUpdate = true;
                    logger.info("添加缺失的英文属性: {} = 10", statName);
                }
            }
            
            if (needsUpdate) {
                skillsState.put("attributes", attributes);
                
                // 保存更新后的技能状态
                String updatedSkillsStateJson = objectMapper.writeValueAsString(skillsState);
                session.setSkillsState(updatedSkillsStateJson);
                session.setVersion(session.getVersion() + 1);
                session.setChecksum(calculateSessionChecksum(session));
                
                chatSessionService.saveSession(session);
                
                logger.info("角色属性修复完成: sessionId={}", sessionId);
                logger.info("修复后的属性: {}", attributes);
            } else {
                logger.info("角色属性无需修复: sessionId={}", sessionId);
            }
            
        } catch (Exception e) {
            logger.error("修复角色属性失败: sessionId={}", sessionId, e);
            throw new RuntimeException("修复角色属性失败", e);
        }
    }

    /**
     * 创建初始技能等级
     */
    private Map<String, Integer> createInitialSkillLevels(java.util.List<String> skills) {
        Map<String, Integer> skillLevels = new HashMap<>();
        for (String skill : skills) {
            skillLevels.put(skill, 1); // 初始技能等级为1
        }
        return skillLevels;
    }

    /**
     * 根据职业创建初始属性
     */
    private Map<String, Integer> createInitialAttributes(String profession) {
        Map<String, Integer> attributes = new HashMap<>();
        
        // 生成10-15之间的随机属性值
        java.util.Random random = new java.util.Random();
        
        // 根据职业设置不同的初始属性
        switch (profession) {
            case "warrior":
            case "knight":
            case "swordsman":
                attributes.put("strength", random.nextInt(6) + 10); // 10-15
                attributes.put("dexterity", random.nextInt(6) + 10);
                attributes.put("constitution", random.nextInt(6) + 10);
                attributes.put("intelligence", random.nextInt(6) + 10);
                attributes.put("wisdom", random.nextInt(6) + 10);
                attributes.put("charisma", random.nextInt(6) + 10);
                break;
            case "mage":
            case "wizard":
            case "scholar":
                attributes.put("strength", random.nextInt(6) + 10);
                attributes.put("dexterity", random.nextInt(6) + 10);
                attributes.put("constitution", random.nextInt(6) + 10);
                attributes.put("intelligence", random.nextInt(6) + 10);
                attributes.put("wisdom", random.nextInt(6) + 10);
                attributes.put("charisma", random.nextInt(6) + 10);
                break;
            case "rogue":
            case "assassin":
                attributes.put("strength", random.nextInt(6) + 10);
                attributes.put("dexterity", random.nextInt(6) + 10);
                attributes.put("constitution", random.nextInt(6) + 10);
                attributes.put("intelligence", random.nextInt(6) + 10);
                attributes.put("wisdom", random.nextInt(6) + 10);
                attributes.put("charisma", random.nextInt(6) + 10);
                break;
            case "cleric":
            case "paladin":
                attributes.put("strength", random.nextInt(6) + 10);
                attributes.put("dexterity", random.nextInt(6) + 10);
                attributes.put("constitution", random.nextInt(6) + 10);
                attributes.put("intelligence", random.nextInt(6) + 10);
                attributes.put("wisdom", random.nextInt(6) + 10);
                attributes.put("charisma", random.nextInt(6) + 10);
                break;
            default:
                // 默认随机属性
                attributes.put("strength", random.nextInt(6) + 10);
                attributes.put("dexterity", random.nextInt(6) + 10);
                attributes.put("constitution", random.nextInt(6) + 10);
                attributes.put("intelligence", random.nextInt(6) + 10);
                attributes.put("wisdom", random.nextInt(6) + 10);
                attributes.put("charisma", random.nextInt(6) + 10);
                break;
        }
        
        return attributes;
    }

    /**
     * 创建角色初始化事件数据
     */
    private String createCharacterInitializationEventData(String characterName, String profession, 
                                                        java.util.List<String> skills, String background) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("characterName", characterName);
            eventData.put("profession", profession);
            eventData.put("skills", skills);
            eventData.put("background", background);
            eventData.put("timestamp", LocalDateTime.now().toString());
            
            return objectMapper.writeValueAsString(eventData);
        } catch (JsonProcessingException e) {
            logger.warn("创建角色初始化事件数据失败", e);
            return "{}";
        }
    }

    /**
     * 获取世界状态摘要
     * @param sessionId 会话ID
     * @return 世界状态摘要
     */
    @Override
    public String getWorldStateSummary(String sessionId) {
        logger.debug("获取世界状态摘要: sessionId={}", sessionId);
        
        try {
            ChatSession session = chatSessionService.getSessionWithMessages(sessionId);
            if (session == null) {
                return "{}";
            }
            
            Map<String, Object> summary = new HashMap<>();
            summary.put("version", session.getVersion());
            summary.put("hasWorldState", session.getWorldState() != null && !session.getWorldState().trim().isEmpty());
            summary.put("hasSkillsState", session.getSkillsState() != null && !session.getSkillsState().trim().isEmpty());
            summary.put("hasActiveQuests", session.getActiveQuests() != null && !session.getActiveQuests().trim().isEmpty());
            // characterStats字段已移除，使用skillsState代替
            summary.put("lastUpdated", session.getUpdatedAt());
            
            return objectMapper.writeValueAsString(summary);
            
        } catch (Exception e) {
            logger.error("获取世界状态摘要失败: sessionId={}", sessionId, e);
            return "{}";
        }
    }
    
    /**
     * 创建初始世界状态
     */
    private WorldState createInitialWorldState(String sessionId, String worldType, String godModeRules) {
        WorldState state = new WorldState();
        state.setSessionId(sessionId);
        state.setVersion(1);
        
        // 设置初始位置
        Map<String, Object> initialLocation = new HashMap<>();
        initialLocation.put("name", "起始地点");
        initialLocation.put("description", "你的冒险从这里开始");
        initialLocation.put("worldType", worldType);
        
        try {
            state.setCurrentLocation(objectMapper.writeValueAsString(initialLocation));
        } catch (JsonProcessingException e) {
            logger.warn("设置初始位置失败", e);
            state.setCurrentLocation("{}");
        }
        
        // 初始化其他状态
        state.setCharacters("{}");
        state.setFactions("{}");
        state.setInventory("{}");
        state.setActiveQuests("{}");
        state.setCompletedQuests("{}");
        state.setEventHistory("[]");
        
        // 计算校验和
        String checksum = calculateStateChecksum(state);
        state.setChecksum(checksum);
        
        return state;
    }
    
    
    /**
     * 生成骰子结果
     */
    private int rollDiceResult(int diceType) {
        return random.nextInt(diceType) + 1;
    }
    
    /**
     * 计算状态校验和
     */
    private String calculateStateChecksum(WorldState state) {
        StringBuilder sb = new StringBuilder();
        sb.append(state.getSessionId());
        sb.append(state.getVersion());
        sb.append(state.getCurrentLocation());
        sb.append(state.getCharacters());
        sb.append(state.getFactions());
        sb.append(state.getInventory());
        sb.append(state.getActiveQuests());
        sb.append(state.getCompletedQuests());
        sb.append(state.getEventHistory());
        
        return DigestUtils.md5DigestAsHex(sb.toString().getBytes());
    }
    
    /**
     * 计算会话校验和
     */
    private String calculateSessionChecksum(ChatSession session) {
        StringBuilder sb = new StringBuilder();
        sb.append(session.getSessionId());
        sb.append(session.getVersion());
        sb.append(session.getWorldState());
        sb.append(session.getSkillsState());
        sb.append(session.getActiveQuests());
        sb.append(session.getCompletedQuests());
        // characterStats字段已移除，使用skillsState代替
        sb.append(session.getCurrentArcName());
        sb.append(session.getTotalRounds());
        
        return DigestUtils.md5DigestAsHex(sb.toString().getBytes());
    }
    
    /**
     * 记录世界事件
     */
    private void recordWorldEvent(String sessionId, WorldEvent.EventType eventType, String eventData) {
        try {
            // 获取下一个事件序号
            Optional<Integer> maxSequence = worldEventRepository.findMaxSequenceBySessionId(sessionId);
            int nextSequence = maxSequence.orElse(0) + 1;
            
            // 创建事件记录
            WorldEvent event = new WorldEvent();
            event.setSessionId(sessionId);
            event.setEventType(eventType);
            event.setEventData(eventData);
            event.setSequence(nextSequence);
            event.setTimestamp(LocalDateTime.now());
            
            // 计算事件校验和
            String checksum = DigestUtils.md5DigestAsHex((sessionId + nextSequence + eventData).getBytes());
            event.setChecksum(checksum);
            
            // 保存事件
            worldEventRepository.save(event);
            
        } catch (Exception e) {
            logger.error("记录世界事件失败: sessionId={}, eventType={}", sessionId, eventType, e);
        }
    }
    
    /**
     * 创建初始化事件数据
     */
    private String createInitializationEventData(String worldType, String godModeRules, User user) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("worldType", worldType);
            eventData.put("godModeRules", godModeRules);
            eventData.put("userId", user.getId());
            eventData.put("username", user.getUsername());
            eventData.put("timestamp", LocalDateTime.now().toString());
            
            return objectMapper.writeValueAsString(eventData);
        } catch (JsonProcessingException e) {
            logger.warn("创建初始化事件数据失败", e);
            return "{}";
        }
    }
    
    /**
     * 创建骰子检定事件数据
     */
    private String createDiceRollEventData(DiceRoll diceRoll) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("rollId", diceRoll.getRollId());
            eventData.put("diceType", diceRoll.getDiceType());
            eventData.put("modifier", diceRoll.getModifier());
            eventData.put("result", diceRoll.getResult());
            eventData.put("finalResult", diceRoll.getFinalResult());
            eventData.put("context", diceRoll.getContext());
            eventData.put("difficultyClass", diceRoll.getDifficultyClass());
            eventData.put("isSuccessful", diceRoll.getIsSuccessful());
            eventData.put("timestamp", diceRoll.getCreatedAt().toString());
            
            return objectMapper.writeValueAsString(eventData);
        } catch (JsonProcessingException e) {
            logger.warn("创建骰子检定事件数据失败", e);
            return "{}";
        }
    }
    
    /**
     * 创建状态变更事件数据
     */
    private String createStateChangeEventData(Integer oldVersion, Integer newVersion, String newWorldState, String skillsState) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("oldVersion", oldVersion);
            eventData.put("newVersion", newVersion);
            eventData.put("hasWorldState", newWorldState != null && !newWorldState.trim().isEmpty());
            eventData.put("hasSkillsState", skillsState != null && !skillsState.trim().isEmpty());
            eventData.put("timestamp", LocalDateTime.now().toString());
            
            return objectMapper.writeValueAsString(eventData);
        } catch (JsonProcessingException e) {
            logger.warn("创建状态变更事件数据失败", e);
            return "{}";
        }
    }
}

