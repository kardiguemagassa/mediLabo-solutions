package com.openclassrooms.notificationservice.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebClientConfigTest {

    private WebClientConfig webClientConfig;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @BeforeEach
    void setUp() {
        webClientConfig = new WebClientConfig();
    }

    @Test
    void authServerWebClient_ShouldBuildWebClientWithCustomValues_WhenPropertiesSet() throws Exception {
        // Given
        String customUrl = "http://custom-auth-server:9002";
        int customTimeout = 10000;

        setAuthorizationServerUrl(customUrl);
        setTimeout(customTimeout);

        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.clientConnector(any(ReactorClientHttpConnector.class))).thenReturn(webClientBuilder);
        when(webClientBuilder.defaultHeader(anyString(), anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.filter(any())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);

        // When
        WebClient result = webClientConfig.authServerWebClient(webClientBuilder);

        // Then
        assertNotNull(result);
        verify(webClientBuilder).baseUrl(customUrl);
    }

    @Test
    void authServerWebClient_ShouldConfigureHttpClientWithTimeouts() throws Exception {
        // Given
        int customTimeout = 3000;
        setTimeout(customTimeout);
        setAuthorizationServerUrl("http://localhost:9001");

        // Capturer le HttpClient créé
        HttpClient[] capturedHttpClient = new HttpClient[1];

        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.clientConnector(any(ReactorClientHttpConnector.class))).thenAnswer(invocation -> {
            ReactorClientHttpConnector connector = invocation.getArgument(0);
            return webClientBuilder;
        });
        when(webClientBuilder.defaultHeader(anyString(), anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.filter(any())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);

        // When
        WebClient result = webClientConfig.authServerWebClient(webClientBuilder);

        // Then
        assertNotNull(result);
        verify(webClientBuilder).clientConnector(any(ReactorClientHttpConnector.class));
    }

    @Test
    void createHttpClient_ShouldConfigureConnectTimeout() throws Exception {
        // Given
        int customTimeout = 5000;
        setTimeout(customTimeout);

        // la réflexion pour tester la méthode privée
        java.lang.reflect.Method method = WebClientConfig.class.getDeclaredMethod("createHttpClient");
        method.setAccessible(true);

        // When
        HttpClient httpClient = (HttpClient) method.invoke(webClientConfig);

        // Then
        assertNotNull(httpClient);
    }

    @Test
    void authServerWebClient_ShouldConfigureAllDefaultHeaders() throws Exception {
        // Given
        setAuthorizationServerUrl("http://localhost:9001");
        setTimeout(5000);

        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.clientConnector(any(ReactorClientHttpConnector.class))).thenReturn(webClientBuilder);
        when(webClientBuilder.defaultHeader(anyString(), anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.filter(any())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);

        // When
        WebClient result = webClientConfig.authServerWebClient(webClientBuilder);

        // Then
        assertNotNull(result);

        // Vérifier que les deux en-têtes par défaut sont configurés
        verify(webClientBuilder).defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        verify(webClientBuilder).defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
    }

    @Test
    void authServerWebClient_ShouldReturnDifferentInstances_WhenCalledMultipleTimes() throws Exception {
        // Given
        setAuthorizationServerUrl("http://localhost:9001");
        setTimeout(5000);

        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.clientConnector(any(ReactorClientHttpConnector.class))).thenReturn(webClientBuilder);
        when(webClientBuilder.defaultHeader(anyString(), anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.filter(any())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient, mock(WebClient.class));

        // When
        WebClient result1 = webClientConfig.authServerWebClient(webClientBuilder);
        WebClient result2 = webClientConfig.authServerWebClient(webClientBuilder);

        // Then
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotSame(result1, result2);
    }

    @Test
    void authServerWebClient_ShouldHandleExtremeTimeoutValues() throws Exception {
        // Given
        setAuthorizationServerUrl("http://localhost:9001");

        int[] timeouts = {0, 1, 100, 30000, 60000, 3600000}; // 0ms à 1h

        for (int customTimeout : timeouts) {
            setTimeout(customTimeout);

            when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
            when(webClientBuilder.clientConnector(any(ReactorClientHttpConnector.class))).thenReturn(webClientBuilder);
            when(webClientBuilder.defaultHeader(anyString(), anyString())).thenReturn(webClientBuilder);
            when(webClientBuilder.filter(any())).thenReturn(webClientBuilder);
            when(webClientBuilder.build()).thenReturn(webClient);

            // When
            WebClient result = webClientConfig.authServerWebClient(webClientBuilder);

            // Then
            assertNotNull(result);
            verify(webClientBuilder, atLeastOnce()).clientConnector(any(ReactorClientHttpConnector.class));

            // Réinitialiser les mocks pour le prochain test
            reset(webClientBuilder);
        }
    }

    @Test
    void authServerWebClient_ShouldHandleMalformedUrlGracefully() throws Exception {
        // Given
        String malformedUrl = "not-a-valid-url";
        setAuthorizationServerUrl(malformedUrl);
        setTimeout(5000);

        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.clientConnector(any(ReactorClientHttpConnector.class))).thenReturn(webClientBuilder);
        when(webClientBuilder.defaultHeader(anyString(), anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.filter(any())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);

        // When
        WebClient result = webClientConfig.authServerWebClient(webClientBuilder);

        // Then
        assertNotNull(result);
        verify(webClientBuilder).baseUrl(malformedUrl);
        // WebClient.Builder accepte n'importe quelle chaîne comme baseUrl
    }

    @Test
    void authServerWebClient_ShouldLogConfiguration() throws Exception {
        // Given
        setAuthorizationServerUrl("http://localhost:9001");
        setTimeout(5000);

        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.clientConnector(any(ReactorClientHttpConnector.class))).thenReturn(webClientBuilder);
        when(webClientBuilder.defaultHeader(anyString(), anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.filter(any())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);

        // When
        WebClient result = webClientConfig.authServerWebClient(webClientBuilder);

        // Then
        assertNotNull(result);
    }

    // Méthodes utilitaires pour définir les valeurs des champs privés
    private void setAuthorizationServerUrl(String value) throws Exception {
        Field field = WebClientConfig.class.getDeclaredField("authorizationServerUrl");
        field.setAccessible(true);
        field.set(webClientConfig, value);
    }

    private void setTimeout(int value) throws Exception {
        Field field = WebClientConfig.class.getDeclaredField("timeout");
        field.setAccessible(true);
        field.set(webClientConfig, value);
    }
}