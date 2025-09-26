package com.qncontest.repository;

import com.qncontest.entity.WorldState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 世界状态数据访问层
 */
@Repository
public interface WorldStateRepository extends JpaRepository<WorldState, Long> {
    
    /**
     * 获取指定会话的最新状态
     */
    Optional<WorldState> findTop1BySessionIdOrderByVersionDescCreatedAtDesc(String sessionId);
    
    /**
     * 获取指定会话和版本的状态
     */
    Optional<WorldState> findBySessionIdAndVersion(String sessionId, Integer version);
    
    /**
     * 获取指定会话的状态历史（分页）
     */
    @Query("SELECT w FROM WorldState w WHERE w.sessionId = :sessionId ORDER BY w.version DESC")
    Page<WorldState> findHistoryBySessionId(@Param("sessionId") String sessionId, Pageable pageable);
    
    /**
     * 获取指定会话的所有状态版本号
     */
    @Query("SELECT w.version FROM WorldState w WHERE w.sessionId = :sessionId ORDER BY w.version ASC")
    List<Integer> findVersionsBySessionId(@Param("sessionId") String sessionId);
    
    /**
     * 删除指定会话的所有状态
     */
    void deleteBySessionId(String sessionId);
    
    /**
     * 获取指定会话的最新版本号
     */
    @Query("SELECT MAX(w.version) FROM WorldState w WHERE w.sessionId = :sessionId")
    Optional<Integer> findMaxVersionBySessionId(@Param("sessionId") String sessionId);
}



