package com.qncontest.repository;

import com.qncontest.entity.ChatMessage;
import com.qncontest.entity.ChatSession;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    /**
     * 获取会话的最近消息（智能上下文窗口）
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.chatSession = :session ORDER BY cm.sequenceNumber DESC")
    List<ChatMessage> findRecentMessagesBySession(@Param("session") ChatSession session, Pageable pageable);
    
    /**
     * 获取会话的所有消息
     */
    List<ChatMessage> findByChatSessionOrderBySequenceNumberAsc(ChatSession chatSession);
    
    /**
     * 获取会话中指定数量的最新消息
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.chatSession = :session ORDER BY cm.sequenceNumber DESC")
    List<ChatMessage> findTopMessagesBySession(@Param("session") ChatSession session, Pageable pageable);
    
    /**
     * 计算会话的总token数
     */
    @Query("SELECT COALESCE(SUM(cm.tokens), 0) FROM ChatMessage cm WHERE cm.chatSession = :session")
    Long calculateTotalTokensBySession(@Param("session") ChatSession session);
    
    /**
     * 获取会话中最大的序号
     */
    @Query("SELECT COALESCE(MAX(cm.sequenceNumber), 0) FROM ChatMessage cm WHERE cm.chatSession = :session")
    Integer getMaxSequenceNumberBySession(@Param("session") ChatSession session);
    
    /**
     * 统计会话的消息数量
     */
    @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE cm.chatSession = :session")
    long countBySession(@Param("session") ChatSession session);
}
