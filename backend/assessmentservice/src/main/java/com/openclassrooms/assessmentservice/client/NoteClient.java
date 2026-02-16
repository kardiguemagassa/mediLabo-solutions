package com.openclassrooms.assessmentservice.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassrooms.assessmentservice.domain.Response;
import com.openclassrooms.assessmentservice.dtoresponse.NoteResponse;
import com.openclassrooms.assessmentservice.exception.ApiException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Client HTTP réactif pour communiquer avec le Notes Service.
 * Implémentation asynchrone avec gestion de la résilience.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NoteClient {

    private final WebClient notesWebClient;
    private final ObjectMapper objectMapper;
    private static final String CIRCUIT_BREAKER_NAME = "notesService";

    /**
     * Logique de récupération brute via WebClient.
     */
    private Mono<List<NoteResponse>> fetchNotes(String patientUuid) {
        log.debug("Fetching notes for patient: {}", patientUuid);

        return notesWebClient.get()
                .uri("/api/notes/patient/{patientUuid}", patientUuid)
                .retrieve()
                .onStatus(HttpStatusCode::isError,
                        resp -> Mono.error(new ApiException("Erreur lors de la récupération des notes pour le patient: " + patientUuid)))
                .bodyToMono(Response.class)
                .map(this::extractNotesFromResponse);
    }

    /**
     * Méthode principale asynchrone utilisée par la couche Service.
     * Protégée par Circuit Breaker, Retry et TimeLimiter.
     */
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getNotesFallback")
    @Retry(name = CIRCUIT_BREAKER_NAME)
    @TimeLimiter(name = CIRCUIT_BREAKER_NAME)
    public CompletableFuture<List<NoteResponse>> getNotesByPatientUuidAsync(String patientUuid) {
        log.info("Appel résilient vers Notes Service pour le patient {}", patientUuid);

        return fetchNotes(patientUuid)
                .toFuture()
                .exceptionally(throwable -> {
                    log.error("Échec critique lors de l'appel Notes Service : {}", throwable.getMessage());
                    return Collections.emptyList();
                });
    }

    // FALLBACK & UTILS

    /**
     * Fallback en cas d'ouverture du circuit ou d'erreur persistante.
     */
    public CompletableFuture<List<NoteResponse>> getNotesFallback(String patientUuid, Throwable throwable) {
        log.warn("Fallback activé pour Notes Service. Circuit ouvert ou Timeout. Patient: {}", patientUuid);
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    /**
     * Désérialisation propre des données imbriquées.
     */
    private List<NoteResponse> extractNotesFromResponse(Response response) {
        if (response == null || response.data() == null || !response.data().containsKey("notes")) {
            return Collections.emptyList();
        }
        Object notesData = response.data().get("notes");
        return objectMapper.convertValue(notesData, new TypeReference<List<NoteResponse>>() {});
    }
}