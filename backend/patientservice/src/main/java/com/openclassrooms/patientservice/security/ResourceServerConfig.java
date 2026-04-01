package com.openclassrooms.patientservice.security;

import com.openclassrooms.patientservice.handler.CustomAccessDeniedHandler;
import com.openclassrooms.patientservice.handler.CustomAuthenticationEntryPoint;
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
 * Configuration OAuth2 Resource Server.
 * Ce service valide les JWT émis par Authorization Server.
 * La validation se fait via l'issuer-uri configuré dans application.yml.
 * Flow:
 * Requête arrive avec Bearer token
 * Spring récupère les clés publiques depuis AuthServer (/oauth2/jwks)
 * Valide la signature du JWT
 * JwtConverter extrait les authorities
 * @PreAuthorize vérifie les permissions
 *
 * @author Kardigué MAGASSA
 * @version 2.0
 * @since 2026-01-09
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class ResourceServerConfig {

    private static final String[] PUBLIC_ENDPOINTS = {"/actuator/**"};

    private static final String[] SWAGGER_ENDPOINTS = {
            "/v3/api-docs/**",
            "/api/patients/v3/api-docs/**",
            "/v3/api-docs.yaml",
            "/v3/api-docs/swagger-config",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/webjars/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        log.info("Configuring OAuth2 Resource Server Security for PatientService");

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
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