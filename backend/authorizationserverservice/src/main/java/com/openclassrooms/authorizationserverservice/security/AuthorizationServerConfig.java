package com.openclassrooms.authorizationserverservice.security;

import com.openclassrooms.authorizationserverservice.handler.CustomAccessDeniedHandler;
import com.openclassrooms.authorizationserverservice.handler.CustomAuthenticationEntryPoint;
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
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
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
 * <p>
 * Cette classe configure :
 * <ul>
 *     <li>Le serveur d'autorisation OAuth2 (génération de tokens, clients, scopes, etc.)</li>
 *     <li>La sécurité des formulaires (login, MFA, logout)</li>
 *     <li>La sécurité des API REST avec JWT et validation des tokens</li>
 *     <li>Les règles CORS et les filtres HTTP personnalisés</li>
 * </ul>
 * <p>
 * Deux SecurityFilterChain sont définis avec des ordres différents :
 * <ol>
 *     <li>Ordre 1 : OAuth2 Authorization Server</li>
 *     <li>Ordre 2 : Formulaires MFA + API REST</li>
 * </ol>
 *
 * @author FirstName LastName
 * @version 1.0
 * @since 2026-05-01
 */

@EnableMethodSecurity
@Configuration
@Slf4j
@RequiredArgsConstructor
@EnableWebSecurity
public class AuthorizationServerConfig {

    /**
     * Configuration des JWT (clé publique/privée, algorithme, etc.)
     */
    private final JwtConfiguration configuration;

    /**
     * URI du JWK Set utilisé pour valider les JWT côté Resource Server.
     */
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


    // SECURITY FILTER CHAIN 1

