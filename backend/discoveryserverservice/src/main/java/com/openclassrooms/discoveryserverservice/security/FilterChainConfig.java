package com.openclassrooms.discoveryserverservice.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static com.openclassrooms.discoveryserverservice.constants.Roles.*;

/**
 * OAuth2 Authorization Server configuration
 * @author Kardigué MAGASSA
 * @version 1.0
 * @email magassakara@gmail.com
 * @since 2026-05-01
 */

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class FilterChainConfig {
    private final DiscoveryUserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.ignoringRequestMatchers("/eureka/**"))
                .userDetailsService(userDetailsService)
                .exceptionHandling(exception -> exception.accessDeniedHandler(new DiscoveryAccessDeniedHandler()))
                .authorizeHttpRequests( authorize -> authorize
                        // Ressources publiques (CSS, JS, images)
                        .requestMatchers("/eureka/fonts/**", "/eureka/css/**", "/eureka/js/**", "/eureka/images/**", "/icon/**").permitAll()
                        // API Eureka pour les services (Basic Auth)
                        .requestMatchers("/eureka/**").hasAnyAuthority(EUREKA_READ)
                        // Dashboard Eureka - réservé aux admins
                        .requestMatchers("/**").hasAnyAuthority(EUREKA_READ,ROLE_ADMIN, ROLE_ORGANIZER).anyRequest()
                        .authenticated())
                .httpBasic(httpBasic -> httpBasic.authenticationEntryPoint(new DiscoveryAuthenticationEntryPoint()));
        return http.build();
    }

//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        log.info("Configuration de la sécurité Eureka (DEV MODE - Sans authentification)");
//        http.csrf(AbstractHttpConfigurer::disable).authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
//        return http.build();
//    }
}