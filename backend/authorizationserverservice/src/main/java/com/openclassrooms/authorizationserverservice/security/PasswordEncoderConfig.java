package com.openclassrooms.authorizationserverservice.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * PasswordEncoderConfig
 * @author FirstName LastName
 * @version 1.0
 * @email magassa***REMOVED_USER***@gmail.com
 * @since 2026-05-01
 */

@Configuration
public class PasswordEncoderConfig {
    private static final int STRENGTH = 12;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(STRENGTH);
    }
}