package com.openclassrooms.assessmentservice.service.implementation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassrooms.assessmentservice.domain.Response;
import com.openclassrooms.assessmentservice.dtoresponse.PatientResponse;
import com.openclassrooms.assessmentservice.exception.ApiException;
import com.openclassrooms.assessmentservice.service.PatientServiceClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatientServiceClientImpl implements PatientServiceClient {

    private final WebClient patientServiceWebClient;
    private final ObjectMapper objectMapper;

    private static final String CIRCUIT_BREAKER_NAME = "patientService";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    @Override
    @Retry(name = CIRCUIT_BREAKER_NAME)
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getPatientByUuidFallback")
    public Mono<PatientResponse> getPatientByUuid(String patientUuid, String token) {
        log.info("Fetching patient from PatientService: {}", patientUuid);

        return patientServiceWebClient.get()
                .uri("/api/patients/{patientUuid}", patientUuid)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .onStatus(status -> status.equals(HttpStatus.NOT_FOUND),
                        response -> {
                            log.warn("Patient not found: {}", patientUuid);
                            return Mono.empty();
                        })
                .onStatus(HttpStatusCode::is4xxClientError,
                        response -> Mono.error(new ApiException("Erreur client PatientService")))
                .onStatus(HttpStatusCode::is5xxServerError,
                        response -> Mono.error(new ApiException("Erreur serveur PatientService")))
                .bodyToMono(Response.class)
                .filter(response -> response != null && response.data() != null
                        && response.data().containsKey("patient"))
                .map(this::extractPatient)
                .doOnSuccess(patient -> {
                    if (patient != null) log.debug("Patient found: {}", patientUuid);
                })
                .doOnError(error -> log.error("Error fetching patient {}: {}", patientUuid, error.getMessage()))
                .timeout(TIMEOUT);
    }

    public Mono<PatientResponse> getPatientByUuidFallback(String patientUuid, String token, Throwable throwable) {
        log.error("Fallback getPatientByUuid - UUID: {}, Cause: {}", patientUuid, throwable.getMessage());
        return Mono.empty();
    }

    private PatientResponse extractPatient(Response response) {
        try {
            Object patientData = response.data().get("patient");
            PatientResponse patient = objectMapper.convertValue(patientData, PatientResponse.class);
            log.debug("Extracted patient: {}", patient.getPatientUuid());
            return patient;
        } catch (Exception e) {
            log.error("Error extracting patient from response: {}", e.getMessage());
            throw new ApiException("Erreur lors de la conversion de la réponse patient");
        }
    }
}