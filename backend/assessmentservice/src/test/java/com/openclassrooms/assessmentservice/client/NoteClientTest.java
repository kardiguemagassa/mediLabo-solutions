package com.openclassrooms.assessmentservice.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassrooms.assessmentservice.dtoresponse.NoteResponse;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class NoteClientTest {

    private static MockWebServer mockWebServer;
    private NoteClient noteClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

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
        String baseUrl = String.format("http://localhost:%s", mockWebServer.getPort());
        WebClient webClient = WebClient.builder().baseUrl(baseUrl).build();
        noteClient = new NoteClient(webClient, objectMapper);
    }

    @Test
    void getNotesByPatientUuid_Success() throws Exception {
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
                                "note": "Patient feels better"
                            }
                        ]
                    }
                }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(mockJsonResponse));

        // WHEN
        CompletableFuture<List<NoteResponse>> future = noteClient.getNotesByPatientUuidAsync("uuid-123");
        List<NoteResponse> result = future.get();

        // THEN
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        assertThat(result.getFirst().getNoteUuid()).isEqualTo("note-1");
    }

    @Test
    @DisplayName("Should return empty list when API returns error or empty data")
    void getNotesByPatientUuid_Empty() throws Exception {
        // GIVEN: Réponse vide ou structure data nulle
        String mockEmptyResponse = "{\"status\":\"OK\", \"data\": null}";
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(mockEmptyResponse)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        // WHEN
        List<NoteResponse> result = noteClient.getNotesByPatientUuidAsync("uuid-123").get();

        // THEN
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should trigger fallback and return empty list on exception")
    void getNotesFallback_Test() throws Exception {
        // GIVEN
        Throwable ex = new RuntimeException("Timeout or Connection Error");

        // WHEN
        CompletableFuture<List<NoteResponse>> fallback = noteClient.getNotesFallback("uuid-123", ex);

        // THEN
        assertThat(fallback.get()).isEmpty();
    }
}