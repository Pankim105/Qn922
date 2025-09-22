package com.qncontest.config;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.dashscope.QwenStreamingChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class LangChainConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(LangChainConfig.class);
    
    @Value("${langchain4j.dashscope.api-key:}")
    private String dashscopeApiKey;
    
    @Value("${langchain4j.dashscope.model-name:qwen-plus}")
    private String modelName;
    
    @Value("${langchain4j.dashscope.temperature:0.7}")
    private Float temperature;
    
    @Value("${langchain4j.dashscope.max-tokens:2000}")
    private Integer maxTokens;
    
    @Bean
    public StreamingChatLanguageModel streamingChatLanguageModel() {
        if (dashscopeApiKey == null || dashscopeApiKey.trim().isEmpty()) {
            logger.warn("DashScope API key is not configured. Using mock model for development.");
            return new MockStreamingChatLanguageModel();
        }
        
        logger.info("Initializing DashScope streaming chat model with model: {}, temperature: {}, maxTokens: {}", 
                   modelName, temperature, maxTokens);
        
        return QwenStreamingChatModel.builder()
                .apiKey(dashscopeApiKey)
                .modelName(modelName)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .build();
    }
    
    /**
     * Mock implementation for development when API key is not available
     */
    private static class MockStreamingChatLanguageModel implements StreamingChatLanguageModel {
        
        @Override
        public void generate(List<ChatMessage> messages, StreamingResponseHandler<AiMessage> handler) {
            // 模拟流式响应
            String mockResponse = "这是一个模拟的AI回复。当前使用的是开发模式，因为没有配置有效的API密钥。请配置langchain.dashscope.api-key以使用真实的AI服务。";
            
            // 模拟分块传输
            String[] chunks = mockResponse.split("。");
            
            new Thread(() -> {
                try {
                    for (String chunk : chunks) {
                        if (!chunk.trim().isEmpty()) {
                            handler.onNext(chunk + "。");
                            Thread.sleep(200); // 模拟网络延迟
                        }
                    }
                    handler.onComplete(null);
                } catch (InterruptedException e) {
                    handler.onError(e);
                }
            }).start();
        }
    }
}
