# 后端架构设计

QN Contest后端采用Spring Boot微服务架构，基于面向接口编程的设计模式，提供高性能、可扩展的角色扮演AI服务。

## 架构概览

```
┌─────────────────────────────────────────────────────────────┐
│                    Controller Layer                        │
├─────────────────────────────────────────────────────────────┤
│  AuthController  │  ChatController  │  RoleplayController   │
└─────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────┐
│                     Service Layer                          │
├─────────────────────────────────────────────────────────────┤
│  StreamAiService  │  RoleplayWorldService  │  MemoryService │
│  AssessmentGameLogicProcessor │  ConvergenceStatusService   │
└─────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────┐
│                   Interface Layer                          │
├─────────────────────────────────────────────────────────────┤
│  StreamChatServiceInterface  │  MemoryManagerInterface     │
└─────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────┐
│                Implementation Layer                        │
├─────────────────────────────────────────────────────────────┤
│  StreamChatService  │  RoleplayStreamService  │  MemoryService│
└─────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────┐
│                   Repository Layer                         │
├─────────────────────────────────────────────────────────────┤
│  UserRepository  │  ChatSessionRepository  │  WorldRepository│
│  DMAssessmentRepository │  ConvergenceStatusRepository      │
└─────────────────────────────────────────────────────────────┘
```

## 核心组件

### 1. 控制器层 (Controller Layer)

#### AuthController
- **职责**: 用户认证和授权
- **功能**: 登录、注册、令牌刷新、登出
- **接口**: RESTful API

#### ChatController
- **职责**: 聊天会话管理
- **功能**: 会话创建、消息获取、会话删除
- **接口**: RESTful API

#### RoleplayController
- **职责**: 角色扮演功能
- **功能**: 世界管理、会话初始化、骰子检定、状态更新
- **接口**: RESTful API + SSE

### 2. 服务层 (Service Layer)

#### StreamAiService
- **职责**: AI流式响应统一入口
- **功能**: 流式聊天、角色扮演聊天
- **特点**: 统一分发、错误处理、超时管理

#### RoleplayWorldService
- **职责**: 世界状态管理
- **功能**: 世界初始化、状态更新、骰子检定
- **特点**: 状态持久化、版本控制

#### RoleplayMemoryService
- **职责**: 记忆管理
- **功能**: 记忆存储、检索、清理
- **特点**: 智能评估、自动优化

#### AssessmentGameLogicProcessor
- **职责**: 评估游戏逻辑处理
- **功能**: 解析AI评估JSON、执行游戏逻辑更新
- **特点**: 支持骰子检定、任务管理、状态更新、情节管理、收敛状态管理

#### ConvergenceStatusService
- **职责**: 收敛状态管理
- **功能**: 收敛进度跟踪、场景管理、引导提示管理
- **特点**: 智能收敛引导、多场景支持、动态提示生成

### 3. 接口层 (Interface Layer)

基于面向接口编程设计，提供以下核心接口：

#### StreamChatServiceInterface
```java
public interface StreamChatServiceInterface {
    SseEmitter handleStreamChat(ChatRequest request, User user);
    SseEmitter handleRoleplayStreamChat(RoleplayRequest request, User user);
}
```

#### MemoryManagerInterface
```java
public interface MemoryManagerInterface {
    String buildMemoryContext(String sessionId, String currentMessage);
    double assessMemoryImportance(String content, String userAction);
    void storeMemory(String sessionId, String content, String memoryType, double importance);
    void processMemoryMarkers(String sessionId, String aiResponse, String userAction);
}
```

#### WorldStateManagerInterface
```java
public interface WorldStateManagerInterface {
    void initializeRoleplaySession(String sessionId, String worldType, String godModeRules, User user);
    DiceRoll rollDice(String sessionId, Integer diceType, Integer modifier, String context, Integer difficultyClass);
    void updateWorldState(String sessionId, String newWorldState, String skillsState);
    String getWorldStateSummary(String sessionId);
}
```

