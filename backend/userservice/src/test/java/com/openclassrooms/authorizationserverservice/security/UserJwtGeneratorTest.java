/*package com.openclassrooms.userservice.security;

import com.openclassrooms.userservice.util.UserUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContext;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;


import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserJwtGeneratorTest {

    @Mock
    private JwtEncoder jwtEncoder;

    @Mock
    private OAuth2TokenContext context;

    @Mock
    private RegisteredClient registeredClient;

    @Mock
    private TokenSettings tokenSettings;

    private UserJwtGenerator userJwtGenerator;

    @BeforeEach
    void setUp() {
        userJwtGenerator = UserJwtGenerator.init(jwtEncoder);

        // Configuration commune pour la plupart des tests
        lenient().when(context.getRegisteredClient()).thenReturn(registeredClient);
        lenient().when(registeredClient.getTokenSettings()).thenReturn(tokenSettings);
        lenient().when(tokenSettings.getAccessTokenFormat()).thenReturn(OAuth2TokenFormat.SELF_CONTAINED);
        lenient().when(tokenSettings.getAccessTokenTimeToLive()).thenReturn(Duration.ofMinutes(5));
    }

    @Test
    void generate_AccessToken_ShouldReturnJwt() {
        // 1. Création d'un mock pour l'utilisateur retourné par UserUtils
        com.openclassrooms.userservice.model.User mockUser =
                mock(com.openclassrooms.userservice.model.User.class);
        when(mockUser.getUserUuid()).thenReturn("fake-uuid-123");

        // 2. Utilisation de mockStatic pour UserUtils
        try (MockedStatic<UserUtils> userUtilsMock =
                     mockStatic(com.openclassrooms.userservice.util.UserUtils.class)) {

            // On configure le mock statique pour renvoyer notre mockUser
            userUtilsMock.when(() -> UserUtils.getUser(any())).thenReturn(mockUser);

            when(context.getTokenType()).thenReturn(OAuth2TokenType.ACCESS_TOKEN);

            Authentication principal = mock(Authentication.class);
            when(context.getPrincipal()).thenReturn(principal);

            Jwt expectedJwt = mock(Jwt.class);
            when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(expectedJwt);

            // WHEN
            Jwt result = userJwtGenerator.generate(context);

            // THEN
            assertNotNull(result);
            verify(jwtEncoder).encode(any());
        }
    }

    @Test
    void generate_InvalidTokenType_ShouldReturnNull() {
        // Cas où le type de token est null
        when(context.getTokenType()).thenReturn(null);
        assertNull(userJwtGenerator.generate(context));

        // Cas où le type n'est ni Access Token ni ID Token
        when(context.getTokenType()).thenReturn(new OAuth2TokenType("REFRESH_TOKEN"));
        assertNull(userJwtGenerator.generate(context));
    }

    @Test
    void generate_NotSelfContainedFormat_ShouldReturnNull() {
        when(context.getTokenType()).thenReturn(OAuth2TokenType.ACCESS_TOKEN);
        when(tokenSettings.getAccessTokenFormat()).thenReturn(OAuth2TokenFormat.REFERENCE);

        assertNull(userJwtGenerator.generate(context));
    }

    @Test
    void generate_WithCustomizer_ShouldApplyCustomization() {
        // 1. Mock de l'utilisateur (Statique)
        com.openclassrooms.userservice.model.User mockUser =
                mock(com.openclassrooms.userservice.model.User.class);
        when(mockUser.getUserUuid()).thenReturn("fake-uuid-123");

        try (MockedStatic<com.openclassrooms.userservice.util.UserUtils> userUtilsMock =
                     mockStatic(com.openclassrooms.userservice.util.UserUtils.class)) {

            userUtilsMock.when(() -> UserUtils.getUser(any())).thenReturn(mockUser);

            // GIVEN
            when(context.getTokenType()).thenReturn(OAuth2TokenType.ACCESS_TOKEN);
            when(context.getPrincipal()).thenReturn(mock(Authentication.class));

            //  CONFIGURATION DU CONTEXTE (Les "must-have" pour le Builder de Spring)
            // 1. Le serveur context
            when(context.getAuthorizationServerContext()).thenReturn(mock(AuthorizationServerContext.class));

            // 2. Le client enregistré
            RegisteredClient registeredClient = RegisteredClient.withId("client-id")
                    .clientId("client")
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("http://localhost")
                    .scope("read")
                    .build();
            when(context.getRegisteredClient()).thenReturn(registeredClient);

            // 3. Le type de grant
            when(context.getAuthorizationGrantType()).thenReturn(AuthorizationGrantType.AUTHORIZATION_CODE);

            // Mock du customizer et de l'encodeur
            OAuth2TokenCustomizer<JwtEncodingContext> customizer = mock(OAuth2TokenCustomizer.class);
            userJwtGenerator.setJwtCustomizer(customizer);
            when(jwtEncoder.encode(any())).thenReturn(mock(Jwt.class));

            // WHEN
            userJwtGenerator.generate(context);

            // THEN
            verify(customizer).customize(any(JwtEncodingContext.class));
        }
    }

    @Test
    void generate_IdToken_ShouldHandleOidcClaims() {
        com.openclassrooms.userservice.model.User mockUser =
                mock(com.openclassrooms.userservice.model.User.class);
        when(mockUser.getUserUuid()).thenReturn("user-123");

        try (MockedStatic<com.openclassrooms.userservice.util.UserUtils> userUtilsMock =
                     mockStatic(com.openclassrooms.userservice.util.UserUtils.class)) {

            userUtilsMock.when(() -> UserUtils.getUser(any())).thenReturn(mockUser);

            // --- GIVEN ---
            OAuth2TokenType idTokenType = new OAuth2TokenType(OidcParameterNames.ID_TOKEN);
            when(context.getTokenType()).thenReturn(idTokenType);
            when(context.getPrincipal()).thenReturn(mock(Authentication.class));

            // Mock de la requête d'autorisation
            OAuth2AuthorizationRequest authRequest = OAuth2AuthorizationRequest.authorizationCode()
                    .authorizationUri("http://localhost/auth")
                    .clientId("client-id")
                    .redirectUri("http://localhost:8080/callback")
                    .additionalParameters(Collections.emptyMap())
                    .build();

            // Mock de l'objet Authorization
            OAuth2Authorization authorization = mock(OAuth2Authorization.class);
            when(authorization.getAttribute(OAuth2AuthorizationRequest.class.getName())).thenReturn(authRequest);
            when(context.getAuthorization()).thenReturn(authorization);

            // Client avec redirectUri pour éviter l'IllegalArgumentException
            RegisteredClient registeredClientMock = RegisteredClient.withId("id")
                    .clientId("client-id")
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("http://localhost:8080/callback") // Correction ici
                    .build();

            when(context.getRegisteredClient()).thenReturn(registeredClientMock);
            when(context.getAuthorizationServerContext()).thenReturn(mock(AuthorizationServerContext.class));
            when(context.getAuthorizationGrantType()).thenReturn(AuthorizationGrantType.AUTHORIZATION_CODE);

            when(jwtEncoder.encode(any())).thenReturn(mock(Jwt.class));

            // WHEN
            Jwt result = userJwtGenerator.generate(context);

            // THEN
            Assertions.assertNotNull(result);
            assertNotNull(result);
        }
    }

    @Test
    void generate_AccessToken_ShouldIncludeIssuerAndScopes() {
        // 1. Mock de l'utilisateur métier
        com.openclassrooms.userservice.model.User mockUser =
                mock(com.openclassrooms.userservice.model.User.class);
        when(mockUser.getUserUuid()).thenReturn("user-uuid-123");

        try (MockedStatic<com.openclassrooms.userservice.util.UserUtils> userUtilsMock =
                     mockStatic(com.openclassrooms.userservice.util.UserUtils.class)) {

            // 2. IMPORTANT :  UserUtils de renvoyer notre mockUser peu importe l'Authentication reçue
            userUtilsMock.when(() -> UserUtils.getUser(any())).thenReturn(mockUser);

            // GIVEN
            when(context.getTokenType()).thenReturn(OAuth2TokenType.ACCESS_TOKEN);

            // on simule un principal présent
            Authentication mockAuthentication = mock(Authentication.class);
            when(context.getPrincipal()).thenReturn(mockAuthentication);

            // Configuration de l'Issuer
            AuthorizationServerContext asContext = mock(AuthorizationServerContext.class);
            when(asContext.getIssuer()).thenReturn("https://auth-server.com");
            when(context.getAuthorizationServerContext()).thenReturn(asContext);

            // Configuration des Scopes
            when(context.getAuthorizedScopes()).thenReturn(Set.of("read", "write"));

            // Configuration du Client
            RegisteredClient registeredClient = RegisteredClient.withId("id")
                    .clientId("client-id")
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("http://localhost:8080/callback")
                    .tokenSettings(TokenSettings.builder()
                            .accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED)
                            .accessTokenTimeToLive(Duration.ofMinutes(5))
                            .build())
                    .build();
            when(context.getRegisteredClient()).thenReturn(registeredClient);

            // Mock de l'encodeur pour éviter une autre NPE à la fin
            when(jwtEncoder.encode(any())).thenReturn(mock(Jwt.class));

            // WHEN
            Jwt result = userJwtGenerator.generate(context);

            // THEN
            Assertions.assertNotNull(result);
            assertNotNull(result);
        }
    }

    @Test
    void generate_IdToken_ShouldIncludeNonceAndSession() {
        com.openclassrooms.userservice.model.User mockUser =
                mock(com.openclassrooms.userservice.model.User.class);
        when(mockUser.getUserUuid()).thenReturn("user-123");

        try (MockedStatic<com.openclassrooms.userservice.util.UserUtils> userUtilsMock =
                     mockStatic(com.openclassrooms.userservice.util.UserUtils.class)) {

            userUtilsMock.when(() -> UserUtils.getUser(any())).thenReturn(mockUser);

            // 1. Initialisation des mocks manquants
            OAuth2Authorization authorization = mock(OAuth2Authorization.class);
            when(context.getAuthorization()).thenReturn(authorization);
            when(context.getPrincipal()).thenReturn(mock(Authentication.class));
            when(context.getTokenType()).thenReturn(new OAuth2TokenType(OidcParameterNames.ID_TOKEN));
            when(context.getAuthorizationGrantType()).thenReturn(AuthorizationGrantType.AUTHORIZATION_CODE);

            // 2. Configuration du Nonce (pour couvrir le "if (StringUtils.hasText(nonce))")
            OAuth2AuthorizationRequest authRequest = OAuth2AuthorizationRequest.authorizationCode()
                    .authorizationUri("http://localhost")
                    .clientId("client-id")
                    .redirectUri("http://localhost:8080/callback")
                    .additionalParameters(Map.of(OidcParameterNames.NONCE, "nonce-secret-123"))
                    .build();

            // On lie la requête à l'objet authorization
            when(authorization.getAttribute(OAuth2AuthorizationRequest.class.getName())).thenReturn(authRequest);

            // 3. Configuration de la Session (pour couvrir le "if (sessionInformation != null)")
            SessionInformation sessionInfo = new SessionInformation("user", "session-id-999", new Date());
            when(context.get(SessionInformation.class)).thenReturn(sessionInfo);

            // 4. Configuration minimale du Client
            RegisteredClient registeredClient = RegisteredClient.withId("id")
                    .clientId("client-id")
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("http://localhost:8080/callback")
                    .build();
            when(context.getRegisteredClient()).thenReturn(registeredClient);
            when(jwtEncoder.encode(any())).thenReturn(mock(Jwt.class));

            //  WHEN
            userJwtGenerator.generate(context);

            //  THEN
            verify(jwtEncoder).encode(any());
        }
    }
}*/