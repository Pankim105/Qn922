package com.qncontest.repository;

import com.qncontest.entity.WorldEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 世界事件数据访问层
 */
@Repository
public interface WorldEventRepository extends JpaRepository<WorldEvent, Long> {
    
    /**
     * 获取指定会话的所有事件（按序号排序）
     */
    List<WorldEvent> findBySessionIdOrderBySequenceAsc(String sessionId);
    
    /**
     * 获取指定会话到指定序号的所有事件
     */
    @Query("SELECT e FROM WorldEvent e WHERE e.sessionId = :sessionId AND e.sequence <= :maxSequence ORDER BY e.sequence ASC")
    List<WorldEvent> findBySessionIdUpToSequence(@Param("sessionId") String sessionId, @Param("maxSequence") Integer maxSequence);
    
    /**
     * 获取指定会话和事件类型的事件
     */
    List<WorldEvent> findBySessionIdAndEventTypeOrderBySequenceAsc(String sessionId, WorldEvent.EventType eventType);
    
    /**
     * 获取指定会话的事件（分页）
     */
    @Query("SELECT e FROM WorldEvent e WHERE e.sessionId = :sessionId ORDER BY e.sequence DESC")
    Page<WorldEvent> findBySessionIdOrderBySequenceDesc(@Param("sessionId") String sessionId, Pageable pageable);
    
    /**
     * 获取指定会话的最新事件序号
     */
    @Query("SELECT MAX(e.sequence) FROM WorldEvent e WHERE e.sessionId = :sessionId")
    Optional<Integer> findMaxSequenceBySessionId(@Param("sessionId") String sessionId);
    
    /**
     * 删除指定会话的所有事件
     */
    void deleteBySessionId(String sessionId);
    
    /**
     * 获取指定会话的事件数量
     */
    long countBySessionId(String sessionId);

    /**
     * 获取指定会话的最新事件（按序号降序）
     */
    Optional<WorldEvent> findTopBySessionIdOrderBySequenceDesc(String sessionId);

    /**
     * 获取指定会话和事件类型的事件（按时间降序）
     */
    List<WorldEvent> findBySessionIdAndEventTypeOrderByTimestampDesc(String sessionId, WorldEvent.EventType eventType);
    
    /**
     * 获取指定会话的所有事件（按时间降序）
     */
    List<WorldEvent> findBySessionIdOrderByTimestampDesc(String sessionId);
}



