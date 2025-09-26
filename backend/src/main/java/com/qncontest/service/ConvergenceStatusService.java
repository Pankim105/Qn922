package com.qncontest.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qncontest.entity.ConvergenceStatus;
import com.qncontest.repository.ConvergenceStatusRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 收敛状态服务
 * 负责管理故事收敛进度和状态
 */
@Service
public class ConvergenceStatusService {
    
    private static final Logger logger = LoggerFactory.getLogger(ConvergenceStatusService.class);
    
    @Autowired
    private ConvergenceStatusRepository convergenceStatusRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * 获取或创建收敛状态
     */
    @Transactional
    public ConvergenceStatus getOrCreateConvergenceStatus(String sessionId) {
        Optional<ConvergenceStatus> existing = convergenceStatusRepository.findBySessionId(sessionId);
        if (existing.isPresent()) {
            return existing.get();
        }
        
        // 创建新的收敛状态
        ConvergenceStatus newStatus = new ConvergenceStatus(sessionId);
        return convergenceStatusRepository.save(newStatus);
    }
    
    /**
     * 更新收敛进度
     */
    @Transactional
    public void updateProgress(String sessionId, double progress) {
        try {
            ConvergenceStatus status = getOrCreateConvergenceStatus(sessionId);
            status.updateProgress(progress);
            convergenceStatusRepository.save(status);
            
            logger.info("更新收敛进度: sessionId={}, progress={}", sessionId, progress);
        } catch (Exception e) {
            logger.error("更新收敛进度失败: sessionId={}, progress={}", sessionId, progress, e);
        }
    }
    
    /**
     * 增加收敛进度
     */
    @Transactional
    public void addProgress(String sessionId, double increment) {
        try {
            ConvergenceStatus status = getOrCreateConvergenceStatus(sessionId);
            status.addProgress(increment);
            convergenceStatusRepository.save(status);
            
            logger.info("增加收敛进度: sessionId={}, increment={}, newProgress={}", 
                       sessionId, increment, status.getProgress());
        } catch (Exception e) {
            logger.error("增加收敛进度失败: sessionId={}, increment={}", sessionId, increment, e);
        }
    }
    
    /**
     * 更新最近场景信息
     */
    @Transactional
    public void updateNearestScenario(String sessionId, String scenarioId, String scenarioTitle, double distance) {
        try {
            ConvergenceStatus status = getOrCreateConvergenceStatus(sessionId);
            status.setNearestScenarioId(scenarioId);
            status.setNearestScenarioTitle(scenarioTitle);
            status.setDistanceToNearest(distance);
            status.setLastUpdated(java.time.LocalDateTime.now());
            
            convergenceStatusRepository.save(status);
            
            logger.info("更新最近场景: sessionId={}, scenarioId={}, distance={}", 
                       sessionId, scenarioId, distance);
        } catch (Exception e) {
            logger.error("更新最近场景失败: sessionId={}, scenarioId={}", sessionId, scenarioId, e);
        }
    }
    
    /**
     * 更新场景进度
     */
    @Transactional
    public void updateScenarioProgress(String sessionId, Map<String, Double> scenarioProgress) {
        try {
            ConvergenceStatus status = getOrCreateConvergenceStatus(sessionId);
            String progressJson = objectMapper.writeValueAsString(scenarioProgress);
            status.setScenarioProgress(progressJson);
            status.setLastUpdated(java.time.LocalDateTime.now());
            
            convergenceStatusRepository.save(status);
            
            logger.info("更新场景进度: sessionId={}, progressCount={}", 
                       sessionId, scenarioProgress.size());
        } catch (Exception e) {
            logger.error("更新场景进度失败: sessionId={}", sessionId, e);
        }
    }
    
    /**
     * 更新活跃提示
     */
    @Transactional
    public void updateActiveHints(String sessionId, List<String> activeHints) {
        try {
            ConvergenceStatus status = getOrCreateConvergenceStatus(sessionId);
            String hintsJson = objectMapper.writeValueAsString(activeHints);
            status.setActiveHints(hintsJson);
            status.setLastUpdated(java.time.LocalDateTime.now());
            
            convergenceStatusRepository.save(status);
            
            logger.info("更新活跃提示: sessionId={}, hintsCount={}", 
                       sessionId, activeHints.size());
        } catch (Exception e) {
            logger.error("更新活跃提示失败: sessionId={}", sessionId, e);
        }
    }
    
    /**
     * 获取收敛状态
     */
    @Transactional(readOnly = true)
    public Optional<ConvergenceStatus> getConvergenceStatus(String sessionId) {
        return convergenceStatusRepository.findBySessionId(sessionId);
    }
    
    /**
     * 获取收敛状态摘要（用于提示词）
     */
    @Transactional(readOnly = true)
    public String getConvergenceStatusSummary(String sessionId) {
        Optional<ConvergenceStatus> statusOpt = getConvergenceStatus(sessionId);
        if (statusOpt.isEmpty()) {
            return "收敛进度: 0% (刚开始)";
        }
        
        ConvergenceStatus status = statusOpt.get();
        StringBuilder summary = new StringBuilder();
        
        summary.append(String.format("收敛进度: %s", status.getProgressPercentage()));
        
        if (status.getNearestScenarioTitle() != null) {
            summary.append(String.format(" | 最近场景: %s", status.getNearestScenarioTitle()));
        }
        
        if (status.getDistanceToNearest() != null) {
            summary.append(String.format(" | 距离: %.2f", status.getDistanceToNearest()));
        }
        
        if (status.isApproachingConvergence()) {
            summary.append(" | 状态: 接近收敛");
        } else if (status.isInConvergencePhase()) {
            summary.append(" | 状态: 收敛阶段");
        } else if (status.isEarlyStage()) {
            summary.append(" | 状态: 早期阶段");
        }
        
        return summary.toString();
    }
    
    /**
     * 获取接近收敛的会话列表
     */
    @Transactional(readOnly = true)
    public List<ConvergenceStatus> getApproachingConvergenceSessions() {
        return convergenceStatusRepository.findApproachingConvergence();
    }
    
    /**
     * 获取处于收敛阶段的会话列表
     */
    @Transactional(readOnly = true)
    public List<ConvergenceStatus> getInConvergencePhaseSessions() {
        return convergenceStatusRepository.findInConvergencePhase();
    }
    
    /**
     * 删除会话的收敛状态
     */
    @Transactional
    public void deleteConvergenceStatus(String sessionId) {
        try {
            convergenceStatusRepository.deleteBySessionId(sessionId);
            logger.info("删除收敛状态: sessionId={}", sessionId);
        } catch (Exception e) {
            logger.error("删除收敛状态失败: sessionId={}", sessionId, e);
        }
    }
}