### 4. 实现层 (Implementation Layer)

#### StreamChatService
- **实现**: 标准聊天服务
- **特点**: 基础流式响应处理

#### RoleplayStreamService
- **实现**: 角色扮演聊天服务
- **特点**: 集成世界状态、记忆系统

#### UnifiedStreamChatService
- **实现**: 统一分发服务
- **特点**: 根据请求类型分发到具体服务

### 5. 数据访问层 (Repository Layer)

#### UserRepository
- **职责**: 用户数据访问
- **功能**: 用户CRUD操作、认证查询

#### ChatSessionRepository
- **职责**: 聊天会话数据访问
- **功能**: 会话管理、消息查询

#### WorldEventRepository
- **职责**: 世界事件数据访问
- **功能**: 事件记录、历史查询

#### DMAssessmentRepository
- **职责**: AI评估数据访问
- **功能**: 评估记录存储、查询、分析

#### ConvergenceStatusRepository
- **职责**: 收敛状态数据访问
- **功能**: 收敛状态CRUD操作、进度查询

## 技术实现

### 1. 流式响应处理

#### SSE (Server-Sent Events)
```java
@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter handleStreamChat(@RequestBody ChatRequest request, 
                                 Authentication authentication) {
    SseEmitter emitter = new SseEmitter(300000L);
    
    CompletableFuture.runAsync(() -> {
        try {
            // 流式处理逻辑
            StreamingResponseHandler<AiMessage> handler = createHandler(emitter);
            chatService.streamChat(request, handler);
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    });
    
    return emitter;
}
```

#### 流式响应处理器
```java
public class StreamResponseHandler implements StreamingResponseHandler<AiMessage> {
    @Override
    public void onNext(String token) {
        // 处理流式token
        String eventData = String.format("{\"content\":\"%s\"}", 
            escapeJsonString(token));
        emitter.send(SseEmitter.event()
            .name("message")
            .data(eventData));
    }
    
    @Override
    public void onComplete(Response<AiMessage> response) {
        // 完成处理
        emitter.complete();
    }
    
    @Override
    public void onError(Throwable error) {
        // 错误处理
        emitter.completeWithError(error);
    }
}
```

### 2. AI集成

#### LangChain4j集成
```java
@Service
public class AiService {
    
    @Autowired
    private ChatLanguageModel chatLanguageModel;
    
    public void streamChat(String message, StreamingResponseHandler<AiMessage> handler) {
        ChatLanguageModelStreamingResponseHandler streamingHandler = 
            new ChatLanguageModelStreamingResponseHandler() {
                @Override
                public void onNext(String token) {
                    handler.onNext(token);
                }
                
                @Override
                public void onComplete(Response<AiMessage> response) {
                    handler.onComplete(response);
                }
                
                @Override
                public void onError(Throwable error) {
                    handler.onError(error);
                }
            };
            
        chatLanguageModel.generateStreaming(message, streamingHandler);
    }
}
```

#### 提示词构建
```java
@Service
public class PromptBuilder implements PromptBuilderInterface {
    
    public String buildLayeredPrompt(RoleplayContext context) {
        StringBuilder prompt = new StringBuilder();
        
        // Layer 1: 世界基础规则
        prompt.append(getWorldRules(context.getWorldType()));
        
        // Layer 2: 当前世界状态
        prompt.append(getWorldStateSummary(context.getSessionId()));
        
        // Layer 3: 记忆上下文
        prompt.append(getMemoryContext(context.getSessionId(), context.getCurrentMessage()));
        
        // Layer 4: 当前任务上下文
        prompt.append(getCurrentQuestContext(context.getSessionId()));
        
        return prompt.toString();
    }
}
```

### 3. 记忆系统

