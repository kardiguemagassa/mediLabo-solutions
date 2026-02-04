package com.openclassrooms.assessmentservice.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-09
 */

@Configuration
public class PasswordEncoderConfig {

    public static final int STRENGTH = 12;

    @Bean
    public BCryptPasswordEncoder encoder () {return new BCryptPasswordEncoder(STRENGTH);}
}
