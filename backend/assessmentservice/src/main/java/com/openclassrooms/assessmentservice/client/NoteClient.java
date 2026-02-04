package com.openclassrooms.assessmentservice.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassrooms.assessmentservice.domain.Response;
import com.openclassrooms.assessmentservice.dtoresponse.NoteResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Client HTTP pour communiquer avec le Notes Service.
 * Utilise Resilience4j Circuit Breaker pour la résilience.
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-09
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NoteClient {

    private final WebClient notesWebClient;
    private final ObjectMapper objectMapper;

    private static final String CIRCUIT_BREAKER_NAME = "notesService";

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getNotesFallback")
    @io.github.resilience4j.retry.annotation.Retry(name = CIRCUIT_BREAKER_NAME)
    @io.github.resilience4j.timelimiter.annotation.TimeLimiter(name = CIRCUIT_BREAKER_NAME)
    public CompletableFuture<List<NoteResponse>> getNotesByPatientUuid(String patientUuid) {
        log.info("Calling Notes Service for patient: {}", patientUuid);

        return CompletableFuture.supplyAsync(() -> {
            Response response = notesWebClient.get()
                    .uri("/api/notes/patient/{patientUuid}", patientUuid)
                    .retrieve()
                    .bodyToMono(Response.class)
                    .block();

            if (response == null || response.data() == null) {
                log.warn("No notes found for patient: {}", patientUuid);
                return Collections.emptyList();
            }

            List<NoteResponse> notes = extractNotes(response);
            log.info("Retrieved {} notes for patient {}", notes.size(), patientUuid);
            return notes;
        });
    }

    public CompletableFuture<List<NoteResponse>> getNotesFallback(String patientUuid, Throwable throwable) {
        log.error("Fallback triggered for Notes Service. Patient {}: {}", patientUuid, throwable.getMessage());
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    private List<NoteResponse> extractNotes(Response response) {
        Object notesData = response.data().get("notes");
        return objectMapper.convertValue(notesData, new TypeReference<List<NoteResponse>>() {});
    }
}
