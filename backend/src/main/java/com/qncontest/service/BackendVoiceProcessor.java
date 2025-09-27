package com.qncontest.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * 后端语音处理服务
 * 负责处理音频文件并转换为文本
 * 
 * 注意：这是一个示例实现，实际使用时需要集成真正的语音识别服务
 * 如：Google Cloud Speech-to-Text, Azure Speech Services, 百度语音识别等
 */
@Service
public class BackendVoiceProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(BackendVoiceProcessor.class);
    
    // 支持的音频格式
    private static final String[] SUPPORTED_FORMATS = {".wav", ".mp3", ".m4a", ".webm", ".ogg"};
    
    /**
     * 处理音频文件并转换为文本
     * 
     * @param audioFile 音频文件
     * @param language 语言代码 (如: "zh-CN", "en-US")
     * @return 识别出的文本
     */
    public String processAudioToText(MultipartFile audioFile, String language) {
        if (audioFile == null || audioFile.isEmpty()) {
            throw new IllegalArgumentException("音频文件不能为空");
        }
        
        // 验证文件格式
        String fileName = audioFile.getOriginalFilename();
        if (!isSupportedFormat(fileName)) {
            throw new IllegalArgumentException("不支持的音频格式: " + fileName);
        }
        
        try {
            // 保存临时文件
            Path tempFile = saveTemporaryFile(audioFile);
            
            try {
                // 调用语音识别服务
                String recognizedText = performSpeechRecognition(tempFile, language);
                
                logger.info("语音识别完成: 文件={}, 语言={}, 结果='{}'", 
                           fileName, language, recognizedText);
                
                return recognizedText;
                
            } finally {
                // 清理临时文件
                Files.deleteIfExists(tempFile);
            }
            
        } catch (IOException e) {
            logger.error("处理音频文件失败: {}", fileName, e);
            throw new RuntimeException("处理音频文件失败", e);
        }
    }
    
    /**
     * 执行语音识别
     * 这里是示例实现，实际需要调用真正的语音识别API
     */
    private String performSpeechRecognition(Path audioFile, String language) {
        // TODO: 集成真正的语音识别服务
        // 例如：
        // 1. Google Cloud Speech-to-Text
        // 2. Azure Speech Services  
        // 3. 百度语音识别
        // 4. 阿里云语音识别
        
        logger.info("模拟语音识别: 文件={}, 语言={}", audioFile, language);
        
        // 模拟识别结果（实际使用时删除这部分）
        return "这是模拟的语音识别结果，请集成真正的语音识别服务";
    }
    
    /**
     * 保存临时文件
     */
    private Path saveTemporaryFile(MultipartFile audioFile) throws IOException {
        String fileName = UUID.randomUUID().toString() + getFileExtension(audioFile.getOriginalFilename());
        Path tempFile = Files.createTempFile("voice_", fileName);
        
        Files.copy(audioFile.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);
        
        return tempFile;
    }
    
    /**
     * 检查文件格式是否支持
     */
    private boolean isSupportedFormat(String fileName) {
        if (fileName == null) return false;
        
        String extension = getFileExtension(fileName).toLowerCase();
        for (String format : SUPPORTED_FORMATS) {
            if (extension.equals(format)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }
    
    /**
     * 验证音频文件
     */
    public boolean validateAudioFile(MultipartFile audioFile) {
        if (audioFile == null || audioFile.isEmpty()) {
            return false;
        }
        
        // 检查文件大小（限制为10MB）
        if (audioFile.getSize() > 10 * 1024 * 1024) {
            return false;
        }
        
        // 检查文件格式
        return isSupportedFormat(audioFile.getOriginalFilename());
    }
}

