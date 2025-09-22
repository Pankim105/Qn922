package com.qncontest.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qncontest.config.AiConfig;
import com.qncontest.dto.ChatRequest;
import com.qncontest.dto.ChatResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

@Service
public class StreamAiService {
    
    private static final Logger log = LoggerFactory.getLogger(StreamAiService.class);
    
    @Autowired
    private AiConfig aiConfig;
    
    @Autowired
    private ChatSessionService chatSessionService;
    
    @Autowired
    private IntelligentChatService intelligentChatService;
    
    private final ObjectMapper objectMapper = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    
    // 创建带有安全上下文的执行器
    private final Executor securityContextExecutor;
    
    public StreamAiService() {
        // 创建线程池
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("SSE-");
        executor.initialize();
        
        // 包装为支持安全上下文的执行器
        this.securityContextExecutor = new DelegatingSecurityContextAsyncTaskExecutor(executor);
    }
    
    /**
     * 流式对话接口
     */
    public SseEmitter streamChat(ChatRequest chatRequest) {
        SseEmitter emitter = new SseEmitter(60000L); // 60秒超时，增加超时时间
        
        // 获取当前用户信息
        String currentUser = getCurrentUsername();
        if (currentUser == null) {
            log.warn("无法获取当前用户信息");
            emitter.completeWithError(new RuntimeException("用户认证失败"));
            return emitter;
        }
        
        // 使用AtomicBoolean来跟踪emitter状态
        final java.util.concurrent.atomic.AtomicBoolean completed = new java.util.concurrent.atomic.AtomicBoolean(false);
        final StringBuilder assistantResponse = new StringBuilder();
        
        // 设置超时回调
        emitter.onTimeout(() -> {
            log.debug("SSE流式对话超时，正常关闭连接");
            if (completed.compareAndSet(false, true)) {
                try {
                    emitter.complete();
                } catch (Exception e) {
                    log.debug("完成超时的SSE连接时出错", e);
                }
            }
        });
        
        // 设置错误回调
        emitter.onError(throwable -> {
            log.debug("SSE连接发生错误，可能是客户端断开连接: {}", throwable.getMessage());
            if (completed.compareAndSet(false, true)) {
                try {
                    emitter.complete();
                } catch (Exception e) {
                    log.debug("完成错误的SSE连接时出错", e);
                }
            }
        });
        
        // 设置完成回调
        emitter.onCompletion(() -> {
            log.debug("SSE流式对话正常完成");
        });
        
        // 准备会话信息（在异步任务外部定义）
        final String sessionId = chatRequest.getSessionId() != null ? chatRequest.getSessionId() : "default";
        
        // 获取当前安全上下文
        final SecurityContext securityContext = SecurityContextHolder.getContext();
        
        CompletableFuture.runAsync(() -> {
            try {
                // 获取或创建智能会话
                com.qncontest.entity.ChatSession dbSession = intelligentChatService.getOrCreateSession(currentUser, sessionId);
                
                // 添加用户消息到数据库
                intelligentChatService.addMessage(dbSession, "user", chatRequest.getMessage());
                
                String apiKey = resolveApiKey();
                String url = aiConfig.getBaseUrl() + "/chat/completions";
                
                // 构建请求体
                StreamChatRequest requestBody = new StreamChatRequest();
                requestBody.setModel(aiConfig.getModel());
                requestBody.setStream(true);
                requestBody.setMaxTokens(2000);
                requestBody.setTemperature(0.7);
                
                // 构建智能上下文消息历史
                List<Message> messages = buildIntelligentMessageHistory(chatRequest, dbSession);
                requestBody.setMessages(messages);
                
                // 发送流式请求
                sendStreamRequest(url, apiKey, requestBody, emitter, completed, assistantResponse, dbSession);
                
            } catch (IllegalStateException e) {
                // API密钥未配置，返回模拟响应
                log.warn("DashScope API密钥未配置，返回模拟响应: {}", e.getMessage());
                // 重新获取会话（在catch块中）
                com.qncontest.entity.ChatSession dbSession = intelligentChatService.getOrCreateSession(currentUser, sessionId);
                sendMockResponse(emitter, chatRequest.getMessage(), completed, assistantResponse, dbSession);
            } catch (Exception e) {
                log.error("流式对话失败，使用模拟响应", e);
                // 对于任何异常（包括SSL错误），都返回模拟响应
                com.qncontest.entity.ChatSession dbSession = intelligentChatService.getOrCreateSession(currentUser, sessionId);
                sendMockResponse(emitter, chatRequest.getMessage(), completed, assistantResponse, dbSession);
            }
        }, securityContextExecutor).exceptionally(throwable -> {
            // 处理异步任务中的异常
            log.error("异步流式对话任务失败", throwable);
            if (completed.compareAndSet(false, true)) {
                try {
                    Map<String, String> error = new HashMap<>();
                    error.put("error", "流式对话服务异常");
                    error.put("message", throwable.getMessage());
                    error.put("type", "service_error");
                    
                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data(error));
                    emitter.complete();
                } catch (Exception e) {
                    log.error("发送错误响应失败", e);
                    emitter.completeWithError(e);
                }
            }
            return null;
        });
        
