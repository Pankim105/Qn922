package com.qncontest.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Configuration
@EnableConfigurationProperties(AiConfig.AiProperties.class)
public class AiConfig {
    
    @ConfigurationProperties(prefix = "ai.chat")
    public static class AiProperties {
        
        /**
         * 默认系统提示
         */
        private String defaultSystemPrompt = "你是一个智能助手，可以帮助用户解答各种问题。请用友好、专业的语气回答。记住之前的对话内容，保持对话的连贯性。";
        
        /**
         * 最大历史消息数量
         */
        private int maxHistoryMessages = 20;
        
        /**
         * 单次对话最大token数
         */
        private int maxTokens = 2000;
        
        /**
         * 温度参数，控制回复的随机性
         */
        private double temperature = 0.7;
        
        /**
         * 流式响应的块大小
         */
        private int streamChunkSize = 50;
        
        /**
         * 会话超时时间（分钟）
         */
        private int sessionTimeoutMinutes = 60;
        
        // Getters and Setters
        public String getDefaultSystemPrompt() {
            return defaultSystemPrompt;
        }
        
        public void setDefaultSystemPrompt(String defaultSystemPrompt) {
            this.defaultSystemPrompt = defaultSystemPrompt;
        }
        
        public int getMaxHistoryMessages() {
            return maxHistoryMessages;
        }
        
        public void setMaxHistoryMessages(int maxHistoryMessages) {
            this.maxHistoryMessages = maxHistoryMessages;
        }
        
        public int getMaxTokens() {
            return maxTokens;
        }
        
        public void setMaxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
        }
        
        public double getTemperature() {
            return temperature;
        }
        
        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }
        
        public int getStreamChunkSize() {
            return streamChunkSize;
        }
        
        public void setStreamChunkSize(int streamChunkSize) {
            this.streamChunkSize = streamChunkSize;
        }
        
        public int getSessionTimeoutMinutes() {
            return sessionTimeoutMinutes;
        }
        
        public void setSessionTimeoutMinutes(int sessionTimeoutMinutes) {
            this.sessionTimeoutMinutes = sessionTimeoutMinutes;
        }
    }
}

