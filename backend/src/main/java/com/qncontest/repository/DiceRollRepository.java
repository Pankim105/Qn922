package com.qncontest.repository;

import com.qncontest.entity.DiceRoll;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 骰子记录数据访问层
 */
@Repository
public interface DiceRollRepository extends JpaRepository<DiceRoll, Long> {
    
    /**
     * 获取指定会话的所有骰子记录
     */
    List<DiceRoll> findBySessionIdOrderByCreatedAtDesc(String sessionId);
    
    /**
     * 获取指定会话的骰子记录（分页）
     */
    Page<DiceRoll> findBySessionIdOrderByCreatedAtDesc(String sessionId, Pageable pageable);
    
    /**
     * 获取指定会话的最近N次骰子记录
     */
    @Query("SELECT d FROM DiceRoll d WHERE d.sessionId = :sessionId ORDER BY d.createdAt DESC")
    List<DiceRoll> findRecentBySessionId(@Param("sessionId") String sessionId, Pageable pageable);
    
    /**
     * 获取指定会话的成功检定数量
     */
    long countBySessionIdAndIsSuccessfulTrue(String sessionId);
    
    /**
     * 获取指定会话的失败检定数量
     */
    long countBySessionIdAndIsSuccessfulFalse(String sessionId);
    
    /**
     * 获取指定会话在指定时间范围内的骰子记录
     */
    @Query("SELECT d FROM DiceRoll d WHERE d.sessionId = :sessionId AND d.createdAt BETWEEN :startTime AND :endTime ORDER BY d.createdAt DESC")
    List<DiceRoll> findBySessionIdAndTimeRange(@Param("sessionId") String sessionId, 
                                               @Param("startTime") LocalDateTime startTime, 
                                               @Param("endTime") LocalDateTime endTime);
    
    /**
     * 删除指定会话的所有骰子记录
     */
    void deleteBySessionId(String sessionId);
    
    /**
     * 获取指定会话的骰子记录统计
     */
    @Query("SELECT d.diceType, COUNT(d), AVG(d.result) FROM DiceRoll d WHERE d.sessionId = :sessionId GROUP BY d.diceType")
    List<Object[]> getStatisticsBySessionId(@Param("sessionId") String sessionId);
}



