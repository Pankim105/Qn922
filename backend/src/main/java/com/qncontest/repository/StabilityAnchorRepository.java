package com.qncontest.repository;

import com.qncontest.entity.StabilityAnchor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 稳定性锚点数据访问层
 */
@Repository
public interface StabilityAnchorRepository extends JpaRepository<StabilityAnchor, Long> {
    
    /**
     * 获取指定会话的所有锚点
     */
    List<StabilityAnchor> findBySessionIdOrderByPriorityDesc(String sessionId);
    
    /**
     * 获取指定会话和类型的锚点
     */
    List<StabilityAnchor> findBySessionIdAndAnchorType(String sessionId, StabilityAnchor.AnchorType anchorType);
    
    /**
     * 获取指定会话的不可变锚点
     */
    @Query("SELECT a FROM StabilityAnchor a WHERE a.sessionId = :sessionId AND a.isImmutable = true ORDER BY a.priority DESC")
    List<StabilityAnchor> findImmutableBySessionId(@Param("sessionId") String sessionId);
    
    /**
     * 根据会话ID和锚点键查找
     */
    Optional<StabilityAnchor> findBySessionIdAndAnchorKey(String sessionId, String anchorKey);
    
    /**
     * 删除指定会话的所有锚点
     */
    void deleteBySessionId(String sessionId);
    
    /**
     * 检查锚点是否存在
     */
    boolean existsBySessionIdAndAnchorKey(String sessionId, String anchorKey);
}



