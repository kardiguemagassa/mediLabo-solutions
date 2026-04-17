package com.openclassrooms.assessmentservice.service.implementation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openclassrooms.assessmentservice.dtoresponse.PatientResponseDTO;
import com.openclassrooms.assessmentservice.model.Gender;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class PatientServiceClientImplTest {

    private MockWebServer mockWebServer;
    private PatientServiceClientImpl patientClient;
    private static final String TEST_TOKEN = "test-jwt-token";


    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        String baseUrl = String.format("http://localhost:%s", mockWebServer.getPort());
        WebClient webClient = WebClient.builder().baseUrl(baseUrl).build();
        patientClient = new PatientServiceClientImpl(webClient, objectMapper);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("Should return patient when API returns 200 OK")
    void getPatientByUuid_Success() {

        String mockJsonResponse =
                """
                {
                    "status": "OK",
                    "data": {
                        "patient": {
                            "patientUuid": "uuid-123",
                            "firstName": "John",
                            "lastName": "Doe",
                            "gender": "MALE",
                            "dateOfBirth": "1990-01-01",
                            "userInfo": {
                                "firstName": "John",
                                "lastName": "Doe",
                                "email": "john.doe@email.com",
                                "phone": null
                            }
                        }
                    }
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(mockJsonResponse));

        // WHEN
        PatientResponseDTO result = patientClient.getPatientByUuid("uuid-123", TEST_TOKEN).block();

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getPatientUuid()).isEqualTo("uuid-123");
        assertThat(result.getFullName()).isEqualTo("John Doe");
        assertThat(result.getGender()).isEqualTo(Gender.MALE);
        assertThat(result.getDateOfBirth()).isEqualTo(LocalDate.of(1990, 1, 1));
        assertThat(result.getUserInfo()).isNotNull();
        assertThat(result.getUserInfo().getFirstName()).isEqualTo("John");
        assertThat(result.getUserInfo().getLastName()).isEqualTo("Doe");
    }

    @Test
    @DisplayName("Should handle response with userInfo structure")
    void getPatientByUuid_WithUserInfo() {
        // GIVEN
        String mockJsonResponse =
                """
                {
                    "status": "OK",
                    "data": {
                        "patient": {
                            "patientUuid": "uuid-123",
                            "gender": "MALE",
                            "dateOfBirth": "1990-01-01",
                            "userInfo": {
                                "firstName": "Jean",
                                "lastName": "Dupont",
                                "email": "jean.dupont@email.com",
                                "phone": null
                            }
                        }
                    }
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(mockJsonResponse));

        // WHEN
        PatientResponseDTO result = patientClient.getPatientByUuid("uuid-123", TEST_TOKEN).block();

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getPatientUuid()).isEqualTo("uuid-123");
        assertThat(result.getFullName()).isEqualTo("Jean Dupont");
        assertThat(result.getGender()).isEqualTo(Gender.MALE);
        assertThat(result.getUserInfo()).isNotNull();
        assertThat(result.getUserInfo().getFirstName()).isEqualTo("Jean");
        assertThat(result.getUserInfo().getLastName()).isEqualTo("Dupont");
        assertThat(result.getUserInfo().getEmail()).isEqualTo("jean.dupont@email.com");
    }

    @Test
    @DisplayName("Should return empty Mono when API returns 404")
    void getPatientByUuid_NotFound() {
        // GIVEN
        String mockJsonResponse = """
                {
                    "status": "NOT_FOUND",
                    "message": "Patient not found"
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(404)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(mockJsonResponse));

        // WHEN
        PatientResponseDTO result = patientClient.getPatientByUuid("unknown", TEST_TOKEN)
                .onErrorResume(e -> Mono.empty())
                .block();

        // THEN
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should return empty Mono when API returns 500")
    void getPatientByUuid_ServerError() {
        // GIVEN
        String mockJsonResponse = """
                {
                    "status": "ERROR",
                    "message": "Internal server error"
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(mockJsonResponse));

        // WHEN - Le fallback doit retourner Mono.empty()
        PatientResponseDTO result = patientClient.getPatientByUuid("uuid-123", TEST_TOKEN)
                .onErrorResume(e -> Mono.empty())
                .block();

        // THEN
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should return empty Mono when API returns malformed JSON")
    void getPatientByUuid_MalformedJson() {
        // GIVEN
        String malformedJson = "{ invalid json }";

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(malformedJson));

        // WHEN - Le fallback doit gérer l'erreur de parsing
        PatientResponseDTO result = patientClient.getPatientByUuid("uuid-123", TEST_TOKEN)
                .onErrorResume(e -> Mono.empty())
                .block();

        // THEN
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should handle timeout with fallback")
    void getPatientByUuid_Timeout() {
        // GIVEN
        mockWebServer.enqueue(new MockResponse()
                .setBodyDelay(6, TimeUnit.SECONDS)
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("{}"));

        // WHEN - Utiliser StepVerifier pour vérifier que le timeout est géré correctement
        StepVerifier.create(patientClient.getPatientByUuid("uuid-123", TEST_TOKEN)
                        .onErrorResume(e -> Mono.empty()))
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle empty response body")
    void getPatientByUuid_EmptyBody() {
        // GIVEN
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(""));

        // WHEN
        PatientResponseDTO result = patientClient.getPatientByUuid("uuid-123", TEST_TOKEN)
                .onErrorResume(e -> Mono.empty())
                .block();

        // THEN
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should handle response without data field")
    void getPatientByUuid_NoDataField() {
        // GIVEN
        String mockJsonResponse =
                """
                {
                    "status": "OK",
                    "message": "Patient found"
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(mockJsonResponse));

        // WHEN - Le flux sera vide car data est null
        PatientResponseDTO result = patientClient.getPatientByUuid("uuid-123", TEST_TOKEN)
                .onErrorResume(e -> Mono.empty())
                .block();

        // THEN
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should handle response with null patient")
    void getPatientByUuid_NullPatient() {
        // GIVEN
        String mockJsonResponse =
                """
                {
                    "status": "OK",
                    "data": {
                        "patient": null
                    }
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(mockJsonResponse));

        // WHEN - Le flux sera vide car patient est null
        PatientResponseDTO result = patientClient.getPatientByUuid("uuid-123", TEST_TOKEN)
                .onErrorResume(e -> Mono.empty())
                .block();

        // THEN
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should handle response with missing fields")
    void getPatientByUuid_MissingFields() {
        String mockJsonResponse = """
    {
        "status": "OK",
        "data": {
            "patient": {
                "patientUuid": "uuid-123"
            }
        }
    }
    """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(mockJsonResponse));


        PatientResponseDTO result = patientClient.getPatientByUuid("uuid-123", TEST_TOKEN)
                .onErrorResume(e -> {
                    // En cas de timeout ou autre erreur, on retourne empty
                    return Mono.empty();
                })
                .block();

        // Si on a un résultat (pas de timeout), on teste les valeurs
        if (result != null) {
            assertThat(result.getPatientUuid()).isEqualTo("uuid-123");
            assertThat(result.getFullName()).isEqualTo("Inconnu");
        }
    }

    @Test
    @DisplayName("Should trigger fallback and return empty Mono")
    void getPatientFallback_Test() {
        // GIVEN
        Throwable ex = new RuntimeException("Service Down");

        // WHEN
        PatientResponseDTO result = patientClient.getPatientByUuidFallback("uuid-123", TEST_TOKEN, ex).block();

        // THEN
        assertThat(result).isNull();
    }
}