#### 记忆存储
```java
@Service
public class RoleplayMemoryService implements MemoryManagerInterface {
    
    public void storeMemory(String sessionId, String content, String memoryType, double importance) {
        // 获取当前记忆
        List<MemoryEntry> memories = getMemories(sessionId);
        
        // 创建新记忆
        MemoryEntry newMemory = new MemoryEntry(content, memoryType, importance, LocalDateTime.now());
        memories.add(newMemory);
        
        // 智能清理：保留最重要的15个记忆
        memories.sort((a, b) -> Double.compare(b.getImportance(), a.getImportance()));
        if (memories.size() > 15) {
            memories = memories.subList(0, 15);
        }
        
        // 保存到数据库
        saveMemories(sessionId, memories);
    }
}
```

#### 记忆检索
```java
public List<MemoryEntry> retrieveRelevantMemories(String sessionId, String query, int maxResults) {
    List<MemoryEntry> memories = getMemories(sessionId);
    
    // 计算相似性分数
    List<MemoryEntry> scoredMemories = memories.stream()
        .map(memory -> {
            double score = calculateSimilarity(query, memory.getContent());
            return new ScoredMemory(memory, score);
        })
        .filter(scored -> scored.getScore() > 0.3) // 相似性阈值
        .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
        .limit(maxResults)
        .map(ScoredMemory::getMemory)
        .collect(Collectors.toList());
        
    return scoredMemories;
}
```

### 4. 评估系统处理

#### 评估JSON解析
```java
@Service
public class AssessmentGameLogicProcessor {
    
    @Transactional
    public void processAssessmentGameLogic(String sessionId, String assessmentJson) {
        try {
            // 解析评估JSON
            ObjectMapper mapper = new ObjectMapper();
            JsonNode assessment = mapper.readTree(assessmentJson);
            
            // 处理骰子检定
            processDiceRolls(sessionId, assessment);
            
            // 处理学习挑战
            processLearningChallenges(sessionId, assessment);
            
            // 处理状态更新
            processStateUpdates(sessionId, assessment);
            
            // 处理任务更新
            processQuestUpdates(sessionId, assessment);
            
            // 处理世界状态更新
            processWorldStateUpdates(sessionId, assessment);
            
            // 处理技能状态更新
            processSkillsStateUpdates(sessionId, assessment);
            
            // 处理情节更新
            processArcUpdates(sessionId, assessment);
            
            // 处理收敛状态更新
            processConvergenceStatusUpdates(sessionId, assessment);
            
        } catch (Exception e) {
            logger.error("处理评估游戏逻辑失败: {}", e.getMessage(), e);
        }
    }
}
```

#### 收敛状态管理
```java
@Service
public class ConvergenceStatusService {
    
    @Transactional
    public void updateProgress(String sessionId, double progress) {
        ConvergenceStatus status = getOrCreateConvergenceStatus(sessionId);
        status.updateProgress(progress);
        convergenceStatusRepository.save(status);
    }
    
    @Transactional(readOnly = true)
    public String getConvergenceStatusSummary(String sessionId) {
        Optional<ConvergenceStatus> statusOpt = convergenceStatusRepository.findBySessionId(sessionId);
        if (statusOpt.isEmpty()) {
            return "";
        }
        
        ConvergenceStatus status = statusOpt.get();
        StringBuilder summary = new StringBuilder();
        summary.append(String.format("- **整体收敛进度**: %.1f%%\n", status.getProgress() * 100));
        
        if (status.getNearestScenarioId() != null) {
            summary.append(String.format("- **最近收敛场景**: %s (%s), 距离: %.2f\n",
                    status.getNearestScenarioTitle(), status.getNearestScenarioId(), status.getDistanceToNearest()));
        }
        
        return summary.toString();
    }
}
```

### 5. 世界状态管理

#### 状态更新
```java
@Service
public class RoleplayWorldService implements WorldStateManagerInterface {
    
    @Transactional
    public void updateWorldState(String sessionId, String newWorldState, String skillsState) {
        ChatSession session = getSession(sessionId);
        
        // 版本控制
        String currentState = session.getWorldState();
        int version = getCurrentVersion(sessionId);
        
        // 状态合并
        String mergedState = mergeWorldStates(currentState, newWorldState);
        
        // 更新会话
        session.setWorldState(mergedState);
        session.setSkillsState(skillsState);
        session.setUpdatedAt(LocalDateTime.now());
        
        // 记录事件
        recordWorldEvent(sessionId, "WORLD_STATE_UPDATE", mergedState);
        
        chatSessionRepository.save(session);
    }
}
```

