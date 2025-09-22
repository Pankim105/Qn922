package com.qncontest.repository;

import com.qncontest.entity.ChatSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    
    /**
     * 根据会话ID和用户ID查找会话
     */
    Optional<ChatSession> findBySessionIdAndUserId(String sessionId, String userId);
    
    /**
     * 查找用户的所有活跃会话，按更新时间倒序
     */
    List<ChatSession> findByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(String userId);
    
    /**
     * 分页查找用户的会话
     */
    Page<ChatSession> findByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(String userId, Pageable pageable);
    
    /**
     * 查找指定时间之前的非活跃会话（用于清理）
     */
    @Query("SELECT cs FROM ChatSession cs WHERE cs.updatedAt < :cutoffTime AND cs.isActive = true")
    List<ChatSession> findInactiveSessionsBefore(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    /**
     * 统计用户的会话数量
     */
    long countByUserIdAndIsActiveTrue(String userId);
    
    /**
     * 删除用户的所有会话
     */
    void deleteByUserId(String userId);
}
