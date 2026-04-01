package com.openclassrooms.gatewayserverservice.security;

import com.openclassrooms.gatewayserverservice.handler.GatewayAccessDeniedHandler;
import com.openclassrooms.gatewayserverservice.handler.GatewayAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

import static org.springframework.http.HttpHeaders.*;
import static org.springframework.http.HttpMethod.*;

/**
 * Configuration de sécurité pour le Gateway - Version REACTIVE.
 * Gère l'authentification OAuth2/JWT et les règles d'autorisation.
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-05-01
 */

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ResourceServerConfig {


    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    private final GatewayAccessDeniedHandler accessDeniedHandler;
    private final GatewayAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtConverter jwtConverter;

    private static final String[] PUBLIC_ENDPOINTS = {
            "/eureka/**",
            "/actuator/**", // Gateway's actuator
            "/api/*/actuator/**",      // All microservices actuators
            "/authorization/**",
            "/user/register/**",
            "/user/verify/account/**",
            "/user/verify/password/**",
            "/user/resetpassword/**",
            "/.well-known/**",
           "/fallback/**" // <--- pour que le Circuit Breaker réponde toujours
    };

    private static final String[] SWAGGER_ENDPOINTS = {
            // UI et Ressources de base
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/webjars/**",
            "/swagger-resources/**",

            // Docs de la Gateway
            "/v3/api-docs/**",
            "/v3/api-docs/swagger-config",

            // Docs des Microservices (Via tes routes manuelles /api/...)
            "/api/*/v3/api-docs/**",
            "/api/*/v3/api-docs/swagger-config",

            // Docs des Microservices (Via les routes automatiques Eureka)
            "/*/v3/api-docs/**",
            "/*/v3/api-docs/swagger-config"
    };


    /**
     * Configuration de la chaîne de filtres de sécurité REACTIVE.
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        log.info("Configuration de la sécurité du Gateway (Reactive)");

        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeExchange(exchanges -> exchanges
                        // 1. ON FORCE L'ACCÈS LIBRE AUX ACTUATORS (Ordre de priorité élevé)
//                        .pathMatchers("/actuator/**").permitAll()
//                        .pathMatchers("/api/notes/actuator/**").permitAll()
//                        .pathMatchers("/api/patients/actuator/**").permitAll()

                        // 2. TES AUTRES ENDPOINTS PUBLICS
                        .pathMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .pathMatchers(SWAGGER_ENDPOINTS).permitAll()

                        // 3. LE RESTE DOIT ÊTRE AUTHENTIFIÉ
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .accessDeniedHandler(accessDeniedHandler)
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .jwt(jwt -> jwt
                                .jwkSetUri(jwkSetUri)
                                .jwtAuthenticationConverter(jwtConverter)
                        )
                );

        return http.build();
    }

    /**
     * Configuration CORS pour WebFlux (REACTIVE).
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        log.info("Configuration CORS pour le Gateway");

        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowCredentials(true);
        corsConfiguration.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:4200",
                "http://localhost:8080"
        ));

        corsConfiguration.setAllowedHeaders(Arrays.asList(
                "File-Name",
                ORIGIN,
                ACCESS_CONTROL_ALLOW_ORIGIN,
                CONTENT_TYPE,
                ACCEPT,
                AUTHORIZATION,
                "X-Requested-With",
                ACCESS_CONTROL_REQUEST_METHOD,
                ACCESS_CONTROL_REQUEST_HEADERS,
                ACCESS_CONTROL_ALLOW_CREDENTIALS
        ));

        corsConfiguration.setExposedHeaders(Arrays.asList(
                "File-Name",
                ORIGIN,
                ACCESS_CONTROL_ALLOW_ORIGIN,
                CONTENT_TYPE,
                ACCEPT,
                AUTHORIZATION,
                "X-Requested-With",
                ACCESS_CONTROL_REQUEST_METHOD,
                ACCESS_CONTROL_REQUEST_HEADERS,
                ACCESS_CONTROL_ALLOW_CREDENTIALS
        ));

        corsConfiguration.setAllowedMethods(Arrays.asList(
                GET.name(),
                POST.name(),
                PUT.name(),
                PATCH.name(),
                DELETE.name(),
                OPTIONS.name()
        ));

        corsConfiguration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);

        return source;
    }
}
