package com.qncontest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class ChatResponse {
    
    private String content;
    private LocalDateTime timestamp;
    private boolean isComplete;
    
    // Constructors
    public ChatResponse() {
        this.timestamp = LocalDateTime.now();
    }
    
    public ChatResponse(String content) {
        this();
        this.content = content;
    }
    
    public ChatResponse(String content, boolean isComplete) {
        this(content);
        this.isComplete = isComplete;
    }
    
    // Getters and Setters
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    @JsonProperty("isComplete")
    public boolean isComplete() {
        return isComplete;
    }
    
    public void setComplete(boolean complete) {
        isComplete = complete;
    }
}
