# 🚀 LangChain4j 集成指南

## 为什么要使用 LangChain4j？

### 1. **内存管理优化**
- **自动摘要**: 当上下文过长时自动生成对话摘要
- **智能裁剪**: 根据重要性保留关键对话片段
- **向量存储**: 将历史对话转换为向量，相似性检索

### 2. **高级功能**
- **RAG (检索增强生成)**: 结合外部知识库
- **工具调用**: AI可以调用外部API和工具
- **多模态支持**: 文本、图片、音频等

## 集成步骤

### 1. 添加依赖

```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-spring-boot-starter</artifactId>
    <version>0.27.0</version>
</dependency>

<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-dashscope</artifactId>
    <version>0.27.0</version>
</dependency>

<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-embeddings</artifactId>
    <version>0.27.0</version>
</dependency>

<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-chroma</artifactId>
    <version>0.27.0</version>
</dependency>
```

### 2. 配置 LangChain4j

```yaml
langchain4j:
  dashscope:
    chat-model:
      api-key: ${DASHSCOPE_API_KEY}
      model-name: qwen-plus-latest
      temperature: 0.7
      max-tokens: 2000
    embedding-model:
      api-key: ${DASHSCOPE_API_KEY}
      model-name: text-embedding-v1
  memory:
    chat-memory-store:
      type: in-memory  # 或 persistent
  vector-store:
    chroma:
      base-url: http://localhost:8000
      collection-name: chat-history
```

### 3. 智能对话服务

```java
@Service
public class LangChainChatService {
    
    @Autowired
    private ChatLanguageModel chatModel;
    
    @Autowired
    private EmbeddingModel embeddingModel;
    
    @Autowired
    private VectorStore vectorStore;
    
    /**
     * 智能对话 - 自动管理上下文
     */
    public Flux<String> intelligentChat(String userId, String sessionId, String message) {
        
        // 1. 检索相关历史对话
        List<TextSegment> relevantHistory = retrieveRelevantHistory(userId, sessionId, message);
        
        // 2. 构建智能提示
        String intelligentPrompt = buildIntelligentPrompt(message, relevantHistory);
        
        // 3. 流式生成回复
        return chatModel.generate(intelligentPrompt)
                .doOnNext(response -> saveToVectorStore(userId, sessionId, message, response));
    }
    
    /**
     * 检索相关历史对话（向量相似性）
     */
    private List<TextSegment> retrieveRelevantHistory(String userId, String sessionId, String message) {
        // 将当前消息转换为向量
        Embedding queryEmbedding = embeddingModel.embed(message).content();
        
        // 在向量数据库中搜索相似对话
        return vectorStore.findRelevant(queryEmbedding, 5); // 检索5个最相关的对话片段
    }
}
```

### 4. 高级内存管理

```java
@Component
public class IntelligentMemoryManager {
    
    /**
     * 对话摘要压缩
     */
    public String summarizeConversation(List<ChatMessage> messages) {
        String prompt = """
            请将以下对话总结为简洁的要点，保留关键信息：
            
            %s
            
            总结要点：
            """.formatted(formatMessages(messages));
            
        return chatModel.generate(prompt);
    }
    
    /**
     * 智能消息过滤
     */
    public List<ChatMessage> filterImportantMessages(List<ChatMessage> messages) {
        // 使用AI判断消息的重要性
        return messages.stream()
                .filter(this::isImportantMessage)
                .collect(Collectors.toList());
    }
}
```

## 优势对比

| 功能 | 当前实现 | LangChain4j实现 |
|------|----------|----------------|
| 上下文管理 | 简单截断 | 智能摘要+向量检索 |
| Token效率 | 线性增长 | 常数级别 |
| 历史检索 | 时间顺序 | 语义相似性 |
| 扩展性 | 有限 | 丰富的工具链 |
| 多模态 | 不支持 | 全面支持 |

## 推荐架构

```
用户消息 → 向量化 → 相似性检索 → 智能提示构建 → AI生成 → 向量存储
    ↓           ↓           ↓             ↓         ↓         ↓
 本地存储   向量数据库   历史对话库     提示工程   流式输出   持久化
```

## 实施建议

### 阶段1：基础优化
1. ✅ 实现数据库持久化（已完成）
2. 🔄 集成 LangChain4j 基础功能
3. 🔄 实现智能上下文管理

### 阶段2：高级功能
1. 🆕 向量数据库集成（Chroma/Pinecone）
2. 🆕 对话摘要和压缩
3. 🆕 RAG功能集成

### 阶段3：企业级
1. 🆕 多模态支持
2. 🆕 工具调用能力
3. 🆕 分布式部署

这样的架构能够真正解决Token浪费问题，同时提供更智能的对话体验！
