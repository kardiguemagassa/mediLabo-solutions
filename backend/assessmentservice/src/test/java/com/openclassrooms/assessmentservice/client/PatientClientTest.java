package com.openclassrooms.assessmentservice.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassrooms.assessmentservice.dtoresponse.PatientResponse;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

class PatientClientTest {

    private static MockWebServer mockWebServer;
    private PatientClient patientClient;
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
        patientClient = new PatientClient(webClient, objectMapper);
    }

    @Test
    @DisplayName("Should return patient when API returns 200 OK")
    void getPatientByUuid_Success() throws Exception {
        // GIVEN: Une réponse JSON simulant votre objet Response global
        String mockJsonResponse =
            """
                {
                    "status": "OK",
                    "data": {
                        "patient": {
                            "firstName": "John",
                            "lastName": "Doe",
                            "gender": "M"
                        }
                    }
                }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(mockJsonResponse));

        // WHEN
        CompletableFuture<Optional<PatientResponse>> future = patientClient.getPatientByUuid("uuid-123");
        Optional<PatientResponse> result = future.get();

        // THEN
        assertThat(result).isPresent();
        assertThat(result.get().getFirstName()).isEqualTo("John");

        // Vérification de l'appel réseau
        var recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getPath()).isEqualTo("/api/patients/uuid-123");
    }

    @Test
    @DisplayName("Should return empty Optional when API returns 404")
    void getPatientByUuid_NotFound() throws Exception {
        // GIVEN
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));

        // WHEN
        Optional<PatientResponse> result = patientClient.getPatientByUuid("unknown").get();

        // THEN
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should trigger fallback when service is down")
    void getPatientFallback_Test() throws ExecutionException, InterruptedException {
        // GIVEN
        Throwable ex = new RuntimeException("Service Down");

        // WHEN
        CompletableFuture<Optional<PatientResponse>> fallback = patientClient.getPatientFallback("uuid-123", ex);

        // THEN
        assertThat(fallback.get()).isEmpty();
    }
}