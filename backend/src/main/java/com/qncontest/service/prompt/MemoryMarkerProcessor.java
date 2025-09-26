package com.qncontest.service.prompt;

import com.qncontest.service.interfaces.MemoryManagerInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 记忆标记处理器 - 实现MemoryManagerInterface接口
 * 负责处理AI回复中的记忆标记
 * 注意：指令处理已迁移到评估JSON中，不再在此处处理
 */
@Component
public class MemoryMarkerProcessor implements MemoryManagerInterface {
    
    private static final Logger logger = LoggerFactory.getLogger(MemoryMarkerProcessor.class);
    
    @Autowired
    private com.qncontest.service.RoleplayMemoryService memoryService;
    
    /**
     * 处理AI回复中的记忆标记
     */
    public void processMemoryMarkers(String sessionId, String aiResponse, String userAction) {
        try {
            logger.info("=== 开始处理AI回复中的记忆标记 ===");
            logger.info("会话ID: {}", sessionId);
            logger.info("用户行为: {}", userAction);
            logger.info("AI回复内容长度: {} 字符", aiResponse.length());
            
            // 解析AI回复中的记忆标记
            List<String> memoryMarkers = extractMemoryMarkers(aiResponse);
            logger.info("发现记忆标记数量: {}", memoryMarkers.size());
            if (!memoryMarkers.isEmpty()) {
                logger.info("记忆标记列表: {}", memoryMarkers);
            }
            
            // 注意：指令标记处理已迁移到评估JSON中，不再在此处处理

            // 处理记忆标记
            for (String marker : memoryMarkers) {
                try {
                    logger.info("处理记忆标记: {}", marker);
                    // 解析标记格式 [MEMORY:TYPE:PARAMS]
                    String[] parts = marker.split(":");
                    if (parts.length >= 3) {
                        String memoryType = parts[1];
                        String content = String.join(":", Arrays.copyOfRange(parts, 2, parts.length));

                        // 评估记忆重要性
                        double importance = assessMemoryImportance(content, userAction);
                        
                        // 存储记忆
                        storeMemory(sessionId, content, memoryType, importance);
                        
                        logger.info("记忆标记处理完成: type={}, importance={}", memoryType, importance);
                    } else {
                        logger.warn("记忆标记格式不正确: {}", marker);
                    }
                } catch (Exception e) {
                    logger.warn("解析记忆标记失败: marker={}", marker, e);
                }
            }
            
            logger.info("=== 记忆标记处理完成 ===");
            logger.info("📊 处理统计: 记忆标记={}", memoryMarkers.size());
        } catch (Exception e) {
            logger.error("处理AI回复记忆标记失败: sessionId={}", sessionId, e);
        }
    }
    
    /**
     * 从AI回复中提取记忆标记
     */
    private List<String> extractMemoryMarkers(String response) {
        List<String> markers = new ArrayList<>();
        // 匹配 [MEMORY:TYPE:CONTENT] 格式
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\[MEMORY:[^\\]]+\\]");
        java.util.regex.Matcher matcher = pattern.matcher(response);

        while (matcher.find()) {
            markers.add(matcher.group());
        }

        return markers;
    }
    
    // ==================== 接口实现 ====================
    
    /**
     * 构建记忆上下文
     */
    @Override
    public String buildMemoryContext(String sessionId, String currentMessage) {
        return memoryService.buildMemoryContext(sessionId, currentMessage);
    }
    
    /**
     * 评估记忆重要性
     */
    @Override
    public double assessMemoryImportance(String content, String userAction) {
        return memoryService.assessMemoryImportance(content, userAction);
    }
    
    /**
     * 存储记忆
     */
    @Override
    public void storeMemory(String sessionId, String content, String memoryType, double importance) {
        memoryService.storeMemory(sessionId, content, memoryType, importance);
    }
}