package com.qros.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * AuthConfig - Configure authentication and authorization components.
 */
@Configuration
public class AuthConfig {

    @Value("${app.security.bcrypt-strength:10}")
    private int bcryptStrength;

    /**
     * PasswordEncoder bean for secure password hashing and verification.
     * Uses BCryptPasswordEncoder with a configurable work factor.
     *
     * @return BCryptPasswordEncoder instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(bcryptStrength);
    }
}
