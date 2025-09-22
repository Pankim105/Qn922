package com.qncontest.dto;

import jakarta.validation.constraints.NotBlank;

public class RefreshTokenRequest {
    
    @NotBlank(message = "刷新令牌不能为空")
    private String refreshToken;
    
    // Constructors
    public RefreshTokenRequest() {}
    
    public RefreshTokenRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }
    
    // Getters and Setters
    public String getRefreshToken() {
        return refreshToken;
    }
    
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
