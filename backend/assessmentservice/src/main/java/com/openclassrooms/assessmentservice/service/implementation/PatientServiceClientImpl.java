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
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Implémentation réactive du client PatientService.
 * REACTIVE:
 * - Retourne Mono<PatientResponse>
 * - Utilise Resilience4j pour Circuit Breaker et Retry
 *
 * @author Kardigué MAGASSA
 * @version 2.0
 * @since 2026-02-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PatientServiceClientImpl implements PatientServiceClient {

    private final WebClient patientServiceWebClient;
    private final ObjectMapper objectMapper;

    private static final String CIRCUIT_BREAKER_NAME = "patientService";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    // PUBLIC METHODS

    /**
     * Récupère les informations d'un patient par son UUID.
     *
     * FLUX:
     * 1. Appel HTTP vers PatientService
     * 2. Gestion des erreurs (404, 4xx, 5xx)
     * 3. Extraction du PatientResponse depuis la réponse
     * 4. Circuit Breaker et Retry via Resilience4j
     */
    @Override
    @Retry(name = CIRCUIT_BREAKER_NAME)
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getPatientByUuidFallback")
    public Mono<PatientResponse> getPatientByUuid(String patientUuid) {
        log.info("Fetching patient from PatientService: {}", patientUuid);

        return patientServiceWebClient.get()
                .uri("/api/patients/{patientUuid}", patientUuid)
                .retrieve()
                // Gestion 404 : Patient non trouvé → Mono.empty()
                .onStatus(status -> status.equals(HttpStatus.NOT_FOUND),
                        response -> {
                            log.warn("Patient not found: {}", patientUuid);
                            return Mono.empty();
                        })
                // Gestion autres erreurs 4xx
                .onStatus(HttpStatusCode::is4xxClientError, response -> Mono.error(new ApiException("Erreur client PatientService")))
                // Gestion erreurs 5xx
                .onStatus(HttpStatusCode::is5xxServerError, response -> Mono.error(new ApiException("Erreur serveur PatientService")))
                // Conversion de la réponse
                .bodyToMono(Response.class)
                // Filtrer les réponses vides
                .filter(response -> response != null && response.data() != null && response.data().containsKey("patient"))
                // Extraire PatientResponse
                .map(this::extractPatient)
                // Logging succès
                .doOnSuccess(patient -> {
                    if (patient != null) {
                        log.debug("Patient found: {} - {}", patientUuid, patient.getFullName());
                    }
                })
                // Logging erreur
                .doOnError(error -> log.error("Error fetching patient {}: {}", patientUuid, error.getMessage()))
                // Timeout
                .timeout(TIMEOUT);
    }

    // FALLBACK METHODS

    /**
     * Fallback pour getPatientByUuid.
     * Appelé quand le Circuit Breaker est ouvert ou timeout.
     */
    public Mono<PatientResponse> getPatientByUuidFallback(String patientUuid, Throwable throwable) {
        log.error("Fallback getPatientByUuid - UUID: {}, Cause: {}", patientUuid, throwable.getMessage());
        return Mono.empty();
    }

    // PRIVATE METHODS

    /**
     * Extrait PatientResponse depuis la réponse.
     */
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