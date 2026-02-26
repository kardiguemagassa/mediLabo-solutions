package com.openclassrooms.notesservice.service;

import com.openclassrooms.notesservice.dto.NoteRequest;
import com.openclassrooms.notesservice.dto.NoteResponse;
import com.openclassrooms.notesservice.exception.ApiException;
import com.openclassrooms.notesservice.model.Note;
import com.openclassrooms.notesservice.repository.NoteRepository;
import com.openclassrooms.notesservice.service.implementation.NoteServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NoteServiceImpl Reactive Unit Tests")
class NoteServiceImplReactiveTest {

    @Mock
    private NoteRepository noteRepository;

    @Mock
    private PatientServiceClient patientServiceClient;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private NoteServiceImpl noteService;

    private Note testNote;
    private NoteRequest testRequest;
    private final String PATIENT_UUID = "patient-uuid-123";
    private final String PRACTITIONER_UUID = "practitioner-uuid-456";
    private final String PRACTITIONER_NAME = "Dr. Jean Dupont";
    private final String NOTE_UUID = "note-uuid-789";

    @BeforeEach
    void setUp() {
        testNote = Note.builder()
                .id("mongo-id-123")
                .noteUuid(NOTE_UUID)
                .patientUuid(PATIENT_UUID)
                .practitionerUuid(PRACTITIONER_UUID)
                .practitionerName(PRACTITIONER_NAME)
                .content("Patient présente des symptômes de diabète. Taux de cholestérol élevé.")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testRequest = NoteRequest.builder()
                .patientUuid(PATIENT_UUID)
                .content(testNote.getContent())
                .build();
    }

    @Nested
    @DisplayName("createNote() Tests")
    class CreateNoteTests {

        @Test
        void shouldCreateNoteSuccessfully() {
            when(noteRepository.save(any(Note.class))).thenReturn(testNote);

            StepVerifier.create(noteService.createNote(testRequest, PRACTITIONER_UUID, PRACTITIONER_NAME))
                    .expectNextMatches(resp ->
                            resp.getNoteUuid().equals(NOTE_UUID) &&
                                    resp.getPatientUuid().equals(PATIENT_UUID) &&
                                    resp.getPractitionerUuid().equals(PRACTITIONER_UUID))
                    .verifyComplete();

            verify(noteRepository).save(any(Note.class));
        }
    }

    @Nested
    @DisplayName("getNoteByUuid() Tests")
    class GetNoteByUuidTests {

        @Test
        void shouldReturnNoteWhenFound() {
            when(noteRepository.findByNoteUuidAndActiveTrue(NOTE_UUID)).thenReturn(Optional.of(testNote));

            StepVerifier.create(noteService.getNoteByUuid(NOTE_UUID))
                    .expectNextMatches(resp -> resp.getNoteUuid().equals(NOTE_UUID))
                    .verifyComplete();
        }

        @Test
        void shouldReturnErrorWhenNoteNotFound() {
            when(noteRepository.findByNoteUuidAndActiveTrue(anyString())).thenReturn(Optional.empty());

            StepVerifier.create(noteService.getNoteByUuid("unknown-uuid"))
                    .expectErrorMatches(err -> err instanceof ApiException &&
                            err.getMessage().contains("Note non trouvée"))
                    .verify();
        }
    }

    @Nested
    @DisplayName("getNotesByPatientUuid() Tests")
    class GetNotesByPatientUuidTests {

        @Test
        void shouldReturnNotesForPatient() {
            Note note2 = testNote.toBuilder()
                    .noteUuid("note-uuid-002")
                    .content("Consultation de suivi")
                    .build();

            when(noteRepository.findByPatientUuidAndActiveTrueOrderByCreatedAtDesc(PATIENT_UUID))
                    .thenReturn(List.of(testNote, note2));

            StepVerifier.create(noteService.getNotesByPatientUuid(PATIENT_UUID))
                    .expectNextCount(2)
                    .verifyComplete();
        }

        @Test
        void shouldReturnEmptyFluxWhenNoNotes() {
            when(noteRepository.findByPatientUuidAndActiveTrueOrderByCreatedAtDesc(anyString()))
                    .thenReturn(List.of());

            StepVerifier.create(noteService.getNotesByPatientUuid("unknown-patient"))
                    .expectNextCount(0)
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("updateNote() Tests")
    class UpdateNoteTests {

        @Test
        void shouldUpdateNoteWhenAuthorized() {
            NoteRequest updateRequest = NoteRequest.builder()
                    .patientUuid(PATIENT_UUID)
                    .content("Contenu mis à jour")
                    .build();

            Note updatedNote = testNote.toBuilder().content("Contenu mis à jour").build();

            when(noteRepository.findByNoteUuidAndActiveTrue(NOTE_UUID)).thenReturn(Optional.of(testNote));
            when(noteRepository.save(any(Note.class))).thenReturn(updatedNote);

            StepVerifier.create(noteService.updateNote(NOTE_UUID, updateRequest, PRACTITIONER_UUID))
                    .expectNextMatches(resp -> resp.getContent().equals("Contenu mis à jour"))
                    .verifyComplete();

            verify(noteRepository).save(any(Note.class));
        }

        @Test
        void shouldErrorWhenUnauthorizedPractitioner() {
            when(noteRepository.findByNoteUuidAndActiveTrue(NOTE_UUID)).thenReturn(Optional.of(testNote));

            StepVerifier.create(noteService.updateNote(NOTE_UUID, testRequest, "other-practitioner"))
                    .expectErrorMatches(err -> err instanceof ApiException &&
                            err.getMessage().contains("Vous n'êtes pas autorisé"))
                    .verify();
        }

        @Test
        void shouldErrorWhenNoteNotFound() {
            when(noteRepository.findByNoteUuidAndActiveTrue(anyString())).thenReturn(Optional.empty());

            StepVerifier.create(noteService.updateNote("unknown-uuid", testRequest, PRACTITIONER_UUID))
                    .expectErrorMatches(err -> err instanceof ApiException &&
                            err.getMessage().contains("Note non trouvée"))
                    .verify();
        }
    }

    @Nested
    @DisplayName("deleteNote() Tests")
    class DeleteNoteTests {

        @Test
        void shouldSoftDeleteNote() {
            when(noteRepository.findByNoteUuidAndActiveTrue(NOTE_UUID)).thenReturn(Optional.of(testNote));
            when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> {
                Note saved = invocation.getArgument(0);
                assertThat(saved.getActive()).isFalse();
                return saved;
            });

            StepVerifier.create(noteService.deleteNote(NOTE_UUID))
                    .verifyComplete();

            verify(noteRepository).save(any(Note.class));
        }

        @Test
        void shouldErrorWhenNoteNotFound() {
            when(noteRepository.findByNoteUuidAndActiveTrue(anyString())).thenReturn(Optional.empty());

            StepVerifier.create(noteService.deleteNote("unknown-uuid"))
                    .expectErrorMatches(err -> err instanceof ApiException &&
                            err.getMessage().contains("Note non trouvée"))
                    .verify();
        }
    }

    @Nested
    @DisplayName("countNotesByPatientUuid() Tests")
    class CountNotesTests {

        @Test
        void shouldReturnNoteCount() {
            when(noteRepository.countByPatientUuidAndActiveTrue(PATIENT_UUID)).thenReturn(5L);

            StepVerifier.create(noteService.countNotesByPatientUuid(PATIENT_UUID))
                    .expectNext(5L)
                    .verifyComplete();
        }
    }
}