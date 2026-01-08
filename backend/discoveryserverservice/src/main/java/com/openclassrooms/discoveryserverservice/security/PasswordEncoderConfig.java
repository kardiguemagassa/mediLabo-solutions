package com.openclassrooms.discoveryserverservice.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * OAuth2 Authorization Server configuration
 * @author Kardigué MAGASSA
 * @version 1.0
 * @email magassakara@gmail.com
 * @since 2026-05-01
 */

@Configuration
public class PasswordEncoderConfig {
    public static final int STRENGTH = 12;

    @Bean
    public BCryptPasswordEncoder encoder() {
        return new BCryptPasswordEncoder(STRENGTH);
    }
}