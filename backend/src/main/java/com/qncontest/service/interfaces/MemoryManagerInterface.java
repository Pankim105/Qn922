package com.qncontest.service.interfaces;

/**
 * 记忆管理器接口
 * 定义记忆管理的标准行为
 */
public interface MemoryManagerInterface {
    
    /**
     * 构建记忆上下文
     * @param sessionId 会话ID
     * @param currentMessage 当前消息
     * @return 记忆上下文
     */
    String buildMemoryContext(String sessionId, String currentMessage);
    
    /**
     * 评估记忆重要性
     * @param content 记忆内容
     * @param userAction 用户行为
     * @return 重要性评分 (0-1)
     */
    double assessMemoryImportance(String content, String userAction);
    
    /**
     * 存储记忆
     * @param sessionId 会话ID
     * @param content 记忆内容
     * @param memoryType 记忆类型
     * @param importance 重要性评分
     */
    void storeMemory(String sessionId, String content, String memoryType, double importance);
    
    /**
     * 处理记忆标记
     * @param sessionId 会话ID
     * @param aiResponse AI回复
     * @param userAction 用户行为
     */
    void processMemoryMarkers(String sessionId, String aiResponse, String userAction);
}
