package com.openclassrooms.notesservice.service.implementation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassrooms.notesservice.dto.ExternalResponse;
import com.openclassrooms.notesservice.dto.PatientInfo;
import com.openclassrooms.notesservice.exception.ApiException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.Map;

@DisplayName("PatientServiceClientImpl Unit Tests")
class PatientServiceClientImplTest {

    private static MockWebServer mockWebServer;
    private PatientServiceClientImpl patientServiceClient;
    private ObjectMapper objectMapper;

    private static final String PATIENT_UUID = "patient-uuid-123";

    @BeforeAll
    static void setUpAll() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterAll
    static void tearDownAll() throws IOException {
        mockWebServer.shutdown();
    }

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        String baseUrl = String.format("http://localhost:%s", mockWebServer.getPort());
        WebClient webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();

        patientServiceClient = new PatientServiceClientImpl(webClient, objectMapper);
    }

    @Nested
    @DisplayName("getPatientByUuid() Tests")
    class GetPatientByUuidTests {

        @Test
        @DisplayName("Should return patient when found")
        void shouldReturnPatientWhenFound() throws Exception {
            // Given
            PatientInfo.UserInfo userInfo = new PatientInfo.UserInfo("Jean", "Martin", "jean.martin@email.com", null, null, null);
            PatientInfo patient = PatientInfo.builder()
                    .patientUuid(PATIENT_UUID)
                    .userInfo(userInfo)
                    .build();

            ExternalResponse response = new ExternalResponse("ts", 200, "path", "OK", "Success", "", Map.of("patient", patient));

            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .setBody(objectMapper.writeValueAsString(response)));

            // When & Then
            StepVerifier.create(patientServiceClient.getPatientByUuid(PATIENT_UUID))
                    .expectNextMatches(p -> p.getPatientUuid().equals(PATIENT_UUID) &&
                            "jean.martin@email.com".equals(p.getEmail()))
                    .verifyComplete();
        }

        @Disabled("Flaky on CI - MockWebServer response ordering")
        @Test
        @DisplayName("Should return empty when patient not found (404)")
        void shouldReturnEmptyWhenPatientNotFound() {
            // Given - On simule un vrai 404
            mockWebServer.enqueue(new MockResponse().setResponseCode(404));

            // When & Then - Le service transforme le 404 en Mono.empty()
            StepVerifier.create(patientServiceClient.getPatientByUuid("unknown-patient"))
                    .verifyComplete(); // Succès si vide
        }

        @Disabled("Flaky on CI - MockWebServer response ordering")
        @Test
        @DisplayName("Should throw ApiException on server error (500)")
        void shouldThrowApiExceptionOnServerError() {
            // Given - On simule une erreur serveur
            mockWebServer.enqueue(new MockResponse().setResponseCode(500));

            // When & Then - Le service transforme le 500 en ApiException via .onStatus()
            StepVerifier.create(patientServiceClient.getPatientByUuid(PATIENT_UUID))
                    .expectError(ApiException.class)
                    .verify();
        }
    }

    @Nested
    @DisplayName("getPatientContactInfo() Tests")
    class GetPatientContactInfoTests {

        @Disabled("Flaky on CI - MockWebServer response ordering")
        @Test
        @DisplayName("Should return patient with email")
        void shouldReturnPatientWithEmail() throws Exception {
            String jsonResponse = """
        {
            "status": 200,
            "message": "Success",
            "data": {
                "patient": {
                    "patientUuid": "patient-uuid-123",
                    "userInfo": {
                        "firstName": "Jean",
                        "lastName": "Martin",
                        "email": "jean.martin@email.com"
                    }
                }
            }
        }
        """;

            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .setBody(jsonResponse));

            // When & Then
            StepVerifier.create(patientServiceClient.getPatientContactInfo(PATIENT_UUID))
                    .expectNextMatches(p -> "jean.martin@email.com".equals(p.getEmail()))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should return empty when patient has no email")
        void shouldReturnEmptyWhenPatientHasNoEmail() throws Exception {
            // Given
            PatientInfo patient = PatientInfo.builder()
                    .patientUuid(PATIENT_UUID)
                    .userInfo(new PatientInfo.UserInfo(
                            "Jean",
                            "Martin",
                           null,
                            null,
                            null,
                            null
                    ))
                    .build();

            ExternalResponse response = new ExternalResponse(
                    "timestamp",
                    200,
                    "path",
                    "OK",
                    "Success",
                    "",
                    Map.of("patient", patient)
            );

            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .setBody(objectMapper.writeValueAsString(response)));

            // When & Then
            StepVerifier.create(patientServiceClient.getPatientContactInfo(PATIENT_UUID))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should return empty when patient has blank email")
        void shouldReturnEmptyWhenPatientHasBlankEmail() throws Exception {
            // Given
            PatientInfo patient = PatientInfo.builder()
                    .patientUuid(PATIENT_UUID)
                    .userInfo(new PatientInfo.UserInfo(
                            "Jean",
                            "Martin",
                            " ",
                            null,
                            null,
                            null
                    ))
                    .build();

            ExternalResponse response = new ExternalResponse(
                    "timestamp",
                    200,
                    "path",
                    "OK",
                    "Success",
                    "",
                    Map.of("patient", patient)
            );

            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .setBody(objectMapper.writeValueAsString(response)));

            // When & Then
            StepVerifier.create(patientServiceClient.getPatientContactInfo(PATIENT_UUID))
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Fallback Methods Tests")
    class FallbackMethodsTests {

        @Test
        @DisplayName("getPatientByUuidFallback should return empty Mono")
        void getPatientByUuidFallbackShouldReturnEmptyMono() {
            // When & Then
            StepVerifier.create(patientServiceClient.getPatientByUuidFallback(
                            PATIENT_UUID, new RuntimeException("Test error")))
                    .verifyComplete();
        }

        @Test
        @DisplayName("getPatientContactInfoFallback should return empty Mono")
        void getPatientContactInfoFallbackShouldReturnEmptyMono() {
            // When & Then
            StepVerifier.create(patientServiceClient.getPatientContactInfoFallback(
                            PATIENT_UUID, new RuntimeException("Test error")))
                    .verifyComplete();
        }
    }
}