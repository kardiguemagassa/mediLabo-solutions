package com.openclassrooms.assessmentservice.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassrooms.assessmentservice.domain.Response;
import com.openclassrooms.assessmentservice.dtoresponse.PatientResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Client HTTP pour communiquer avec le Patient Service.
 * Utilise Resilience4j Circuit Breaker pour la résilience.
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-09
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PatientClient {

    private final WebClient patientWebClient;
    private final ObjectMapper objectMapper;

    private static final String CIRCUIT_BREAKER_NAME = "patientService";

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getPatientFallback")
    @io.github.resilience4j.retry.annotation.Retry(name = CIRCUIT_BREAKER_NAME)
    @io.github.resilience4j.timelimiter.annotation.TimeLimiter(name = CIRCUIT_BREAKER_NAME)
    public CompletableFuture<Optional<PatientResponse>> getPatientByUuid(String patientUuid) {
        log.info("Calling Patient Service for UUID: {}", patientUuid);

        return CompletableFuture.supplyAsync(() -> {
            Response response = patientWebClient.get()
                    .uri("/api/patients/{patientUuid}", patientUuid)
                    .retrieve()
                    .onStatus(status -> status == HttpStatus.NOT_FOUND, clientResponse -> Mono.empty())
                    .bodyToMono(Response.class)
                    .block();

            if (response == null || response.data() == null) {
                log.warn("Patient not found: {}", patientUuid);
                return Optional.<PatientResponse>empty();
            }

            PatientResponse patient = extractPatient(response);
            log.info("Patient retrieved successfully: {}", patient.getFullName());
            return Optional.of(patient);
        });
    }

    public CompletableFuture<Optional<PatientResponse>> getPatientFallback(String patientUuid, Throwable throwable) {
        log.error("Fallback triggered for Patient Service. UUID {}: {}", patientUuid, throwable.getMessage());
        return CompletableFuture.completedFuture(Optional.empty());
    }

    private PatientResponse extractPatient(Response response) {
        Object patientData = response.data().get("patient");
        return objectMapper.convertValue(patientData, PatientResponse.class);
    }
}
