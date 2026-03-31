/*package com.openclassrooms.userservice.security;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClientRefreshTokenAuthenticationConverterTest {

    private ClientRefreshTokenAuthenticationConverter converter;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        converter = new ClientRefreshTokenAuthenticationConverter();
        request = mock(HttpServletRequest.class);
    }

    @Test
    @DisplayName("Convert : Devrait retourner null si le grant_type n'est pas refresh_token")
    void convert_WhenGrantTypeIsNotRefreshToken_ShouldReturnNull() {
        // GIVEN
        when(request.getParameter(OAuth2ParameterNames.GRANT_TYPE)).thenReturn("authorization_code");

        // WHEN
        Authentication result = converter.convert(request);

        // THEN
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Convert : Devrait retourner null si le client_id est manquant")
    void convert_WhenClientIdIsMissing_ShouldReturnNull() {
        // GIVEN
        when(request.getParameter(OAuth2ParameterNames.GRANT_TYPE)).thenReturn("refresh_token");
        when(request.getParameter(OAuth2ParameterNames.CLIENT_ID)).thenReturn("");

        // WHEN
        Authentication result = converter.convert(request);

        // THEN
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Convert : Devrait retourner une authentification si les paramètres sont valides")
    void convert_WhenParametersValid_ShouldReturnAuthentication() {
        // GIVEN
        String clientId = "my-test-client";
        when(request.getParameter(OAuth2ParameterNames.GRANT_TYPE)).thenReturn("refresh_token");
        when(request.getParameter(OAuth2ParameterNames.CLIENT_ID)).thenReturn(clientId);

        // WHEN
        Authentication result = converter.convert(request);

        // THEN
        assertThat(result).isInstanceOf(ClientRefreshTokenAuthentication.class);
        Assertions.assertNotNull(result);
        assertThat(result.getPrincipal()).isEqualTo(clientId);
    }
}*/