package com.openclassrooms.notificationservice.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassrooms.notificationservice.dto.UserInfo;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires pour AuthServerClient.
 * Utilise MockWebServer pour simuler les réponses de l'Auth Server.
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-09
 */
@DisplayName("AuthServerClient Tests")
class AuthServerClientTest {

    private static MockWebServer mockWebServer;
    private AuthServerClient authServerClient;
    private ObjectMapper objectMapper;

    @BeforeAll
    static void setUpServer() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterAll
    static void tearDownServer() throws IOException {
        mockWebServer.shutdown();
    }

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        WebClient webClient = WebClient.builder().baseUrl(mockWebServer.url("/").toString()).build();

        authServerClient = new AuthServerClient(webClient);
    }

    @Nested
    @DisplayName("getUserByEmail Tests")
    class GetUserByEmailTests {

        @Test
        @DisplayName("Devrait retourner UserInfo quand l'utilisateur existe")
        void shouldReturnUserInfoWhenUserExists() throws Exception {
            // Given
            UserInfo expectedUser = UserInfo.builder()
                    .userUuid("user-uuid-123")
                    .firstName("Jean")
                    .lastName("Dupont")
                    .email("jean.dupont@medilabo.fr")
                    .imageUrl("https://example.com/jean.jpg")
                    .role("DOCTOR")
                    .build();

            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .setBody(objectMapper.writeValueAsString(expectedUser)));

            // When
            Optional<UserInfo> result = authServerClient.getUserByEmail("jean.dupont@medilabo.fr").get();

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getUserUuid()).isEqualTo("user-uuid-123");
            assertThat(result.get().getFirstName()).isEqualTo("Jean");
            assertThat(result.get().getLastName()).isEqualTo("Dupont");
            assertThat(result.get().getName()).isEqualTo("Jean Dupont");
        }

        @Test
        @DisplayName("Devrait retourner Optional vide quand l'utilisateur n'existe pas")
        void shouldReturnEmptyWhenUserNotFound() throws ExecutionException, InterruptedException {
            // Given
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(404)
                    .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .setBody(""));

            // When
            Optional<UserInfo> result = authServerClient.getUserByEmail("unknown@email.com").get();

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getUserByUuid Tests")
    class GetUserByUuidTests {

        @Test
        @DisplayName("Devrait retourner UserInfo quand l'utilisateur existe")
        void shouldReturnUserInfoWhenUserExists() throws Exception {
            // Given
            UserInfo expectedUser = UserInfo.builder()
                    .userUuid("user-uuid-456")
                    .firstName("Marie")
                    .lastName("Martin")
                    .email("marie.martin@patient.fr")
                    .role("PATIENT")
                    .build();

            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .setBody(objectMapper.writeValueAsString(expectedUser)));

            // When
            Optional<UserInfo> result = authServerClient.getUserByUuid("user-uuid-456").get();

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getEmail()).isEqualTo("marie.martin@patient.fr");
            assertThat(result.get().getRole()).isEqualTo("PATIENT");
        }
    }
}