    /**
     * SecurityFilterChain pour le serveur d'autorisation OAuth2.
     * <p>
     * Cette configuration :
     * <ul>
     *     <li>Active les endpoints OAuth2 (/oauth2/token, /oauth2/authorize, etc.)</li>
     *     <li>Configure les clients enregistrés via {@link RegisteredClientRepository}</li>
     *     <li>Configure la génération de tokens (JWT + refresh token)</li>
     *     <li>Applique une authentification obligatoire sur tous les endpoints OAuth2</li>
     * </ul>
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


    // SECURITY FILTER CHAIN 2

    /**
     * SecurityFilterChain pour la sécurité des formulaires et des API REST.
     * <p>
     * Cette configuration :
     * <ul>
     *     <li>Désactive le CSRF pour les API REST</li>
     *     <li>Configure les endpoints publics (/login, /user/register, /user/resetpassword, etc.)</li>
     *     <li>Exige l'authentification pour tous les autres endpoints</li>
     *     <li>Gère la MFA via l'autorité "MFA_REQUIRED"</li>
     *     <li>Configure les formulaires de login et logout</li>
     *     <li>Configure la validation des JWT pour les API REST</li>
     * </ul>
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
                        // Endpoints Publics
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(SWAGGER_ENDPOINTS).permitAll()
                        .requestMatchers(POST, "/logout").permitAll()
                        // MFA Required
                        .requestMatchers("/mfa").hasAuthority("MFA_REQUIRED")
                        // Tous les autres endpoints → Authentification requise
                        .anyRequest().authenticated())
                .anonymous(anonymous -> anonymous
                        .authorities("MFA_REQUIRED"));

        // Configuration Formulaires (Login, Logout)
        http.formLogin(login -> login
                .loginPage("/login")
                .successHandler(new MfaAuthenticationHandler("/mfa", "MFA_REQUIRED"))
                .failureHandler(new SimpleUrlAuthenticationFailureHandler("/login?error")));
        http.logout(logout -> logout.logoutSuccessUrl(redirectUri)
                .addLogoutHandler(new CookieClearingLogoutHandler("JSESSIONID")));

        // Configuration OAuth2 Resource Server (Validation JWT pour API REST)
        http.oauth2ResourceServer(oauth2 -> oauth2
                        .accessDeniedHandler(new CustomAccessDeniedHandler())
                        .authenticationEntryPoint(new CustomAuthenticationEntryPoint())
                        .jwt(jwt -> jwt
                                .jwkSetUri(jwkSetUri)
                                .jwtAuthenticationConverter(new JwtConverter())));

        return http.build();
    }

    // 1. REGISTERED CLIENT REPOSITORY

    /**
     * Bean pour gérer les clients OAuth2 enregistrés.
     * <p>
     * Utilise JDBC pour persister les clients dans la base de données.
     *
     * @param jdbcOperations template JDBC
     * @return repository des clients OAuth2
     */
    @Bean
    public RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcOperations) {
        return new JdbcRegisteredClientRepository(jdbcOperations);
    }

    // AUTHENTICATION_SUCCESS_HANDLER

    /**
     * Gestionnaire de succès après authentification.
     * <p>
     * Redirige automatiquement l'utilisateur vers la page demandée avant login.
     *
     * @return AuthenticationSuccessHandler
     */
    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return new SavedRequestAwareAuthenticationSuccessHandler();
    }

    // AUTHORIZATION SERVER SETTINGS

    /**
     * Paramètres du serveur d’autorisation OAuth2.
     * <p>
     * Permet de configurer l'URL des endpoints (token, authorize, introspection, etc.).
     * Ici, on utilise les paramètres par défaut.
     *
     * @return AuthorizationServerSettings
     */
    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder().build();
    }

    // CUSTOM JWT TOKEN

    /**
     * Personnalise le contenu du JWT access token.
     * <p>
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

    // TOKEN GENERATOR

    /**
     * Générateur de tokens OAuth2.
     * <p>
     * Combine :
     * <ul>
     *     <li>JWT pour l'access token</li>
     *     <li>Refresh token pour renouveler les access tokens</li>
     * </ul>
     *
     * @return OAuth2TokenGenerator
     */
    @Bean
    public OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator() {
        var jwtGenerator = UserJwtGenerator.init(new NimbusJwtEncoder(configuration.jwkSource())); // access token
        jwtGenerator.setJwtCustomizer(customizer());
        OAuth2TokenGenerator<OAuth2RefreshToken> refreshTokenOAuth2TokenGenerator = new ClientOAuth2RefreshTokenGenerator(); // refresh token
        return new DelegatingOAuth2TokenGenerator(jwtGenerator, refreshTokenOAuth2TokenGenerator);
    }

    // RETRIEVE AUTHORITIES

    /**
     * Récupère les autorités de l'utilisateur en tant que chaîne séparée par des virgules.
     *
     * @param context contexte JWT
     * @return chaîne des autorités
     */
    private String getAuthorities(JwtEncodingContext context) {
        return context.getPrincipal().getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(joining(","));
    }

    // HTTP FIREWALL

//    @Bean
//    public WebSecurityCustomizer webSecurityCustomizer(HttpFirewall httpFirewall) {
//        return (web) -> web.httpFirewall(httpFirewall);
//    }

    /**
     * Bean pour configurer le firewall HTTP.
     * <p>
     *  protéger l'application contre les URL malveillantes ou suspectes.
     *  Par défaut, Spring Security bloque certaines requêtes considérées comme dangereuses,
     *  Autorise certains caractères dans l’URL comme ";" et "\\".
     * </p>
     *
     * @return HttpFirewall
     */
    @Bean
    public HttpFirewall getHttpFirewall() {
        StrictHttpFirewall strictHttpFirewall = new StrictHttpFirewall();
        strictHttpFirewall.setAllowSemicolon(true);
        strictHttpFirewall.setAllowBackSlash(true);
        return strictHttpFirewall;
    }

    // CORS CONFIGURATION

    /**
     * Source de configuration CORS.
     * <p>
     * Autorise l'accès aux API depuis les frontends Angular (4200) et React (3000).
     * Définit les méthodes HTTP autorisées, les headers, et la durée maximale de mise en cache des règles.
     *
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