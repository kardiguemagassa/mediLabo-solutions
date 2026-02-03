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
@DisplayName("NoteServiceImpl Unit Tests")
class NoteServiceImplTest {

    @Mock
    private NoteRepository noteRepository;

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
                .content("Patient présente des symptômes de diabète. Taux de cholestérol élevé.")
                .build();
    }

    // CREATE TESTS

    @Nested
    @DisplayName("createNote() Tests")
    class CreateNoteTests {

        @Test
        @DisplayName("Should create note successfully")
        void createNote_validRequest_returnsNoteResponse() {
            // Given
            when(noteRepository.save(any(Note.class))).thenReturn(testNote);

            // When
            NoteResponse result = noteService.createNote(testRequest, PRACTITIONER_UUID, PRACTITIONER_NAME);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getPatientUuid()).isEqualTo(PATIENT_UUID);
            assertThat(result.getPractitionerUuid()).isEqualTo(PRACTITIONER_UUID);
            assertThat(result.getPractitionerName()).isEqualTo(PRACTITIONER_NAME);
            assertThat(result.getContent()).isEqualTo(testRequest.getContent());

            verify(noteRepository).save(any(Note.class));
        }

        @Test
        @DisplayName("Should set active to true when creating note")
        void createNote_setsActiveTrue() {
            // Given
            when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> {
                Note savedNote = invocation.getArgument(0);
                assertThat(savedNote.getActive()).isTrue();
                return testNote;
            });

            // When
            noteService.createNote(testRequest, PRACTITIONER_UUID, PRACTITIONER_NAME);

            // Then
            verify(noteRepository).save(any(Note.class));
        }
    }

    // READ TESTS

    @Nested
    @DisplayName("getNoteByUuid() Tests")
    class GetNoteByUuidTests {

        @Test
        @DisplayName("Should return note when found")
        void getNoteByUuid_noteExists_returnsNote() {
            // Given
            when(noteRepository.findByNoteUuidAndActiveTrue(NOTE_UUID)).thenReturn(Optional.of(testNote));

            // When
            NoteResponse result = noteService.getNoteByUuid(NOTE_UUID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getNoteUuid()).isEqualTo(NOTE_UUID);
            assertThat(result.getContent()).isEqualTo(testNote.getContent());
        }

        @Test
        @DisplayName("Should throw exception when note not found")
        void getNoteByUuid_noteNotFound_throwsException() {
            // Given
            when(noteRepository.findByNoteUuidAndActiveTrue(anyString())).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> noteService.getNoteByUuid("unknown-uuid"))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("Note non trouvée");
        }
    }

    @Nested
    @DisplayName("getNotesByPatientUuid() Tests")
    class GetNotesByPatientUuidTests {

        @Test
        @DisplayName("Should return notes for patient")
        void getNotesByPatientUuid_notesExist_returnsNotes() {
            // Given
            Note note2 = testNote.toBuilder()
                    .noteUuid("note-uuid-002")
                    .content("Consultation de suivi")
                    .build();

            when(noteRepository.findByPatientUuidAndActiveTrueOrderByCreatedAtDesc(PATIENT_UUID))
                    .thenReturn(List.of(testNote, note2));

            // When
            List<NoteResponse> results = noteService.getNotesByPatientUuid(PATIENT_UUID);

            // Then
            assertThat(results).hasSize(2);
            assertThat(results.getFirst().getNoteUuid()).isEqualTo(NOTE_UUID);
        }

        @Test
        @DisplayName("Should return empty list when no notes for patient")
        void getNotesByPatientUuid_noNotes_returnsEmptyList() {
            // Given
            when(noteRepository.findByPatientUuidAndActiveTrueOrderByCreatedAtDesc(anyString()))
                    .thenReturn(Collections.emptyList());

            // When
            List<NoteResponse> results = noteService.getNotesByPatientUuid("unknown-patient");

            // Then
            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("getNotesByPractitionerUuid() Tests")
    class GetNotesByPractitionerUuidTests {

        @Test
        @DisplayName("Should return notes for practitioner")
        void getNotesByPractitionerUuid_notesExist_returnsNotes() {
            // Given
            when(noteRepository.findByPractitionerUuidAndActiveTrueOrderByCreatedAtDesc(PRACTITIONER_UUID))
                    .thenReturn(List.of(testNote));

            // When
            List<NoteResponse> results = noteService.getNotesByPractitionerUuid(PRACTITIONER_UUID);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.getFirst().getPractitionerUuid()).isEqualTo(PRACTITIONER_UUID);
        }
    }

    // UPDATE TESTS

    @Nested
    @DisplayName("updateNote() Tests")
    class UpdateNoteTests {

        @Test
        @DisplayName("Should update note when practitioner is author")
        void updateNote_authorizedPractitioner_updatesNote() {
            // Given
            NoteRequest updateRequest = NoteRequest.builder()
                    .patientUuid(PATIENT_UUID)
                    .content("Contenu mis à jour")
                    .build();

            Note updatedNote = testNote.toBuilder()
                    .content("Contenu mis à jour")
                    .build();

            when(noteRepository.findByNoteUuidAndActiveTrue(NOTE_UUID)).thenReturn(Optional.of(testNote));
            when(noteRepository.save(any(Note.class))).thenReturn(updatedNote);

            // When
            NoteResponse result = noteService.updateNote(NOTE_UUID, updateRequest, PRACTITIONER_UUID);

            // Then
            assertThat(result.getContent()).isEqualTo("Contenu mis à jour");
            verify(noteRepository).save(any(Note.class));
        }

        @Test
        @DisplayName("Should throw exception when practitioner is not author")
        void updateNote_unauthorizedPractitioner_throwsException() {
            // Given
            when(noteRepository.findByNoteUuidAndActiveTrue(NOTE_UUID)).thenReturn(Optional.of(testNote));

            // When & Then
            assertThatThrownBy(() -> noteService.updateNote(NOTE_UUID, testRequest, "other-practitioner"))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("Vous n'êtes pas autorisé");
        }

        @Test
        @DisplayName("Should throw exception when note not found")
        void updateNote_noteNotFound_throwsException() {
            // Given
            when(noteRepository.findByNoteUuidAndActiveTrue(anyString())).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> noteService.updateNote("unknown-uuid", testRequest, PRACTITIONER_UUID))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("Note non trouvée");
        }
    }

    // DELETE TESTS

    @Nested
    @DisplayName("deleteNote() Tests")
    class DeleteNoteTests {

        @Test
        @DisplayName("Should soft delete note")
        void deleteNote_noteExists_setsActiveFalse() {
            // Given
            when(noteRepository.findByNoteUuidAndActiveTrue(NOTE_UUID)).thenReturn(Optional.of(testNote));
            when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> {
                Note savedNote = invocation.getArgument(0);
                assertThat(savedNote.getActive()).isFalse();
                return savedNote;
            });

            // When
            noteService.deleteNote(NOTE_UUID);

            // Then
            verify(noteRepository).save(any(Note.class));
        }

        @Test
        @DisplayName("Should throw exception when note not found")
        void deleteNote_noteNotFound_throwsException() {
            // Given
            when(noteRepository.findByNoteUuidAndActiveTrue(anyString())).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> noteService.deleteNote("unknown-uuid"))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("Note non trouvée");
        }
    }

    // COUNT TESTS

    @Nested
    @DisplayName("countNotesByPatientUuid() Tests")
    class CountNotesTests {

        @Test
        @DisplayName("Should return count of notes for patient")
        void countNotesByPatientUuid_returnsCount() {
            // Given
            when(noteRepository.countByPatientUuidAndActiveTrue(PATIENT_UUID)).thenReturn(5L);

            // When
            long count = noteService.countNotesByPatientUuid(PATIENT_UUID);

            // Then
            assertThat(count).isEqualTo(5L);
        }
    }
}