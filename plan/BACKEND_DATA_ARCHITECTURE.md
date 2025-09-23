# 角色扮演世界后端数据架构设计

## 1. 核心问题分析

### 结局稳定性挑战
- **用户输入不确定性**：语音识别错误、文本歧义、突发奇想
- **AI回复一致性**：同样输入可能产生不同回复，破坏剧情连贯性
- **世界状态漂移**：长对话中世界设定逐渐偏离初始规则

### 状态同步挑战  
- **多端一致性**：用户在不同设备间切换需保持世界状态
- **实时性**：语音交互要求低延迟状态更新
- **冲突解决**：并发操作可能导致状态不一致

## 2. 数据结构设计

### 2.1 扩展现有ChatSession实体

```sql
-- 扩展chat_sessions表
ALTER TABLE chat_sessions ADD COLUMN world_type VARCHAR(50);
ALTER TABLE chat_sessions ADD COLUMN world_rules JSON;
ALTER TABLE chat_sessions ADD COLUMN god_mode_rules JSON;
ALTER TABLE chat_sessions ADD COLUMN world_state JSON;
ALTER TABLE chat_sessions ADD COLUMN skills_state JSON;
ALTER TABLE chat_sessions ADD COLUMN story_checkpoints JSON;
ALTER TABLE chat_sessions ADD COLUMN stability_anchor JSON;
```

### 2.2 新增核心实体

#### WorldTemplate（世界模板）
```java
@Entity
@Table(name = "world_templates")
public class WorldTemplate {
    @Id
    private String worldId; // isekai, western_fantasy, wuxia_history, jp_school, edutainment
    
    @Column(columnDefinition = "JSON")
    private String defaultRules;        // 默认世界规则
    
    @Column(columnDefinition = "JSON") 
    private String systemPromptTemplate; // 系统提示词模板
    
    @Column(columnDefinition = "JSON")
    private String stabilityAnchors;    // 稳定性锚点
    
    @Column(columnDefinition = "JSON")
    private String questTemplates;      // 任务模板
}
```

#### WorldState（世界状态快照）
```java
@Entity
@Table(name = "world_states")
public class WorldState {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String sessionId;
    private Integer version;            // 版本号，用于冲突检测
    
    @Column(columnDefinition = "JSON")
    private String currentLocation;     // 当前地点
    
    @Column(columnDefinition = "JSON") 
    private String characters;          // 角色状态
    
    @Column(columnDefinition = "JSON")
    private String factions;            // 势力关系
    
    @Column(columnDefinition = "JSON")
    private String inventory;           // 物品背包
    
    @Column(columnDefinition = "JSON")
    private String activeQuests;        // 活跃任务
    
    @Column(columnDefinition = "JSON")
    private String completedQuests;     // 已完成任务
    
    @Column(columnDefinition = "JSON")
    private String eventHistory;       // 事件历史
    
    private LocalDateTime createdAt;
    private String checksum;            // 状态校验和
}
```

#### StabilityAnchor（稳定性锚点）
```java
@Entity
@Table(name = "stability_anchors")
public class StabilityAnchor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String sessionId;
    private String anchorType;          // WORLD_RULE, CHARACTER, LOCATION, QUEST
    private String anchorKey;           // 锚点标识符
    
    @Column(columnDefinition = "TEXT")
    private String anchorValue;         // 锚点内容
    
    private Integer priority;           // 优先级
    private Boolean isImmutable;        // 是否不可变
    private LocalDateTime createdAt;
}
```

#### DiceRoll（骰子记录）
```java
@Entity
@Table(name = "dice_rolls")
public class DiceRoll {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String sessionId;
    private Integer diceType;           // d20, d6等
    private Integer modifier;           // 修正值
    private Integer result;             // 结果
    private String context;             // 检定上下文
    private Boolean isSuccessful;       // 是否成功
    private LocalDateTime createdAt;
}
```

## 3. 结局稳定性保障机制

### 3.1 稳定性锚点系统
```java
public class StabilityService {
    
    // 核心稳定性锚点
    public void initializeStabilityAnchors(String sessionId, WorldTemplate template) {
        // 不可变锚点：世界基础规则
        addAnchor(sessionId, "WORLD_RULE", "physics", template.getPhysicsRules(), true);
        addAnchor(sessionId, "WORLD_RULE", "magic_system", template.getMagicSystem(), true);
        
        // 可变锚点：角色状态、地点信息
        addAnchor(sessionId, "CHARACTER", "protagonist", getInitialCharacter(), false);
    }
    
    // 每次AI回复前验证一致性
    public String validateAndCorrectPrompt(String sessionId, String userInput, String aiResponse) {
        List<StabilityAnchor> anchors = getSessionAnchors(sessionId);
        
        for (StabilityAnchor anchor : anchors) {
            if (violatesAnchor(aiResponse, anchor)) {
                aiResponse = correctViolation(aiResponse, anchor);
            }
        }
        return aiResponse;
    }
}
```

