package com.qncontest.service;

import com.qncontest.entity.ChatMessage;
import com.qncontest.entity.ChatSession;
import com.qncontest.repository.ChatMessageRepository;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * LangChain4j 增强的AI服务
 * 提供智能摘要、上下文分析等高级功能
 */
@Service
@ConditionalOnBean(ChatLanguageModel.class)
public class LangChainEnhancedService {
    
    private static final Logger log = LoggerFactory.getLogger(LangChainEnhancedService.class);
    
    @Autowired(required = false)
    private ChatLanguageModel chatLanguageModel;
    
    @Autowired(required = false)
    private EmbeddingModel embeddingModel;
    
    @Autowired
    @Lazy
    private IntelligentChatService intelligentChatService;
    
    @Autowired
    private ChatMessageRepository chatMessageRepository;
    
    /**
     * 生成对话摘要
     */
    public String generateConversationSummary(ChatSession session) {
        if (chatLanguageModel == null) {
            log.warn("ChatLanguageModel 未配置，使用简单摘要");
            return generateSimpleSummary(session);
        }
        
        try {
            List<ChatMessage> messages = intelligentChatService.getIntelligentContext(session);
            if (messages.isEmpty()) {
                return "空对话";
            }
            
            // 构建对话历史文本
            String conversationText = messages.stream()
                    .map(msg -> String.format("%s: %s", 
                        msg.getRole().toUpperCase(), msg.getContent()))
                    .collect(Collectors.joining("\n"));
            
            // 创建摘要提示
            List<dev.langchain4j.data.message.ChatMessage> prompt = List.of(
                SystemMessage.from("你是一个专业的对话摘要助手。请将提供的对话历史总结为简洁的要点，保留关键信息和上下文。摘要应该在100字以内。"),
                UserMessage.from("请总结以下对话:\n\n" + conversationText)
            );
            
            Response<AiMessage> response = chatLanguageModel.generate(prompt);
            String summary = response.content().text();
            
            log.info("为会话 {} 生成LangChain摘要，长度: {}", session.getSessionId(), summary.length());
            return summary;
            
        } catch (Exception e) {
            log.error("LangChain摘要生成失败，回退到简单摘要", e);
            return generateSimpleSummary(session);
        }
    }
    
    /**
     * 智能分析消息重要性
     */
    public double analyzeMessageImportance(String message, List<ChatMessage> context) {
        if (chatLanguageModel == null) {
            return estimateSimpleImportance(message);
        }
        
        try {
            String contextText = context.stream()
                    .map(ChatMessage::getContent)
                    .collect(Collectors.joining(" "));
            
            List<dev.langchain4j.data.message.ChatMessage> prompt = List.of(
                SystemMessage.from("你是一个对话分析专家。请分析给定消息在对话上下文中的重要性，返回1-10的分数，其中10表示最重要。只返回数字。"),
                UserMessage.from(String.format("上下文: %s\n\n要分析的消息: %s\n\n重要性分数:", 
                    contextText.substring(0, Math.min(500, contextText.length())), message))
            );
            
            Response<AiMessage> response = chatLanguageModel.generate(prompt);
            String scoreText = response.content().text().trim();
            
            try {
                double score = Double.parseDouble(scoreText);
                return Math.max(1.0, Math.min(10.0, score)); // 限制在1-10范围内
            } catch (NumberFormatException e) {
                log.warn("无法解析重要性分数: {}", scoreText);
                return estimateSimpleImportance(message);
            }
            
        } catch (Exception e) {
            log.error("LangChain重要性分析失败", e);
            return estimateSimpleImportance(message);
        }
    }
    
    /**
     * 智能筛选相关历史消息
     */
    public List<ChatMessage> selectRelevantMessages(String currentQuestion, List<ChatMessage> allMessages, int maxMessages) {
        if (allMessages.size() <= maxMessages) {
            return allMessages;
        }
        
        if (chatLanguageModel == null) {
            // 如果没有AI模型，返回最近的消息
            return allMessages.subList(Math.max(0, allMessages.size() - maxMessages), allMessages.size());
        }
        
        try {
            // 为每个消息计算相关性分数
            List<ScoredMessage> scoredMessages = new ArrayList<>();
            
            for (ChatMessage msg : allMessages) {
                double relevanceScore = calculateRelevanceScore(currentQuestion, msg.getContent());
                double importanceScore = analyzeMessageImportance(msg.getContent(), allMessages);
                double combinedScore = relevanceScore * 0.7 + importanceScore * 0.3;
                
                scoredMessages.add(new ScoredMessage(msg, combinedScore));
            }
            
            // 按分数排序并选择前maxMessages个
            return scoredMessages.stream()
                    .sorted((a, b) -> Double.compare(b.score, a.score))
                    .limit(maxMessages)
                    .map(sm -> sm.message)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("智能消息筛选失败，使用时间顺序", e);
            return allMessages.subList(Math.max(0, allMessages.size() - maxMessages), allMessages.size());
        }
    }
    
