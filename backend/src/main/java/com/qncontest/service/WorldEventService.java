package com.qncontest.service;

import com.qncontest.entity.WorldEvent;
import com.qncontest.repository.WorldEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 世界事件服务
 */
@Service
public class WorldEventService {
    
    private static final Logger logger = LoggerFactory.getLogger(WorldEventService.class);
    
    @Autowired
    private WorldEventRepository worldEventRepository;
    
    /**
     * 获取指定会话的最新N条事件
     */
    public List<WorldEvent> getLatestEvents(String sessionId, int limit) {
        try {
            Pageable pageable = PageRequest.of(0, limit);
            return worldEventRepository.findBySessionIdOrderBySequenceDesc(sessionId, pageable).getContent();
        } catch (Exception e) {
            logger.error("获取最新事件失败: sessionId={}, limit={}", sessionId, limit, e);
            return List.of();
        }
    }
    
    /**
     * 获取指定会话的所有事件（按序号排序）
     */
    public List<WorldEvent> getAllEvents(String sessionId) {
        try {
            return worldEventRepository.findBySessionIdOrderBySequenceAsc(sessionId);
        } catch (Exception e) {
            logger.error("获取所有事件失败: sessionId={}", sessionId, e);
            return List.of();
        }
    }
    
    /**
     * 获取指定会话和事件类型的事件
     */
    public List<WorldEvent> getEventsByType(String sessionId, WorldEvent.EventType eventType) {
        try {
            return worldEventRepository.findBySessionIdAndEventTypeOrderBySequenceAsc(sessionId, eventType);
        } catch (Exception e) {
            logger.error("获取指定类型事件失败: sessionId={}, eventType={}", sessionId, eventType, e);
            return List.of();
        }
    }
    
    /**
     * 获取指定会话的事件数量
     */
    public long getEventCount(String sessionId) {
        try {
            return worldEventRepository.countBySessionId(sessionId);
        } catch (Exception e) {
            logger.error("获取事件数量失败: sessionId={}", sessionId, e);
            return 0;
        }
    }
    
    /**
     * 获取指定会话的最新事件序号
     */
    public Integer getMaxSequence(String sessionId) {
        try {
            return worldEventRepository.findMaxSequenceBySessionId(sessionId).orElse(0);
        } catch (Exception e) {
            logger.error("获取最新事件序号失败: sessionId={}", sessionId, e);
            return 0;
        }
    }
}
