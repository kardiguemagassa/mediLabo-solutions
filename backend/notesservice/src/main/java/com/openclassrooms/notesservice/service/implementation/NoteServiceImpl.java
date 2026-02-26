package com.openclassrooms.notesservice.service.implementation;

import com.openclassrooms.notesservice.dto.NoteRequest;
import com.openclassrooms.notesservice.dto.NoteResponse;
import com.openclassrooms.notesservice.exception.ApiException;
import com.openclassrooms.notesservice.model.Note;
import com.openclassrooms.notesservice.repository.NoteRepository;
import com.openclassrooms.notesservice.service.NoteService;
import com.openclassrooms.notesservice.service.PatientServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import com.openclassrooms.notesservice.enumeration.EventType;
import com.openclassrooms.notesservice.event.Event;


import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Implémentation réactive du service de gestion des notes.
 *
 * ARCHITECTURE RÉACTIVE:
 * - Mono<T> : Représente 0 ou 1 élément
 * - Flux<T> : Représente 0 à N éléments
 * - Mono.fromCallable() : Encapsule les appels MongoDB bloquants
 * - subscribeOn(Schedulers.boundedElastic()) : Exécute sur thread-pool élastique
 *
 * @author Kardigué MAGASSA
 * @version 2.0
 * @since 2026-02-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoteServiceImpl implements NoteService {

    private final NoteRepository noteRepository;
    private final PatientServiceClient patientServiceClient;
    private final ApplicationEventPublisher eventPublisher;

    //  CREATE

    /**
     * Crée une nouvelle note pour un patient.
     *
     * FLUX:
     * 1. Construction de l'entité Note
     * 2. Sauvegarde en MongoDB (sur boundedElastic)
     * 3. Conversion en NoteResponse
     */

    @Override
    public Mono<NoteResponse> createNote(NoteRequest request, String practitionerUuid, String practitionerName) {
        log.info("Creating note for patient: {} by practitioner: {}", request.getPatientUuid(), practitionerUuid);

        return Mono.fromCallable(() -> {
                    Note note = Note.builder()
                            .noteUuid(UUID.randomUUID().toString())
                            .patientUuid(request.getPatientUuid())
                            .practitionerUuid(practitionerUuid)
                            .practitionerName(practitionerName)
                            .content(request.getContent())
                            .active(true)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();

                    Note savedNote = noteRepository.save(note);
                    log.info("Note created successfully: {}", savedNote.getNoteUuid());

                    return savedNote;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(note -> publishNoteCreatedEvent(note, practitionerName))
                .map(this::toResponse);
    }

    //  READ

    /**
     * Récupère une note par son UUID.
     */
    @Override
    public Mono<NoteResponse> getNoteByUuid(String noteUuid) {
        log.debug("Fetching note: {}", noteUuid);

        return Mono.fromCallable(() -> noteRepository.findByNoteUuidAndActiveTrue(noteUuid))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(optional -> optional
                        .map(note -> Mono.just(toResponse(note)))
                        .orElseGet(() -> Mono.error(new ApiException("Note non trouvée: " + noteUuid))));
    }

    /**
     * Récupère toutes les notes d'un patient.
     */
    @Override
    public Flux<NoteResponse> getNotesByPatientUuid(String patientUuid) {
        log.debug("Fetching notes for patient: {}", patientUuid);

        return Mono.fromCallable(() -> noteRepository.findByPatientUuidAndActiveTrueOrderByCreatedAtDesc(patientUuid))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable)
                .map(this::toResponse);
    }

    /**
     * Récupère toutes les notes créées par un praticien.
     */
    @Override
    public Flux<NoteResponse> getNotesByPractitionerUuid(String practitionerUuid) {
        log.debug("Fetching notes by practitioner: {}", practitionerUuid);

        return Mono.fromCallable(() -> noteRepository.findByPractitionerUuidAndActiveTrueOrderByCreatedAtDesc(practitionerUuid))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable)
                .map(this::toResponse);
    }

    // UPDATE

    /**
     * Met à jour une note existante.
     *
     * FLUX:
     * 1. Récupération de la note existante
     * 2. Vérification des droits (praticien = auteur)
     * 3. Mise à jour du contenu
     * 4. Sauvegarde
     */
    @Override
    public Mono<NoteResponse> updateNote(String noteUuid, NoteRequest request, String practitionerUuid) {
        log.info("Updating note: {}", noteUuid);

        return Mono.fromCallable(() -> noteRepository.findByNoteUuidAndActiveTrue(noteUuid))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(optional -> {
                    if (optional.isEmpty()) {
                        return Mono.error(new ApiException("Note non trouvée: " + noteUuid));
                    }

                    Note existingNote = optional.get();

                    if (!existingNote.getPractitionerUuid().equals(practitionerUuid)) {
                        return Mono.error(new ApiException("Vous n'êtes pas autorisé à modifier cette note"));
                    }

                    existingNote.setContent(request.getContent());
                    existingNote.setUpdatedAt(LocalDateTime.now());

                    return Mono.fromCallable(() -> {
                                Note updatedNote = noteRepository.save(existingNote);
                                log.info("Note updated successfully: {}", noteUuid);
                                return updatedNote;
                            })
                            .subscribeOn(Schedulers.boundedElastic())
                            .doOnSuccess(this::publishNoteUpdatedEvent)
                            .map(this::toResponse);
                });
    }


    // DELETE

    /**
     * Supprime une note (soft delete).
     */
    @Override
    public Mono<Void> deleteNote(String noteUuid) {
        log.info("Soft deleting note: {}", noteUuid);

        return Mono.fromCallable(() -> noteRepository.findByNoteUuidAndActiveTrue(noteUuid))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(optional -> {
                    if (optional.isEmpty()) {
                        return Mono.error(new ApiException("Note non trouvée: " + noteUuid));
                    }

                    Note note = optional.get();
                    note.setActive(false);
                    note.setUpdatedAt(LocalDateTime.now());

                    return Mono.fromCallable(() -> {
                                noteRepository.save(note);
                                log.info("Note deleted successfully: {}", noteUuid);
                                return note;
                            })
                            .subscribeOn(Schedulers.boundedElastic());
                })
                .then();
    }

    //COUNT

    /**
     * Compte le nombre de notes pour un patient.
     */
    @Override
    public Mono<Long> countNotesByPatientUuid(String patientUuid) {
        log.debug("Counting notes for patient: {}", patientUuid);

        return Mono.fromCallable(() -> noteRepository.countByPatientUuidAndActiveTrue(patientUuid))
                .subscribeOn(Schedulers.boundedElastic());
    }

    //  EVENT PUBLISHING

    private void publishNoteCreatedEvent(Note note, String practitionerName) {
        patientServiceClient.getPatientContactInfo(note.getPatientUuid())
                .subscribe(
                        patient -> {
                            Map<String, Object> data = new HashMap<>();
                            data.put("name", patient != null ? patient.getFullName() : null);
                            data.put("email", patient != null ? patient.getEmail() : null);
                            data.put("patientNumber", note.getPatientUuid());
                            data.put("doctorName", practitionerName);
                            data.put("department", "Médecine générale");
                            data.put("date", LocalDateTime.now().toString());
                            data.put("notePreview", truncateContent(note.getContent(), 100));

                            Event event = Event.builder()
                                    .eventType(EventType.NOTE_CREATED)
                                    .data(data)
                                    .build();
                            eventPublisher.publishEvent(event);
                            log.debug("NOTE_CREATED event published for note: {}", note.getNoteUuid());
                        },
                        error -> log.error("Erreur lors de la récupération patient pour event: {}", error.getMessage()),
                        () -> log.debug("No patient info found for event, skipping notification")
                );
    }

    private void publishNoteUpdatedEvent(Note note) {
        patientServiceClient.getPatientContactInfo(note.getPatientUuid())
                .subscribe(
                        patient -> {
                            Map<String, Object> data = new HashMap<>();
                            data.put("name", patient != null ? patient.getFullName() : null);
                            data.put("email", patient != null ? patient.getEmail() : null);
                            data.put("patientNumber", note.getPatientUuid());
                            data.put("doctorName", note.getPractitionerName());
                            data.put("date", LocalDateTime.now().toString());
                            data.put("notePreview", truncateContent(note.getContent(), 100));

                            Event event = Event.builder()
                                    .eventType(EventType.NOTE_UPDATED)
                                    .data(data)
                                    .build();
                            eventPublisher.publishEvent(event);
                            log.debug("NOTE_UPDATED event published for note: {}", note.getNoteUuid());
                        },
                        error -> log.error("Erreur lors de la récupération patient pour event: {}", error.getMessage()),
                        () -> log.debug("No patient info found for event, skipping notification")
                );
    }

    // PRIVATE METHODS

    /**
     * Convertit une entité Note en NoteResponse.
     */
    private NoteResponse toResponse(Note note) {
        return NoteResponse.builder()
                .noteUuid(note.getNoteUuid())
                .patientUuid(note.getPatientUuid())
                .practitionerUuid(note.getPractitionerUuid())
                .practitionerName(note.getPractitionerName())
                .content(note.getContent())
                .createdAt(note.getCreatedAt())
                .updatedAt(note.getUpdatedAt())
                .fileCount(note.getFileCount())
                .commentCount(note.getCommentCount())
                .build();
    }

    private String truncateContent(String content, int maxLength) {
        if (content == null) return "";
        if (content.length() <= maxLength) return content;
        return content.substring(0, maxLength - 3) + "...";
    }
}