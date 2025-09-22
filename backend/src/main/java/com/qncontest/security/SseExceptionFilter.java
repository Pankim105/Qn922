package com.qncontest.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * SSE专用异常处理过滤器
 * 用于处理SSE连接中的认证异常，确保返回适当的错误响应
 */
@Component
@Order(1) // 确保在JWT过滤器之前执行
public class SseExceptionFilter implements Filter {
    
    private static final Logger logger = LoggerFactory.getLogger(SseExceptionFilter.class);
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        try {
            chain.doFilter(request, response);
        } catch (Exception e) {
            // 检查是否是异步调度的正常情况
            boolean isAsyncDispatch = httpRequest.getDispatcherType() == jakarta.servlet.DispatcherType.ASYNC;
            boolean isSSERequest = isSseRequest(httpRequest);
            
            if (isAsyncDispatch && isSSERequest) {
                // SSE异步调度的正常情况，只记录DEBUG级别日志
                logger.debug("SSE async dispatch completed for URI: {}", httpRequest.getRequestURI());
                return;
            }
            
            logger.error("Exception in filter chain for URI: {}", httpRequest.getRequestURI(), e);
            
            // 检查响应是否已经提交
            if (httpResponse.isCommitted()) {
                logger.warn("Response already committed, cannot send error response for URI: {}", httpRequest.getRequestURI());
                return;
            }
            
            // 如果是SSE请求，返回SSE格式的错误
            if (isSSERequest) {
                handleSseError(httpResponse, e);
            } else {
                handleJsonError(httpResponse, e);
            }
        }
    }
    
    /**
     * 判断是否为SSE请求
     */
    private boolean isSseRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String accept = request.getHeader("Accept");
        
        return uri.contains("/stream") || 
               (accept != null && accept.contains("text/event-stream"));
    }
    
    /**
     * 处理SSE错误
     */
    private void handleSseError(HttpServletResponse response, Exception e) throws IOException {
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        
        // 设置SSE相关的头部
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type");
        
        String errorMessage = "认证失败，请重新登录";
        String sseError = String.format("event: error\ndata: {\"error\":\"%s\"}\n\n", errorMessage);
        
        response.getWriter().write(sseError);
        response.getWriter().flush();
    }
    
    /**
     * 处理JSON错误
     */
    private void handleJsonError(HttpServletResponse response, Exception e) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        
        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("success", false);
        errorBody.put("message", "认证失败，请重新登录");
        errorBody.put("error", e.getMessage());
        
        ObjectMapper mapper = new ObjectMapper();
        response.getWriter().write(mapper.writeValueAsString(errorBody));
        response.getWriter().flush();
    }
}