    /**
     * 计算消息相关性分数
     */
    private double calculateRelevanceScore(String question, String messageContent) {
        if (embeddingModel == null) {
            // 简单的关键词匹配
            String[] questionWords = question.toLowerCase().split("\\s+");
            String lowerContent = messageContent.toLowerCase();
            
            long matchCount = java.util.Arrays.stream(questionWords)
                    .filter(word -> word.length() > 2)
                    .mapToLong(word -> lowerContent.contains(word) ? 1 : 0)
                    .sum();
            
            return Math.min(10.0, (double) matchCount / questionWords.length * 10);
        }
        
        try {
            // TODO: 使用向量相似性计算（需要实现向量存储）
            // 这里先用简单实现
            return estimateSimpleRelevance(question, messageContent);
            
        } catch (Exception e) {
            log.error("向量相似性计算失败", e);
            return estimateSimpleRelevance(question, messageContent);
        }
    }
    
    /**
     * 检查是否需要对话压缩
     */
    public boolean shouldCompressConversation(ChatSession session) {
        try {
            // 使用数据库查询获取消息数量，避免LazyInitializationException
            long messageCount = chatMessageRepository.countBySession(session);
            
            // 基于消息数量
            if (messageCount > 50) {
                return true;
            }
            
            // 基于总token数量
            Long totalTokens = chatMessageRepository.calculateTotalTokensBySession(session);
            
            return totalTokens != null && totalTokens > 8000;
        } catch (Exception e) {
            log.error("检查对话压缩条件时出错", e);
            return false;
        }
    }
    
    /**
     * 压缩对话历史
     */
    public void compressConversationHistory(ChatSession session) {
        if (!shouldCompressConversation(session)) {
            return;
        }
        
        try {
            String summary = generateConversationSummary(session);
            
            // TODO: 实现历史压缩逻辑
            // 1. 生成摘要
            // 2. 保留最近的重要消息
            // 3. 用摘要替换旧消息
            
            log.info("压缩会话 {} 的历史记录", session.getSessionId());
            
        } catch (Exception e) {
            log.error("对话历史压缩失败", e);
        }
    }
    
    /**
     * 生成简单摘要（备用方案）
     */
    private String generateSimpleSummary(ChatSession session) {
        List<ChatMessage> messages = session.getMessages();
        
        if (messages.isEmpty()) {
            return "空对话";
        }
        
        long userMessages = messages.stream().filter(m -> "user".equals(m.getRole())).count();
        long assistantMessages = messages.stream().filter(m -> "assistant".equals(m.getRole())).count();
        
        String firstUserMessage = messages.stream()
                .filter(m -> "user".equals(m.getRole()))
                .map(ChatMessage::getContent)
                .findFirst()
                .orElse("未知");
        
        return String.format("对话摘要: 用户提问%d次，AI回复%d次。首个问题: %s", 
                userMessages, assistantMessages, 
                firstUserMessage.substring(0, Math.min(30, firstUserMessage.length())));
    }
    
    /**
     * 简单重要性估算
     */
    private double estimateSimpleImportance(String message) {
        // 基于长度和关键词的简单估算
        double lengthScore = Math.min(5.0, message.length() / 20.0);
        
        String[] importantKeywords = {"问题", "错误", "帮助", "重要", "紧急", "请", "谢谢", "抱歉"};
        long keywordCount = java.util.Arrays.stream(importantKeywords)
                .mapToLong(keyword -> message.contains(keyword) ? 1 : 0)
                .sum();
        
        double keywordScore = Math.min(5.0, keywordCount * 2);
        
        return lengthScore + keywordScore;
    }
    
    /**
     * 简单相关性估算
     */
    private double estimateSimpleRelevance(String question, String content) {
        String[] questionWords = question.toLowerCase().split("\\s+");
        String lowerContent = content.toLowerCase();
        
        long matchCount = java.util.Arrays.stream(questionWords)
                .filter(word -> word.length() > 2)
                .mapToLong(word -> lowerContent.contains(word) ? 1 : 0)
                .sum();
        
        return questionWords.length > 0 ? (double) matchCount / questionWords.length * 10 : 0;
    }
    
    /**
     * Token数量估算
     */
    private int estimateTokens(String text) {
        if (text == null) return 0;
        long chineseChars = text.chars().filter(ch -> ch >= 0x4E00 && ch <= 0x9FFF).count();
        long englishWords = text.split("\\s+").length;
        return (int)(chineseChars * 1.5 + englishWords);
    }
    
    /**
     * 检查LangChain4j是否可用
     */
    public boolean isAvailable() {
        return chatLanguageModel != null;
    }
    
    /**
     * 内部类：带分数的消息
     */
    private static class ScoredMessage {
        final ChatMessage message;
        final double score;
        
        ScoredMessage(ChatMessage message, double score) {
            this.message = message;
            this.score = score;
        }
    }
}
