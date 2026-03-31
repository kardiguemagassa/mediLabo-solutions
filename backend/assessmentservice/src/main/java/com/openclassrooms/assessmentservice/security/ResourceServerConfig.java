package com.openclassrooms.assessmentservice.security;

import com.openclassrooms.assessmentservice.handler.CustomAccessDeniedHandler;
import com.openclassrooms.assessmentservice.handler.CustomAuthenticationEntryPoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuration OAuth2 Resource Server pour Note Service.
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-09
 */

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class ResourceServerConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
            "/actuator/health",
            "/actuator/health/**",
            "/actuator/info",
            "/actuator/prometheus"
    };
    private static final String[] SWAGGER_ENDPOINTS = {
            "/v3/api-docs/**",
            "/api/assessments/v3/api-docs/**",
            "/v3/api-docs.yaml",
            "/v3/api-docs/swagger-config",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/webjars/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        log.info("Configuring OAuth2 Resource Server Security for NoteService");

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(SWAGGER_ENDPOINTS).permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .accessDeniedHandler(new CustomAccessDeniedHandler())
                        .authenticationEntryPoint(new CustomAuthenticationEntryPoint())
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(new JwtConverter())));

        return http.build();
    }
}