package com.qncontest.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.dashscope.QwenChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.dashscope.QwenEmbeddingModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LangChain4j 配置类
 */
@Configuration
@ConditionalOnProperty(name = "langchain4j.dashscope.chat-model.api-key")
public class LangChainConfig {
    
    private static final Logger log = LoggerFactory.getLogger(LangChainConfig.class);
    
    @Value("${langchain4j.dashscope.chat-model.api-key:}")
    private String apiKey;
    
    @Value("${langchain4j.dashscope.chat-model.model-name:qwen-plus}")
    private String chatModelName;
    
    @Value("${langchain4j.dashscope.chat-model.temperature:0.7}")
    private Double temperature;
    
    @Value("${langchain4j.dashscope.chat-model.max-tokens:2000}")
    private Integer maxTokens;
    
    @Value("${langchain4j.dashscope.embedding-model.model-name:text-embedding-v2}")
    private String embeddingModelName;
    
    /**
     * 配置聊天模型
     */
    @Bean
    @ConditionalOnProperty(name = "langchain4j.dashscope.chat-model.api-key")
    public ChatLanguageModel chatLanguageModel() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("DashScope API Key 未配置，LangChain4j 聊天模型不可用");
            return null;
        }
        
        log.info("初始化 LangChain4j 聊天模型: {}", chatModelName);
        
        return QwenChatModel.builder()
                .apiKey(apiKey)
                .modelName(chatModelName)
                .temperature(temperature.floatValue())
                .maxTokens(maxTokens)
                .topP(0.8)
                .build();
    }
    
    /**
     * 配置嵌入模型
     */
    @Bean
    @ConditionalOnProperty(name = "langchain4j.dashscope.chat-model.api-key")
    public EmbeddingModel embeddingModel() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("DashScope API Key 未配置，LangChain4j 嵌入模型不可用");
            return null;
        }
        
        log.info("初始化 LangChain4j 嵌入模型: {}", embeddingModelName);
        
        return QwenEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName(embeddingModelName)
                .build();
    }
}
