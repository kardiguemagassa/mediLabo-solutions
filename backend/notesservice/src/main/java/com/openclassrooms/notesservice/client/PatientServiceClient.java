package com.openclassrooms.notesservice.client;

import com.openclassrooms.notesservice.domain.Response;
import com.openclassrooms.notesservice.dto.PatientInfo;
import com.openclassrooms.notesservice.exception.ApiException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.openclassrooms.notesservice.util.RequestUtils.convertResponse;

/**
 * Client pour communiquer avec le PatientService.
 * Utilise Resilience4j pour Circuit Breaker, Retry et TimeLimiter.
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-09
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PatientServiceClient {

    private final WebClient patientServiceWebClient;
    private static final String CIRCUIT_BREAKER_NAME = "patientService";

    /**
     * Cœur de la logique : Récupération réactive brute.
     * Cette méthode sert de base aux autres.
     */
    public Mono<PatientInfo> fetchPatient(String patientUuid) {
        log.debug("Fetching patient by UUID: {}", patientUuid);

        return patientServiceWebClient.get()
                .uri("/api/patients/{patientUuid}", patientUuid)
                .retrieve()
                .onStatus(status -> status == HttpStatus.NOT_FOUND,
                        resp -> Mono.error(new ApiException("Patient non trouvé: " + patientUuid)))
                .onStatus(HttpStatusCode::isError,
                        resp -> Mono.error(new ApiException("Erreur technique PatientService")))
                .bodyToMono(Response.class)
                .map(response -> {
                    if (response == null || response.data() == null) {
                        throw new ApiException("Réponse vide du PatientService");
                    }
                    return convertResponse(response, PatientInfo.class, "patient");
                });
    }

    /**
     * Récupération Asynchrone avec Résilience (Circuit Breaker & Retry).
     * C'est la méthode principale utilisée par les services métier.
     */
    @TimeLimiter(name = CIRCUIT_BREAKER_NAME) // Le TimeLimiter est important sur le futur
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getPatientByUuidFallback")
    @Retry(name = CIRCUIT_BREAKER_NAME)
    public CompletableFuture<Optional<PatientInfo>> getPatientByUuidAsync(String patientUuid) {
        log.info("Calling PatientService with Resilience: {}", patientUuid);

        return fetchPatient(patientUuid)
                .map(Optional::of)
                // Resilience4j interceptera les erreurs ici si tu as configuré
                // le CircuitBreaker pour les types réactifs
                .toFuture()
                .exceptionally(throwable -> {
                    // Ce bloc capture les erreurs SI le circuit breaker ne l'a pas fait
                    log.warn("L'appel a échoué malgré la résilience pour {}: {}", patientUuid, throwable.getMessage());
                    return Optional.empty();
                });
    }

    /**
     * Récupère uniquement les infos de contact.
     * Réutilisabilité maximum grâce au chaînage réactif.
     */
    public CompletableFuture<Optional<PatientInfo>> getPatientContactInfoAsync(String patientUuid) {
        return fetchPatient(patientUuid)
                .map(patient -> {
                    if (patient.getEmail() == null) {
                        log.warn("Patient {} has no email configured", patientUuid);
                        return Optional.<PatientInfo>empty();
                    }
                    return Optional.of(patient);
                })
                .toFuture()
                .exceptionally(ex -> Optional.empty());
    }

    // FALLBACK METHODS

    /**
     * Fallback respectant la signature de getPatientByUuidAsync.
     */
    public CompletableFuture<Optional<PatientInfo>> getPatientByUuidFallback(String patientUuid, Throwable throwable) {
        log.error("Circuit Breaker ouvert / Timeout pour PatientService (uuid: {}). Raison: {}",
                patientUuid, throwable.getMessage());
        return CompletableFuture.completedFuture(Optional.empty());
    }
}