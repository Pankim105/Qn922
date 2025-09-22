package com.qncontest.config;

import com.qncontest.entity.Role;
import com.qncontest.entity.User;
import com.qncontest.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Override
    public void run(String... args) throws Exception {
        // 创建默认管理员用户
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@qncontest.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);
            System.out.println("默认管理员用户已创建: admin/admin123");
        }
        
        // 创建测试用户
        if (!userRepository.existsByUsername("testuser")) {
            User testUser = new User();
            testUser.setUsername("testuser");
            testUser.setEmail("test@qncontest.com");
            testUser.setPassword(passwordEncoder.encode("test123"));
            testUser.setRole(Role.USER);
            userRepository.save(testUser);
            System.out.println("测试用户已创建: testuser/test123");
        }
    }
}
