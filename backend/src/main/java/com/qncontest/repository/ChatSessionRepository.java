package com.qncontest.repository;

import com.qncontest.entity.ChatSession;
import com.qncontest.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, String> {
    
    /**
     * 根据用户查找所有聊天会话，按更新时间降序排列
     */
    List<ChatSession> findByUserOrderByUpdatedAtDesc(User user);
    
    /**
     * 根据会话ID和用户查找会话
     */
    Optional<ChatSession> findBySessionIdAndUser(String sessionId, User user);
    
    /**
     * 删除用户的指定会话
     */
    void deleteBySessionIdAndUser(String sessionId, User user);
    
    /**
     * 获取用户的会话数量
     */
    long countByUser(User user);
    
    /**
     * 查询用户的会话列表，包含消息数量
     */
    @Query("SELECT cs FROM ChatSession cs LEFT JOIN FETCH cs.messages WHERE cs.user = :user ORDER BY cs.updatedAt DESC")
    List<ChatSession> findByUserWithMessages(@Param("user") User user);
    
    /**
     * 根据会话ID查找会话，并预加载消息
     */
    @Query("SELECT cs FROM ChatSession cs LEFT JOIN FETCH cs.messages WHERE cs.sessionId = :sessionId")
    ChatSession findBySessionIdWithMessages(@Param("sessionId") String sessionId);
}
