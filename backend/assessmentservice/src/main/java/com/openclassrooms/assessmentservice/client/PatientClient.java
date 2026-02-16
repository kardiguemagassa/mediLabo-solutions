package com.openclassrooms.assessmentservice.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassrooms.assessmentservice.domain.Response;
import com.openclassrooms.assessmentservice.dtoresponse.PatientResponse;
import com.openclassrooms.assessmentservice.exception.ApiException;
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

/**
 * Client HTTP réactif pour communiquer avec le Patient Service.
 * Implémentation asynchrone optimisée avec Resilience4j.
 *
 * @author Kardigué MAGASSA
 * @version 1.1
 * @since 2026-02-09
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PatientClient {

    private final WebClient patientWebClient;
    private final ObjectMapper objectMapper;

    private static final String CIRCUIT_BREAKER_NAME = "patientService";

    /**
     * Récupère les informations d'un patient par son UUID de manière asynchrone.
     * Cette méthode est protégée par Circuit Breaker, Retry et TimeLimiter.
     */
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getPatientFallback")
    @Retry(name = CIRCUIT_BREAKER_NAME)
    @TimeLimiter(name = CIRCUIT_BREAKER_NAME)
    public CompletableFuture<Optional<PatientResponse>> getPatientByUuid(String patientUuid) {
        log.info("Calling Patient Service for UUID: {}", patientUuid);

        return patientWebClient.get()
                .uri("/api/patients/{patientUuid}", patientUuid)
                .retrieve()
                // Gestion propre du 404 : on retourne un Mono vide au lieu de lever une exception
                .onStatus(status -> status == HttpStatus.NOT_FOUND,
                        resp -> {
                            log.warn("Patient not found in remote service: {}", patientUuid);
                            return Mono.empty();
                        })
                // Gestion des autres erreurs (5xx, etc.)
                .onStatus(HttpStatusCode::isError,
                        resp -> Mono.error(new ApiException("Erreur technique lors de l'appel au Patient Service")))
                .bodyToMono(Response.class)
                .map(response -> {
                    if (response == null || response.data() == null || !response.data().containsKey("patient")) {
                        return Optional.<PatientResponse>empty();
                    }
                    return Optional.of(extractPatient(response));
                })
                // Valeur par défaut si le Mono est vide (cas du 404)
                .defaultIfEmpty(Optional.empty())
                // Conversion en futur sans bloquer de thread
                .toFuture()
                .exceptionally(throwable -> {
                    log.error("Échec critique lors de l'appel Patient Service : {}", throwable.getMessage());
                    return Optional.empty();
                });
    }

    /**
     * Fallback exécuté si le Circuit Breaker est ouvert ou si toutes les tentatives de Retry échouent.
     */
    public CompletableFuture<Optional<PatientResponse>> getPatientFallback(String patientUuid, Throwable throwable) {
        log.warn("Fallback activé pour Patient Service (UUID: {}). Raison: {}", patientUuid, throwable.getMessage());
        return CompletableFuture.completedFuture(Optional.empty());
    }

    /**
     * Désérialisation sécurisée des données du patient.
     */
    private PatientResponse extractPatient(Response response) {
        Object patientData = response.data().get("patient");
        return objectMapper.convertValue(patientData, PatientResponse.class);
    }
}