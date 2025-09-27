package com.qncontest.controller;

import com.qncontest.dto.ChatResponse;
import com.qncontest.entity.User;
import com.qncontest.service.BackendVoiceProcessor;
import com.qncontest.service.UserDetailsServiceImpl;
import com.qncontest.service.VoiceInstructionParser;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/**
 * 语音处理控制器
 * 处理音频文件上传和语音识别
 */
@RestController
@RequestMapping("/voice")
@CrossOrigin(origins = "*", maxAge = 3600)
public class VoiceController {
    
    private static final Logger logger = LoggerFactory.getLogger(VoiceController.class);
    
    @Autowired
    private BackendVoiceProcessor voiceProcessor;
    
    @Autowired
    private VoiceInstructionParser voiceInstructionParser;
    
    @Autowired
    private UserDetailsServiceImpl userDetailsService;
    
    /**
     * 上传音频文件并进行语音识别
     */
    @PostMapping("/recognize")
    public ResponseEntity<ChatResponse> recognizeVoice(
            @RequestParam("audio") MultipartFile audioFile,
            @RequestParam(value = "language", defaultValue = "zh-CN") String language,
            @RequestParam(value = "worldType", required = false) String worldType,
            @RequestParam(value = "sessionId", required = false) String sessionId) {
        
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(401)
                    .body(ChatResponse.error("用户未认证"));
            }
            
            // 验证音频文件
            if (!voiceProcessor.validateAudioFile(audioFile)) {
                return ResponseEntity.badRequest()
                    .body(ChatResponse.error("无效的音频文件"));
            }
            
            // 进行语音识别
            String recognizedText = voiceProcessor.processAudioToText(audioFile, language);
            
            // 如果提供了世界类型和会话ID，进行指令解析
            if (worldType != null && sessionId != null) {
                recognizedText = voiceInstructionParser.parseVoiceInstruction(
                    recognizedText, worldType, sessionId);
            }
            
            return ResponseEntity.ok(ChatResponse.success("语音识别成功", recognizedText));
            
        } catch (Exception e) {
            logger.error("语音识别失败", e);
            return ResponseEntity.status(500)
                .body(ChatResponse.error("语音识别失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取当前用户
     */
    private User getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return null;
            }
            
            String username = authentication.getName();
            return userDetailsService.findUserByUsername(username);
        } catch (Exception e) {
            logger.error("获取当前用户失败", e);
            return null;
        }
    }
}
