package com.qncontest.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qncontest.entity.ChatSession;
import com.qncontest.entity.DMAssessment;
import com.qncontest.entity.DiceRoll;
import com.qncontest.entity.WorldEvent;
import com.qncontest.repository.DiceRollRepository;
import com.qncontest.repository.WorldEventRepository;
import com.qncontest.service.interfaces.WorldStateManagerInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.util.*;

/**
 * 评估游戏逻辑处理器
 * 负责处理评估JSON中的游戏逻辑（骰子、学习挑战、状态更新等）
 */
@Component
public class AssessmentGameLogicProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(AssessmentGameLogicProcessor.class);
    
    @Autowired
    @Lazy
    private WorldStateManagerInterface worldStateManager;
    
    @Autowired
    private DiceRollRepository diceRollRepository;
    
    @Autowired
    private WorldEventRepository worldEventRepository;
    
    @Autowired
    private ChatSessionService chatSessionService;
    
    @Autowired
    private ConvergenceStatusService convergenceStatusService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * 处理评估JSON中的游戏逻辑
     */
    @Transactional
    public void processAssessmentGameLogic(String sessionId, DMAssessment assessment) {
        try {
            logger.info("=== 开始处理评估JSON中的游戏逻辑 ===");
            logger.info("会话ID: {}", sessionId);
            logger.info("评估ID: {}", assessment.getId());
            logger.info("评估策略: {}", assessment.getStrategy());
            logger.info("综合评分: {}", assessment.getOverallScore());
            
            // 统计需要处理的字段
            int fieldCount = 0;
            StringBuilder fieldSummary = new StringBuilder();
            
            // 处理骰子检定
            if (assessment.getDiceRolls() != null) {
                fieldCount++;
                fieldSummary.append("diceRolls ");
                logger.info("📊 检测到骰子检定数据，开始处理...");
                processDiceRolls(sessionId, assessment.getDiceRolls());
            }
            
            // 处理学习挑战
            if (assessment.getLearningChallenges() != null) {
                fieldCount++;
                fieldSummary.append("learningChallenges ");
                logger.info("🎓 检测到学习挑战数据，开始处理...");
                processLearningChallenges(sessionId, assessment.getLearningChallenges());
            }
            
            // 处理状态更新
            if (assessment.getStateUpdates() != null) {
                fieldCount++;
                fieldSummary.append("stateUpdates ");
                logger.info("📝 检测到状态更新数据，开始处理...");
                processStateUpdates(sessionId, assessment.getStateUpdates());
            }
            
            // 处理任务更新（优先处理，因为可能包含奖励）
            if (assessment.getQuestUpdates() != null) {
                fieldCount++;
                fieldSummary.append("questUpdates ");
                logger.info("🎯 检测到任务更新数据，开始处理...");
                processQuestUpdates(sessionId, assessment.getQuestUpdates());
            }
            
            // 处理世界状态更新
            if (assessment.getWorldStateUpdates() != null) {
                fieldCount++;
                fieldSummary.append("worldStateUpdates ");
                logger.info("🌍 检测到世界状态更新数据，开始处理...");
                processWorldStateUpdates(sessionId, assessment.getWorldStateUpdates());
            }
            
            // 处理技能状态更新（在任务奖励处理之后，需要合并结果）
            if (assessment.getSkillsStateUpdates() != null) {
                fieldCount++;
                fieldSummary.append("skillsStateUpdates ");
                logger.info("⚔️ 检测到技能状态更新数据，开始处理...");
                processSkillsStateUpdatesWithQuestRewards(sessionId, assessment.getSkillsStateUpdates(), assessment.getQuestUpdates());
            }
            
            // 处理情节更新
            if (assessment.getArcUpdates() != null) {
                fieldCount++;
                fieldSummary.append("arcUpdates ");
                logger.info("📖 检测到情节更新数据，开始处理...");
                processArcUpdates(sessionId, assessment.getArcUpdates());
            }
            
            // 处理收敛状态更新
            if (assessment.getConvergenceStatusUpdates() != null) {
                fieldCount++;
                fieldSummary.append("convergenceStatusUpdates ");
                logger.info("🎯 检测到收敛状态更新数据，开始处理...");
                processConvergenceStatusUpdates(sessionId, assessment.getConvergenceStatusUpdates());
            }
            
            logger.info("=== 评估JSON游戏逻辑处理完成 ===");
            logger.info("会话ID: {}", sessionId);
            logger.info("处理字段数量: {}", fieldCount);
            logger.info("处理字段列表: {}", fieldSummary.toString().trim());
            logger.info("所有数据库更新操作已完成");
            
        } catch (Exception e) {
            logger.error("❌ 处理评估JSON游戏逻辑失败: sessionId={}", sessionId, e);
            logger.error("错误详情: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 处理骰子检定
     */
    @SuppressWarnings("unchecked")
    private void processDiceRolls(String sessionId, Object diceRollsData) {
        try {
            logger.info("🎲 开始处理骰子检定数据: sessionId={}", sessionId);
            List<Map<String, Object>> diceRolls = (List<Map<String, Object>>) diceRollsData;
            logger.info("骰子检定数量: {}", diceRolls.size());
            
            int savedCount = 0;
            for (int i = 0; i < diceRolls.size(); i++) {
                Map<String, Object> diceRollData = diceRolls.get(i);
                logger.info("处理第{}个骰子检定: {}", i + 1, diceRollData);
                
                Integer diceType = getIntegerValue(diceRollData, "diceType");
                Integer modifier = getIntegerValue(diceRollData, "modifier");
                String context = getStringValue(diceRollData, "context");
                Integer result = getIntegerValue(diceRollData, "result");
                Boolean isSuccessful = getBooleanValue(diceRollData, "isSuccessful");
                
                logger.info("解析结果 - diceType: {}, modifier: {}, context: {}, result: {}, isSuccessful: {}", 
                           diceType, modifier, context, result, isSuccessful);
                
                if (diceType != null && result != null) {
                    // 创建骰子检定记录
                    DiceRoll diceRoll = new DiceRoll();
                    diceRoll.setSessionId(sessionId);
                    diceRoll.setDiceType(diceType);
                    diceRoll.setModifier(modifier != null ? modifier : 0);
                    diceRoll.setContext(context != null ? context : "未知检定");
                    diceRoll.setResult(result);
                    diceRoll.setIsSuccessful(isSuccessful != null ? isSuccessful : false);
                    
                    DiceRoll savedDiceRoll = diceRollRepository.save(diceRoll);
                    savedCount++;
                    
                    logger.info("✅ 骰子检定记录已保存到数据库: ID={}, sessionId={}, diceType={}, result={}, isSuccessful={}", 
                               savedDiceRoll.getId(), sessionId, diceType, result, isSuccessful);
                } else {
                    logger.warn("⚠️ 跳过无效的骰子检定数据: diceType={}, result={}", diceType, result);
                }
            }
            
            logger.info("🎲 骰子检定处理完成: sessionId={}, 总数={}, 保存成功={}", sessionId, diceRolls.size(), savedCount);
        } catch (Exception e) {
            logger.error("❌ 处理骰子检定失败: sessionId={}", sessionId, e);
        }
    }
    
    /**
     * 处理学习挑战
     */
    @SuppressWarnings("unchecked")
    private void processLearningChallenges(String sessionId, Object challengesData) {
        try {
            logger.info("🎓 开始处理学习挑战数据: sessionId={}", sessionId);
            List<Map<String, Object>> challenges = (List<Map<String, Object>>) challengesData;
            logger.info("学习挑战数量: {}", challenges.size());
            
            int processedCount = 0;
            for (int i = 0; i < challenges.size(); i++) {
                Map<String, Object> challengeData = challenges.get(i);
                logger.info("处理第{}个学习挑战: {}", i + 1, challengeData);
                
                String type = getStringValue(challengeData, "type");
                String difficulty = getStringValue(challengeData, "difficulty");
                String question = getStringValue(challengeData, "question");
                String answer = getStringValue(challengeData, "answer");
                Boolean isCorrect = getBooleanValue(challengeData, "isCorrect");
                
                logger.info("解析结果 - type: {}, difficulty: {}, question: {}, answer: {}, isCorrect: {}", 
                           type, difficulty, question, answer, isCorrect);
                
                // 记录学习挑战事件
                Map<String, Object> eventData = new HashMap<>();
                eventData.put("type", type);
                eventData.put("difficulty", difficulty);
                eventData.put("question", question);
                eventData.put("answer", answer);
                eventData.put("isCorrect", isCorrect);
                
                recordEvent(sessionId, WorldEvent.EventType.SYSTEM_EVENT, 
                           "学习挑战", eventData);
                processedCount++;
                
                logger.info("✅ 学习挑战事件已记录: sessionId={}, type={}, difficulty={}, isCorrect={}", 
                           sessionId, type, difficulty, isCorrect);
            }
            
            logger.info("🎓 学习挑战处理完成: sessionId={}, 总数={}, 处理成功={}", sessionId, challenges.size(), processedCount);
        } catch (Exception e) {
            logger.error("❌ 处理学习挑战失败: sessionId={}", sessionId, e);
        }
    }
    
    /**
     * 处理状态更新
     */
    @SuppressWarnings("unchecked")
    private void processStateUpdates(String sessionId, Object stateUpdatesData) {
        try {
            logger.info("📝 开始处理状态更新数据: sessionId={}", sessionId);
            List<Map<String, Object>> stateUpdates = (List<Map<String, Object>>) stateUpdatesData;
            logger.info("状态更新数量: {}", stateUpdates.size());
            
            int processedCount = 0;
            for (int i = 0; i < stateUpdates.size(); i++) {
                Map<String, Object> stateUpdate = stateUpdates.get(i);
                logger.info("处理第{}个状态更新: {}", i + 1, stateUpdate);
                
                String type = getStringValue(stateUpdate, "type");
                String value = getStringValue(stateUpdate, "value");
                
                logger.info("解析结果 - type: {}, value: {}", type, value);
                
                if (type != null && value != null) {
                    // 创建JSON格式的世界状态更新
                    Map<String, Object> stateUpdateMap = new HashMap<>();
                    stateUpdateMap.put(type.toLowerCase(), value);
                    
                    String stateUpdateJson;
                    try {
                        stateUpdateJson = objectMapper.writeValueAsString(stateUpdateMap);
                        logger.info("准备更新世界状态: {}", stateUpdateJson);
                    } catch (Exception e) {
                        logger.error("创建状态更新JSON失败: type={}, value={}", type, value, e);
                        continue;
                    }
                    
                    worldStateManager.updateWorldState(sessionId, stateUpdateJson, null);
                    
                    // 记录状态更新事件
                    Map<String, Object> eventData = new HashMap<>();
                    eventData.put("type", type);
                    eventData.put("value", value);
                    
                    recordEvent(sessionId, WorldEvent.EventType.STATE_CHANGE, 
                               "状态更新", eventData);
                    processedCount++;
                    
                    logger.info("✅ 状态更新已完成: sessionId={}, type={}, value={}", 
                               sessionId, type, value);
                } else {
                    logger.warn("⚠️ 跳过无效的状态更新数据: type={}, value={}", type, value);
                }
            }
            
            logger.info("📝 状态更新处理完成: sessionId={}, 总数={}, 处理成功={}", sessionId, stateUpdates.size(), processedCount);
        } catch (Exception e) {
            logger.error("❌ 处理状态更新失败: sessionId={}", sessionId, e);
        }
    }
    
    /**
     * 处理任务更新
     */
    @SuppressWarnings("unchecked")
    private void processQuestUpdates(String sessionId, Object questUpdatesData) {
        try {
            logger.info("🎯 开始处理任务更新数据: sessionId={}", sessionId);
            Map<String, Object> questUpdates = (Map<String, Object>) questUpdatesData;
            logger.info("任务更新数据: {}", questUpdates);
            
            // 统计各种任务更新
            int totalUpdates = 0;
            if (questUpdates.containsKey("created")) {
                List<?> created = (List<?>) questUpdates.get("created");
                logger.info("新创建任务数量: {}", created.size());
                totalUpdates += created.size();
            }
            if (questUpdates.containsKey("completed")) {
                List<?> completed = (List<?>) questUpdates.get("completed");
                logger.info("完成任务数量: {}", completed.size());
                totalUpdates += completed.size();
                
                // 处理任务奖励
                processQuestRewards(sessionId, completed);
            }
            if (questUpdates.containsKey("progress")) {
                List<?> progress = (List<?>) questUpdates.get("progress");
                logger.info("进度更新任务数量: {}", progress.size());
                totalUpdates += progress.size();
            }
            if (questUpdates.containsKey("expired")) {
                List<?> expired = (List<?>) questUpdates.get("expired");
                logger.info("过期任务数量: {}", expired.size());
                totalUpdates += expired.size();
            }
            
            // 记录任务更新事件
            recordEvent(sessionId, WorldEvent.EventType.SYSTEM_EVENT, 
                       "任务更新", questUpdates);
            
            logger.info("✅ 任务更新事件已记录: sessionId={}, 总更新数={}", sessionId, totalUpdates);
            logger.info("🎯 任务更新处理完成: sessionId={}", sessionId);
            
        } catch (Exception e) {
            logger.error("❌ 处理任务更新失败: sessionId={}", sessionId, e);
        }
    }
    
    /**
     * 处理任务奖励
     */
    @SuppressWarnings("unchecked")
    private void processQuestRewards(String sessionId, List<?> completedQuests) {
        try {
            logger.info("🎁 开始处理任务奖励: sessionId={}, 完成任务数={}", sessionId, completedQuests.size());
            
            // 获取当前角色状态
            logger.info("🔍 查询会话状态进行任务奖励处理: sessionId={}", sessionId);
            ChatSession session = chatSessionService.getSessionWithMessages(sessionId);
            if (session == null) {
                logger.warn("⚠️ 未找到会话，跳过任务奖励处理: sessionId={}", sessionId);
                return;
            }
            
            String currentSkillsState = session.getSkillsState();
            logger.info("📊 任务奖励处理前技能状态长度: {}", currentSkillsState != null ? currentSkillsState.length() : 0);
            Map<String, Object> skillsState = new HashMap<>();
            
            // 解析当前技能状态
            if (currentSkillsState != null && !currentSkillsState.isEmpty()) {
                try {
                    skillsState = objectMapper.readValue(currentSkillsState, Map.class);
                } catch (Exception e) {
                    logger.warn("⚠️ 解析当前技能状态失败，使用默认值: sessionId={}", sessionId);
                    skillsState = new HashMap<>();
                }
            }
            
            // 初始化默认值
            skillsState.putIfAbsent("level", 1);
            skillsState.putIfAbsent("experience", 0);
            skillsState.putIfAbsent("gold", 0);
            skillsState.putIfAbsent("inventory", new ArrayList<>());
            skillsState.putIfAbsent("abilities", new ArrayList<>());
            skillsState.putIfAbsent("stats", new HashMap<>());
            
            // 处理每个完成任务的奖励
            for (Object questObj : completedQuests) {
                if (questObj instanceof Map) {
                    Map<String, Object> quest = (Map<String, Object>) questObj;
                    String questId = getStringValue(quest, "questId");
                    Object rewardsObj = quest.get("rewards");
                    
                    if (rewardsObj instanceof Map) {
                        Map<String, Object> rewards = (Map<String, Object>) rewardsObj;
                        logger.info("处理任务奖励: questId={}, rewards={}", questId, rewards);
                        
                        // 处理经验值奖励
                        Object expReward = rewards.get("exp");
                        if (expReward instanceof Number) {
                            int currentExp = (Integer) skillsState.get("experience");
                            int expGain = ((Number) expReward).intValue();
                            int newExp = currentExp + expGain;
                            skillsState.put("experience", newExp);
                            logger.info("经验值奖励: +{} ({} -> {})", expGain, currentExp, newExp);
                            
                            // 检查升级
                            checkAndProcessLevelUp(sessionId, skillsState);
                        }
                        
                        // 处理金币奖励
                        Object goldReward = rewards.get("gold");
                        if (goldReward instanceof Number) {
                            int currentGold = (Integer) skillsState.get("gold");
                            int goldGain = ((Number) goldReward).intValue();
                            int newGold = currentGold + goldGain;
                            skillsState.put("gold", newGold);
                            logger.info("金币奖励: +{} ({} -> {})", goldGain, currentGold, newGold);
                        }
                        
                        // 处理物品奖励
                        Object itemsReward = rewards.get("items");
                        if (itemsReward instanceof List) {
                            List<String> currentInventory = (List<String>) skillsState.get("inventory");
                            List<String> newItems = (List<String>) itemsReward;
                            currentInventory.addAll(newItems);
                            skillsState.put("inventory", currentInventory);
                            logger.info("物品奖励: +{}", newItems);
                        }
                        
                        // 处理属性奖励
                        Object statsReward = rewards.get("stats");
                        if (statsReward instanceof Map) {
                            Map<String, Object> currentStats = (Map<String, Object>) skillsState.get("stats");
                            Map<String, Object> statGains = (Map<String, Object>) statsReward;
                            
                            for (Map.Entry<String, Object> entry : statGains.entrySet()) {
                                String statName = entry.getKey();
                                Object statValue = entry.getValue();
                                if (statValue instanceof Number) {
                                    int currentStat = (Integer) currentStats.getOrDefault(statName, 0);
                                    int statGain = ((Number) statValue).intValue();
                                    int newStat = currentStat + statGain;
                                    currentStats.put(statName, newStat);
                                    logger.info("属性奖励: {} +{} ({} -> {})", statName, statGain, currentStat, newStat);
                                }
                            }
                        }
                        
                        // 处理技能奖励
                        Object abilitiesReward = rewards.get("abilities");
                        if (abilitiesReward instanceof List) {
                            List<String> currentAbilities = (List<String>) skillsState.get("abilities");
                            List<String> newAbilities = (List<String>) abilitiesReward;
                            currentAbilities.addAll(newAbilities);
                            skillsState.put("abilities", currentAbilities);
                            logger.info("技能奖励: +{}", newAbilities);
                        }
                    }
                }
            }
            
            // 更新角色状态
            String updatedSkillsStateJson = objectMapper.writeValueAsString(skillsState);
            logger.info("📝 任务奖励处理后技能状态JSON长度: {}", updatedSkillsStateJson.length());
            logger.info("📝 任务奖励处理后技能状态详情: level={}, experience={}, gold={}, inventory数量={}, stats={}", 
                skillsState.get("level"), 
                skillsState.get("experience"), 
                skillsState.get("gold"),
                skillsState.get("inventory") != null ? ((List<?>) skillsState.get("inventory")).size() : 0,
                skillsState.get("stats"));
            
            logger.info("💾 调用worldStateManager更新任务奖励后的技能状态: sessionId={}", sessionId);
            worldStateManager.updateWorldState(sessionId, null, updatedSkillsStateJson);
            
            logger.info("✅ 任务奖励处理完成: sessionId={}", sessionId);
            
        } catch (Exception e) {
            logger.error("❌ 处理任务奖励失败: sessionId={}", sessionId, e);
        }
    }
    
    /**
     * 检查并处理升级
     */
    @SuppressWarnings("unchecked")
    private void checkAndProcessLevelUp(String sessionId, Map<String, Object> skillsState) {
        try {
            int currentLevel = (Integer) skillsState.get("level");
            int currentExp = (Integer) skillsState.get("experience");
            
            // 计算升级所需经验：下一级所需经验 = 当前等级 × 100
            int expNeeded = currentLevel * 100;
            
            if (currentExp >= expNeeded) {
                // 升级
                int newLevel = currentLevel + 1;
                int remainingExp = currentExp - expNeeded;
                
                skillsState.put("level", newLevel);
                skillsState.put("experience", remainingExp);
                
                // 升级时提升属性：每次升级随机提升2-3个属性点，总提升点数 = 等级
                Map<String, Object> stats = (Map<String, Object>) skillsState.get("stats");
                int totalStatPoints = newLevel;
                int statPointsToDistribute = Math.min(totalStatPoints, 3); // 最多3个属性点
                
                String[] statNames = {"力量", "敏捷", "智力", "体质"};
                for (int i = 0; i < statPointsToDistribute; i++) {
                    String randomStat = statNames[(int) (Math.random() * statNames.length)];
                    int currentStat = (Integer) stats.getOrDefault(randomStat, 8);
                    stats.put(randomStat, currentStat + 1);
                }
                
                // 升级时恢复生命值和魔力值到满值
                skillsState.put("生命值", "100/100");
                skillsState.put("魔力值", "50/50");
                
                logger.info("🎉 角色升级: {} -> {} (剩余经验: {})", currentLevel, newLevel, remainingExp);
                logger.info("属性提升: {}", stats);
                
                // 记录升级事件
                recordEvent(sessionId, WorldEvent.EventType.CHARACTER_UPDATE, 
                           "角色升级", Map.of(
                               "oldLevel", currentLevel,
                               "newLevel", newLevel,
                               "remainingExp", remainingExp,
                               "statGains", stats
                           ));
            }
            
        } catch (Exception e) {
            logger.error("❌ 检查升级失败: sessionId={}", sessionId, e);
        }
    }
    
    /**
     * 处理世界状态更新
     */
    @SuppressWarnings("unchecked")
    private void processWorldStateUpdates(String sessionId, Object worldStateUpdatesData) {
        try {
            logger.info("🌍 开始处理世界状态更新数据: sessionId={}", sessionId);
            Map<String, Object> worldStateUpdates = (Map<String, Object>) worldStateUpdatesData;
            logger.info("世界状态更新数据: {}", worldStateUpdates);
            
            // 统计更新字段
            int fieldCount = worldStateUpdates.size();
            logger.info("世界状态更新字段数量: {}", fieldCount);
            
            // 记录关键字段
            if (worldStateUpdates.containsKey("currentLocation")) {
                logger.info("位置更新: {}", worldStateUpdates.get("currentLocation"));
            }
            if (worldStateUpdates.containsKey("environment")) {
                logger.info("环境更新: {}", worldStateUpdates.get("environment"));
            }
            if (worldStateUpdates.containsKey("npcs")) {
                List<?> npcs = (List<?>) worldStateUpdates.get("npcs");
                logger.info("NPC更新数量: {}", npcs.size());
            }
            
            // 使用通用的世界状态更新方法
            String worldStateJson = objectMapper.writeValueAsString(worldStateUpdates);
            logger.info("准备更新世界状态JSON: {}", worldStateJson);
            
            worldStateManager.updateWorldState(sessionId, worldStateJson, null);
            
            logger.info("✅ 世界状态更新完成: sessionId={}, 字段数={}", sessionId, fieldCount);
            logger.info("🌍 世界状态更新处理完成: sessionId={}", sessionId);
            
        } catch (Exception e) {
            logger.error("❌ 处理世界状态更新失败: sessionId={}", sessionId, e);
        }
    }
    
    /**
     * 处理技能状态更新（合并任务奖励）
     */
    @SuppressWarnings("unchecked")
    private void processSkillsStateUpdatesWithQuestRewards(String sessionId, Object skillsStateUpdatesData, Object questUpdatesData) {
        try {
            logger.info("⚔️ 开始处理技能状态更新数据（合并任务奖励）: sessionId={}", sessionId);
            Map<String, Object> skillsStateUpdates = (Map<String, Object>) skillsStateUpdatesData;
            logger.info("技能状态更新数据: {}", skillsStateUpdates);
            
            // 获取当前角色状态（可能已经被任务奖励更新过）
            logger.info("🔍 查询当前会话状态: sessionId={}", sessionId);
            ChatSession session = chatSessionService.getSessionWithMessages(sessionId);
            if (session == null) {
                logger.warn("⚠️ 未找到会话，跳过技能状态更新: sessionId={}", sessionId);
                return;
            }
            
            String currentSkillsState = session.getSkillsState();
            logger.info("📊 当前技能状态长度: {}", currentSkillsState != null ? currentSkillsState.length() : 0);
            Map<String, Object> currentSkillsStateMap = new HashMap<>();
            
            // 解析当前技能状态
            if (currentSkillsState != null && !currentSkillsState.isEmpty()) {
                try {
                    currentSkillsStateMap = objectMapper.readValue(currentSkillsState, Map.class);
                } catch (Exception e) {
                    logger.warn("⚠️ 解析当前技能状态失败，使用默认值: sessionId={}", sessionId);
                    currentSkillsStateMap = new HashMap<>();
                }
            }
            
            // 初始化默认值
            currentSkillsStateMap.putIfAbsent("level", 1);
            currentSkillsStateMap.putIfAbsent("experience", 0);
            currentSkillsStateMap.putIfAbsent("gold", 0);
            currentSkillsStateMap.putIfAbsent("inventory", new ArrayList<>());
            currentSkillsStateMap.putIfAbsent("abilities", new ArrayList<>());
            currentSkillsStateMap.putIfAbsent("stats", new HashMap<>());
            
            // 合并技能状态更新（不覆盖任务奖励已更新的字段）
            boolean hasUpdates = false;
            
            // 只更新AI明确指定的字段，避免覆盖任务奖励
            if (skillsStateUpdates.containsKey("level")) {
                currentSkillsStateMap.put("level", skillsStateUpdates.get("level"));
                hasUpdates = true;
                logger.info("等级更新: {}", skillsStateUpdates.get("level"));
            }
            
            if (skillsStateUpdates.containsKey("gold")) {
                currentSkillsStateMap.put("gold", skillsStateUpdates.get("gold"));
                hasUpdates = true;
                logger.info("金币更新: {}", skillsStateUpdates.get("gold"));
            }
            
            if (skillsStateUpdates.containsKey("inventory")) {
                currentSkillsStateMap.put("inventory", skillsStateUpdates.get("inventory"));
                hasUpdates = true;
                logger.info("物品清单更新数量: {}", ((List<?>) skillsStateUpdates.get("inventory")).size());
            }
            
            if (skillsStateUpdates.containsKey("abilities")) {
                currentSkillsStateMap.put("abilities", skillsStateUpdates.get("abilities"));
                hasUpdates = true;
                logger.info("技能更新数量: {}", ((List<?>) skillsStateUpdates.get("abilities")).size());
            }
            
            if (skillsStateUpdates.containsKey("stats")) {
                currentSkillsStateMap.put("stats", skillsStateUpdates.get("stats"));
                hasUpdates = true;
                logger.info("属性更新数量: {}", ((Map<?, ?>) skillsStateUpdates.get("stats")).size());
            }
            
            
            if (hasUpdates) {
                // 更新角色状态
                String updatedSkillsStateJson = objectMapper.writeValueAsString(currentSkillsStateMap);
                logger.info("📝 准备更新技能状态JSON: 长度={}", updatedSkillsStateJson.length());
                logger.info("📝 更新后技能状态详情: level={}, experience={}, gold={}, inventory数量={}, stats={}", 
                    currentSkillsStateMap.get("level"), 
                    currentSkillsStateMap.get("experience"), 
                    currentSkillsStateMap.get("gold"),
                    currentSkillsStateMap.get("inventory") != null ? ((List<?>) currentSkillsStateMap.get("inventory")).size() : 0,
                    currentSkillsStateMap.get("stats"));
                
                logger.info("💾 调用worldStateManager更新技能状态: sessionId={}", sessionId);
                worldStateManager.updateWorldState(sessionId, null, updatedSkillsStateJson);
                
                logger.info("✅ 技能状态更新完成: sessionId={}, 字段数={}", sessionId, skillsStateUpdates.size());
            } else {
                logger.info("ℹ️ 技能状态无实际更新，跳过: sessionId={}", sessionId);
            }
            
            logger.info("⚔️ 技能状态更新处理完成: sessionId={}", sessionId);
            
        } catch (Exception e) {
            logger.error("❌ 处理技能状态更新失败: sessionId={}", sessionId, e);
        }
    }
    
    /**
     * 处理技能状态更新（原方法，保留兼容性）
     */
    @SuppressWarnings("unchecked")
    private void processSkillsStateUpdates(String sessionId, Object skillsStateUpdatesData) {
        try {
            logger.info("⚔️ 开始处理技能状态更新数据: sessionId={}", sessionId);
            Map<String, Object> skillsStateUpdates = (Map<String, Object>) skillsStateUpdatesData;
            logger.info("技能状态更新数据: {}", skillsStateUpdates);
            
            // 统计更新字段
            int fieldCount = skillsStateUpdates.size();
            logger.info("技能状态更新字段数量: {}", fieldCount);
            
            // 记录关键字段
            if (skillsStateUpdates.containsKey("level")) {
                logger.info("等级更新: {}", skillsStateUpdates.get("level"));
            }
            if (skillsStateUpdates.containsKey("experience")) {
                logger.info("经验更新: {}", skillsStateUpdates.get("experience"));
            }
            if (skillsStateUpdates.containsKey("gold")) {
                logger.info("金币更新: {}", skillsStateUpdates.get("gold"));
            }
            if (skillsStateUpdates.containsKey("inventory")) {
                List<?> inventory = (List<?>) skillsStateUpdates.get("inventory");
                logger.info("物品清单更新数量: {}", inventory.size());
            }
            if (skillsStateUpdates.containsKey("abilities")) {
                List<?> abilities = (List<?>) skillsStateUpdates.get("abilities");
                logger.info("技能更新数量: {}", abilities.size());
            }
            if (skillsStateUpdates.containsKey("stats")) {
                Map<?, ?> stats = (Map<?, ?>) skillsStateUpdates.get("stats");
                logger.info("属性更新数量: {}", stats.size());
            }
            
            // 使用通用的世界状态更新方法
            String skillsStateJson = objectMapper.writeValueAsString(skillsStateUpdates);
            logger.info("准备更新技能状态JSON: {}", skillsStateJson);
            
            worldStateManager.updateWorldState(sessionId, null, skillsStateJson);
            
            logger.info("✅ 技能状态更新完成: sessionId={}, 字段数={}", sessionId, fieldCount);
            logger.info("⚔️ 技能状态更新处理完成: sessionId={}", sessionId);
            
        } catch (Exception e) {
            logger.error("❌ 处理技能状态更新失败: sessionId={}", sessionId, e);
        }
    }
    
    /**
     * 处理情节更新
     */
    @SuppressWarnings("unchecked")
    private void processArcUpdates(String sessionId, Object arcUpdatesData) {
        try {
            logger.info("📖 开始处理情节更新数据: sessionId={}", sessionId);
            Map<String, Object> arcUpdates = (Map<String, Object>) arcUpdatesData;
            logger.info("情节更新数据: {}", arcUpdates);
            
            // 获取当前会话
            ChatSession session = chatSessionService.getSessionWithMessages(sessionId);
            if (session == null) {
                logger.warn("⚠️ 未找到会话，跳过情节更新: sessionId={}", sessionId);
                return;
            }
            
            logger.info("当前会话情节信息 - 名称: {}, 起始轮数: {}, 总轮数: {}", 
                       session.getCurrentArcName(), session.getCurrentArcStartRound(), session.getTotalRounds());
            
            boolean sessionUpdated = false;
            
            // 处理情节名称更新
            String newArcName = getStringValue(arcUpdates, "currentArcName");
            if (newArcName != null && !newArcName.isEmpty() && !newArcName.equals(session.getCurrentArcName())) {
                logger.info("检测到情节名称变化: {} -> {}", session.getCurrentArcName(), newArcName);
                session.setCurrentArcName(newArcName);
                sessionUpdated = true;
                logger.info("✅ 更新情节名称: sessionId={}, newArcName={}", sessionId, newArcName);
            }
            
            // 处理情节起始轮数更新
            Integer newArcStartRound = getIntegerValue(arcUpdates, "currentArcStartRound");
            if (newArcStartRound != null && !newArcStartRound.equals(session.getCurrentArcStartRound())) {
                logger.info("检测到情节起始轮数变化: {} -> {}", session.getCurrentArcStartRound(), newArcStartRound);
                session.setCurrentArcStartRound(newArcStartRound);
                sessionUpdated = true;
                logger.info("✅ 更新情节起始轮数: sessionId={}, newArcStartRound={}", sessionId, newArcStartRound);
            }
            
            // 处理总轮数更新
            Integer newTotalRounds = getIntegerValue(arcUpdates, "totalRounds");
            if (newTotalRounds != null && !newTotalRounds.equals(session.getTotalRounds())) {
                logger.info("检测到总轮数变化: {} -> {}", session.getTotalRounds(), newTotalRounds);
                session.setTotalRounds(newTotalRounds);
                sessionUpdated = true;
                logger.info("✅ 更新总轮数: sessionId={}, newTotalRounds={}", sessionId, newTotalRounds);
            }
            
            // 保存会话更新
            if (sessionUpdated) {
                ChatSession savedSession = chatSessionService.saveSession(session);
                logger.info("✅ 情节更新已保存到数据库: sessionId={}, 版本={}", sessionId, savedSession.getVersion());
            } else {
                logger.info("ℹ️ 情节信息无变化，跳过更新: sessionId={}", sessionId);
            }
            
            // 记录情节更新事件
            recordEvent(sessionId, WorldEvent.EventType.SYSTEM_EVENT, 
                       "情节更新", arcUpdates);
            
            logger.info("📖 情节更新处理完成: sessionId={}, 是否有更新={}", sessionId, sessionUpdated);
            
        } catch (Exception e) {
            logger.error("❌ 处理情节更新失败: sessionId={}", sessionId, e);
        }
    }
    
    /**
     * 处理收敛状态更新
     */
    @SuppressWarnings("unchecked")
    private void processConvergenceStatusUpdates(String sessionId, Object convergenceStatusUpdatesData) {
        try {
            logger.info("🎯 开始处理收敛状态更新数据: sessionId={}", sessionId);
            Map<String, Object> updates = (Map<String, Object>) convergenceStatusUpdatesData;
            logger.info("收敛状态更新数据: {}", updates);
            
            int updateCount = 0;
            
            // 处理进度更新
            Double progress = getDoubleValue(updates, "progress");
            if (progress != null) {
                logger.info("检测到收敛进度更新: {}", progress);
                convergenceStatusService.updateProgress(sessionId, progress);
                updateCount++;
                logger.info("✅ 更新收敛进度: sessionId={}, progress={}", sessionId, progress);
            }
            
            // 处理进度增量
            Double progressIncrement = getDoubleValue(updates, "progressIncrement");
            if (progressIncrement != null) {
                logger.info("检测到收敛进度增量: {}", progressIncrement);
                convergenceStatusService.addProgress(sessionId, progressIncrement);
                updateCount++;
                logger.info("✅ 增加收敛进度: sessionId={}, increment={}", sessionId, progressIncrement);
            }
            
            // 处理最近场景更新
            String nearestScenarioId = getStringValue(updates, "nearestScenarioId");
            String nearestScenarioTitle = getStringValue(updates, "nearestScenarioTitle");
            Double distanceToNearest = getDoubleValue(updates, "distanceToNearest");
            
            if (nearestScenarioId != null && nearestScenarioTitle != null && distanceToNearest != null) {
                logger.info("检测到最近场景更新: ID={}, 标题={}, 距离={}", nearestScenarioId, nearestScenarioTitle, distanceToNearest);
                convergenceStatusService.updateNearestScenario(sessionId, nearestScenarioId, nearestScenarioTitle, distanceToNearest);
                updateCount++;
                logger.info("✅ 更新最近场景: sessionId={}, scenarioId={}, title={}", 
                           sessionId, nearestScenarioId, nearestScenarioTitle);
            }
            
            // 处理场景进度更新
            Object scenarioProgressData = updates.get("scenarioProgress");
            if (scenarioProgressData instanceof Map) {
                Map<String, Double> scenarioProgress = (Map<String, Double>) scenarioProgressData;
                logger.info("检测到场景进度更新: 场景数量={}", scenarioProgress.size());
                logger.info("场景进度详情: {}", scenarioProgress);
                convergenceStatusService.updateScenarioProgress(sessionId, scenarioProgress);
                updateCount++;
                logger.info("✅ 更新场景进度: sessionId={}, scenarioCount={}", sessionId, scenarioProgress.size());
            }
            
            // 处理活跃提示更新
            Object activeHintsData = updates.get("activeHints");
            if (activeHintsData instanceof List) {
                List<String> activeHints = (List<String>) activeHintsData;
                logger.info("检测到活跃提示更新: 提示数量={}", activeHints.size());
                logger.info("活跃提示详情: {}", activeHints);
                convergenceStatusService.updateActiveHints(sessionId, activeHints);
                updateCount++;
                logger.info("✅ 更新活跃提示: sessionId={}, hintsCount={}", sessionId, activeHints.size());
            }
            
            // 记录收敛状态更新事件
            recordEvent(sessionId, WorldEvent.EventType.SYSTEM_EVENT, 
                       "收敛状态更新", updates);
            
            logger.info("🎯 收敛状态更新处理完成: sessionId={}, 更新操作数={}", sessionId, updateCount);
            
        } catch (Exception e) {
            logger.error("❌ 处理收敛状态更新失败: sessionId={}", sessionId, e);
        }
    }
    
    /**
     * 记录事件
     */
    private void recordEvent(String sessionId, WorldEvent.EventType eventType, String description, Map<String, Object> eventData) {
        try {
            logger.info("📝 开始记录事件: sessionId={}, eventType={}, description={}", sessionId, eventType, description);
            
            WorldEvent event = new WorldEvent();
            event.setSessionId(sessionId);
            event.setEventType(eventType);
            String eventDataJson = objectMapper.writeValueAsString(eventData);
            event.setEventData(eventDataJson);
            event.setSequence(getNextEventSequence(sessionId));
            
            // 设置校验和
            event.setChecksum(DigestUtils.md5DigestAsHex(eventDataJson.getBytes()));
            
            logger.info("事件数据JSON: {}", event.getEventData());
            
            // 设置当前会话的轮次和情节信息
            try {
                ChatSession session = chatSessionService.getSessionWithMessages(sessionId);
                if (session != null) {
                    event.setTotalRounds(session.getTotalRounds());
                    event.setCurrentArcStartRound(session.getCurrentArcStartRound());
                    event.setCurrentArcName(session.getCurrentArcName());
                    
                    logger.info("设置事件轮次信息 - 总轮数: {}, 情节起始轮数: {}, 情节名称: {}", 
                               session.getTotalRounds(), session.getCurrentArcStartRound(), session.getCurrentArcName());
                } else {
                    logger.warn("⚠️ 未找到会话，跳过轮次信息设置: sessionId={}", sessionId);
                }
            } catch (Exception e) {
                logger.warn("⚠️ 获取会话信息失败，继续记录事件但不设置轮次信息: sessionId={}, error={}", sessionId, e.getMessage());
            }
            
            WorldEvent savedEvent = worldEventRepository.save(event);
            logger.info("✅ 事件已保存到数据库: eventId={}, sessionId={}, eventType={}, sequence={}", 
                       savedEvent.getId(), sessionId, eventType, savedEvent.getSequence());
            
        } catch (Exception e) {
            logger.error("❌ 记录事件失败: sessionId={}, eventType={}, description={}", sessionId, eventType, description, e);
        }
    }
    
    // ==================== 辅助方法 ====================
    
    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        String result = value != null ? value.toString() : null;
        logger.debug("解析字符串值: key={}, value={}, result={}", key, value, result);
        return result;
    }
    
    private Integer getIntegerValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        Integer result = null;
        
        if (value instanceof Integer) {
            result = (Integer) value;
        } else if (value instanceof Number) {
            result = ((Number) value).intValue();
        } else if (value instanceof String) {
            try {
                result = Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                logger.warn("⚠️ 字符串转整数失败: key={}, value={}, error={}", key, value, e.getMessage());
                result = null;
            }
        }
        
        logger.debug("解析整数值: key={}, value={}, result={}", key, value, result);
        return result;
    }
    
    private Boolean getBooleanValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        Boolean result = null;
        
        if (value instanceof Boolean) {
            result = (Boolean) value;
        } else if (value instanceof String) {
            result = Boolean.parseBoolean((String) value);
        }
        
        logger.debug("解析布尔值: key={}, value={}, result={}", key, value, result);
        return result;
    }
    
    private Double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        Double result = null;
        
        if (value instanceof Double) {
            result = (Double) value;
        } else if (value instanceof Number) {
            result = ((Number) value).doubleValue();
        } else if (value instanceof String) {
            try {
                result = Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                logger.warn("⚠️ 字符串转浮点数失败: key={}, value={}, error={}", key, value, e.getMessage());
                result = null;
            }
        }
        
        logger.debug("解析浮点数值: key={}, value={}, result={}", key, value, result);
        return result;
    }
    
    /**
     * 获取下一个事件序号
     */
    private int getNextEventSequence(String sessionId) {
        return worldEventRepository.findMaxSequenceBySessionId(sessionId).orElse(0) + 1;
    }
}
