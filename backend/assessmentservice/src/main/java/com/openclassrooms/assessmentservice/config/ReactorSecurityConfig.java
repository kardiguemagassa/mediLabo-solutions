package com.openclassrooms.assessmentservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.annotation.PostConstruct;

/**
 * Configuration pour propager le SecurityContext aux threads Reactor.
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-24
 */
@Configuration
public class ReactorSecurityConfig {

    @PostConstruct
    public void enableSecurityContextPropagation() {
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
    }
}