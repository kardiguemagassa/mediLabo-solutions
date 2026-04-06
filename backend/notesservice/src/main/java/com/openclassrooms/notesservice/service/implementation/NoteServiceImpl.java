package com.openclassrooms.notesservice.service.implementation;

import com.openclassrooms.notesservice.dto.NoteRequest;
import com.openclassrooms.notesservice.dto.NoteResponse;
import com.openclassrooms.notesservice.enumeration.EventType;
import com.openclassrooms.notesservice.event.Event;
import com.openclassrooms.notesservice.exception.ApiException;
import com.openclassrooms.notesservice.mapper.NoteMapper;
import com.openclassrooms.notesservice.model.Note;
import com.openclassrooms.notesservice.repository.NoteRepository;
import com.openclassrooms.notesservice.service.NoteService;
import com.openclassrooms.notesservice.service.PatientServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoteServiceImpl implements NoteService {

    private final NoteRepository noteRepository;
    private final NoteMapper noteMapper;
    private final PatientServiceClient patientServiceClient;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Flux<NoteResponse> getAllActiveNotes() {
        return Mono.fromCallable(noteRepository::findByActiveTrueOrderByCreatedAtDesc)
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable)
                .map(noteMapper::toResponse);
    }

    @Override
    public Mono<Page<NoteResponse>> getAllActiveNotesPageable(Pageable pageable) {
        return Mono.fromCallable(() -> noteRepository.findByActiveTrueOrderByCreatedAtDesc(pageable))
                .subscribeOn(Schedulers.boundedElastic())
                .map(page -> page.map(noteMapper::toResponse));
    }

    @Override
    public Flux<NoteResponse> getNotesByUserUuid(String userUuid) {
        log.debug("Fetching notes for userUuid: {}", userUuid);
        return patientServiceClient.getMyPatient()
                .flatMapMany(patient -> getNotesByPatientUuid(patient.getPatientUuid()));
    }

    @Override
    public Mono<NoteResponse> createNote(NoteRequest request, String practitionerUuid, String practitionerName) {
        log.info("Creating note for patient: {} by practitioner: {}", request.getPatientUuid(), practitionerUuid);

        return Mono.fromCallable(() -> {
                    Note note = noteMapper.toEntity(request, practitionerUuid, practitionerName);
                    return noteRepository.save(note);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(note -> publishNoteCreatedEvent(note, practitionerName))
                .map(noteMapper::toResponse);
    }

    @Override
    public Mono<NoteResponse> getNoteByUuid(String noteUuid) {
        log.debug("Fetching note: {}", noteUuid);

        return Mono.fromCallable(() -> noteRepository.findByNoteUuidAndActiveTrue(noteUuid))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(optional -> optional
                        .map(note -> Mono.just(noteMapper.toResponse(note)))
                        .orElseGet(() -> Mono.error(new ApiException("Note non trouvée: " + noteUuid))));
    }

    @Override
    public Flux<NoteResponse> getNotesByPatientUuid(String patientUuid) {
        log.debug("Fetching notes for patient: {}", patientUuid);

        return Mono.fromCallable(() -> noteRepository.findByPatientUuidAndActiveTrueOrderByCreatedAtDesc(patientUuid))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable)
                .map(noteMapper::toResponse);
    }

    @Override
    public Mono<Page<NoteResponse>> getNotesByPatientUuidPageable(String patientUuid, Pageable pageable) {
        log.debug("Fetching notes page for patient: {}", patientUuid);

        return Mono.fromCallable(() -> noteRepository.findByPatientUuidAndActiveTrueOrderByCreatedAtDesc(patientUuid, pageable))
                .subscribeOn(Schedulers.boundedElastic())
                .map(page -> page.map(noteMapper::toResponse));
    }

    @Override
    public Flux<NoteResponse> getNotesByPractitionerUuid(String practitionerUuid) {
        log.debug("Fetching notes by practitioner: {}", practitionerUuid);

        return Mono.fromCallable(() -> noteRepository.findByPractitionerUuidAndActiveTrueOrderByCreatedAtDesc(practitionerUuid))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable)
                .map(noteMapper::toResponse);
    }

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
                    existingNote.setContent(request.getContent());
                    existingNote.setUpdatedAt(LocalDateTime.now());

                    return Mono.fromCallable(() -> noteRepository.save(existingNote))
                            .subscribeOn(Schedulers.boundedElastic())
                            .doOnSuccess(this::publishNoteUpdatedEvent)
                            .map(noteMapper::toResponse);
                });
    }

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

                    return Mono.fromCallable(() -> noteRepository.save(note))
                            .subscribeOn(Schedulers.boundedElastic());
                })
                .then();
    }

    @Override
    public Mono<Long> countNotesByPatientUuid(String patientUuid) {
        return Mono.fromCallable(() -> noteRepository.countByPatientUuidAndActiveTrue(patientUuid))
                .subscribeOn(Schedulers.boundedElastic());
    }

    // EVENT PUBLISHING

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
                            data.put("notePreview", truncate(note.getContent(), 100));

                            eventPublisher.publishEvent(Event.builder()
                                    .eventType(EventType.NOTE_CREATED).data(data).build());
                        },
                        error -> log.error("Error publishing NOTE_CREATED: {}", error.getMessage())
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
                            data.put("notePreview", truncate(note.getContent(), 100));

                            eventPublisher.publishEvent(Event.builder()
                                    .eventType(EventType.NOTE_UPDATED).data(data).build());
                        },
                        error -> log.error("Error publishing NOTE_UPDATED: {}", error.getMessage())
                );
    }

    private String truncate(String content, int max) {
        if (content == null) return "";
        return content.length() <= max ? content : content.substring(0, max - 3) + "...";
    }
}