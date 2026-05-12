package com.sacmauquan.qrordering.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * AuthConfig - Configure authentication and authorization components.
 */
@Configuration
public class AuthConfig {

    /**
     * PasswordEncoder bean for secure password hashing and verification.
     * Uses BCryptPasswordEncoder with a work factor of 10.
     * 
     * @return BCryptPasswordEncoder instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