#### 骰子检定
```java
public DiceRoll rollDice(String sessionId, Integer diceType, Integer modifier, 
                        String context, Integer difficultyClass) {
    // 生成随机数
    Random random = new Random();
    int result = random.nextInt(diceType) + 1;
    int finalResult = result + (modifier != null ? modifier : 0);
    
    // 判断是否成功
    boolean isSuccessful = difficultyClass == null || finalResult >= difficultyClass;
    
    // 创建骰子记录
    DiceRoll diceRoll = new DiceRoll();
    diceRoll.setSessionId(sessionId);
    diceRoll.setDiceType(diceType);
    diceRoll.setModifier(modifier);
    diceRoll.setResult(result);
    diceRoll.setFinalResult(finalResult);
    diceRoll.setContext(context);
    diceRoll.setIsSuccessful(isSuccessful);
    diceRoll.setDifficultyClass(difficultyClass);
    diceRoll.setCreatedAt(LocalDateTime.now());
    
    // 保存记录
    diceRollRepository.save(diceRoll);
    
    return diceRoll;
}
```

## 配置管理

### 1. 应用配置
```yaml
# application.yml
spring:
  application:
    name: qncontest
  datasource:
    url: jdbc:mysql://localhost:3306/qncontest
    username: ${DB_USERNAME:qncontest}
    password: ${DB_PASSWORD:password}
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect

# DashScope配置
dashscope:
  api-key: ${DASHSCOPE_API_KEY}
  model: qwen-plus
  temperature: 0.7
  max-tokens: 2000

# JWT配置
jwt:
  secret: ${JWT_SECRET}
  expiration: 900000
  refresh-expiration: 86400000
```

### 2. 安全配置
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/health").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
            
        return http.build();
    }
}
```

## 性能优化

### 1. 数据库优化
- 索引优化：为常用查询字段添加索引
- 连接池配置：优化数据库连接池参数
- 查询优化：使用JPA查询优化减少N+1问题

### 2. 缓存策略
- Redis缓存：缓存热点数据
- 本地缓存：缓存配置信息
- 查询缓存：缓存复杂查询结果

### 3. 异步处理
- 异步任务：耗时操作异步处理
- 线程池：合理配置线程池参数
- 流式处理：减少内存占用

## 监控和日志

### 1. 日志配置
```yaml
logging:
  level:
    com.qncontest: DEBUG
    org.springframework.security: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

### 2. 健康检查
```java
@Component
public class CustomHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        try {
            // 检查数据库连接
            // 检查外部服务
            // 检查系统资源
            
            return Health.up()
                .withDetail("database", "UP")
                .withDetail("ai-service", "UP")
                .build();
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}
```

## 部署配置

### 1. Docker配置
```dockerfile
FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/qncontest-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 2. 环境变量
```bash
# 生产环境变量
export DASHSCOPE_API_KEY=your_production_api_key
export JWT_SECRET=your_production_jwt_secret
export DB_PASSWORD=your_production_db_password
export SPRING_PROFILES_ACTIVE=prod
```

## 总结

QN Contest后端架构采用现代化的微服务设计，具有以下特点：

- **模块化设计**: 清晰的层次结构，职责分离
- **接口驱动**: 基于接口编程，易于测试和扩展
- **流式处理**: 支持实时AI响应，提升用户体验
- **智能记忆**: 自动记忆管理，保证故事连贯性
- **高性能**: 优化的数据库访问和缓存策略
- **可扩展**: 支持水平扩展和功能扩展

这种架构设计确保了系统的可维护性、可扩展性和高性能，为角色扮演AI系统提供了坚实的技术基础。
