package com.openclassrooms.notificationservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassrooms.notificationservice.domain.Response;
import com.openclassrooms.notificationservice.dto.UserRequestDTO;
import com.openclassrooms.notificationservice.service.implementation.UserServiceClientImpl;
import lombok.extern.slf4j.Slf4j;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires pour UserServiceImpl (AuthServerClient).
 * Utilise MockWebServer pour simuler les réponses de l'Auth Server.
 *
 * IMPORTANT: Chaque classe de test imbriquée utilise son propre MockWebServer
 * pour éviter les problèmes de queue FIFO entre tests.
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-09
 */
@Slf4j
@DisplayName("UserServiceImpl (AuthServerClient) Tests")
class UserServiceClientImplTest {

    private MockWebServer mockWebServer;
    private UserServiceClientImpl userService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

        String baseUrl = mockWebServer.url("").toString();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        WebClient webClient = WebClient.builder().baseUrl(baseUrl).build();

        userService = new UserServiceClientImpl(webClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        // Fermer le serveur après chaque test
        mockWebServer.shutdown();
    }

    // Helper Methods

    /**
     * Crée une Response valide contenant un user dans data.
     * Le service attend ce format : Response avec data.user
     */
    private Response createValidResponse(UserRequestDTO user) {
        return new Response(
                LocalDateTime.now().toString(),
                HttpStatus.OK.value(),
                "/user/" + user.getUserUuid(),
                HttpStatus.OK,
                "User found",
                "",
                Map.of("user", user)
        );
    }

    // getUserByEmail Tests

    @Nested
    @DisplayName("getUserByEmail Tests")
    class GetUserByEmailTests {

        @Test
        @DisplayName("Devrait retourner UserRequest quand l'utilisateur existe")
        void shouldReturnUserRequestWhenUserExists() throws Exception {
            // Given
            UserRequestDTO expectedUser = UserRequestDTO.builder()
                    .userUuid("user-uuid-123")
                    .firstName("Jean")
                    .lastName("Dupont")
                    .email("jean.dupont@medilabo.fr")
                    .role("DOCTOR")
                    .build();

            Response response = createValidResponse(expectedUser);

            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .setBody(objectMapper.writeValueAsString(response)));

            // When & Then
            StepVerifier.create(userService.getUserByEmail("jean.dupont@medilabo.fr"))
                    .assertNext(user -> {
                        assertThat(user.getUserUuid()).isEqualTo("user-uuid-123");
                        assertThat(user.getFirstName()).isEqualTo("Jean");
                        assertThat(user.getEmail()).isEqualTo("jean.dupont@medilabo.fr");
                    })
                    .expectComplete()
                    .verify(Duration.ofSeconds(5));
        }

        @Test
        @DisplayName("Devrait retourner Mono vide quand l'utilisateur n'existe pas (404)")
        void shouldReturnEmptyWhenUserNotFound() {
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(404)
                    .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .setBody("{}"));

            StepVerifier.create(userService.getUserByUuid("unknown-uuid"))
                    .expectSubscription()
                    .expectComplete()
                    .verify(Duration.ofSeconds(5));
        }

        @Test
        @DisplayName("Devrait retourner Mono vide quand la réponse n'a pas de user dans data")
        void shouldReturnEmptyWhenNoUserInData() throws Exception {

            Response responseWithoutUser = new Response(
                    LocalDateTime.now().toString(),
                    HttpStatus.OK.value(),
                    "/user/email/test@test.com",
                    HttpStatus.OK,
                    "No user",
                    "",
                    Map.of()
            );

            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .setBody(objectMapper.writeValueAsString(responseWithoutUser)));


            StepVerifier.create(userService.getUserByEmail("test@test.com"))
                    .expectComplete()
                    .verify(Duration.ofSeconds(5));
        }

        @Test
        @DisplayName("Devrait retourner Mono vide en cas d'erreur client (400) sur l'email")
        void shouldReturnEmptyOnEmailClientError() { // Changé le nom pour être clair
            mockWebServer.enqueue(new MockResponse().setResponseCode(400));

            StepVerifier.create(userService.getUserByEmail("invalid@email.com"))
                    .expectSubscription()
                    .expectComplete()
                    .verify(Duration.ofSeconds(5));
        }

        @Test
        @DisplayName("Devrait retourner Mono vide en cas d'erreur serveur (500) sur l'email")
        void shouldReturnEmptyOnEmailServerError() {
            mockWebServer.enqueue(new MockResponse().setResponseCode(500));

            StepVerifier.create(userService.getUserByEmail("any@email.com"))
                    .expectSubscription()
                    .expectComplete()
                    .verify(Duration.ofSeconds(5));
        }
    }

    // getUserByUuid Tests

    @Nested
    @DisplayName("getUserByUuid Tests")
    class GetUserByUuidTests {

        @Test
        @DisplayName("Devrait retourner Mono vide quand l'utilisateur n'existe pas (404) par UUID")
        void shouldReturnEmptyWhenUuidNotFound() {
            mockWebServer.enqueue(new MockResponse().setResponseCode(404));

            StepVerifier.create(userService.getUserByUuid("unknown-uuid"))
                    .expectSubscription()
                    .expectComplete()
                    .verify(Duration.ofSeconds(5));
        }

        @Test
        @DisplayName("Devrait retourner Mono vide en cas d'erreur client (400) sur l'UUID")
        void shouldReturnEmptyOnUuidClientError() {
            mockWebServer.enqueue(new MockResponse().setResponseCode(400));

            StepVerifier.create(userService.getUserByUuid("invalid-uuid"))
                    .expectSubscription()
                    .expectComplete()
                    .verify(Duration.ofSeconds(5));
        }

        @Test
        @DisplayName("Devrait retourner Mono vide en cas d'erreur serveur (500) sur l'UUID")
        void shouldReturnEmptyOnUuidServerError() {
            mockWebServer.enqueue(new MockResponse().setResponseCode(500));

            StepVerifier.create(userService.getUserByUuid("any-uuid"))
                    .expectSubscription()
                    .expectComplete()
                    .verify(Duration.ofSeconds(5));
        }
    }

    // Fallback Tests (Direct)

    @Nested
    @DisplayName("Fallback Methods Tests (Direct)")
    class FallbackMethodsTests {

        @Test
        @DisplayName("Fallback getUserByUuid devrait retourner Mono.empty()")
        void fallbackGetUserByUuidShouldReturnEmpty() {
            // Test direct du fallback (sans passer par AOP)
            StepVerifier.create(userService.getUserByUuidFallback("any-uuid", new RuntimeException("Test")))
                    .expectComplete()
                    .verify(Duration.ofSeconds(5));
        }

        @Test
        @DisplayName("Fallback getUserByEmail devrait retourner Mono.empty()")
        void fallbackGetUserByEmailShouldReturnEmpty() {
            // Test direct du fallback
            StepVerifier.create(userService.getUserByEmailFallback("any@email.com", new RuntimeException("Test")))
                    .expectComplete()
                    .verify(Duration.ofSeconds(5));
        }
    }
}