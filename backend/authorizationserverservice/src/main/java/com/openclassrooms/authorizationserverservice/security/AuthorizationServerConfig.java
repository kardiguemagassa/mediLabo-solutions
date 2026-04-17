package com.openclassrooms.authorizationserverservice.security;

import com.openclassrooms.authorizationserverservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.joining;
import static org.springframework.http.HttpHeaders.*;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS;
import static org.springframework.http.HttpMethod.*;
import static org.springframework.http.HttpMethod.OPTIONS;
import static org.springframework.security.oauth2.server.authorization.OAuth2TokenType.ACCESS_TOKEN;

/**
 * Configuration de sécurité pour l'Authorization Server et les endpoints REST.
 * Cette classe configure :
 * Le serveur d'autorisation OAuth2 (génération de tokens, clients, scopes, etc.)
 * La sécurité des formulaires (login, MFA, logout)
 * La sécurité des API REST avec JWT et validation des tokens
 * Les règles CORS et les filtres HTTP personnalisés

 * Deux SecurityFilterChain sont définis avec des ordres différents :
 * Ordre 1 : OAuth2 Authorization Server
 * Ordre 2 : Formulaires MFA + API REST
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-05-01
 */

@EnableMethodSecurity
@Configuration
@Slf4j
@RequiredArgsConstructor
@EnableWebSecurity
public class AuthorizationServerConfig {

    /** Configuration des JWT (clé publique/privée, algorithme, etc.) */
    private final JwtConfiguration configuration;

    /** URI du JWK Set utilisé pour valider les JWT côté Resource Server. */
    @Value("${jwks.uri}")
    private String jwkSetUri;

    @Value("${ui.app.url}")
    private String redirectUri;

    private static final String[] PUBLIC_ENDPOINTS = {
            "/login",
            "/error",
            "/user/register/**",
            "/user/verify/account/**",
            "/user/verify/password/**",
            "/user/resetpassword/**",
            "/.well-known/**",
            "/user/image/**",
            "/actuator/health",
            "/actuator/info",
            "/actuator/prometheus"};

    private static final String[] SWAGGER_ENDPOINTS = {
            "/v3/api-docs/**",
            "/v3/api-docs.yaml",
            "/v3/api-docs/swagger-config",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/webjars/**"
    };


    /**
     * SecurityFilterChain pour le serveur d'autorisation OAuth2.
     * Cette configuration :
     * Active les endpoints OAuth2 (/oauth2/token, /oauth2/authorize, etc.)</li>
     * Configure les clients enregistrés via {@link RegisteredClientRepository}</li>
     * Configure la génération de tokens (JWT + refresh token)</li>
     * Applique une authentification obligatoire sur tous les endpoints OAuth2</li>
     *
     * @param http configuration HttpSecurity
     * @param registeredClientRepository repository des clients OAuth2
     * @param userService service utilisateur pour récupération des infos
     * @return SecurityFilterChain appliquée aux endpoints OAuth2
     * @throws Exception en cas d'erreur de configuration
     */
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http, RegisteredClientRepository registeredClientRepository, UserService userService) throws Exception {
        log.info("CONFIGURING OAUTH2 AUTHORIZATION SERVER SECURITY FILTER CHAIN");
        http.cors(corsConfigurer -> corsConfigurer.configurationSource(corsConfigurationSource()));
        OAuth2AuthorizationServerConfigurer authorizationConfig =
                OAuth2AuthorizationServerConfigurer.authorizationServer();
        http
                .securityMatcher(authorizationConfig.getEndpointsMatcher())
                .with(authorizationConfig, authorizationServer ->
                        authorizationServer.oidc(Customizer.withDefaults())
                                .authorizationServerSettings(authorizationServerSettings())
                                .registeredClientRepository(registeredClientRepository)
                                .tokenGenerator(tokenGenerator())
                                .clientAuthentication(authentication -> {
                                    authentication.authenticationConverter(new ClientRefreshTokenAuthenticationConverter());
                                    authentication.authenticationProvider(new ClientAuthenticationProvider(registeredClientRepository));
                                }))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated());
        http
                .exceptionHandling(exceptions -> exceptions.accessDeniedPage("/accessdenied")
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/login"), new MediaTypeRequestMatcher(MediaType.TEXT_HTML)));
        return http.build();
    }


