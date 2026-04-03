package com.openclassrooms.patientservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.annotation.PostConstruct;

@Configuration
public class ReactorSecurityConfig {

    @PostConstruct
    public void enableSecurityContextPropagation() {
        /** Permet à Reactor de propager le SecurityContext */
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
    }
}