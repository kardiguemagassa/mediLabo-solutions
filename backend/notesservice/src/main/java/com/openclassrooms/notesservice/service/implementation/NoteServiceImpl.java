package com.openclassrooms.notesservice.service.implementation;

import com.openclassrooms.notesservice.model.Note;

import com.openclassrooms.notesservice.dto.NoteRequest;
import com.openclassrooms.notesservice.dto.NoteResponse;
import com.openclassrooms.notesservice.exception.ApiException;
import com.openclassrooms.notesservice.repository.NoteRepository;
import com.openclassrooms.notesservice.service.NoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Implémentation du service de gestion des notes
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-02
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class NoteServiceImpl implements NoteService {

    private final NoteRepository noteRepository;

    @Override
    public NoteResponse createNote(NoteRequest request, String practitionerUuid, String practitionerName) {
        log.info("Creating note for patient: {} by practitioner: {}", request.getPatientUuid(), practitionerUuid);

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

        return toResponse(savedNote);
    }

    @Override
    public NoteResponse getNoteByUuid(String noteUuid) {
        log.debug("Fetching note: {}", noteUuid);

        Note note = noteRepository.findByNoteUuidAndActiveTrue(noteUuid)
                .orElseThrow(() -> new ApiException("Note non trouvée: " + noteUuid));

        return toResponse(note);
    }

    @Override
    public List<NoteResponse> getNotesByPatientUuid(String patientUuid) {
        log.debug("Fetching notes for patient: {}", patientUuid);
        List<Note> notes = noteRepository.findByPatientUuidAndActiveTrueOrderByCreatedAtDesc(patientUuid);
        return notes.stream().map(this::toResponse).toList();
    }

    @Override
    public List<NoteResponse> getNotesByPractitionerUuid(String practitionerUuid) {
        log.debug("Fetching notes by practitioner: {}", practitionerUuid);
        List<Note> notes = noteRepository.findByPractitionerUuidAndActiveTrueOrderByCreatedAtDesc(practitionerUuid);
        return notes.stream().map(this::toResponse).toList();
    }

    @Override
    public NoteResponse updateNote(String noteUuid, NoteRequest request, String practitionerUuid) {
        log.info("Updating note: {}", noteUuid);

        Note existingNote = noteRepository.findByNoteUuidAndActiveTrue(noteUuid)
                .orElseThrow(() -> new ApiException("Note non trouvée: " + noteUuid));

        // Vérifier que le praticien est bien l'auteur de la note
        if (!existingNote.getPractitionerUuid().equals(practitionerUuid)) {
            throw new ApiException("Vous n'êtes pas autorisé à modifier cette note");
        }

        existingNote.setContent(request.getContent());
        existingNote.setUpdatedAt(LocalDateTime.now());

        Note updatedNote = noteRepository.save(existingNote);
        log.info("Note updated successfully: {}", noteUuid);

        return toResponse(updatedNote);
    }

    @Override
    public void deleteNote(String noteUuid) {
        log.info("Soft deleting note: {}", noteUuid);

        Note note = noteRepository.findByNoteUuidAndActiveTrue(noteUuid)
                .orElseThrow(() -> new ApiException("Note non trouvée: " + noteUuid));

        note.setActive(false);
        note.setUpdatedAt(LocalDateTime.now());

        noteRepository.save(note);
        log.info("Note deleted successfully: {}", noteUuid);
    }

    @Override
    public long countNotesByPatientUuid(String patientUuid) {
        return noteRepository.countByPatientUuidAndActiveTrue(patientUuid);
    }

    // PRIVATE METHODS

    private NoteResponse toResponse(Note note) {
        return NoteResponse.builder()
                .noteUuid(note.getNoteUuid())
                .patientUuid(note.getPatientUuid())
                .practitionerUuid(note.getPractitionerUuid())
                .practitionerName(note.getPractitionerName())
                .content(note.getContent())
                .createdAt(note.getCreatedAt())
                .updatedAt(note.getUpdatedAt())
                .build();
    }
}