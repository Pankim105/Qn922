package com.qncontest.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qncontest.service.interfaces.AssessmentExtractorInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AssessmentExtractor implements AssessmentExtractorInterface {

    private static final Logger logger = LoggerFactory.getLogger(AssessmentExtractor.class);
    private static final String ASSESSMENT_START_MARKER = "§";
    private static final String ASSESSMENT_END_MARKER = "§";
    private static final int MAX_ASSESSMENT_SIZE = 10000; // 评估JSON最大长度
    private final ObjectMapper objectMapper;

    public AssessmentExtractor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 从完整内容中提取评估结果
     * @param fullContent 完整的AI响应内容
     * @return 提取的评估结果，如果没有找到则返回null
     */
    public Map<String, Object> extractAssessmentEntity(String fullContent) {
        logger.info("🔍 开始提取评估JSON: 内容长度={}", fullContent != null ? fullContent.length() : 0);
        
        if (fullContent == null || fullContent.isEmpty()) {
            logger.warn("⚠️ 输入内容为空，无法提取评估");
            return null;
        }

        try {
            // 查找评估开始标记
            int startIndex = fullContent.indexOf(ASSESSMENT_START_MARKER);
            if (startIndex == -1) {
                logger.info("ℹ️ 未找到评估开始标记 §，跳过评估提取");
                return null;
            }
            logger.info("✅ 找到评估开始标记: 位置={}", startIndex);

            // 查找评估结束标记（从开始标记后开始查找）
            int endIndex = fullContent.indexOf(ASSESSMENT_END_MARKER, startIndex + 1);
            if (endIndex == -1) {
                logger.warn("⚠️ 找到评估开始标记但未找到结束标记");
                return null;
            }
            logger.info("✅ 找到评估结束标记: 位置={}", endIndex);

            // 提取评估内容
            String assessmentContent = fullContent.substring(
                startIndex + ASSESSMENT_START_MARKER.length(), 
                endIndex
            ).trim();

            logger.info("📄 提取到评估内容: 长度={}", assessmentContent.length());
            logger.info("📄 评估内容预览: {}", 
                       assessmentContent.length() > 200 ? 
                       assessmentContent.substring(0, 200) + "..." : 
                       assessmentContent);

            if (assessmentContent.isEmpty()) {
                logger.warn("⚠️ 评估内容为空");
                return null;
            }

            if (assessmentContent.length() > MAX_ASSESSMENT_SIZE) {
                logger.warn("⚠️ 评估内容过大，长度: {} (最大允许: {})", assessmentContent.length(), MAX_ASSESSMENT_SIZE);
                return null;
            }

            // 解析评估JSON
            Map<String, Object> result = parseAssessmentJson(assessmentContent);
            if (result != null) {
                logger.info("✅ 评估JSON提取成功: strategy={}, score={}", 
                           result.get("strategy"), result.get("overallScore"));
            } else {
                logger.warn("⚠️ 评估JSON解析失败");
            }
            return result;

        } catch (Exception e) {
            logger.error("提取评估内容失败", e);
            return null;
        }
    }

    /**
     * 解析评估JSON内容
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseAssessmentJson(String assessmentContent) {
        try {
            logger.info("🔧 开始解析评估JSON: 长度={}", assessmentContent.length());
            
            // 清理JSON内容，移除可能的注释或多余字符
            String cleanedJson = cleanJsonContent(assessmentContent);
            
            logger.info("🧹 JSON清理完成: 原始长度={}, 清理后长度={}", 
                       assessmentContent.length(), cleanedJson.length());
            logger.debug("清理后的评估JSON: {}", cleanedJson);
            
            // 解析为Map对象
            Map<String, Object> assessment = objectMapper.readValue(cleanedJson, Map.class);
            
            logger.info("✅ 成功解析评估结果: strategy={}, score={}, compliance={}, consistency={}, convergence={}", 
                       assessment.get("strategy"), assessment.get("overallScore"),
                       assessment.get("ruleCompliance"), assessment.get("contextConsistency"), 
                       assessment.get("convergenceProgress"));
            
            // 记录各个字段的解析情况
            logAssessmentFields(assessment);
            
            return assessment;
            
        } catch (JsonProcessingException e) {
            logger.error("❌ 解析评估JSON失败: {}", assessmentContent, e);
            return null;
        }
    }
    
    /**
     * 记录评估字段的解析情况
     */
    private void logAssessmentFields(Map<String, Object> assessment) {
        logger.info("📊 评估字段解析情况:");
        logger.info("  - diceRolls: {}", assessment.get("diceRolls") != null ? "有数据" : "无数据");
        logger.info("  - learningChallenges: {}", assessment.get("learningChallenges") != null ? "有数据" : "无数据");
        logger.info("  - stateUpdates: {}", assessment.get("stateUpdates") != null ? "有数据" : "无数据");
        logger.info("  - memoryUpdates: {}", assessment.get("memoryUpdates") != null ? "有数据" : "无数据");
        logger.info("  - questUpdates: {}", assessment.get("questUpdates") != null ? "有数据" : "无数据");
        logger.info("  - worldStateUpdates: {}", assessment.get("worldStateUpdates") != null ? "有数据" : "无数据");
        logger.info("  - arcUpdates: {}", assessment.get("arcUpdates") != null ? "有数据" : "无数据");
        logger.info("  - convergenceStatusUpdates: {}", assessment.get("convergenceStatusUpdates") != null ? "有数据" : "无数据");
    }

    /**
     * 清理JSON内容，移除可能的注释和多余字符
     */
    private String cleanJsonContent(String content) {
        // 移除可能的注释标记
        content = content.replaceAll("/\\*.*?\\*/", "").trim();

        // 移除可能的单行注释
        content = content.replaceAll("//.*$", "").trim();

        // 移除重复的字段（修复AI响应中的重复字段问题）
        content = removeDuplicateFields(content);

        // 确保内容以{开始，以}结束
        int startBrace = content.indexOf('{');
        int endBrace = content.lastIndexOf('}');

        if (startBrace != -1 && endBrace != -1 && endBrace > startBrace) {
            content = content.substring(startBrace, endBrace + 1);
        }

        return content.trim();
    }

    /**
     * 移除JSON中的重复字段
     */
    private String removeDuplicateFields(String json) {
        // 简单的重复字段移除，针对常见的重复字段
        String[] duplicateFields = {"worldStateUpdates", "questUpdates", "memoryUpdates"};

        for (String field : duplicateFields) {
            String pattern = String.format(", \"%s\": \\{[^}]*\\}, \"%s\": \\{[^}]*\\}", field, field);
            json = json.replaceAll(pattern, "");

            // 如果是第一个字段的重复
            pattern = String.format("\"%s\": \\{[^}]*\\}, \"%s\": \\{[^}]*\\}", field, field);
            json = json.replaceAll(pattern, "");
        }

        return json;
    }

    /**
     * 检查内容是否包含评估标记
     */
    public boolean containsAssessment(String content) {
        return content != null && 
               content.contains(ASSESSMENT_START_MARKER) && 
               content.contains(ASSESSMENT_END_MARKER);
    }

    /**
     * 移除内容中的评估部分，返回纯用户内容
     */
    public String removeAssessment(String fullContent) {
        if (fullContent == null || fullContent.isEmpty()) {
            return fullContent;
        }

        try {
            int startIndex = fullContent.indexOf(ASSESSMENT_START_MARKER);
            if (startIndex == -1) {
                return fullContent; // 没有评估内容
            }

            int endIndex = fullContent.indexOf(ASSESSMENT_END_MARKER, startIndex);
            if (endIndex == -1) {
                return fullContent; // 没有完整的评估内容
            }

            // 移除评估部分
            String beforeAssessment = fullContent.substring(0, startIndex);
            String afterAssessment = fullContent.substring(endIndex + ASSESSMENT_END_MARKER.length());
            
            return (beforeAssessment + afterAssessment).trim();
            
        } catch (Exception e) {
            logger.error("移除评估内容失败", e);
            return fullContent;
        }
    }
    
    // ==================== 接口实现 ====================
    
    /**
     * 从AI回复中提取评估信息
     */
    @Override
    public Map<String, Object> extractAssessment(String aiResponse) {
        return extractAssessmentEntity(aiResponse);
    }
    
    /**
     * 验证评估格式是否正确
     */
    @Override
    public boolean validateAssessment(Map<String, Object> assessment) {
        if (assessment == null) {
            return false;
        }
        
        // 检查必需字段
        String[] requiredFields = {
            "ruleCompliance", "contextConsistency", "convergenceProgress", 
            "overallScore", "strategy"
        };
        
        for (String field : requiredFields) {
            if (!assessment.containsKey(field)) {
                logger.warn("评估缺少必需字段: {}", field);
                return false;
            }
        }
        
        // 验证strategy字段值
        Object strategy = assessment.get("strategy");
        if (strategy == null || !strategy.toString().matches("ACCEPT|ADJUST|CORRECT")) {
            logger.warn("无效的strategy值: {}", strategy);
            return false;
        }
        
        // 验证数值字段范围
        try {
            double ruleCompliance = Double.parseDouble(assessment.get("ruleCompliance").toString());
            double contextConsistency = Double.parseDouble(assessment.get("contextConsistency").toString());
            double convergenceProgress = Double.parseDouble(assessment.get("convergenceProgress").toString());
            double overallScore = Double.parseDouble(assessment.get("overallScore").toString());
            
            if (ruleCompliance < 0 || ruleCompliance > 1 ||
                contextConsistency < 0 || contextConsistency > 1 ||
                convergenceProgress < 0 || convergenceProgress > 1 ||
                overallScore < 0 || overallScore > 1) {
                logger.warn("评估分数超出有效范围 [0,1]");
                return false;
            }
            
        } catch (NumberFormatException e) {
            logger.warn("评估分数格式无效", e);
            return false;
        }
        
        return true;
    }
}