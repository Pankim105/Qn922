package com.qncontest.service.stream;

import com.alibaba.dashscope.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * API错误处理器 - 用于分析和处理DashScope API错误
 */
@Component
public class ApiErrorHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiErrorHandler.class);
    
    /**
     * 分析DashScope API错误
     */
    public void analyzeApiError(ApiException error) {
        logger.error("=== DashScope API 错误分析 ===");
        logger.error("错误类型: {}", error.getClass().getSimpleName());
        logger.error("错误消息: {}", error.getMessage());
        
        // 尝试解析错误详情
        String errorMessage = error.getMessage();
        if (errorMessage != null) {
            if (errorMessage.contains("response_error")) {
                logger.error("错误类型: 响应格式错误");
                logger.error("可能原因: API返回了非JSON格式的响应");
                logger.error("建议: 检查API请求参数和网络连接");
            } else if (errorMessage.contains("timeout")) {
                logger.error("错误类型: 请求超时");
                logger.error("建议: 检查网络连接或增加超时时间");
            } else if (errorMessage.contains("unauthorized")) {
                logger.error("错误类型: 认证失败");
                logger.error("建议: 检查API密钥是否正确");
            } else if (errorMessage.contains("quota")) {
                logger.error("错误类型: 配额超限");
                logger.error("建议: 检查API调用配额");
            } else {
                logger.error("错误类型: 未知错误");
                logger.error("建议: 查看完整错误堆栈");
            }
        }
        
        // 打印完整错误堆栈
        logger.error("完整错误堆栈:", error);
        logger.error("=== 错误分析结束 ===");
    }
    
    /**
     * 检查是否是网络相关错误
     */
    public boolean isNetworkError(Throwable error) {
        if (error instanceof ApiException) {
            String message = error.getMessage();
            return message != null && (
                message.contains("timeout") ||
                message.contains("connection") ||
                message.contains("network") ||
                message.contains("response_error")
            );
        }
        return false;
    }
    
    /**
     * 检查是否是认证相关错误
     */
    public boolean isAuthError(Throwable error) {
        if (error instanceof ApiException) {
            String message = error.getMessage();
            return message != null && (
                message.contains("unauthorized") ||
                message.contains("forbidden") ||
                message.contains("invalid") ||
                message.contains("api-key")
            );
        }
        return false;
    }
    
    /**
     * 获取用户友好的错误消息
     */
    public String getUserFriendlyMessage(Throwable error) {
        if (error instanceof ApiException) {
            String message = error.getMessage();
            if (message != null) {
                if (message.contains("response_error")) {
                    return "AI服务响应异常，系统正在尝试恢复，请稍后重试";
                } else if (message.contains("timeout")) {
                    return "请求超时，请检查网络连接后重试";
                } else if (message.contains("unauthorized")) {
                    return "服务认证失败，请联系管理员";
                } else if (message.contains("quota")) {
                    return "服务配额已用完，请稍后重试";
                } else if (message.contains("rate_limit")) {
                    return "请求频率过高，请稍后重试";
                } else if (message.contains("server_error")) {
                    return "服务器内部错误，请稍后重试";
                }
            }
        }
        return "AI服务出现异常，请稍后重试";
    }
    
    /**
     * 检查是否应该重试
     */
    public boolean shouldRetry(Throwable error, int currentRetryCount) {
        if (currentRetryCount >= 3) {
            return false; // 最多重试3次
        }
        
        if (error instanceof ApiException) {
            String message = error.getMessage();
            if (message != null) {
                // 这些错误类型可以重试
                return message.contains("response_error") || 
                       message.contains("timeout") || 
                       message.contains("server_error") ||
                       message.contains("rate_limit");
            }
        }
        return false;
    }
    
    /**
     * 计算重试延迟时间（指数退避）
     */
    public long calculateRetryDelay(int retryCount) {
        // 指数退避：1秒, 2秒, 4秒
        return Math.min(1000L * (1L << retryCount), 10000L); // 最大10秒
    }
}