### 3.2 确定性种子机制
```java
public class DeterministicService {
    
    // 基于会话ID和轮次生成确定性种子
    public long generateSeed(String sessionId, int messageSequence) {
        return sessionId.hashCode() * 31L + messageSequence;
    }
    
    // 确定性随机事件生成
    public String generateDeterministicEvent(String sessionId, int sequence, String context) {
        Random random = new Random(generateSeed(sessionId, sequence));
        return selectEventFromTemplate(context, random);
    }
}
```

### 3.3 分层System Prompt策略
```java
public class PromptBuilder {
    
    public String buildSystemPrompt(String sessionId) {
        StringBuilder prompt = new StringBuilder();
        
        // Layer 1: 不可变世界规则
        prompt.append(getWorldRules(sessionId));
        
        // Layer 2: 当前世界状态摘要
        prompt.append(getWorldStateSummary(sessionId));
        
        // Layer 3: 稳定性约束
        prompt.append(getStabilityConstraints(sessionId));
        
        // Layer 4: 当前任务上下文
        prompt.append(getCurrentQuestContext(sessionId));
        
        return prompt.toString();
    }
}
```

## 4. 状态同步机制

### 4.1 乐观锁版本控制
```java
@Service
public class WorldStateService {
    
    @Transactional
    public WorldState updateWorldState(String sessionId, WorldState newState) {
        WorldState currentState = getCurrentState(sessionId);
        
        // 版本冲突检测
        if (newState.getVersion() != currentState.getVersion()) {
            throw new OptimisticLockException("世界状态已被其他操作更新");
        }
        
        // 增量更新
        WorldState mergedState = mergeStates(currentState, newState);
        mergedState.setVersion(currentState.getVersion() + 1);
        mergedState.setChecksum(calculateChecksum(mergedState));
        
        return worldStateRepository.save(mergedState);
    }
    
    // 状态校验和计算
    private String calculateChecksum(WorldState state) {
        String content = state.getCurrentLocation() + state.getCharacters() + 
                        state.getFactions() + state.getInventory();
        return DigestUtils.md5Hex(content);
    }
}
```

### 4.2 事件溯源模式
```java
@Entity
@Table(name = "world_events")
public class WorldEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String sessionId;
    private String eventType;          // USER_ACTION, AI_RESPONSE, DICE_ROLL, QUEST_UPDATE
    private String eventData;          // JSON格式事件数据
    private Integer sequence;          // 事件序号
    private LocalDateTime timestamp;
    private String checksum;           // 事件校验和
}

@Service
public class EventSourcingService {
    
    // 重放事件重建世界状态
    public WorldState rebuildWorldState(String sessionId, Integer toSequence) {
        List<WorldEvent> events = getEventsUpToSequence(sessionId, toSequence);
        
        WorldState state = getInitialState(sessionId);
        for (WorldEvent event : events) {
            state = applyEvent(state, event);
        }
        
        return state;
    }
}
```

### 4.3 Redis缓存热状态
```java
@Service
public class WorldStateCacheService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    // 缓存世界状态摘要
    public void cacheWorldStateSummary(String sessionId, WorldStateSummary summary) {
        String key = "world_state:" + sessionId;
        redisTemplate.opsForValue().set(key, summary, Duration.ofHours(2));
    }
    
    // 获取缓存状态，支持降级
    public WorldStateSummary getWorldStateSummary(String sessionId) {
        String key = "world_state:" + sessionId;
        WorldStateSummary cached = (WorldStateSummary) redisTemplate.opsForValue().get(key);
        
        if (cached == null) {
            // 缓存未命中，从数据库重建
            cached = rebuildSummaryFromDatabase(sessionId);
            cacheWorldStateSummary(sessionId, cached);
        }
        
        return cached;
    }
}
```

## 5. 语音特有优化

### 5.1 语音输入预处理
```java
@Service
public class VoiceInputProcessor {
    
    // 语音识别结果置信度过滤
    public String processVoiceInput(String rawInput, float confidence) {
        if (confidence < 0.7f) {
            return addUncertaintyMarker(rawInput);
        }
        
        // 常见语音识别错误纠正
        return correctCommonMistakes(rawInput);
    }
    
    // 为不确定输入添加标记
    private String addUncertaintyMarker(String input) {
        return "[语音不清晰]" + input;
    }
}
```

### 5.2 实时状态增量更新
```java
@Service
public class RealtimeStateService {
    
    // 微增量状态更新，减少延迟
    public void updateStateIncremental(String sessionId, String stateKey, Object value) {
        String lockKey = "state_lock:" + sessionId;
        
        try (RLock lock = redissonClient.getLock(lockKey)) {
            if (lock.tryLock(100, TimeUnit.MILLISECONDS)) {
                updateSingleStateField(sessionId, stateKey, value);
                broadcastStateChange(sessionId, stateKey, value);
            }
        }
    }
    
    // WebSocket广播状态变更
    private void broadcastStateChange(String sessionId, String key, Object value) {
        StateChangeEvent event = new StateChangeEvent(sessionId, key, value);
        messagingTemplate.convertAndSend("/topic/world-state/" + sessionId, event);
    }
}
```

