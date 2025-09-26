package com.qncontest.service.interfaces;

import java.util.Map;

/**
 * 评估提取器接口
 * 定义AI回复评估提取的标准行为
 */
public interface AssessmentExtractorInterface {
    
    /**
     * 从AI回复中提取评估信息
     * @param aiResponse AI回复内容
     * @return 评估信息映射
     */
    Map<String, Object> extractAssessment(String aiResponse);
    
    /**
     * 验证评估格式是否正确
     * @param assessment 评估信息
     * @return 是否有效
     */
    boolean validateAssessment(Map<String, Object> assessment);
}
