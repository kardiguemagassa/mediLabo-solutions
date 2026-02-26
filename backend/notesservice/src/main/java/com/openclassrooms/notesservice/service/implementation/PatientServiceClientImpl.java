package com.openclassrooms.notesservice.service.implementation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassrooms.notesservice.dto.ExternalResponse;
import com.openclassrooms.notesservice.dto.PatientInfo;
import com.openclassrooms.notesservice.exception.ApiException;
import com.openclassrooms.notesservice.service.PatientServiceClient;
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
 *
 * ARCHITECTURE RÉACTIVE:
 * - Toutes les méthodes retournent Mono<PatientInfo>
 * - Utilise Resilience4j pour Circuit Breaker et Retry
 * - Timeout configuré à 5 secondes
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

    // ==================== PUBLIC METHODS ====================

    /**
     * Récupère les informations d'un patient par son UUID.
     *
     * FLUX:
     * 1. Appel HTTP vers PatientService
     * 2. Gestion des erreurs (404, 4xx, 5xx)
     * 3. Conversion de la réponse en PatientInfo
     * 4. Timeout et Circuit Breaker
     */
    @Override
    @Retry(name = CIRCUIT_BREAKER_NAME)
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getPatientByUuidFallback")
    public Mono<PatientInfo> getPatientByUuid(String patientUuid) {
        log.debug("Fetching patient by UUID: {}", patientUuid);

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
                .onStatus(HttpStatusCode::is4xxClientError,
                        response -> Mono.error(new ApiException("Erreur client PatientService")))
                // Gestion erreurs 5xx
                .onStatus(HttpStatusCode::is5xxServerError,
                        response -> Mono.error(new ApiException("Erreur serveur PatientService")))
                // Conversion de la réponse
                .bodyToMono(ExternalResponse.class)
                // Filtrer les réponses vides
                .filter(response -> response != null && response.data() != null && response.data().containsKey("patient"))
                // Mapper vers PatientInfo
                .map(this::extractPatientInfo)
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

    /**
     * Récupère les infos de contact d'un patient.
     * Vérifie que le patient a un email configuré.
     *
     * @param patientUuid UUID du patient
     * @return Mono<PatientInfo> ou Mono.empty() si pas d'email
     */
    @Override
    @Retry(name = CIRCUIT_BREAKER_NAME)
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getPatientContactInfoFallback")
    public Mono<PatientInfo> getPatientContactInfo(String patientUuid) {
        log.debug("Fetching patient contact info: {}", patientUuid);

        return getPatientByUuid(patientUuid)
                // Filtrer si pas d'email
                .filter(patient -> {
                    if (patient.getEmail() == null || patient.getEmail().isBlank()) {
                        log.warn("Patient {} has no email configured", patientUuid);
                        return false;
                    }
                    return true;
                })
                .doOnSuccess(patient -> {
                    if (patient != null) {
                        log.debug("Patient contact info found: {}", patient.getEmail());
                    }
                });
    }

    // ==================== FALLBACK METHODS ====================

    /**
     * Fallback pour getPatientByUuid.
     * Appelé quand le Circuit Breaker est ouvert ou timeout.
     */
    public Mono<PatientInfo> getPatientByUuidFallback(String patientUuid, Throwable throwable) {
        log.error("Fallback getPatientByUuid - UUID: {}, Cause: {}", patientUuid, throwable.getMessage());
        return Mono.empty();
    }

    /**
     * Fallback pour getPatientContactInfo.
     */
    public Mono<PatientInfo> getPatientContactInfoFallback(String patientUuid, Throwable throwable) {
        log.error("Fallback getPatientContactInfo - UUID: {}, Cause: {}", patientUuid, throwable.getMessage());
        return Mono.empty();
    }

    // ==================== PRIVATE METHODS ====================

    /**
     * Extrait PatientInfo depuis la réponse ExternalResponse.
     */
    private PatientInfo extractPatientInfo(ExternalResponse response) {
        try {
            PatientInfo patient = objectMapper.convertValue(
                    response.data().get("patient"),
                    PatientInfo.class
            );
            log.debug("Converted PatientInfo: email={}, fullName={}",
                    patient.getEmail(), patient.getFullName());
            return patient;
        } catch (Exception e) {
            log.error("Error converting patient response: {}", e.getMessage());
            throw new ApiException("Erreur lors de la conversion de la réponse patient");
        }
    }
}