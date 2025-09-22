package com.qncontest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "admin")
public class AdminConfig {
    
    private String registrationKey;
    
    public String getRegistrationKey() {
        return registrationKey;
    }
    
    public void setRegistrationKey(String registrationKey) {
        this.registrationKey = registrationKey;
    }
}
