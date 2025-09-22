package com.qncontest.repository;

import com.qncontest.entity.ChatMessage;
import com.qncontest.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    /**
     * 根据会话查找所有消息，按序号排序
     */
    List<ChatMessage> findByChatSessionOrderBySequenceNumberAsc(ChatSession chatSession);
    
    /**
     * 获取会话中的最大序号
     */
    @Query("SELECT COALESCE(MAX(cm.sequenceNumber), 0) FROM ChatMessage cm WHERE cm.chatSession = :chatSession")
    Integer findMaxSequenceNumberBySession(@Param("chatSession") ChatSession chatSession);
    
    /**
     * 根据会话删除所有消息
     */
    void deleteByChatSession(ChatSession chatSession);
    
    /**
     * 获取会话的消息数量
     */
    long countByChatSession(ChatSession chatSession);
}
