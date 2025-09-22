package com.qncontest.service;

import com.qncontest.entity.RefreshToken;
import com.qncontest.entity.User;
import com.qncontest.repository.RefreshTokenRepository;
import com.qncontest.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {
    
    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenExpiration;
    
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    
    @Autowired
    private JwtUtils jwtUtils;
    
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }
    
    public RefreshToken createRefreshToken(User user) {
        // 删除用户现有的refresh token
        refreshTokenRepository.deleteByUser(user);
        
        // 创建新的refresh token
        String token = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000);
        
        RefreshToken refreshToken = new RefreshToken(token, user, expiryDate);
        return refreshTokenRepository.save(refreshToken);
    }
    
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isExpired()) {
            refreshTokenRepository.deleteByToken(token.getToken());
            throw new RuntimeException("Refresh token已过期，请重新登录");
        }
        return token;
    }
    
    @Transactional
    public void deleteByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }
    
    @Transactional
    public void deleteByToken(String token) {
        refreshTokenRepository.deleteByToken(token);
    }
    
    @Transactional
    public void deleteExpiredTokens() {
        refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
    }
}
