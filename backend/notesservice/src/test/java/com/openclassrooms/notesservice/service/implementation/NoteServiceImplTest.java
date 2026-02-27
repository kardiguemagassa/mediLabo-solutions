package com.openclassrooms.notesservice.service.implementation;

import com.openclassrooms.notesservice.dto.NoteRequest;
import com.openclassrooms.notesservice.dto.PatientInfo;
import com.openclassrooms.notesservice.exception.ApiException;
import com.openclassrooms.notesservice.model.Note;
import com.openclassrooms.notesservice.repository.NoteRepository;
import com.openclassrooms.notesservice.service.PatientServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NoteServiceImpl Reactive Unit Tests")
class NoteServiceImplTest {

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
    private PatientInfo patientInfo;

    private static final String PATIENT_UUID = "patient-uuid-123";
    private static final String PRACTITIONER_UUID = "practitioner-uuid-456";
    private static final String PRACTITIONER_NAME = "Dr. Jean Dupont";
    private static final String NOTE_UUID = "note-uuid-789";

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


        patientInfo = PatientInfo.builder()
                .patientUuid(PATIENT_UUID)
                .userInfo(new PatientInfo.UserInfo(
                        "Jean",
                        "Martin",
                        "jean.martin@email.com",
                        null,
                        null,
                        null
                )).build();
    }

    /**
     * Configure le mock PatientServiceClient pour les événements
     */
    private void setupPatientServiceClientMock() {
        when(patientServiceClient.getPatientContactInfo(anyString()))
                .thenReturn(Mono.just(patientInfo));
    }

    @Nested
    @DisplayName("createNote() Tests")
    class CreateNoteTests {

        @Test
        @DisplayName("Should create note successfully")
        void shouldCreateNoteSuccessfully() {
            // Given
            setupPatientServiceClientMock();
            when(noteRepository.save(any(Note.class))).thenReturn(testNote);

            // When & Then
            StepVerifier.create(noteService.createNote(testRequest, PRACTITIONER_UUID, PRACTITIONER_NAME))
                    .expectNextMatches(resp ->
                            resp.getNoteUuid().equals(NOTE_UUID) &&
                                    resp.getPatientUuid().equals(PATIENT_UUID) &&
                                    resp.getPractitionerUuid().equals(PRACTITIONER_UUID))
                    .verifyComplete();

            verify(noteRepository).save(any(Note.class));
        }

        @Test
        @DisplayName("Should create note even when patient service fails")
        void shouldCreateNoteWhenPatientServiceFails() {
            // Given
            when(patientServiceClient.getPatientContactInfo(anyString()))
                    .thenReturn(Mono.empty());
            when(noteRepository.save(any(Note.class))).thenReturn(testNote);

            // When & Then
            StepVerifier.create(noteService.createNote(testRequest, PRACTITIONER_UUID, PRACTITIONER_NAME))
                    .expectNextMatches(resp -> resp.getNoteUuid().equals(NOTE_UUID))
                    .verifyComplete();

            verify(noteRepository).save(any(Note.class));
        }
    }

    @Nested
    @DisplayName("getNoteByUuid() Tests")
    class GetNoteByUuidTests {

        @Test
        @DisplayName("Should return note when found")
        void shouldReturnNoteWhenFound() {
            // Given
            when(noteRepository.findByNoteUuidAndActiveTrue(NOTE_UUID))
                    .thenReturn(Optional.of(testNote));

            // When & Then
            StepVerifier.create(noteService.getNoteByUuid(NOTE_UUID))
                    .expectNextMatches(resp -> resp.getNoteUuid().equals(NOTE_UUID))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should return error when note not found")
        void shouldReturnErrorWhenNoteNotFound() {
            // Given
            when(noteRepository.findByNoteUuidAndActiveTrue(anyString()))
                    .thenReturn(Optional.empty());

            // When & Then
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
        @DisplayName("Should return notes for patient")
        void shouldReturnNotesForPatient() {
            // Given
            Note note2 = testNote.toBuilder()
                    .noteUuid("note-uuid-002")
                    .content("Consultation de suivi")
                    .build();

            when(noteRepository.findByPatientUuidAndActiveTrueOrderByCreatedAtDesc(PATIENT_UUID))
                    .thenReturn(List.of(testNote, note2));

            // When & Then
            StepVerifier.create(noteService.getNotesByPatientUuid(PATIENT_UUID))
                    .expectNextCount(2)
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should return empty flux when no notes")
        void shouldReturnEmptyFluxWhenNoNotes() {
            // Given
            when(noteRepository.findByPatientUuidAndActiveTrueOrderByCreatedAtDesc(anyString()))
                    .thenReturn(List.of());

            // When & Then
            StepVerifier.create(noteService.getNotesByPatientUuid("unknown-patient"))
                    .expectNextCount(0)
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("getNotesByPractitionerUuid() Tests")
    class GetNotesByPractitionerUuidTests {

        @Test
        @DisplayName("Should return notes by practitioner")
        void shouldReturnNotesByPractitioner() {
            // Given
            when(noteRepository.findByPractitionerUuidAndActiveTrueOrderByCreatedAtDesc(PRACTITIONER_UUID))
                    .thenReturn(List.of(testNote));

            // When & Then
            StepVerifier.create(noteService.getNotesByPractitionerUuid(PRACTITIONER_UUID))
                    .expectNextMatches(resp -> resp.getPractitionerUuid().equals(PRACTITIONER_UUID))
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("updateNote() Tests")
    class UpdateNoteTests {

        @Test
        @DisplayName("Should update note when authorized")
        void shouldUpdateNoteWhenAuthorized() {
            // Given
            setupPatientServiceClientMock();
            NoteRequest updateRequest = NoteRequest.builder()
                    .patientUuid(PATIENT_UUID)
                    .content("Contenu mis à jour")
                    .build();

            Note updatedNote = testNote.toBuilder().content("Contenu mis à jour").build();

            when(noteRepository.findByNoteUuidAndActiveTrue(NOTE_UUID))
                    .thenReturn(Optional.of(testNote));
            when(noteRepository.save(any(Note.class))).thenReturn(updatedNote);

            // When & Then
            StepVerifier.create(noteService.updateNote(NOTE_UUID, updateRequest, PRACTITIONER_UUID))
                    .expectNextMatches(resp -> resp.getContent().equals("Contenu mis à jour"))
                    .verifyComplete();

            verify(noteRepository).save(any(Note.class));
        }

        @Test
        @DisplayName("Should error when unauthorized practitioner")
        void shouldErrorWhenUnauthorizedPractitioner() {
            // Given
            when(noteRepository.findByNoteUuidAndActiveTrue(NOTE_UUID))
                    .thenReturn(Optional.of(testNote));

            // When & Then
            StepVerifier.create(noteService.updateNote(NOTE_UUID, testRequest, "other-practitioner"))
                    .expectErrorMatches(err -> err instanceof ApiException &&
                            err.getMessage().contains("Vous n'êtes pas autorisé"))
                    .verify();
        }

        @Test
        @DisplayName("Should error when note not found")
        void shouldErrorWhenNoteNotFound() {
            // Given
            when(noteRepository.findByNoteUuidAndActiveTrue(anyString()))
                    .thenReturn(Optional.empty());

            // When & Then
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
        @DisplayName("Should soft delete note")
        void shouldSoftDeleteNote() {
            // Given
            when(noteRepository.findByNoteUuidAndActiveTrue(NOTE_UUID))
                    .thenReturn(Optional.of(testNote));
            when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> {
                Note saved = invocation.getArgument(0);
                assertThat(saved.getActive()).isFalse();
                return saved;
            });

            // When & Then
            StepVerifier.create(noteService.deleteNote(NOTE_UUID))
                    .verifyComplete();

            verify(noteRepository).save(any(Note.class));
        }

        @Test
        @DisplayName("Should error when note not found")
        void shouldErrorWhenNoteNotFound() {
            // Given
            when(noteRepository.findByNoteUuidAndActiveTrue(anyString()))
                    .thenReturn(Optional.empty());

            // When & Then
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
        @DisplayName("Should return note count")
        void shouldReturnNoteCount() {
            // Given
            when(noteRepository.countByPatientUuidAndActiveTrue(PATIENT_UUID)).thenReturn(5L);

            // When & Then
            StepVerifier.create(noteService.countNotesByPatientUuid(PATIENT_UUID))
                    .expectNext(5L)
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should return zero when no notes")
        void shouldReturnZeroWhenNoNotes() {
            // Given
            when(noteRepository.countByPatientUuidAndActiveTrue(anyString())).thenReturn(0L);

            // When & Then
            StepVerifier.create(noteService.countNotesByPatientUuid("unknown-patient"))
                    .expectNext(0L)
                    .verifyComplete();
        }
    }
}