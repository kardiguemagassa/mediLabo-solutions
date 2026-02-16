package com.openclassrooms.authorizationserverservice.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientOAuth2RefreshTokenGeneratorTest {

    private ClientOAuth2RefreshTokenGenerator generator;

    @Mock
    private OAuth2TokenContext context;

    @Mock
    private RegisteredClient registeredClient;

    @Mock
    private TokenSettings tokenSettings;

    @BeforeEach
    void setUp() {
        generator = new ClientOAuth2RefreshTokenGenerator();
    }

    @Test
    @DisplayName("Générateur : Devrait retourner null si le type n'est pas REFRESH_TOKEN")
    void generate_WhenNotRefreshToken_ShouldReturnNull() {
        // GIVEN
        when(context.getTokenType()).thenReturn(OAuth2TokenType.ACCESS_TOKEN);

        // WHEN
        OAuth2RefreshToken result = generator.generate(context);

        // THEN
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Générateur : Devrait générer un token valide si le type est REFRESH_TOKEN")
    void generate_WhenRefreshToken_ShouldReturnToken() {
        // GIVEN
        Duration ttl = Duration.ofDays(1);

        when(context.getTokenType()).thenReturn(OAuth2TokenType.REFRESH_TOKEN);
        when(context.getRegisteredClient()).thenReturn(registeredClient);
        when(registeredClient.getTokenSettings()).thenReturn(tokenSettings);
        when(tokenSettings.getRefreshTokenTimeToLive()).thenReturn(ttl);

        // WHEN
        OAuth2RefreshToken result = generator.generate(context);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getTokenValue()).hasSizeGreaterThan(10); // Base64 96 chars
        assertThat(result.getIssuedAt()).isBeforeOrEqualTo(result.getExpiresAt());

        // Vérification de la durée (TTL)
        Assertions.assertNotNull(result.getIssuedAt());
        long durationSeconds = Duration.between(result.getIssuedAt(), result.getExpiresAt()).toSeconds();
        assertThat(durationSeconds).isEqualTo(ttl.toSeconds());
    }
}