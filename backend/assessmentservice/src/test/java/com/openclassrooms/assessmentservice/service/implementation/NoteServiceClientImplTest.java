package com.openclassrooms.assessmentservice.service.implementation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openclassrooms.assessmentservice.exception.ApiException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class NoteServiceClientImplTest {

    private static MockWebServer mockWebServer;
    private NoteServiceClientImpl noteClient;
    private ObjectMapper objectMapper;
    private static final String TEST_TOKEN = "test-jwt-token";

    @BeforeAll
    static void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @BeforeEach
    void initialize() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        String baseUrl = String.format("http://localhost:%s", mockWebServer.getPort());
        WebClient webClient = WebClient.builder().baseUrl(baseUrl).build();
        noteClient = new NoteServiceClientImpl(webClient, objectMapper);
    }

    @Test
    @DisplayName("Should return notes successfully")
    void getNotesByPatientUuid_Success() {
        String mockJsonResponse =
                """
                {
                    "status": "OK",
                    "message": "Notes retrieved successfully",
                    "data": {
                        "notes": [
                            {
                                "noteUuid": "note-1",
                                "patientUuid": "uuid-123",
                                "content": "Patient feels better",
                                "createdAt": "2024-01-01T10:00:00"
                            }
                        ]
                    }
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(mockJsonResponse));

        StepVerifier.create(noteClient.getNotesByPatientUuid("uuid-123", TEST_TOKEN).collectList())
                .assertNext(result -> {
                    assertThat(result).isNotEmpty();
                    assertThat(result.getFirst().getNoteUuid()).isEqualTo("note-1");
                    assertThat(result.getFirst().getPatientUuid()).isEqualTo("uuid-123");
                    assertThat(result.getFirst().getContent()).isEqualTo("Patient feels better");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return empty list when API returns empty data")
    void getNotesByPatientUuid_Empty() {
        String mockEmptyResponse =
                """
                {
                    "status": "OK",
                    "data": null
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(mockEmptyResponse));

        StepVerifier.create(noteClient.getNotesByPatientUuid("uuid-123", TEST_TOKEN).collectList())
                .assertNext(result -> assertThat(result).isEmpty())
                .verifyComplete();
    }

    @Test
    @DisplayName("Should throw ApiException on 404 Not Found")
    void getNotesByPatientUuid_NotFound() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(404)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("Not Found"));

        // Les erreurs 4xx sont propagées comme ApiException
        StepVerifier.create(noteClient.getNotesByPatientUuid("uuid-123", TEST_TOKEN).collectList())
                .expectErrorMatches(throwable ->
                        throwable instanceof ApiException &&
                                throwable.getMessage().contains("Erreur client NotesService"))
                .verify();
    }

    @Test
    @DisplayName("Should throw ApiException on 400 Bad Request")
    void getNotesByPatientUuid_BadRequest() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(400)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("Bad Request"));

        StepVerifier.create(noteClient.getNotesByPatientUuid("uuid-123", TEST_TOKEN).collectList())
                .expectErrorMatches(throwable ->
                        throwable instanceof ApiException &&
                                throwable.getMessage().contains("Erreur client NotesService"))
                .verify();
    }

    @Test
    @DisplayName("Should return empty list when response has no notes field")
    void getNotesByPatientUuid_NoNotesField() {
        String mockJsonResponse =
                """
                {
                    "status": "OK",
                    "data": {}
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(mockJsonResponse));

        StepVerifier.create(noteClient.getNotesByPatientUuid("uuid-123", TEST_TOKEN).collectList())
                .assertNext(result -> assertThat(result).isEmpty())
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return empty list when notes field is null")
    void getNotesByPatientUuid_NotesFieldNull() {
        String mockJsonResponse =
                """
                {
                    "status": "OK",
                    "data": {
                        "notes": null
                    }
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(mockJsonResponse));

        StepVerifier.create(noteClient.getNotesByPatientUuid("uuid-123", TEST_TOKEN).collectList())
                .assertNext(result -> assertThat(result).isEmpty())
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return empty list when notes field is not an array")
    void getNotesByPatientUuid_NotesFieldNotArray() {
        String mockJsonResponse =
                """
                {
                    "status": "OK",
                    "data": {
                        "notes": "not an array"
                    }
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(mockJsonResponse));

        // convertValue va échouer et retourner Collections.emptyList()
        StepVerifier.create(noteClient.getNotesByPatientUuid("uuid-123", TEST_TOKEN).collectList())
                .assertNext(result -> assertThat(result).isEmpty())
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle network error with fallback")
    void getNotesByPatientUuid_NetworkError() {
        // Simuler une erreur réseau en fermant la connexion
        mockWebServer.enqueue(new MockResponse().setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AT_START));

        // L'erreur réseau déclenche le fallback qui retourne Flux.empty()
        StepVerifier.create(noteClient.getNotesByPatientUuid("uuid-123", TEST_TOKEN).collectList())
                .assertNext(result -> assertThat(result).isEmpty())
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return empty list when response body is empty")
    void getNotesByPatientUuid_EmptyBody() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(""));

        StepVerifier.create(noteClient.getNotesByPatientUuid("uuid-123", TEST_TOKEN).collectList())
                .assertNext(result -> assertThat(result).isEmpty())
                .verifyComplete();
    }
}