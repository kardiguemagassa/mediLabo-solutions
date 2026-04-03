package com.openclassrooms.assessmentservice.service.implementation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassrooms.assessmentservice.domain.Response;
import com.openclassrooms.assessmentservice.dtoresponse.NoteResponseDTO;
import com.openclassrooms.assessmentservice.exception.ApiException;
import com.openclassrooms.assessmentservice.service.NoteServiceClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoteServiceClientImpl implements NoteServiceClient {

    private final WebClient notesServiceWebClient;
    private final ObjectMapper objectMapper;

    private static final String CIRCUIT_BREAKER_NAME = "notesService";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    @Override
    @Retry(name = CIRCUIT_BREAKER_NAME)
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getNotesByPatientUuidFallback")
    public Flux<NoteResponseDTO> getNotesByPatientUuid(String patientUuid, String token) {
        log.info("Fetching notes from NotesService for patient: {}", patientUuid);

        return notesServiceWebClient.get()
                .uri("/api/notes/patient/{patientUuid}", patientUuid)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        response -> Mono.error(new ApiException("Erreur client NotesService")))
                .onStatus(HttpStatusCode::is5xxServerError,
                        response -> Mono.error(new ApiException("Erreur serveur NotesService")))
                .bodyToMono(Response.class)
                .map(this::extractNotes)
                .flatMapMany(Flux::fromIterable)
                .doOnComplete(() -> log.debug("Notes fetched for patient: {}", patientUuid))
                .doOnError(error -> log.error("Error fetching notes for patient {}: {}", patientUuid, error.getMessage()))
                .timeout(TIMEOUT);
    }

    public Flux<NoteResponseDTO> getNotesByPatientUuidFallback(String patientUuid, String token, Throwable throwable) {
        log.error("Fallback getNotesByPatientUuid - UUID: {}, Cause: {}", patientUuid, throwable.getMessage());
        return Flux.empty();
    }

    private List<NoteResponseDTO> extractNotes(Response response) {
        try {
            if (response == null || response.data() == null || !response.data().containsKey("notes")) {
                return Collections.emptyList();
            }
            Object notesData = response.data().get("notes");
            List<NoteResponseDTO> notes = objectMapper.convertValue(notesData, new TypeReference<>() {});
            log.debug("Extracted {} notes", notes.size());
            return notes;
        } catch (Exception e) {
            log.error("Error extracting notes from response: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}