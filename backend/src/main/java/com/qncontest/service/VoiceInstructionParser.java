package com.qncontest.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


/**
 * 语音指令解析服务
 * 负责将语音识别结果进行标准化处理，确保与现有文本处理流程兼容
 */
@Service
public class VoiceInstructionParser {
    
    private static final Logger logger = LoggerFactory.getLogger(VoiceInstructionParser.class);
    
    /**
     * 解析语音指令为标准化文本
     * 主要进行基本的文本清理和标准化，让大模型更好地理解
     */
    public String parseVoiceInstruction(String voiceText, String worldType, String sessionId) {
        if (voiceText == null || voiceText.trim().isEmpty()) {
            return voiceText;
        }
        
        logger.info("解析语音指令: 原始文本='{}', 世界类型='{}', 会话ID='{}'", 
                   voiceText, worldType, sessionId);
        
        String processedText = voiceText.trim();
        
        // 基本文本清理
        processedText = cleanVoiceText(processedText);
        
        // 根据世界类型进行轻微调整（可选）
        processedText = adjustForWorldType(processedText, worldType);
        
        logger.info("语音指令解析完成: 原始='{}', 处理后='{}'", voiceText, processedText);
        
        return processedText;
    }
    
    /**
     * 清理语音识别文本
     */
    private String cleanVoiceText(String text) {
        if (text == null) return text;
        
        // 移除多余的标点符号
        text = text.replaceAll("[。，！？；：]{2,}", "。");
        
        // 标准化常见的语音识别错误
        text = text.replaceAll("\\s+", " "); // 多个空格合并为一个
        
        // 移除开头和结尾的空白字符
        text = text.trim();
        
        return text;
    }
    
    /**
     * 根据世界类型进行轻微调整
     * 这里只做最基本的调整，让大模型更好地理解上下文
     */
    private String adjustForWorldType(String text, String worldType) {
        if (text == null || worldType == null) return text;
        
        // 根据世界类型添加轻微的上下文提示（可选）
        switch (worldType) {
            case "fantasy_adventure":
                // 奇幻冒险世界 - 不需要特殊处理，大模型已经理解
                break;
            case "martial_arts":
                // 武侠世界 - 不需要特殊处理
                break;
            case "western_magic":
                // 西方魔法世界 - 不需要特殊处理
                break;
            case "japanese_school":
                // 日本校园世界 - 不需要特殊处理
                break;
            case "educational":
                // 教育世界 - 不需要特殊处理
                break;
            default:
                // 其他世界类型
                break;
        }
        
        return text;
    }
    
    /**
     * 验证语音指令是否有效
     */
    public boolean isValidVoiceInstruction(String voiceText) {
        if (voiceText == null || voiceText.trim().isEmpty()) {
            return false;
        }
        
        // 检查是否包含有效内容（至少2个字符）
        String cleanText = voiceText.trim();
        if (cleanText.length() < 2) {
            return false;
        }
        
        // 检查是否只包含标点符号
        if (cleanText.matches("[\\p{Punct}\\s]+")) {
            return false;
        }
        
        return true;
    }
}
