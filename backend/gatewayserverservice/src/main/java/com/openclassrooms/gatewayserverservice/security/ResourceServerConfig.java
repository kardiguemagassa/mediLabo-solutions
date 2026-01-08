package com.openclassrooms.gatewayserverservice.security;

import com.openclassrooms.gatewayserverservice.handler.GatewayAccessDeniedHandler;
import com.openclassrooms.gatewayserverservice.handler.GatewayAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
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
 * @author Kardigué MAGASSA
 * @version 1.0
 * @email magassakara@gmail.com
 * @since 2026-05-01
 */

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ResourceServerConfig {

    @Value("${jwks.uri}")
    private String jwkSetUri;

    private final GatewayAccessDeniedHandler accessDeniedHandler;
    private final GatewayAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtConverter jwtConverter;

    /**
     * Configuration de la chaîne de filtres de sécurité REACTIVE.
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        log.info("Configuration de la sécurité du Gateway (Reactive)");

        http
                // Désactiver CSRF (API stateless)
                .csrf(ServerHttpSecurity.CsrfSpec::disable)

                // Configuration CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Règles d'autorisation
                .authorizeExchange(exchanges -> exchanges
                        // Endpoints publics
                        .pathMatchers(
                                "/eureka/**",
                                "/actuator/health",
                                "/actuator/info",
                                "/auth/**",
                                "/user/register/**",
                                "/user/verify/account/**",
                                "/user/verify/password/**",
                                "/user/resetpassword/**",
                                "/.well-known/**"
                        ).permitAll()

                        // Tous les autres endpoints nécessitent une authentification
                        .anyExchange().authenticated()
                )

                // Configuration OAuth2 Resource Server (JWT)
                .oauth2ResourceServer(oauth2 -> oauth2
                        .accessDeniedHandler(accessDeniedHandler)
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .jwt(jwt -> jwt
                                .jwkSetUri(jwkSetUri)
                                .jwtAuthenticationConverter(jwtConverter)
                        )
                );

        log.info("Sécurité Gateway configurée avec JWKS URI: {}", jwkSetUri);
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
                "http://localhost:4200"
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
