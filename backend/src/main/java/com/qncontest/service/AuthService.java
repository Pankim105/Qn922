package com.qncontest.service;

import com.qncontest.config.AdminConfig;
import com.qncontest.dto.*;
import com.qncontest.entity.RefreshToken;
import com.qncontest.entity.Role;
import com.qncontest.entity.User;
import com.qncontest.repository.UserRepository;
import com.qncontest.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtUtils jwtUtils;
    
    @Autowired
    private RefreshTokenService refreshTokenService;
    
    @Autowired
    private AdminConfig adminConfig;
    
    public AuthResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        User user = (User) authentication.getPrincipal();
        String accessToken = jwtUtils.generateAccessToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);
        
        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name()
        );
        
        return new AuthResponse(
                accessToken,
                refreshToken.getToken(),
                jwtUtils.getAccessTokenExpiration(),
                userInfo
        );
    }
    
    public AuthResponse register(RegisterRequest registerRequest) {
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }
        
        // 检查邮箱是否已存在
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("邮箱已被使用");
        }
        
        // 确定用户角色
        Role userRole = Role.USER;
        if (registerRequest.getAdminKey() != null && !registerRequest.getAdminKey().isEmpty()) {
            if (adminConfig.getRegistrationKey().equals(registerRequest.getAdminKey())) {
                userRole = Role.ADMIN;
            } else {
                throw new RuntimeException("管理员注册密钥不正确");
            }
        }
        
        // 创建新用户
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setRole(userRole);
        
        user = userRepository.save(user);
        
        // 生成token
        String accessToken = jwtUtils.generateAccessToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);
        
        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name()
        );
        
        return new AuthResponse(
                accessToken,
                refreshToken.getToken(),
                jwtUtils.getAccessTokenExpiration(),
                userInfo
        );
    }
    
    public AuthResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        String requestRefreshToken = refreshTokenRequest.getRefreshToken();
        
        RefreshToken refreshToken = refreshTokenService.findByToken(requestRefreshToken)
                .orElseThrow(() -> new RuntimeException("Refresh token不存在"));
        
        refreshToken = refreshTokenService.verifyExpiration(refreshToken);
        
        User user = refreshToken.getUser();
        String newAccessToken = jwtUtils.generateAccessToken(user);
        
        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name()
        );
        
        return new AuthResponse(
                newAccessToken,
                refreshToken.getToken(),
                jwtUtils.getAccessTokenExpiration(),
                userInfo
        );
    }
    
    public void logout(String refreshToken) {
        refreshTokenService.deleteByToken(refreshToken);
    }
}
