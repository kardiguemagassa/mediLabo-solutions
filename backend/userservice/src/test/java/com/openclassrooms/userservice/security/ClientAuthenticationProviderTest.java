/*package com.openclassrooms.userservice.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientAuthenticationProviderTest {

    @Mock
    private RegisteredClientRepository registeredClientRepository;

    private ClientAuthenticationProvider provider;

    @BeforeEach
    void setUp() {
        provider = new ClientAuthenticationProvider(registeredClientRepository);
    }

    @Test
    @DisplayName("Authenticate : Devrait retourner null si la méthode n'est pas NONE")
    void authenticate_WhenMethodNotNone_ShouldReturnNull() {
        // GIVEN
        ClientRefreshTokenAuthentication auth = mock(ClientRefreshTokenAuthentication.class);
        // On simule une méthode différente (ex: BASIC)
        when(auth.getClientAuthenticationMethod()).thenReturn(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);

        // WHEN
        var result = provider.authenticate(auth);

        // THEN
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Authenticate : Devrait lancer exception si le client n'existe pas")
    void authenticate_WhenClientNotFound_ShouldThrowException() {
        // GIVEN
        String clientId = "unknown-client";
        ClientRefreshTokenAuthentication auth = new ClientRefreshTokenAuthentication(clientId);
        when(registeredClientRepository.findByClientId(clientId)).thenReturn(null);

        // WHEN & THEN
        assertThatThrownBy(() -> provider.authenticate(auth))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasFieldOrPropertyWithValue("error.errorCode", OAuth2ErrorCodes.INVALID_CLIENT)
                .hasMessageContaining("Le client n'est pas valide");
    }

    @Test
    @DisplayName("Authenticate : Devrait lancer exception si la méthode NONE n'est pas autorisée pour ce client")
    void authenticate_WhenMethodNotAllowedForClient_ShouldThrowException() {
        // GIVEN
        String clientId = "my-client";
        ClientRefreshTokenAuthentication auth = new ClientRefreshTokenAuthentication(clientId);

        // On crée un client qui n'accepte que BASIC (pas NONE)
        RegisteredClient registeredClient = RegisteredClient.withId("1")
                .clientId(clientId)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.REFRESH_TOKEN)
                .build();

        when(registeredClientRepository.findByClientId(clientId)).thenReturn(registeredClient);

        // WHEN & THEN
        assertThatThrownBy(() -> provider.authenticate(auth))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasFieldOrPropertyWithValue("error.errorCode", OAuth2ErrorCodes.INVALID_CLIENT)
                .hasMessageContaining("La méthode d'authentification n'est pas enregistrée");
    }

    @Test
    @DisplayName("Authenticate : Succès nominal")
    void authenticate_WhenValid_ShouldReturnAuthenticatedToken() {
        // GIVEN
        String clientId = "valid-client";
        ClientRefreshTokenAuthentication auth = new ClientRefreshTokenAuthentication(clientId);

        RegisteredClient registeredClient = RegisteredClient.withId("1")
                .clientId(clientId)
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.REFRESH_TOKEN)
                .build();

        when(registeredClientRepository.findByClientId(clientId)).thenReturn(registeredClient);

        // WHEN
        var result = provider.authenticate(auth);

        // THEN
        assertThat(result).isInstanceOf(ClientRefreshTokenAuthentication.class);
        assertThat(result.isAuthenticated()).isTrue();

        // On vérifie que le principal correspond au clientId du registeredClient
        assertThat(result.getName()).isEqualTo(registeredClient.getClientId());

        assertThat(result.getPrincipal().toString()).isEqualTo("valid-client");
    }

    @Test
    @DisplayName("Supports : Devrait supporter uniquement ClientRefreshTokenAuthentication")
    void supports_ShouldCheckClassType() {
        assertThat(provider.supports(ClientRefreshTokenAuthentication.class)).isTrue();
        assertThat(provider.supports(org.springframework.security.authentication.UsernamePasswordAuthenticationToken.class)).isFalse();
    }
}*/