package com.qncontest.repository;

import com.qncontest.entity.ConvergenceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 收敛状态数据访问层
 */
@Repository
public interface ConvergenceStatusRepository extends JpaRepository<ConvergenceStatus, Long> {
    
    /**
     * 根据会话ID查找收敛状态
     */
    Optional<ConvergenceStatus> findBySessionId(String sessionId);
    
    /**
     * 检查会话是否存在收敛状态记录
     */
    boolean existsBySessionId(String sessionId);
    
    /**
     * 根据会话ID删除收敛状态
     */
    void deleteBySessionId(String sessionId);
    
    /**
     * 查找进度大于指定值的收敛状态
     */
    @Query("SELECT cs FROM ConvergenceStatus cs WHERE cs.progress >= :threshold ORDER BY cs.progress DESC")
    java.util.List<ConvergenceStatus> findByProgressGreaterThanEqual(@Param("threshold") Double threshold);
    
    /**
     * 查找接近收敛的会话（进度 > 0.7）
     */
    @Query("SELECT cs FROM ConvergenceStatus cs WHERE cs.progress > 0.7 ORDER BY cs.progress DESC")
    java.util.List<ConvergenceStatus> findApproachingConvergence();
    
    /**
     * 查找处于收敛阶段的会话（进度 > 0.5）
     */
    @Query("SELECT cs FROM ConvergenceStatus cs WHERE cs.progress > 0.5 ORDER BY cs.progress DESC")
    java.util.List<ConvergenceStatus> findInConvergencePhase();
}


