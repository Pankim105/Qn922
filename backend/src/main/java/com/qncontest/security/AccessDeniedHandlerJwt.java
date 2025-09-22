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
        
        // 对于SSE相关的访问拒绝，使用DEBUG级别而不是ERROR
        if (requestPath.contains("/chat/stream")) {
            logger.debug("SSE access denied for {} {}: {}", method, requestPath, accessDeniedException.getMessage());
            // 对于SSE流中断后的匿名请求，直接返回，不写响应
            return;
        } else {
            logger.warn("Access denied for {} {}: {}", method, requestPath, accessDeniedException.getMessage());
        }
        
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
