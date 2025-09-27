package com.qncontest.service;

import com.qncontest.dto.WorldTemplateResponse;
import com.qncontest.entity.WorldTemplate;
import com.qncontest.repository.WorldTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 世界模板服务
 */
@Service
public class WorldTemplateService {
    
    private static final Logger logger = LoggerFactory.getLogger(WorldTemplateService.class);
    
    @Autowired
    private WorldTemplateRepository worldTemplateRepository;
    
    /**
     * 获取所有可用的世界模板
     */
    public List<WorldTemplateResponse> getAllWorldTemplates() {
        logger.debug("获取所有世界模板");
        List<WorldTemplate> templates = worldTemplateRepository.findAllOrderByWorldId();
        return templates.stream()
                .map(this::createWorldTemplateResponseWithCharacterTemplates)
                .collect(Collectors.toList());
    }

    /**
     * 创建包含角色模板的世界模板响应
     */
    private WorldTemplateResponse createWorldTemplateResponseWithCharacterTemplates(WorldTemplate template) {
        WorldTemplateResponse response = new WorldTemplateResponse();
        response.setWorldId(template.getWorldId());
        response.setWorldName(template.getWorldName());
        response.setDescription(template.getDescription());
        response.setCharacterTemplates(template.getCharacterTemplates());
        return response;
    }
    
    /**
     * 根据世界ID获取世界模板详情
     */
    public Optional<WorldTemplateResponse> getWorldTemplate(String worldId) {
        logger.debug("获取世界模板详情: {}", worldId);
        return worldTemplateRepository.findByWorldId(worldId)
                .map(WorldTemplateResponse::new);
    }
    
    /**
     * 检查世界类型是否有效
     */
    public boolean isValidWorldType(String worldType) {
        if (worldType == null || worldType.trim().isEmpty()) {
            return false;
        }
        if ("general".equals(worldType)) {
            return true; // 通用聊天模式
        }
        return worldTemplateRepository.existsByWorldId(worldType);
    }
    
    /**
     * 获取世界的默认规则
     */
    public String getDefaultRules(String worldType) {
        if ("general".equals(worldType)) {
            return "{}"; // 通用模式无特殊规则
        }
        
        return worldTemplateRepository.findByWorldId(worldType)
                .map(WorldTemplate::getDefaultRules)
                .orElse("{}");
    }
    
    /**
     * 获取世界的系统提示词模板
     */
    public String getSystemPromptTemplate(String worldType) {
        if ("general".equals(worldType)) {
            return "你是一个智能助手，可以帮助用户解答各种问题。请用友好、专业的语气回答。";
        }
        
        return worldTemplateRepository.findByWorldId(worldType)
                .map(WorldTemplate::getSystemPromptTemplate)
                .orElse("你是一个智能助手，可以帮助用户解答各种问题。请用友好、专业的语气回答。");
    }
    
    /**
     * 获取世界的稳定性锚点模板
     */
    public String getStabilityAnchors(String worldType) {
        if ("general".equals(worldType)) {
            return "{}";
        }
        
        return worldTemplateRepository.findByWorldId(worldType)
                .map(WorldTemplate::getStabilityAnchors)
                .orElse("{}");
    }
    
    /**
     * 获取世界的任务模板
     */
    public String getQuestTemplates(String worldType) {
        if ("general".equals(worldType)) {
            return "{}";
        }
        
        return worldTemplateRepository.findByWorldId(worldType)
                .map(WorldTemplate::getQuestTemplates)
                .orElse("{}");
    }
    
    /**
     * 获取世界的收敛场景
     */
    public String getConvergenceScenarios(String worldType) {
        if ("general".equals(worldType)) {
            return "{}";
        }
        
        return worldTemplateRepository.findByWorldId(worldType)
                .map(WorldTemplate::getConvergenceScenarios)
                .orElse("{}");
    }
    
    /**
     * 获取世界的DM指令
     */
    public String getDmInstructions(String worldType) {
        if ("general".equals(worldType)) {
            return "你是一个智能助手，可以帮助用户解答各种问题。请用友好、专业的语气回答。";
        }
        
        return worldTemplateRepository.findByWorldId(worldType)
                .map(WorldTemplate::getDmInstructions)
                .orElse("你是一个智能助手，可以帮助用户解答各种问题。请用友好、专业的语气回答。");
    }
    
    /**
     * 获取世界的收敛规则
     */
    public String getConvergenceRules(String worldType) {
        if ("general".equals(worldType)) {
            return "{}";
        }
        
        return worldTemplateRepository.findByWorldId(worldType)
                .map(WorldTemplate::getConvergenceRules)
                .orElse("{}");
    }
}



