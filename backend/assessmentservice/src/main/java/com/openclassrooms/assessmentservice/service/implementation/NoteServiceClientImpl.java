package com.openclassrooms.assessmentservice.service.implementation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassrooms.assessmentservice.domain.Response;
import com.openclassrooms.assessmentservice.dtoresponse.NoteResponse;
import com.openclassrooms.assessmentservice.exception.ApiException;
import com.openclassrooms.assessmentservice.service.NoteServiceClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * Implémentation réactive du client NotesService.
 * ARCHITECTURE RÉACTIVE:
 * - Retourne Flux<NoteResponse>
 * - Utilise Resilience4j pour Circuit Breaker et Retry
 *
 * @author Kardigué MAGASSA
 * @version 2.0
 * @since 2026-02-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoteServiceClientImpl implements NoteServiceClient {

    private final WebClient notesServiceWebClient;
    private final ObjectMapper objectMapper;

    private static final String CIRCUIT_BREAKER_NAME = "notesService";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    // PUBLIC METHODS

    /**
     * Récupère toutes les notes d'un patient.
     *
     * FLUX:
     * 1. Appel HTTP vers NotesService
     * 2. Gestion des erreurs
     * 3. Extraction de la liste de notes
     * 4. Conversion List → Flux
     */
    @Override
    @Retry(name = CIRCUIT_BREAKER_NAME)
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getNotesByPatientUuidFallback")
    public Flux<NoteResponse> getNotesByPatientUuid(String patientUuid) {
        log.info("Fetching notes from NotesService for patient: {}", patientUuid);

        return notesServiceWebClient.get()
                .uri("/api/notes/patient/{patientUuid}", patientUuid)
                .retrieve()
                // Gestion des erreurs
                .onStatus(HttpStatusCode::is4xxClientError, response -> Mono.error(new ApiException("Erreur client NotesService")))
                .onStatus(HttpStatusCode::is5xxServerError, response -> Mono.error(new ApiException("Erreur serveur NotesService")))
                // Conversion de la réponse
                .bodyToMono(Response.class)
                // Extraire la liste de notes
                .map(this::extractNotes)
                // Convertir List → Flux
                .flatMapMany(Flux::fromIterable)
                // Logging succès
                .doOnComplete(() -> log.debug("Notes fetched successfully for patient: {}", patientUuid))
                // Logging erreur
                .doOnError(error -> log.error("Error fetching notes for patient {}: {}", patientUuid, error.getMessage()))
                // Timeout
                .timeout(TIMEOUT);
    }

    // FALLBACK METHODS

    /**
     * Fallback pour getNotesByPatientUuid.
     * Retourne un Flux vide en cas d'erreur.
     */
    public Flux<NoteResponse> getNotesByPatientUuidFallback(String patientUuid, Throwable throwable) {
        log.error("Fallback getNotesByPatientUuid - UUID: {}, Cause: {}", patientUuid, throwable.getMessage());
        return Flux.empty();
    }

    // PRIVATE METHODS

    /**
     * Extrait la liste de notes depuis la réponse.
     */
    private List<NoteResponse> extractNotes(Response response) {
        try {
            if (response == null || response.data() == null || !response.data().containsKey("notes")) {
                log.debug("No notes found in response");
                return Collections.emptyList();
            }

            Object notesData = response.data().get("notes");
            List<NoteResponse> notes = objectMapper.convertValue(notesData, new TypeReference<List<NoteResponse>>() {});

            log.debug("Extracted {} notes", notes.size());
            return notes;
        } catch (Exception e) {
            log.error("Error extracting notes from response: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}