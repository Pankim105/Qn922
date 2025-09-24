package com.qncontest.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qncontest.entity.DMAssessment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


@Service
public class AssessmentExtractor {

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
    public DMAssessment extractAssessment(String fullContent) {
        if (fullContent == null || fullContent.isEmpty()) {
            return null;
        }

        try {
            // 查找评估开始标记
            int startIndex = fullContent.indexOf(ASSESSMENT_START_MARKER);
            if (startIndex == -1) {
                logger.debug("未找到评估开始标记");
                return null;
            }

            // 查找评估结束标记（从开始标记后开始查找）
            int endIndex = fullContent.indexOf(ASSESSMENT_END_MARKER, startIndex + 1);
            if (endIndex == -1) {
                logger.warn("找到评估开始标记但未找到结束标记");
                return null;
            }

            // 提取评估内容
            String assessmentContent = fullContent.substring(
                startIndex + ASSESSMENT_START_MARKER.length(), 
                endIndex
            ).trim();

            if (assessmentContent.isEmpty()) {
                logger.warn("评估内容为空");
                return null;
            }

            if (assessmentContent.length() > MAX_ASSESSMENT_SIZE) {
                logger.warn("评估内容过大，长度: {}", assessmentContent.length());
                return null;
            }

            logger.debug("提取到评估内容，长度: {}", assessmentContent.length());
            logger.debug("评估内容: {}", assessmentContent);

            // 解析评估JSON
            return parseAssessmentJson(assessmentContent);

        } catch (Exception e) {
            logger.error("提取评估内容失败", e);
            return null;
        }
    }

    /**
     * 解析评估JSON内容
     */
    private DMAssessment parseAssessmentJson(String assessmentContent) {
        try {
            // 清理JSON内容，移除可能的注释或多余字符
            String cleanedJson = cleanJsonContent(assessmentContent);
            
            logger.debug("清理后的评估JSON: {}", cleanedJson);
            
            // 解析为DMAssessment对象
            DMAssessment assessment = objectMapper.readValue(cleanedJson, DMAssessment.class);
            
            logger.info("成功解析评估结果: strategy={}, score={}", 
                       assessment.getStrategy(), assessment.getOverallScore());
            
            return assessment;
            
        } catch (JsonProcessingException e) {
            logger.error("解析评估JSON失败: {}", assessmentContent, e);
            return null;
        }
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
        String[] duplicateFields = {"worldStateUpdates", "skillsStateUpdates", "questUpdates"};

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
}