## 6. 数据一致性保障

### 6.1 分布式事务处理
```java
@Service
@Transactional
public class WorldActionService {
    
    // 复合操作的事务一致性
    public void executeQuestAction(String sessionId, QuestAction action) {
        try {
            // 1. 更新任务状态
            questService.updateQuest(action.getQuestId(), action.getNewStatus());
            
            // 2. 更新角色属性
            characterService.updateCharacterStats(sessionId, action.getStatChanges());
            
            // 3. 记录事件
            eventService.recordEvent(sessionId, "QUEST_ACTION", action);
            
            // 4. 更新世界状态版本
            worldStateService.incrementVersion(sessionId);
            
        } catch (Exception e) {
            // 事务回滚，确保一致性
            throw new WorldStateException("执行任务操作失败", e);
        }
    }
}
```

### 6.2 数据校验与修复
```java
@Component
public class DataIntegrityChecker {
    
    @Scheduled(fixedRate = 300000) // 5分钟检查一次
    public void checkDataIntegrity() {
        List<String> activeSessions = getActiveSessions();
        
        for (String sessionId : activeSessions) {
            try {
                validateSessionIntegrity(sessionId);
            } catch (IntegrityException e) {
                repairSessionData(sessionId, e);
            }
        }
    }
    
    private void validateSessionIntegrity(String sessionId) {
        WorldState state = getCurrentState(sessionId);
        String expectedChecksum = calculateChecksum(state);
        
        if (!expectedChecksum.equals(state.getChecksum())) {
            throw new IntegrityException("状态校验和不匹配");
        }
    }
}
```

## 7. 性能优化策略

### 7.1 状态压缩存储
```java
public class StateCompressor {
    
    // 压缩大型世界状态
    public String compressState(WorldState state) {
        try {
            String json = objectMapper.writeValueAsString(state);
            return GzipUtils.compress(json);
        } catch (Exception e) {
            throw new CompressionException("状态压缩失败", e);
        }
    }
    
    // 智能差异存储
    public void storeDeltaState(String sessionId, WorldState oldState, WorldState newState) {
        JsonPatch patch = JsonDiff.asJsonPatch(oldState, newState);
        storePatch(sessionId, patch);
    }
}
```

### 7.2 查询优化
```java
@Repository
public class WorldStateRepository {
    
    // 只查询必要字段的轻量级状态
    @Query("SELECT new WorldStateSummary(w.currentLocation, w.activeQuests, w.version) " +
           "FROM WorldState w WHERE w.sessionId = :sessionId")
    WorldStateSummary findSummaryBySessionId(@Param("sessionId") String sessionId);
    
    // 分页查询历史状态
    @Query("SELECT w FROM WorldState w WHERE w.sessionId = :sessionId ORDER BY w.version DESC")
    Page<WorldState> findHistoryBySessionId(@Param("sessionId") String sessionId, Pageable pageable);
}
```

## 8. 监控与故障恢复

### 8.1 健康检查
```java
@Component
public class WorldStateHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        try {
            // 检查数据库连接
            worldStateRepository.count();
            
            // 检查Redis连接
            redisTemplate.opsForValue().get("health_check");
            
            // 检查状态一致性
            validateRandomSessions(5);
            
            return Health.up()
                    .withDetail("database", "UP")
                    .withDetail("redis", "UP")
                    .withDetail("integrity", "VALID")
                    .build();
                    
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}
```

### 8.2 自动故障恢复
```java
@Service
public class DisasterRecoveryService {
    
    // 会话状态自动恢复
    public void recoverSession(String sessionId) {
        try {
            // 1. 从事件日志重建状态
            WorldState recoveredState = eventSourcingService.rebuildWorldState(sessionId, null);
            
            // 2. 验证恢复结果
            validateRecoveredState(recoveredState);
            
            // 3. 更新缓存
            worldStateCacheService.cacheWorldStateSummary(sessionId, 
                    WorldStateSummary.from(recoveredState));
                    
            log.info("会话 {} 状态恢复成功", sessionId);
            
        } catch (Exception e) {
            log.error("会话 {} 状态恢复失败", sessionId, e);
            // 降级到安全状态
            fallbackToSafeState(sessionId);
        }
    }
}
```

这个架构设计解决了语音/文本模拟世界的核心挑战：

1. **结局稳定性**：通过稳定性锚点、确定性种子、分层提示词保证剧情一致性
2. **状态同步**：采用乐观锁、事件溯源、Redis缓存实现高效同步
3. **数据一致性**：分布式事务、校验修复机制确保数据完整性
4. **性能优化**：状态压缩、增量更新、查询优化提升响应速度
5. **故障恢复**：健康检查、自动恢复保证系统稳定性

基于现有的ChatSession架构进行扩展，最小化迁移成本，同时为角色扮演世界提供强大的数据支撑。



