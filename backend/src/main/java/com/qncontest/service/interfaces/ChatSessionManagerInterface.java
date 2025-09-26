package com.qncontest.service.interfaces;

import com.qncontest.dto.ChatResponse;
import com.qncontest.entity.ChatMessage;
import com.qncontest.entity.ChatSession;
import com.qncontest.entity.User;

import java.util.List;

/**
 * 聊天会话管理器接口
 * 定义聊天会话管理的标准行为
 */
public interface ChatSessionManagerInterface {
    
    /**
     * 获取用户的所有聊天会话
     * @param user 用户信息
     * @return 会话信息列表
     */
    List<ChatResponse.SessionInfo> getUserSessions(User user);
    
    /**
     * 获取或创建聊天会话
     * @param sessionId 会话ID
     * @param user 用户信息
     * @return 聊天会话
     */
    ChatSession getOrCreateSession(String sessionId, User user);
    
    /**
     * 获取会话的所有消息
     * @param sessionId 会话ID
     * @param user 用户信息
     * @return 消息信息列表
     */
    List<ChatResponse.MessageInfo> getSessionMessages(String sessionId, User user);
    
    /**
     * 保存用户消息
     * @param session 聊天会话
     * @param content 消息内容
     * @return 保存的消息
     */
    ChatMessage saveUserMessage(ChatSession session, String content);
    
    /**
     * 保存AI消息
     * @param session 聊天会话
     * @param content 消息内容
     * @return 保存的消息
     */
    ChatMessage saveAiMessage(ChatSession session, String content);
    
    /**
     * 更新会话标题
     * @param session 聊天会话
     * @param title 新标题
     */
    void updateSessionTitle(ChatSession session, String title);
    
    /**
     * 保存会话
     * @param session 聊天会话
     * @return 保存后的会话
     */
    ChatSession saveSession(ChatSession session);
    
    /**
     * 删除会话
     * @param sessionId 会话ID
     * @param user 用户信息
     * @return 是否删除成功
     */
    boolean deleteSession(String sessionId, User user);
    
    /**
     * 获取会话的历史消息
     * @param session 聊天会话
     * @param maxMessages 最大消息数
     * @return 历史消息列表
     */
    List<ChatMessage> getSessionHistory(ChatSession session, int maxMessages);
    
    /**
     * 根据会话ID获取会话
     * @param sessionId 会话ID
     * @return 聊天会话，如果不存在则返回null
     */
    ChatSession getSessionById(String sessionId);
}
