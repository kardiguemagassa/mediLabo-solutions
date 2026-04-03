package com.openclassrooms.patientservice.handler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests pour {@link WebClientInterceptor}.
 */
@DisplayName("Tests l'intercepteur WebClient")
class WebClientInterceptorTest {

    @Test
    @DisplayName("Devrait ajouter le header Authorization avec le token JWT")
    void jwtAuthorizationFilter_ShouldAddAuthorizationHeader() {
        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {

            // Given
            Jwt jwt = mock(Jwt.class);
            when(jwt.getTokenValue()).thenReturn("test-jwt-token");

            JwtAuthenticationToken auth = mock(JwtAuthenticationToken.class);
            when(auth.getToken()).thenReturn(jwt);

            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(
                    new SecurityContext() {
                        @Override
                        public Authentication getAuthentication() {
                            return auth;
                        }

                        @Override
                        public void setAuthentication(Authentication authentication) {
                            // no-op
                        }
                    }
            );

            AtomicReference<ClientRequest> capturedRequest = new AtomicReference<>();
            ExchangeFilterFunction filter = WebClientInterceptor.jwtAuthorizationFilter();

            ClientRequest originalRequest = ClientRequest.create(
                    HttpMethod.GET, URI.create("http://test.com")
            ).build();

            ExchangeFunction exchangeFunction = request -> {capturedRequest.set(request);
                return Mono.just(mock(ClientResponse.class));
            };

            // When
            filter.filter(originalRequest, exchangeFunction).block();

            // Then
            ClientRequest modifiedRequest = capturedRequest.get();
            assertThat(modifiedRequest).isNotNull();
            assertThat(modifiedRequest.headers().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer test-jwt-token");
        }
    }

    @Test
    @DisplayName("Devrait ne pas ajouter de header quand pas de JWT")
    void jwtAuthorizationFilter_ShouldNotAddHeader_WhenNoJwt() {
        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            // Given
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(
                    new org.springframework.security.core.context.SecurityContext() {
                        @Override
                        public org.springframework.security.core.Authentication getAuthentication() {
                            return null; // Pas d'authentification
                        }

                        @Override
                        public void setAuthentication(org.springframework.security.core.Authentication authentication) {
                            // Ne rien faire
                        }
                    }
            );

            // Capture la requête modifiée
            AtomicReference<ClientRequest> capturedRequest = new AtomicReference<>();

            ExchangeFilterFunction filter = WebClientInterceptor.jwtAuthorizationFilter();

            ClientRequest originalRequest = ClientRequest.create(HttpMethod.GET, URI.create("http://test.com")).build();

            // When
            filter.filter(originalRequest, request -> {capturedRequest.set(request);
                return Mono.just(mock(ClientResponse.class));
            }).block();

            // Then
            ClientRequest modifiedRequest = capturedRequest.get();
            assertThat(modifiedRequest).isNotNull();
            assertThat(modifiedRequest.headers().getFirst(HttpHeaders.AUTHORIZATION)).isNull();

        }
    }

    @Test
    @DisplayName("Les filtres de logging devraient exister")
    void logFilters_ShouldExist() {
        // Given & When & Then
        assertThat(WebClientInterceptor.logRequest()).isNotNull();
        assertThat(WebClientInterceptor.logResponse()).isNotNull();
        assertThat(WebClientInterceptor.handleError()).isNotNull();
    }

    @Test
    @DisplayName("Devrait conserver l'URL originale")
    void jwtAuthorizationFilter_ShouldPreserveOriginalUrl() {
        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            // Given
            Jwt jwt = mock(Jwt.class);
            when(jwt.getTokenValue()).thenReturn("test-jwt");

            JwtAuthenticationToken auth = mock(JwtAuthenticationToken.class);
            when(auth.getToken()).thenReturn(jwt);

            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(
                    new org.springframework.security.core.context.SecurityContext() {
                        @Override
                        public org.springframework.security.core.Authentication getAuthentication() {
                            return auth;
                        }

                        @Override
                        public void setAuthentication(org.springframework.security.core.Authentication authentication) {
                            // Ne rien faire
                        }
                    }
            );

            // Capture
            AtomicReference<ClientRequest> capturedRequest = new AtomicReference<>();

            ExchangeFilterFunction filter = WebClientInterceptor.jwtAuthorizationFilter();

            URI originalUri = URI.create("https://api.service.com/v1/users?page=1");
            ClientRequest originalRequest = ClientRequest.create(HttpMethod.GET, originalUri)
                    .build();

            // When
            filter.filter(originalRequest, request -> {capturedRequest.set(request);
                return Mono.just(mock(ClientResponse.class));}).block();

            // Then
            ClientRequest modifiedRequest = capturedRequest.get();
            assertThat(modifiedRequest.url()).isEqualTo(originalUri);

        }
    }
}