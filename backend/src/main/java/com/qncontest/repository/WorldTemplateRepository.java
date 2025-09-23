package com.qncontest.repository;

import com.qncontest.entity.WorldTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 世界模板数据访问层
 */
@Repository
public interface WorldTemplateRepository extends JpaRepository<WorldTemplate, String> {
    
    /**
     * 根据世界ID查找模板
     */
    Optional<WorldTemplate> findByWorldId(String worldId);
    
    /**
     * 获取所有可用的世界模板（用于前端选择）
     */
    @Query("SELECT w FROM WorldTemplate w ORDER BY w.worldId")
    List<WorldTemplate> findAllOrderByWorldId();
    
    /**
     * 检查世界ID是否存在
     */
    boolean existsByWorldId(String worldId);
}



