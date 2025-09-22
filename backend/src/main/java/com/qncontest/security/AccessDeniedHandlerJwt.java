package com.qncontest.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class AccessDeniedHandlerJwt implements AccessDeniedHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(AccessDeniedHandlerJwt.class);
    
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                      AccessDeniedException accessDeniedException) throws IOException, ServletException {
        
        String requestPath = request.getRequestURI();
        String method = request.getMethod();
        
        // 检查是否是响应已提交的情况（SSE流中断后的重试请求）
        if (response.isCommitted()) {
            logger.debug("Response already committed for {} {}, skipping access denied handling", method, requestPath);
            return;
        }
        
        // 检查是否是异步请求
        boolean isAsyncRequest = request.getDispatcherType() == jakarta.servlet.DispatcherType.ASYNC;
        
        // 对于SSE相关的访问拒绝，特殊处理
        if (requestPath != null && requestPath.contains("/chat/stream")) {
            // SSE请求在完成后可能会触发异步调度，这是正常的
            if (isAsyncRequest) {
                logger.debug("SSE async dispatch access denied (normal behavior) for {} {}", method, requestPath);
                return;
            } else {
                // 检查User-Agent来判断是否是浏览器的重连请求
                String userAgent = request.getHeader("User-Agent");
                boolean isBrowserRequest = userAgent != null && (userAgent.contains("Mozilla") || userAgent.contains("Chrome") || userAgent.contains("Safari"));
                
                if (isBrowserRequest) {
                    logger.debug("SSE browser reconnect attempt denied (expected) for {} {}", method, requestPath);
                } else {
                    logger.debug("SSE access denied for {} {}: {}", method, requestPath, accessDeniedException.getMessage());
                }
                
                // 对于SSE请求，如果不是异步调度，返回适当的错误
                if (!response.isCommitted()) {
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    
                    final Map<String, Object> body = new HashMap<>();
                    body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
                    body.put("error", "Unauthorized");
                    body.put("message", "Authentication required for SSE connection");
                    body.put("path", request.getServletPath());
                    
                    final ObjectMapper mapper = new ObjectMapper();
                    mapper.writeValue(response.getOutputStream(), body);
                }
                return;
            }
        }
        
        // 非SSE请求的常规处理
        logger.warn("Access denied for {} {}: {}", method, requestPath, accessDeniedException.getMessage());
        
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        
        final Map<String, Object> body = new HashMap<>();
        body.put("status", HttpServletResponse.SC_FORBIDDEN);
        body.put("error", "Forbidden");
        body.put("message", "Access denied");
        body.put("path", request.getServletPath());
        
        final ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(response.getOutputStream(), body);
    }
}
