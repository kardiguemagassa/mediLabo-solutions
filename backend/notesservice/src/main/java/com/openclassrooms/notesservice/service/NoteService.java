package com.openclassrooms.notesservice.service;

import com.openclassrooms.notesservice.dto.NoteRequest;
import com.openclassrooms.notesservice.dto.NoteResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service de gestion des notes médicales (Full Réactif).
 *
 * @author Kardigué MAGASSA
 * @version 2.0
 * @since 2026-02-25
 */
public interface NoteService {
    Flux<NoteResponse> getAllActiveNotes();
    Flux<NoteResponse> getNotesByUserUuid(String userUuid);
    Mono<NoteResponse> createNote(NoteRequest request, String practitionerUuid, String practitionerName);
    Mono<NoteResponse> getNoteByUuid(String noteUuid);
    Flux<NoteResponse> getNotesByPatientUuid(String patientUuid);
    Flux<NoteResponse> getNotesByPractitionerUuid(String practitionerUuid);
    Mono<NoteResponse> updateNote(String noteUuid, NoteRequest request, String practitionerUuid);
    Mono<Void> deleteNote(String noteUuid);
    Mono<Long> countNotesByPatientUuid(String patientUuid);
}