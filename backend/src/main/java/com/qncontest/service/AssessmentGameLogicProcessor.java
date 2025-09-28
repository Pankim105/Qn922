package com.qncontest.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qncontest.entity.ChatSession;
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
     * 清理会话中的重复物品
     */
    @Transactional
    public void cleanupDuplicateItems(String sessionId) {
        try {
            logger.info("🧹 开始清理会话重复物品: sessionId={}", sessionId);
            
            // 获取当前会话
            ChatSession session = chatSessionService.getSessionWithMessages(sessionId);
            if (session == null) {
                logger.warn("⚠️ 未找到会话，跳过重复物品清理: sessionId={}", sessionId);
                return;
            }
            
            String currentSkillsState = session.getSkillsState();
            if (currentSkillsState == null || currentSkillsState.isEmpty()) {
                logger.info("ℹ️ 技能状态为空，跳过重复物品清理: sessionId={}", sessionId);
                return;
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> skillsState;
            try {
                skillsState = (Map<String, Object>) objectMapper.readValue(currentSkillsState, Map.class);
            } catch (Exception e) {
                logger.warn("⚠️ 解析技能状态失败，跳过重复物品清理: sessionId={}", sessionId);
                return;
            }
            
            Object inventoryObj = skillsState.get("inventory");
            if (!(inventoryObj instanceof List)) {
                logger.info("ℹ️ 物品列表格式不正确，跳过重复物品清理: sessionId={}", sessionId);
                return;
            }
            
            @SuppressWarnings("unchecked")
            List<String> currentInventory = (List<String>) inventoryObj;
            List<String> originalInventory = new ArrayList<>(currentInventory);
            
            // 去重处理
            Map<String, Integer> itemCounts = new HashMap<>();
            for (String item : currentInventory) {
                String itemName = parseItemName(item);
                int count = parseItemCount(item);
                itemCounts.put(itemName, itemCounts.getOrDefault(itemName, 0) + count);
            }
            
            // 重新构建物品列表
            List<String> deduplicatedInventory = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : itemCounts.entrySet()) {
                String itemName = entry.getKey();
                int totalCount = entry.getValue();
                if (totalCount > 0) {
                    deduplicatedInventory.add(itemName + "x" + totalCount);
                }
            }
            
            // 检查是否有变化
            if (originalInventory.size() != deduplicatedInventory.size() || 
                !originalInventory.equals(deduplicatedInventory)) {
                
                skillsState.put("inventory", deduplicatedInventory);
                String updatedSkillsStateJson = objectMapper.writeValueAsString(skillsState);
                
                worldStateManager.updateWorldState(sessionId, null, updatedSkillsStateJson);
                
                logger.info("✅ 重复物品清理完成: sessionId={}, 原数量={}, 清理后数量={}", 
                           sessionId, originalInventory.size(), deduplicatedInventory.size());
                logger.info("清理前: {}", originalInventory);
                logger.info("清理后: {}", deduplicatedInventory);
            } else {
                logger.info("ℹ️ 未发现重复物品，无需清理: sessionId={}", sessionId);
            }
            
        } catch (Exception e) {
            logger.error("❌ 清理重复物品失败: sessionId={}", sessionId, e);
        }
    }
    
    /**
     * 处理评估JSON中的游戏逻辑
     */
    @Transactional
    public void processAssessmentGameLogic(String sessionId, Map<String, Object> assessment) {
        try {
            logger.info("=== 开始处理评估JSON中的游戏逻辑 ===");
            logger.info("会话ID: {}", sessionId);
            logger.info("评估策略: {}", assessment.get("strategy"));
            logger.info("综合评分: {}", assessment.get("overallScore"));
            
            // 统计需要处理的字段
            int fieldCount = 0;
            StringBuilder fieldSummary = new StringBuilder();
            
            // 处理骰子检定
            if (assessment.get("diceRolls") != null) {
                fieldCount++;
                fieldSummary.append("diceRolls ");
                logger.info("📊 检测到骰子检定数据，开始处理...");
                processDiceRolls(sessionId, assessment.get("diceRolls"));
            }
            
            // 处理学习挑战
            if (assessment.get("learningChallenges") != null) {
                fieldCount++;
                fieldSummary.append("learningChallenges ");
                logger.info("🎓 检测到学习挑战数据，开始处理...");
                processLearningChallenges(sessionId, assessment.get("learningChallenges"));
            }
            
            // 处理状态更新
            if (assessment.get("stateUpdates") != null) {
                fieldCount++;
                fieldSummary.append("stateUpdates ");
                logger.info("📝 检测到状态更新数据，开始处理...");
                processStateUpdates(sessionId, assessment.get("stateUpdates"));
            }
            
            // 处理记忆更新
            if (assessment.get("memoryUpdates") != null) {
                fieldCount++;
                fieldSummary.append("memoryUpdates ");
                logger.info("🧠 检测到记忆更新数据，开始处理...");
                processMemoryUpdates(sessionId, assessment.get("memoryUpdates"));
            }
            
            // 处理任务更新（优先处理，因为可能包含奖励）
            if (assessment.get("questUpdates") != null) {
                fieldCount++;
                fieldSummary.append("questUpdates ");
                logger.info("🎯 检测到任务更新数据，开始处理...");
                processQuestUpdates(sessionId, assessment.get("questUpdates"));
            }
            
            // 处理世界状态更新
            if (assessment.get("worldStateUpdates") != null) {
                fieldCount++;
                fieldSummary.append("worldStateUpdates ");
                logger.info("🌍 检测到世界状态更新数据，开始处理...");
                processWorldStateUpdates(sessionId, assessment.get("worldStateUpdates"));
            }
            
            
            // 处理情节更新
            if (assessment.get("arcUpdates") != null) {
                fieldCount++;
                fieldSummary.append("arcUpdates ");
                logger.info("📖 检测到情节更新数据，开始处理...");
                processArcUpdates(sessionId, assessment.get("arcUpdates"));
            }
            
            // 处理收敛状态更新
            if (assessment.get("convergenceStatusUpdates") != null) {
                fieldCount++;
                fieldSummary.append("convergenceStatusUpdates ");
                logger.info("🎯 检测到收敛状态更新数据，开始处理...");
                processConvergenceStatusUpdates(sessionId, assessment.get("convergenceStatusUpdates"));
            }
            
            // 更新ChatSession的评估相关字段
            updateChatSessionAssessment(sessionId, assessment);
            
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
                Integer difficultyClass = getIntegerValue(diceRollData, "difficultyClass");
                String reason = getStringValue(diceRollData, "reason");
                Integer numDice = getIntegerValue(diceRollData, "numDice");
                
                logger.info("解析结果 - diceType: {}, modifier: {}, context: {}, result: {}, isSuccessful: {}", 
                           diceType, modifier, context, result, isSuccessful);
                
                if (diceType != null && result != null) {
                    try {
                        // 创建骰子检定记录
                        DiceRoll diceRoll = new DiceRoll();
                        diceRoll.setSessionId(sessionId);
                        diceRoll.setDiceType(diceType);
                        diceRoll.setModifier(modifier != null ? modifier : 0);
                        diceRoll.setContext(context != null ? context : "未知检定");
                        diceRoll.setResult(result);
                        diceRoll.setIsSuccessful(isSuccessful != null ? isSuccessful : false);
                        diceRoll.setDifficultyClass(difficultyClass);
                        diceRoll.setReason(reason != null ? reason : context);
                        diceRoll.setNumDice(numDice != null ? numDice : 1);
                        
                        // 计算最终结果（骰子结果 + 修正值）
                        Integer finalModifier = modifier != null ? modifier : 0;
                        diceRoll.setFinalResult(result + finalModifier);
                        
                        DiceRoll savedDiceRoll = diceRollRepository.save(diceRoll);
                        savedCount++;
                        
                        logger.info("✅ 骰子检定记录已保存到数据库: ID={}, sessionId={}, diceType={}, result={}, isSuccessful={}", 
                                   savedDiceRoll.getId(), sessionId, diceType, result, isSuccessful);
                    } catch (Exception e) {
                        logger.error("❌ 保存骰子检定记录失败: sessionId={}, diceType={}, result={}", 
                                   sessionId, diceType, result, e);
                    }
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
     * 处理记忆更新
     */
    @SuppressWarnings("unchecked")
    private void processMemoryUpdates(String sessionId, Object memoryUpdatesData) {
        try {
            logger.info("🧠 开始处理记忆更新数据: sessionId={}", sessionId);
            List<Map<String, Object>> memoryUpdates = (List<Map<String, Object>>) memoryUpdatesData;
            logger.info("记忆更新数量: {}", memoryUpdates.size());
            
            int processedCount = 0;
            for (int i = 0; i < memoryUpdates.size(); i++) {
                Map<String, Object> memoryUpdate = memoryUpdates.get(i);
                logger.info("处理第{}个记忆更新: {}", i + 1, memoryUpdate);
                
                String type = getStringValue(memoryUpdate, "type");
                String content = getStringValue(memoryUpdate, "content");
                Double importance = getDoubleValue(memoryUpdate, "importance");
                
                logger.info("解析结果 - type: {}, content: {}, importance: {}", type, content, importance);
                
                if (type != null && content != null && importance != null && importance > 0.6) {
                    // 记录记忆事件
                    Map<String, Object> eventData = new HashMap<>();
                    eventData.put("type", type);
                    eventData.put("content", content);
                    eventData.put("importance", importance);
                    
                    recordEvent(sessionId, WorldEvent.EventType.MEMORY_UPDATE, 
                               "记忆更新", eventData);
                    processedCount++;
                    
                    logger.info("✅ 记忆更新事件已记录: sessionId={}, type={}, content={}, importance={}", 
                               sessionId, type, content, importance);
                } else {
                    logger.warn("⚠️ 跳过无效或重要性不足的记忆更新: type={}, content={}, importance={}", 
                               type, content, importance);
                }
            }
            
            logger.info("🧠 记忆更新处理完成: sessionId={}, 总数={}, 处理成功={}", sessionId, memoryUpdates.size(), processedCount);
        } catch (Exception e) {
            logger.error("❌ 处理记忆更新失败: sessionId={}", sessionId, e);
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
            skillsState.putIfAbsent("attributes", new HashMap<>());
            skillsState.putIfAbsent("生命值", "100/100");
            skillsState.putIfAbsent("魔力值", "50/50");
            
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
                            
                            logger.info("处理物品奖励 - 当前物品: {}, 新物品: {}", currentInventory, newItems);
                            
                            // 智能去重处理：只添加真正的新物品或数量增加
                            Map<String, Integer> currentItemCounts = new HashMap<>();
                            Map<String, Integer> newItemCounts = new HashMap<>();
                            
                            // 统计现有物品数量
                            for (String item : currentInventory) {
                                String itemName = parseItemName(item);
                                int count = parseItemCount(item);
                                currentItemCounts.put(itemName, currentItemCounts.getOrDefault(itemName, 0) + count);
                            }
                            
                            // 统计新物品数量
                            for (String newItem : newItems) {
                                String itemName = parseItemName(newItem);
                                int count = parseItemCount(newItem);
                                newItemCounts.put(itemName, newItemCounts.getOrDefault(itemName, 0) + count);
                            }
                            
                            // 智能合并：只添加数量有增加或新增的物品
                            Map<String, Integer> finalItemCounts = new HashMap<>(currentItemCounts);
                            List<String> actuallyNewItems = new ArrayList<>();
                            
                            for (Map.Entry<String, Integer> entry : newItemCounts.entrySet()) {
                                String itemName = entry.getKey();
                                int newCount = entry.getValue();
                                int currentCount = currentItemCounts.getOrDefault(itemName, 0);
                                
                                if (newCount > currentCount) {
                                    // 数量有增加，更新为新的数量
                                    finalItemCounts.put(itemName, newCount);
                                    actuallyNewItems.add(itemName + "x" + (newCount - currentCount));
                                    logger.info("物品数量增加: {} ({} -> {})", itemName, currentCount, newCount);
                                } else if (currentCount == 0 && newCount > 0) {
                                    // 全新物品
                                    finalItemCounts.put(itemName, newCount);
                                    actuallyNewItems.add(itemName + "x" + newCount);
                                    logger.info("新增物品: {} x{}", itemName, newCount);
                                } else {
                                    // 数量没有增加，忽略
                                    logger.info("忽略重复物品: {} (当前: {}, 新: {})", itemName, currentCount, newCount);
                                }
                            }
                            
                            // 重新构建物品列表
                            List<String> deduplicatedInventory = new ArrayList<>();
                            for (Map.Entry<String, Integer> entry : finalItemCounts.entrySet()) {
                                String itemName = entry.getKey();
                                int totalCount = entry.getValue();
                                if (totalCount > 0) {
                                    deduplicatedInventory.add(itemName + "x" + totalCount);
                                }
                            }
                            
                            skillsState.put("inventory", deduplicatedInventory);
                            logger.info("物品奖励处理完成 - 实际新增: {}, 最终物品列表: {}", actuallyNewItems, deduplicatedInventory);
                        }
                        
                        // 处理属性奖励
                        Object statsReward = rewards.get("stats");
                        if (statsReward instanceof Map) {
                            Map<String, Object> currentAttributes = (Map<String, Object>) skillsState.get("attributes");
                            if (currentAttributes == null) {
                                currentAttributes = new HashMap<>();
                                skillsState.put("attributes", currentAttributes);
                            }
                            
                            Map<String, Object> statGains = (Map<String, Object>) statsReward;
                            
                            for (Map.Entry<String, Object> entry : statGains.entrySet()) {
                                String statName = entry.getKey();
                                Object statValue = entry.getValue();
                                if (statValue instanceof Number) {
                                    int currentStat = (Integer) currentAttributes.getOrDefault(statName, 0);
                                    int statGain = ((Number) statValue).intValue();
                                    int newStat = currentStat + statGain;
                                    currentAttributes.put(statName, newStat);
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
            logger.info("📝 任务奖励处理后技能状态详情: level={}, experience={}, gold={}, inventory数量={}, attributes={}", 
                skillsState.get("level"), 
                skillsState.get("experience"), 
                skillsState.get("gold"),
                skillsState.get("inventory") != null ? ((List<?>) skillsState.get("inventory")).size() : 0,
                skillsState.get("attributes"));
            
            logger.info("💾 调用worldStateManager更新任务奖励后的技能状态: sessionId={}", sessionId);
            worldStateManager.updateWorldState(sessionId, null, updatedSkillsStateJson);
            
            logger.info("✅ 任务奖励处理完成: sessionId={}", sessionId);
            
        } catch (Exception e) {
            logger.error("❌ 处理任务奖励失败: sessionId={}", sessionId, e);
        }
    }
    
    /**
     * 检查并处理升级（支持多次升级）
     */
    @SuppressWarnings("unchecked")
    private void checkAndProcessLevelUp(String sessionId, Map<String, Object> skillsState) {
        try {
            int currentLevel = (Integer) skillsState.get("level");
            int currentExp = (Integer) skillsState.get("experience");
            
            logger.info("检查升级: 当前等级={}, 当前经验={}", currentLevel, currentExp);
            
            // 计算可以升级的次数
            int totalLevelsGained = 0;
            int tempLevel = currentLevel;
            int tempExp = currentExp;
            
            // 循环计算可以升级多少次
            while (true) {
                int expNeeded = tempLevel * 100;
                if (tempExp >= expNeeded) {
                    tempExp -= expNeeded;
                    tempLevel++;
                    totalLevelsGained++;
                    logger.info("可以升级: {} -> {} (消耗经验: {}, 剩余经验: {})", 
                               tempLevel - 1, tempLevel, expNeeded, tempExp);
                } else {
                    break;
                }
            }
            
            if (totalLevelsGained > 0) {
                int newLevel = currentLevel + totalLevelsGained;
                int remainingExp = tempExp;
                
                skillsState.put("level", newLevel);
                skillsState.put("experience", remainingExp);
                
                // 升级时提升属性：每次升级提升2个属性点
                Map<String, Object> attributes = (Map<String, Object>) skillsState.get("attributes");
                if (attributes == null) {
                    attributes = new HashMap<>();
                    skillsState.put("attributes", attributes);
                }
                
                // 定义所有属性名称（英文）
                String[] statNames = {"strength", "dexterity", "intelligence", "constitution", "wisdom", "charisma"};
                
                // 计算总属性点提升
                int totalStatPoints = totalLevelsGained * 2;
                logger.info("总共升级{}级，提升{}个属性点", totalLevelsGained, totalStatPoints);
                
                // 分配属性点
                for (int i = 0; i < totalStatPoints; i++) {
                    String randomStat = statNames[(int) (Math.random() * statNames.length)];
                    int currentStat = (Integer) attributes.getOrDefault(randomStat, 8);
                    attributes.put(randomStat, currentStat + 1);
                    logger.info("属性提升: {} {} -> {}", randomStat, currentStat, currentStat + 1);
                }
                
                // 升级时恢复生命值和魔力值到满值
                skillsState.put("生命值", "100/100");
                skillsState.put("魔力值", "50/50");
                
                logger.info("🎉 角色升级: {} -> {} (升级{}级, 剩余经验: {})", 
                           currentLevel, newLevel, totalLevelsGained, remainingExp);
                logger.info("属性提升: {}", attributes);
                
                // 记录升级事件
                recordEvent(sessionId, WorldEvent.EventType.CHARACTER_UPDATE, 
                           "角色升级", Map.of(
                               "oldLevel", currentLevel,
                               "newLevel", newLevel,
                               "levelsGained", totalLevelsGained,
                               "remainingExp", remainingExp,
                               "attributeGains", attributes
                           ));
            } else {
                logger.info("经验不足，无法升级: 当前等级={}, 当前经验={}, 需要经验={}", 
                           currentLevel, currentExp, currentLevel * 100);
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
                // 验证情节起始轮数的合理性
                if (newArcStartRound > 0 && newArcStartRound <= session.getTotalRounds()) {
                    logger.info("检测到情节起始轮数变化: {} -> {}", session.getCurrentArcStartRound(), newArcStartRound);
                    session.setCurrentArcStartRound(newArcStartRound);
                    sessionUpdated = true;
                    logger.info("✅ 更新情节起始轮数: sessionId={}, newArcStartRound={}", sessionId, newArcStartRound);
                } else {
                    logger.warn("⚠️ 情节起始轮数不合理，跳过更新: sessionId={}, newArcStartRound={}, totalRounds={}", 
                               sessionId, newArcStartRound, session.getTotalRounds());
                }
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
    
    /**
     * 解析物品名称（去除数量后缀）
     */
    private String parseItemName(String item) {
        if (item == null || item.isEmpty()) {
            return "";
        }
        
        // 查找最后一个"x"字符，如果后面跟着数字，则去掉数量部分
        int lastXIndex = item.lastIndexOf('x');
        if (lastXIndex > 0 && lastXIndex < item.length() - 1) {
            String suffix = item.substring(lastXIndex + 1);
            try {
                Integer.parseInt(suffix);
                // 如果后缀是数字，则去掉数量部分
                return item.substring(0, lastXIndex);
            } catch (NumberFormatException e) {
                // 如果后缀不是数字，则返回原字符串
                return item;
            }
        }
        
        return item;
    }
    
    /**
     * 解析物品数量
     */
    private int parseItemCount(String item) {
        if (item == null || item.isEmpty()) {
            return 1;
        }
        
        // 查找最后一个"x"字符，如果后面跟着数字，则解析数量
        int lastXIndex = item.lastIndexOf('x');
        if (lastXIndex > 0 && lastXIndex < item.length() - 1) {
            String suffix = item.substring(lastXIndex + 1);
            try {
                return Integer.parseInt(suffix);
            } catch (NumberFormatException e) {
                // 如果后缀不是数字，则默认为1
                return 1;
            }
        }
        
        return 1;
    }
    
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
    
    /**
     * 更新ChatSession的评估相关字段
     */
    private void updateChatSessionAssessment(String sessionId, Map<String, Object> assessment) {
        try {
            logger.info("📊 开始更新ChatSession评估字段: sessionId={}", sessionId);
            
            // 获取当前会话
            ChatSession session = chatSessionService.getSessionById(sessionId);
            if (session == null) {
                logger.warn("⚠️ 未找到会话: sessionId={}", sessionId);
                return;
            }
            
            boolean sessionUpdated = false;
            
            // 1. 更新收敛进度
            Object convergenceProgressObj = assessment.get("convergenceProgress");
            if (convergenceProgressObj != null) {
                try {
                    Double convergenceProgress = Double.parseDouble(convergenceProgressObj.toString());
                    if (session.getConvergenceProgress() == null || 
                        !session.getConvergenceProgress().equals(convergenceProgress)) {
                        session.setConvergenceProgress(convergenceProgress);
                        sessionUpdated = true;
                        logger.info("✅ 更新收敛进度: {} -> {}", session.getConvergenceProgress(), convergenceProgress);
                    }
                } catch (NumberFormatException e) {
                    logger.warn("⚠️ 收敛进度格式无效: {}", convergenceProgressObj);
                }
            }
            
            // 2. 更新任务相关字段
            updateQuestFields(session, assessment);
            
            // 3. 角色属性字段已通过skillsState管理，无需单独更新
            
            // 4. 更新世界状态字段
            updateWorldStateFields(session, assessment);
            
            // 5. 更新评估历史记录
            try {
                String assessmentJson = objectMapper.writeValueAsString(assessment);
                String currentHistory = session.getAssessmentHistory();
                
                // 构建新的评估历史记录
                List<Map<String, Object>> historyList = new ArrayList<>();
                if (currentHistory != null && !currentHistory.isEmpty()) {
                    try {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> existingHistory = objectMapper.readValue(currentHistory, List.class);
                        historyList.addAll(existingHistory);
                    } catch (Exception e) {
                        logger.warn("⚠️ 解析现有评估历史失败，将重新创建: {}", e.getMessage());
                    }
                }
                
                // 添加新的评估记录
                @SuppressWarnings("unchecked")
                Map<String, Object> newAssessment = objectMapper.readValue(assessmentJson, Map.class);
                newAssessment.put("timestamp", System.currentTimeMillis());
                historyList.add(newAssessment);
                
                // 限制历史记录数量（保留最近50条）
                if (historyList.size() > 50) {
                    historyList = historyList.subList(historyList.size() - 50, historyList.size());
                }
                
                String newHistoryJson = objectMapper.writeValueAsString(historyList);
                session.setAssessmentHistory(newHistoryJson);
                sessionUpdated = true;
                logger.info("✅ 更新评估历史记录: 总数={}", historyList.size());
                
            } catch (Exception e) {
                logger.error("❌ 更新评估历史记录失败: sessionId={}", sessionId, e);
            }
            
            // 6. 更新最后评估ID（使用时间戳作为ID）
            Long lastAssessmentId = System.currentTimeMillis();
            session.setLastAssessmentId(lastAssessmentId);
            sessionUpdated = true;
            logger.info("✅ 更新最后评估ID: {}", lastAssessmentId);
            
            // 保存会话更新
            if (sessionUpdated) {
                ChatSession savedSession = chatSessionService.saveSession(session);
                logger.info("✅ ChatSession评估字段已保存: sessionId={}, version={}", 
                           sessionId, savedSession.getVersion());
            } else {
                logger.info("ℹ️ 评估字段无变化，跳过更新: sessionId={}", sessionId);
            }
            
        } catch (Exception e) {
            logger.error("❌ 更新ChatSession评估字段失败: sessionId={}", sessionId, e);
        }
    }
    
    /**
     * 更新任务相关字段
     */
    @SuppressWarnings("unchecked")
    private void updateQuestFields(ChatSession session, Map<String, Object> assessment) {
        try {
            Object questUpdatesObj = assessment.get("questUpdates");
            if (questUpdatesObj == null) {
                return;
            }
            
            Map<String, Object> questUpdates = (Map<String, Object>) questUpdatesObj;
            boolean questFieldsUpdated = false;
            
            // 获取现有活跃任务列表
            String currentActiveQuests = session.getActiveQuests();
            Map<String, Object> existingActiveQuestsMap = new HashMap<>();
            
            if (currentActiveQuests != null && !currentActiveQuests.isEmpty()) {
                try {
                    List<Object> existingList = objectMapper.readValue(currentActiveQuests, List.class);
                    // 转换为Map，以questId为key，便于去重和更新
                    for (Object quest : existingList) {
                        if (quest instanceof Map) {
                            String questId = getStringValue((Map<String, Object>) quest, "questId");
                            if (questId != null) {
                                existingActiveQuestsMap.put(questId, quest);
                            }
                        }
                    }
                    logger.info("📋 现有活跃任务数量: {}", existingActiveQuestsMap.size());
                } catch (Exception e) {
                    logger.warn("⚠️ 解析现有活跃任务失败: {}", e.getMessage());
                }
            }
            
            // 处理新创建的任务
            if (questUpdates.containsKey("created")) {
                List<?> created = (List<?>) questUpdates.get("created");
                logger.info("📝 处理新创建任务: {}", created.size());
                
                for (Object quest : created) {
                    if (quest instanceof Map) {
                        String questId = getStringValue((Map<String, Object>) quest, "questId");
                        if (questId != null) {
                            existingActiveQuestsMap.put(questId, quest);
                            logger.info("➕ 添加新任务: questId={}", questId);
                        }
                    }
                }
            }
            
            // 处理进度更新的任务
            if (questUpdates.containsKey("progress")) {
                List<?> progress = (List<?>) questUpdates.get("progress");
                logger.info("📈 处理进度更新任务: {}", progress.size());
                
                for (Object quest : progress) {
                    if (quest instanceof Map) {
                        String questId = getStringValue((Map<String, Object>) quest, "questId");
                        if (questId != null) {
                            // 如果任务已存在，更新进度；如果不存在，添加新任务
                            existingActiveQuestsMap.put(questId, quest);
                            logger.info("🔄 更新任务进度: questId={}", questId);
                        }
                    }
                }
            }
                
                // 移除已完成的任务
                if (questUpdates.containsKey("completed")) {
                    List<?> completed = (List<?>) questUpdates.get("completed");
                logger.info("✅ 处理已完成任务: {}", completed.size());
                    
                    for (Object completedQuest : completed) {
                        if (completedQuest instanceof Map) {
                            String completedQuestId = getStringValue((Map<String, Object>) completedQuest, "questId");
                        if (completedQuestId != null && existingActiveQuestsMap.containsKey(completedQuestId)) {
                            existingActiveQuestsMap.remove(completedQuestId);
                            logger.info("🗑️ 移除已完成任务: questId={}", completedQuestId);
                        }
                    }
                }
            }
            
            // 移除过期的任务
            if (questUpdates.containsKey("expired")) {
                List<?> expired = (List<?>) questUpdates.get("expired");
                logger.info("⏰ 处理过期任务: {}", expired.size());
                
                for (Object expiredQuest : expired) {
                    if (expiredQuest instanceof Map) {
                        String expiredQuestId = getStringValue((Map<String, Object>) expiredQuest, "questId");
                        if (expiredQuestId != null && existingActiveQuestsMap.containsKey(expiredQuestId)) {
                            existingActiveQuestsMap.remove(expiredQuestId);
                            logger.info("🗑️ 移除过期任务: questId={}", expiredQuestId);
                        }
                    }
                }
            }
            
            // 更新活跃任务字段
            if (!existingActiveQuestsMap.isEmpty() || questUpdates.containsKey("completed") || questUpdates.containsKey("expired")) {
                List<Object> finalActiveQuests = new ArrayList<>(existingActiveQuestsMap.values());
                String newActiveQuestsJson = objectMapper.writeValueAsString(finalActiveQuests);
                session.setActiveQuests(newActiveQuestsJson);
                questFieldsUpdated = true;
                logger.info("✅ 更新活跃任务列表: 总数={}", finalActiveQuests.size());
            }
            
            // 更新已完成任务列表
            if (questUpdates.containsKey("completed")) {
                List<?> completed = (List<?>) questUpdates.get("completed");
                String currentCompletedQuests = session.getCompletedQuests();
                Map<String, Object> existingCompletedQuestsMap = new HashMap<>();
                
                if (currentCompletedQuests != null && !currentCompletedQuests.isEmpty()) {
                    try {
                        List<Object> existingList = objectMapper.readValue(currentCompletedQuests, List.class);
                        // 转换为Map，以questId为key，便于去重
                        for (Object quest : existingList) {
                            if (quest instanceof Map) {
                                String questId = getStringValue((Map<String, Object>) quest, "questId");
                                if (questId != null) {
                                    existingCompletedQuestsMap.put(questId, quest);
                                }
                            }
                        }
                        logger.info("📋 现有完成任务数量: {}", existingCompletedQuestsMap.size());
                    } catch (Exception e) {
                        logger.warn("⚠️ 解析现有完成任务失败: {}", e.getMessage());
                    }
                }
                
                // 添加新完成的任务（去重）
                for (Object quest : completed) {
                    if (quest instanceof Map) {
                        String questId = getStringValue((Map<String, Object>) quest, "questId");
                        if (questId != null) {
                            existingCompletedQuestsMap.put(questId, quest);
                            logger.info("✅ 添加完成任务: questId={}", questId);
                        }
                    }
                }
                
                List<Object> finalCompletedQuests = new ArrayList<>(existingCompletedQuestsMap.values());
                String newCompletedQuestsJson = objectMapper.writeValueAsString(finalCompletedQuests);
                session.setCompletedQuests(newCompletedQuestsJson);
                questFieldsUpdated = true;
                logger.info("✅ 更新完成任务列表: 总数={}", finalCompletedQuests.size());
            }
            
            if (questFieldsUpdated) {
                logger.info("✅ 任务字段更新完成");
            }
            
        } catch (Exception e) {
            logger.error("❌ 更新任务字段失败: sessionId={}", session.getSessionId(), e);
        }
    }
    
    
    /**
     * 更新世界状态字段
     */
    @SuppressWarnings("unchecked")
    private void updateWorldStateFields(ChatSession session, Map<String, Object> assessment) {
        try {
            Object worldStateUpdatesObj = assessment.get("worldStateUpdates");
            if (worldStateUpdatesObj == null) {
                return;
            }
            
            Map<String, Object> worldStateUpdates = (Map<String, Object>) worldStateUpdatesObj;
            
            // 获取当前世界状态
            String currentWorldState = session.getWorldState();
            Map<String, Object> worldState = new HashMap<>();
            
            if (currentWorldState != null && !currentWorldState.isEmpty()) {
                try {
                    worldState = objectMapper.readValue(currentWorldState, Map.class);
                } catch (Exception e) {
                    logger.warn("⚠️ 解析现有世界状态失败: {}", e.getMessage());
                }
            }
            
            // 合并世界状态更新
            worldState.putAll(worldStateUpdates);
            
            String newWorldStateJson = objectMapper.writeValueAsString(worldState);
            session.setWorldState(newWorldStateJson);
            logger.info("✅ 更新世界状态字段: 合并了{}个更新", worldStateUpdates.size());
            
        } catch (Exception e) {
            logger.error("❌ 更新世界状态字段失败: sessionId={}", session.getSessionId(), e);
        }
    }
}
