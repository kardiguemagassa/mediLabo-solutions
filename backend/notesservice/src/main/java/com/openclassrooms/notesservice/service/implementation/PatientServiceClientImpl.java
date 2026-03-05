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
import org.springframework.web.reactive.function.client.WebClientResponseException;
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
                .onStatus(HttpStatusCode::is5xxServerError, response -> Mono.error(new ApiException("Erreur serveur PatientService")))
                .onStatus(status -> status.is4xxClientError() && status != HttpStatus.NOT_FOUND, response -> Mono.error(new ApiException("Erreur client PatientService")))
                .bodyToMono(ExternalResponse.class)
                .flatMap(response -> {
                    if (response == null || response.data() == null || !response.data().containsKey("patient")) {
                        return Mono.empty();
                    }
                    return Mono.just(extractPatientInfo(response));
                })

                .onErrorResume(WebClientResponseException.NotFound.class, e -> {
                    log.warn("Patient non trouvé (404) pour l'UUID: {}", patientUuid);
                    return Mono.empty(); // Retourne un Mono<PatientInfo> vide (correct !)
                })

                .timeout(TIMEOUT)
                .onErrorResume(java.util.concurrent.TimeoutException.class, e -> {
                    log.error("Timeout pour le patient {}", patientUuid);
                    return Mono.empty();
                });
    }

    /**
     * Récupère les infos de contact d'un patient.
     * Vérifie que le patient a un email configuré.
     *
     * @param patientUuid UUID du patient
     * @return Mono<PatientInfo> ou Mono.empty() si pas d'email
     */
    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getPatientContactInfoFallback")
    public Mono<PatientInfo> getPatientContactInfo(String patientUuid) {
        log.debug("Fetching patient contact info: {}", patientUuid);

        return getPatientByUuid(patientUuid)
                .flatMap(patient -> {
                    if (patient.getEmail() == null || patient.getEmail().isBlank()) {
                        log.warn("Patient {} has no email configured", patientUuid);
                        return Mono.empty();
                    }
                    return Mono.just(patient);
                })
                .switchIfEmpty(Mono.empty());
    }

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