    /**
     * SecurityFilterChain pour la sécurité des formulaires et des API REST.
     * Cette configuration :
     * Désactive le CSRF pour les API REST
     * Configure les endpoints publics (/login, /user/register, /user/resetpassword, etc.)
     * Exige l'authentification pour tous les autres endpoints
     * Gère la MFA via l'autorité "MFA_REQUIRED"
     * Configure les formulaires de login et logout
     * Configure la validation des JWT pour les API REST
     *
     * @param http configuration HttpSecurity
     * @return SecurityFilterChain appliquée aux formulaires et aux API REST
     * @throws Exception en cas d'erreur de configuration
     */
    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {

        log.info("Configuring Default Security Filter Chain (Forms + API)");

        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(SWAGGER_ENDPOINTS).permitAll()
                        .requestMatchers(POST, "/logout").permitAll()
                        .requestMatchers("/mfa").hasAuthority("MFA_REQUIRED")
                        .anyRequest().authenticated())
                .anonymous(anonymous -> anonymous
                        .authorities("MFA_REQUIRED"));
        http.formLogin(login -> login
                .loginPage("/login")
                .successHandler(new MfaAuthenticationHandler("/mfa", "MFA_REQUIRED"))
                .failureHandler(new SimpleUrlAuthenticationFailureHandler("/login?error")));
        http.logout(logout -> logout.logoutSuccessUrl(redirectUri)
                .addLogoutHandler(new CookieClearingLogoutHandler("JSESSIONID")));
        return http.build();
    }


    /**
     * Bean pour gérer les clients OAuth2 enregistrés.
     * Utilise JDBC pour persister les clients dans la base de données.
     *
     * @param jdbcOperations template JDBC
     * @return repository des clients OAuth2
     */
    @Bean
    public RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcOperations) {
        return new JdbcRegisteredClientRepository(jdbcOperations);
    }

    /**
     * Gestionnaire de succès après authentification.
     * Redirige automatiquement l'utilisateur vers la page demandée avant login.
     *
     * @return AuthenticationSuccessHandler
     */
    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return new SavedRequestAwareAuthenticationSuccessHandler();
    }

    /**
     * Paramètres du serveur d’autorisation OAuth2.
     * Permet de configurer l'URL des endpoints (token, authorize, introspection, etc.)
     * Ici, on utilise les paramètres par défaut.
     * @return AuthorizationServerSettings
     */
    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder().build();
    }

    /**
     * Personnalise le contenu du JWT access token.
     * Ajoute la liste des autorités de l'utilisateur dans le token.
     *
     * @return OAuth2TokenCustomizer
     */
    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> customizer() {
        return (context) -> {
            if (ACCESS_TOKEN.equals(context.getTokenType())) {
                context.getClaims().claims(claims -> claims.put("authorities", getAuthorities(context)));
            }
        };
    }
//    @Bean
//    public OAuth2TokenCustomizer<JwtEncodingContext> customizer() {
//        return (context) -> {
//            if (ACCESS_TOKEN.equals(context.getTokenType())) {
//                context.getClaims().claims(claims -> claims.put("authorities", getAuthorities(context)));
//
//                // Le Principal est déjà un objet User !
//                var principal = context.getPrincipal().getPrincipal();
//
//                if (principal instanceof User user) {
//                    log.info(" JWT Customizer - User found: {} {}", user.getFirstName(), user.getLastName());
//
//                    context.getClaims().claims(claims -> {
//                        claims.put("firstName", user.getFirstName());
//                        claims.put("lastName", user.getLastName());
//                        claims.put("email", user.getEmail());
//                        claims.put("name", user.getFirstName() + " " + user.getLastName());
//                        if (user.getImageUrl() != null) {
//                            claims.put("imageUrl", user.getImageUrl());
//                        }
//                    });
//                } else {
//                    log.warn("JWT Customizer - Principal is not a User: {}", principal.getClass().getName());
//                }
//            }
//        };
//    }

    /**
     * Générateur de tokens OAuth2.
     * Combine :
     * JWT pour l'access token
     * Refresh token pour renouveler les access tokens
     * @return OAuth2TokenGenerator
     */
    @Bean
    public OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator() {
        var jwtGenerator = UserJwtGenerator.init(new NimbusJwtEncoder(configuration.jwkSource())); // access token
        jwtGenerator.setJwtCustomizer(customizer());
        OAuth2TokenGenerator<OAuth2RefreshToken> refreshTokenOAuth2TokenGenerator = new ClientOAuth2RefreshTokenGenerator(); // refresh token
        return new DelegatingOAuth2TokenGenerator(jwtGenerator, refreshTokenOAuth2TokenGenerator);
    }

    /**
     * Récupère les autorités de l'utilisateur en tant que chaîne séparée par des virgules.
     *
     * @param context contexte JWT
     * @return chaîne des autorités
     */
    private String getAuthorities(JwtEncodingContext context) {
        return context.getPrincipal().getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(joining(","));
    }

    /**
     * Bean pour configurer le firewall HTTP.
     * protéger l'application contre les URL malveillantes ou suspectes.
     * Par défaut, Spring Security bloque certaines requêtes considérées comme dangereuses,
     * Autorise certains caractères dans l’URL comme ";" et "\\".
     * @return HttpFirewall
     */
    @Bean
    public HttpFirewall getHttpFirewall() {
        StrictHttpFirewall strictHttpFirewall = new StrictHttpFirewall();
        strictHttpFirewall.setAllowSemicolon(true);
        strictHttpFirewall.setAllowBackSlash(true);
        return strictHttpFirewall;
    }

    /**
     * Source de configuration CORS.
     * Autorise l'accès aux API depuis les frontends Angular (4200)
     * Définit les méthodes HTTP autorisées, les headers, et la durée maximale de mise en cache des règles.
     * @return CorsConfigurationSource
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowCredentials(true);
        corsConfiguration.setAllowedOrigins(List.of("http://localhost:4200", "http://localhost:3000"));
        corsConfiguration.setAllowedHeaders(Arrays.asList(ORIGIN, ACCESS_CONTROL_ALLOW_ORIGIN, CONTENT_TYPE, ACCEPT, AUTHORIZATION, "X_REQUESTED_WITH", ACCESS_CONTROL_REQUEST_METHOD, ACCESS_CONTROL_REQUEST_HEADERS, ACCESS_CONTROL_ALLOW_CREDENTIALS));
        corsConfiguration.setExposedHeaders(Arrays.asList(ORIGIN, ACCESS_CONTROL_ALLOW_ORIGIN, CONTENT_TYPE, ACCEPT, AUTHORIZATION, "X_REQUESTED_WITH", ACCESS_CONTROL_REQUEST_METHOD, ACCESS_CONTROL_REQUEST_HEADERS, ACCESS_CONTROL_ALLOW_CREDENTIALS));
        corsConfiguration.setAllowedMethods(Arrays.asList(GET.name(), POST.name(), PUT.name(), PATCH.name(), DELETE.name(), OPTIONS.name()));
        corsConfiguration.setMaxAge(3600L);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }
}