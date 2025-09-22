package com.qncontest.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;

import java.io.IOException;

/**
 * SSE异常处理过滤器
 * 处理SSE流中断后可能产生的异常，避免不必要的错误日志
 */
@Component
@Order(1) // 确保在其他过滤器之前执行
public class SseExceptionFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(SseExceptionFilter.class);
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String requestPath = request.getRequestURI();
        boolean isSseRequest = requestPath != null && requestPath.contains("/chat/stream");
        
        try {
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            // 对于SSE相关的异常，进行特殊处理
            if (isSseRequest) {
                handleSseException(request, response, e);
            } else {
                // 非SSE请求，重新抛出异常让其他处理器处理
                throw e;
            }
        }
    }
    
    private void handleSseException(HttpServletRequest request, HttpServletResponse response, Exception e) 
            throws IOException {
        
        String method = request.getMethod();
        String path = request.getRequestURI();
        
        // 检查是否是异步请求相关的异常
        boolean isAsyncError = e instanceof AsyncRequestTimeoutException ||
                              (e.getCause() != null && e.getCause() instanceof AsyncRequestTimeoutException) ||
                              e.getMessage() != null && e.getMessage().contains("async");
        
        if (response.isCommitted()) {
            // 响应已提交，这在SSE中是正常的，只记录DEBUG日志
            if (isAsyncError || e instanceof AccessDeniedException) {
                // 对于异步超时或认证失败，这是预期的行为，使用DEBUG级别
                logger.debug("SSE response already committed for {} {} (expected behavior): {}", 
                            method, path, e.getClass().getSimpleName());
            } else {
                // 其他异常，记录为WARN
                logger.warn("SSE response already committed for {} {}, exception: {}", 
                           method, path, e.getMessage());
            }
            return;
        }
        
        if (e instanceof AccessDeniedException) {
            // SSE访问拒绝异常，可能是连接中断后的异步请求
            logger.debug("SSE access denied for {} {} (possibly after connection closed): {}", 
                        method, path, e.getMessage());
            
            // 只有在响应未提交时才返回401
            if (!response.isCommitted()) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"SSE connection requires authentication\"}");
            }
            
        } else if (isAsyncError) {
            // 异步请求超时，这在SSE中是正常的
            logger.debug("SSE async timeout for {} {} (normal behavior): {}", 
                        method, path, e.getMessage());
            
            // 不需要特殊处理，让请求正常结束
            
        } else {
            // 其他SSE异常
            logger.warn("SSE exception for {} {}: {} - {}", 
                       method, path, e.getClass().getSimpleName(), e.getMessage());
            
            if (!response.isCommitted()) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"SSE Error\",\"message\":\"Stream connection error\"}");
            }
        }
    }
}