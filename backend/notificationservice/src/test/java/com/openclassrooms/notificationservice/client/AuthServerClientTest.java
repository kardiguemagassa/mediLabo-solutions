package com.openclassrooms.notificationservice.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassrooms.notificationservice.dtorequest.UserRequest;
import com.openclassrooms.notificationservice.dtoresponse.MessageResponse;
import com.openclassrooms.notificationservice.service.implementation.UserServiceImpl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;

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
    private UserServiceImpl authServerClient;
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

        authServerClient = new UserServiceImpl(webClient);
    }

    @Nested
    @DisplayName("getUserByEmail Tests")
    class GetUserByEmailTests {

        @Test
        @DisplayName("Devrait retourner UserRequest quand l'utilisateur existe")
        void shouldReturnUserRequestWhenUserExists() throws Exception {
            // Given : On utilise UserRequest qui est le type attendu par ton service
            UserRequest expectedUser = UserRequest.builder()
                    .userUuid("user-uuid-123")
                    .firstName("Jean")
                    .lastName("Dupont")
                    .email("jean.dupont@medilabo.fr")
                    .role("DOCTOR")
                    .build();

            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .setBody(objectMapper.writeValueAsString(expectedUser)));

            // When & Then
            StepVerifier.create(authServerClient.getUserByEmail("jean.dupont@medilabo.fr"))
                    .assertNext(user -> {
                        assertThat(user.getUserUuid()).isEqualTo("user-uuid-123");
                        assertThat(user.getFirstName()).isEqualTo("Jean");
                        assertThat(user.getEmail()).isEqualTo("jean.dupont@medilabo.fr");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Devrait retourner un Mono vide quand l'utilisateur n'existe pas (404)")
        void shouldReturnEmptyWhenUserNotFound() {
            // Given : Simulation d'une erreur 404
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(404));

            // When & Then
            StepVerifier.create(authServerClient.getUserByEmail("unknown@email.com"))
                    .verifyComplete(); // Un Mono vide termine sans émission (comportement du fallback)
        }
    }

    @Nested
    @DisplayName("getUserByUuid Tests")
    class GetUserByUuidTests {

        @Test
        @DisplayName("Devrait retourner UserRequest quand l'utilisateur existe")
        void shouldReturnUserRequestWhenUserExists() throws Exception {
            // Given
            UserRequest expectedUser = UserRequest.builder()
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

            // When & Then
            StepVerifier.create(authServerClient.getUserByUuid("user-uuid-456"))
                    .assertNext(user -> {
                        assertThat(user.getEmail()).isEqualTo("marie.martin@patient.fr");
                        assertThat(user.getUserUuid()).isEqualTo("user-uuid-456");
                        assertThat(user.getRole()).isEqualTo("PATIENT");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Devrait retourner Mono empty en cas de timeout ou erreur serveur")
        void shouldReturnEmptyOnServerError() {
            // Given : Erreur 500
            mockWebServer.enqueue(new MockResponse().setResponseCode(500));

            // When & Then
            StepVerifier.create(authServerClient.getUserByUuid("any-uuid"))
                    .verifyComplete(); // Ton fallback catch l'erreur et retourne Mono.empty()
        }
    }
}