        return emitter;
    }
    
    private void sendStreamRequest(String url, String apiKey, StreamChatRequest requestBody, SseEmitter emitter, java.util.concurrent.atomic.AtomicBoolean completed, StringBuilder assistantResponse, com.qncontest.entity.ChatSession dbSession) {
        try {
            URL requestUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) requestUrl.openConnection();
            
            try {
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Authorization", "Bearer " + apiKey);
                connection.setDoOutput(true);
                connection.setConnectTimeout(10000); // 10秒连接超时
                connection.setReadTimeout(30000); // 30秒读取超时
                
                // 发送请求体
                String json = objectMapper.writeValueAsString(requestBody);
                connection.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));
                
                // 检查响应状态
                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    log.error("API请求失败，状态码: {}", responseCode);
                    throw new RuntimeException("API请求失败，状态码: " + responseCode);
                }
                
                // 读取流式响应
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    
                    String line;
                    
                    while ((line = reader.readLine()) != null && !completed.get()) {
                        if (line.startsWith("data: ")) {
                            String data = line.substring(6);
                            
                            if ("[DONE]".equals(data)) {
                                // 保存助手回复到数据库
                                if (assistantResponse.length() > 0) {
                                    intelligentChatService.addMessage(dbSession, "assistant", assistantResponse.toString());
                                }
                                
                                // 发送完成信号
                                if (completed.compareAndSet(false, true)) {
                                    try {
                                        // 发送完成事件
                                        emitter.send(SseEmitter.event()
                                                .name("complete")
                                                .data(new ChatResponse("", true)));
                                        
                                        log.debug("SSE流完成，正常关闭连接");
                                        // 短暂延迟确保数据发送完成
                                        Thread.sleep(50);
                                        emitter.complete();
                                    } catch (Exception e) {
                                        log.warn("发送完成信号失败，强制关闭SSE连接", e);
                                        emitter.complete();
                                    }
                                }
                                break;
                            }
                            
                            try {
                                StreamResponse response = objectMapper.readValue(data, StreamResponse.class);
                                if (response.getChoices() != null && !response.getChoices().isEmpty()) {
                                    String content = response.getChoices().get(0).getDelta().getContent();
                                    if (content != null && !content.isEmpty()) {
                                        assistantResponse.append(content);
                                        
                                        // 发送流式数据
                                        if (!completed.get()) {
                                            emitter.send(SseEmitter.event()
                                                    .name("message")
                                                    .data(new ChatResponse(content, false)));
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                log.warn("解析流式响应失败: {}", data, e);
                            }
                        }
                    }
                    
                    // 确保发送完成信号和保存历史
                    if (completed.compareAndSet(false, true)) {
                        // 保存助手回复到数据库
                        if (assistantResponse.length() > 0) {
                            intelligentChatService.addMessage(dbSession, "assistant", assistantResponse.toString());
                        }
                        
                        try {
                            // 发送完成事件
                            emitter.send(SseEmitter.event()
                                    .name("complete")
                                    .data(new ChatResponse("", true)));
                            
                            log.debug("SSE流正常结束");
                            // 短暂延迟确保数据发送完成
                            Thread.sleep(50);
                            emitter.complete();
                        } catch (Exception e) {
                            log.warn("发送流结束信号失败，强制关闭连接", e);
                            emitter.complete();
                        }
                    }
                    
                }
                
            } finally {
                connection.disconnect();
            }
            
        } catch (Exception e) {
            log.error("发送流式请求失败", e);
            if (completed.compareAndSet(false, true)) {
                try {
                    Map<String, String> error = new HashMap<>();
                    error.put("error", "API请求失败");
                    error.put("message", e.getMessage());
                    error.put("type", "api_error");
                    
                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data(error));
                    emitter.complete();
                } catch (Exception sendError) {
                    log.error("发送API错误响应失败", sendError);
                    emitter.completeWithError(sendError);
                }
            }
        }
    }
    
    private void sendMockResponse(SseEmitter emitter, String userMessage, java.util.concurrent.atomic.AtomicBoolean completed, StringBuilder assistantResponse, com.qncontest.entity.ChatSession dbSession) {
        try {
            if (completed.get()) {
                return;
            }
            
            // 模拟AI响应，考虑历史上下文
            String mockResponse = generateMockResponse(userMessage, dbSession);
            
            // 模拟流式输出 - 按字符分割而不是按单词，更好地模拟真实流式效果
            char[] chars = mockResponse.toCharArray();
            StringBuilder buffer = new StringBuilder();
            
            for (int i = 0; i < chars.length && !completed.get(); i++) {
                buffer.append(chars[i]);
                assistantResponse.append(chars[i]);
                
                // 每隔几个字符发送一次，或者遇到标点符号时发送
                if (buffer.length() >= 3 || chars[i] == '，' || chars[i] == '。' || chars[i] == '！' || chars[i] == '？' || 
                    chars[i] == ',' || chars[i] == '.' || chars[i] == '!' || chars[i] == '?' || i == chars.length - 1) {
                    
                    String chunk = buffer.toString();
                    emitter.send(SseEmitter.event()
                        .name("message")
                        .data(new ChatResponse(chunk, false)));
                    
                    buffer.setLength(0); // 清空buffer
                    
                    // 模拟延迟
                    Thread.sleep(150);
                }
            }
            
            // 保存助手回复到数据库
            if (assistantResponse.length() > 0) {
                intelligentChatService.addMessage(dbSession, "assistant", assistantResponse.toString());
            }
            
            // 发送完成信号
            if (completed.compareAndSet(false, true)) {
                try {
                    // 发送完成事件
                    emitter.send(SseEmitter.event()
                        .name("complete")
                        .data(new ChatResponse("", true)));
                    
                    log.debug("模拟SSE流完成");
                    // 短暂延迟确保数据发送完成
                    Thread.sleep(50);
                    emitter.complete();
                } catch (Exception e) {
                    log.warn("发送模拟完成信号失败，强制关闭连接", e);
                    emitter.complete();
                }
            }
            
        } catch (InterruptedException e) {
            log.warn("模拟响应被中断", e);
            Thread.currentThread().interrupt();
            if (completed.compareAndSet(false, true)) {
                try {
                    Map<String, String> error = new HashMap<>();
                    error.put("error", "响应被中断");
                    error.put("type", "interrupted");
                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data(error));
                    emitter.complete();
                } catch (Exception sendError) {
                    emitter.completeWithError(sendError);
                }
            }
        } catch (Exception e) {
            log.error("发送模拟响应失败", e);
            if (completed.compareAndSet(false, true)) {
                try {
                    Map<String, String> error = new HashMap<>();
                    error.put("error", "模拟响应失败");
                    error.put("message", e.getMessage());
                    error.put("type", "mock_error");
                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data(error));
                    emitter.complete();
                } catch (Exception sendError) {
                    emitter.completeWithError(sendError);
                }
            }
        }
    }
    
    /**
     * 构建智能上下文消息历史（避免token浪费）
     */
    private List<Message> buildIntelligentMessageHistory(ChatRequest chatRequest, com.qncontest.entity.ChatSession dbSession) {
        List<Message> messages = new ArrayList<>();
        
        // 添加系统消息
        String systemPrompt = chatRequest.getSystemPrompt() != null ? 
            chatRequest.getSystemPrompt() : 
            "你是一个智能助手，可以帮助用户解答各种问题。请用友好、专业的语气回答。记住之前的对话内容，保持对话的连贯性。";
        messages.add(new Message("system", systemPrompt));
        
        // 获取智能上下文历史（限制token数量）
        List<com.qncontest.entity.ChatMessage> contextHistory = intelligentChatService.getIntelligentContext(dbSession);
        for (com.qncontest.entity.ChatMessage historyMsg : contextHistory) {
            messages.add(new Message(historyMsg.getRole(), historyMsg.getContent()));
        }
        
        // 添加当前用户消息
        messages.add(new Message("user", chatRequest.getMessage()));
        
        log.debug("构建智能消息历史，会话: {}, 消息数: {}", dbSession.getSessionId(), messages.size());
        return messages;
    }
    
    /**
     * 获取当前认证用户的用户名
     */
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : null;
    }
    
    private String generateMockResponse(String userMessage, com.qncontest.entity.ChatSession dbSession) {
        // 获取智能上下文历史以提供更智能的模拟响应
        List<com.qncontest.entity.ChatMessage> history = intelligentChatService.getIntelligentContext(dbSession);
        
        // 基于历史的智能响应逻辑
        if (userMessage.contains("你好") || userMessage.contains("hello")) {
            if (!history.isEmpty()) {
                long conversationRounds = history.stream().filter(msg -> "user".equals(msg.getRole())).count();
                return "又见面了！我们之前聊过 " + conversationRounds + " 轮对话。有什么新的问题吗？";
            } else {
                return "你好！我是AI助手，很高兴为您服务。虽然目前没有配置真实的AI服务，但我可以回答一些基本问题。";
            }
        } else if (userMessage.contains("之前") || userMessage.contains("刚才") || userMessage.contains("刚刚")) {
            if (history.size() >= 2) {
                com.qncontest.entity.ChatMessage lastAssistantMsg = null;
                for (int i = history.size() - 1; i >= 0; i--) {
                    if ("assistant".equals(history.get(i).getRole())) {
                        lastAssistantMsg = history.get(i);
                        break;
                    }
                }
                if (lastAssistantMsg != null) {
                    return "您是指我刚才说的：\"" + lastAssistantMsg.getContent().substring(0, Math.min(50, lastAssistantMsg.getContent().length())) + "...\" 吗？";
                }
            }
            return "抱歉，我们刚开始对话，还没有之前的内容。";
        } else if (userMessage.contains("天气")) {
            return "抱歉，我目前无法获取实时天气信息。建议您查看天气预报应用或网站。";
        } else if (userMessage.contains("时间")) {
            return "当前时间是 " + java.time.LocalDateTime.now().toString() + "。";
        } else {
            long conversationRounds = history.stream().filter(msg -> "user".equals(msg.getRole())).count();
            String responsePrefix = conversationRounds > 0 ? 
                "基于我们之前 " + conversationRounds + " 轮对话，" : 
                "";
            return responsePrefix + "这是一个模拟的AI响应。您的问题很有趣：" + userMessage + "。要获得真实的AI回答，请配置DashScope API密钥。";
        }
    }
    
    private String resolveApiKey() {
        // 优先环境变量，其次配置
        String envKey = System.getenv("DASHSCOPE_API_KEY");
        if (envKey != null && !envKey.isBlank()) {
            return envKey;
        }
        String configKey = aiConfig.getApiKey();
        if (configKey != null && !configKey.isBlank() && !configKey.equals("DASHSCOPE_API_KEY")) {
            return configKey;
        }
        throw new IllegalStateException("未配置 DashScope API Key");
    }
    
    static class Message {
        private String role;
        private String content;
        
        public Message() {}
        
        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
        
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
    
    static class StreamChatRequest {
        private String model;
        private boolean stream = true;
        private int maxTokens = 2000;
        private double temperature = 0.7;
        private List<Message> messages;
        
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public boolean isStream() { return stream; }
        public void setStream(boolean stream) { this.stream = stream; }
        public int getMaxTokens() { return maxTokens; }
        public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
        public double getTemperature() { return temperature; }
        public void setTemperature(double temperature) { this.temperature = temperature; }
        public List<Message> getMessages() { return messages; }
        public void setMessages(List<Message> messages) { this.messages = messages; }
    }
    
    static class StreamResponse {
        private String id;
        private String object;
        private long created;
        private List<Choice> choices;
        private Object usage; // 添加usage字段
        @JsonProperty("system_fingerprint")
        private String systemFingerprint; // 添加system_fingerprint字段
        private String model; // 添加model字段

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getObject() { return object; }
        public void setObject(String object) { this.object = object; }
        public long getCreated() { return created; }
        public void setCreated(long created) { this.created = created; }
        public List<Choice> getChoices() { return choices; }
        public void setChoices(List<Choice> choices) { this.choices = choices; }
        public Object getUsage() { return usage; }
        public void setUsage(Object usage) { this.usage = usage; }
        public String getSystemFingerprint() { return systemFingerprint; }
        public void setSystemFingerprint(String systemFingerprint) { this.systemFingerprint = systemFingerprint; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        
        static class Choice {
            private int index;
            private Delta delta;
            @JsonProperty("finish_reason")
            private String finishReason;
            private Object logprobs; // 添加logprobs字段

            public int getIndex() { return index; }
            public void setIndex(int index) { this.index = index; }
            public Delta getDelta() { return delta; }
            public void setDelta(Delta delta) { this.delta = delta; }
            public String getFinishReason() { return finishReason; }
            public void setFinishReason(String finishReason) { this.finishReason = finishReason; }
            public Object getLogprobs() { return logprobs; }
            public void setLogprobs(Object logprobs) { this.logprobs = logprobs; }
        }
        
        static class Delta {
            private String content;
            private String role; // 添加role字段
            
            public String getContent() { return content; }
            public void setContent(String content) { this.content = content; }
            public String getRole() { return role; }
            public void setRole(String role) { this.role = role; }
        }
    }
}
