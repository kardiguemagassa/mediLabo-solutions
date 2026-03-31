/*package com.openclassrooms.userservice.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import static org.assertj.core.api.Assertions.assertThat;

class ClientRefreshTokenAuthenticationTest {

    @Test
    @DisplayName("Constructeur String : Devrait initialiser avec le clientId et la méthode NONE")
    void constructorWithClientId_ShouldPopulateCorrectly() {
        // GIVEN
        String clientId = "test-client-id";

        // WHEN
        ClientRefreshTokenAuthentication authentication = new ClientRefreshTokenAuthentication(clientId);

        // THEN
        assertThat(authentication.getPrincipal().toString()).isEqualTo(clientId);
        assertThat(authentication.getClientAuthenticationMethod()).isEqualTo(ClientAuthenticationMethod.NONE);
        assertThat(authentication.getCredentials()).isNull();
        assertThat(authentication.isAuthenticated()).isFalse();
    }

    @Test
    @DisplayName("Constructeur RegisteredClient : Devrait initialiser avec l'objet client et la méthode NONE")
    void constructorWithRegisteredClient_ShouldPopulateCorrectly() {
        // GIVEN
        RegisteredClient registeredClient = RegisteredClient.withId("1")
                .clientId("my-client")
                .authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.REFRESH_TOKEN)
                .build();

        // WHEN
        ClientRefreshTokenAuthentication authentication = new ClientRefreshTokenAuthentication(registeredClient);

        // THEN
        // Spring utilise le clientId comme nom du Principal
        assertThat(authentication.getName()).isEqualTo("my-client");

        // regarde si getPrincipal() renvoie le clientId ou l'objet selon la version
        assertThat(authentication.getPrincipal().toString()).contains("my-client");

        assertThat(authentication.getClientAuthenticationMethod()).isEqualTo(ClientAuthenticationMethod.NONE);
        assertThat(authentication.getCredentials()).isNull();
    }
}*/