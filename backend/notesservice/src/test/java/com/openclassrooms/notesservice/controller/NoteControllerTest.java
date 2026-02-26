package com.openclassrooms.notesservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openclassrooms.notesservice.dto.NoteRequest;
import com.openclassrooms.notesservice.dto.NoteResponse;
import com.openclassrooms.notesservice.exception.ApiException;
import com.openclassrooms.notesservice.service.NoteService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests unitaires pour NoteController.
 *
 * Utilise MockMvc standalone pour tester les endpoints REST.
 * Pattern identique à PatientControllerTest pour la cohérence du projet.
 *
 * Note: HandleException nécessite HttpServletRequest injecté, donc on utilise
 * le DefaultHandlerExceptionResolver par défaut de Spring MVC.
 *
 * @author Kardigué MAGASSA
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("NoteController Unit Tests")
@Slf4j
class NoteControllerTest {

    private MockMvc mockMvc;

    @Mock
    private NoteService noteService;

    @Mock
    private Authentication authentication;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private NoteController noteController;

    private ObjectMapper objectMapper;

    // Test data
    private static final String PRACTITIONER_UUID = "practitioner-uuid-123";
    private static final String PRACTITIONER_NAME = "Dr. Jean Dupont";
    private static final String PATIENT_UUID = "patient-uuid-456";
    private static final String NOTE_UUID = "note-uuid-789";

    private NoteRequest validNoteRequest;
    private NoteResponse noteResponse;

    @BeforeEach
    void setUp() {
        log.info("Setting up NoteControllerTest");

        // Setup standalone sans HandleException (qui nécessite HttpServletRequest)
        mockMvc = MockMvcBuilders
                .standaloneSetup(noteController)
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        validNoteRequest = NoteRequest.builder()
                .patientUuid(PATIENT_UUID)
                .content("Patient shows signs of improvement. Blood pressure normalized.")
                .build();

        noteResponse = NoteResponse.builder()
                .noteUuid(NOTE_UUID)
                .patientUuid(PATIENT_UUID)
                .practitionerUuid(PRACTITIONER_UUID)
                .practitionerName(PRACTITIONER_NAME)
                .content("Patient shows signs of improvement. Blood pressure normalized.")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        log.info("Setup completed");
    }

    // CREATE NOTE TESTS

    @Nested
    @DisplayName("POST /api/notes - Create Note")
    class CreateNoteTests {

        @Test
        @DisplayName("Should create note and return 201")
        void createNote_validRequest_returns201() throws Exception {
            log.info("Testing createNote with valid request");

            // Given
            setupMockAuthentication();
            when(noteService.createNote(any(NoteRequest.class), eq(PRACTITIONER_UUID), any(String.class))).thenReturn(Mono.just(noteResponse));

            // When & Then
            mockMvc.perform(post("/api/notes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validNoteRequest))
                            .principal(authentication))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "/api/notes/" + NOTE_UUID))
                    .andExpect(jsonPath("$.data.note.noteUuid", is(NOTE_UUID)))
                    .andExpect(jsonPath("$.data.note.patientUuid", is(PATIENT_UUID)))
                    .andExpect(jsonPath("$.data.note.practitionerUuid", is(PRACTITIONER_UUID)))
                    .andExpect(jsonPath("$.message", is("Note créée avec succès")));

            verify(noteService).createNote(any(NoteRequest.class), eq(PRACTITIONER_UUID), any(String.class));
            log.info("Test createNote_validRequest_returns201 passed");
        }

        @Test
        @DisplayName("Should return 400 when patientUuid is missing")
        void createNote_missingPatientUuid_returns400() throws Exception {
            log.info("Testing createNote with missing patientUuid");

            // Given
            NoteRequest invalidRequest = NoteRequest.builder()
                    .content("Some content")
                    .build();

            setupMockAuthentication();

            // When & Then
            mockMvc.perform(post("/api/notes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest))
                            .principal(authentication))
                    .andExpect(status().isBadRequest());

            verify(noteService, never()).createNote(any(), any(), any());
            log.info("Test createNote_missingPatientUuid_returns400 passed");
        }

        @Test
        @DisplayName("Should return 400 when content is blank")
        void createNote_blankContent_returns400() throws Exception {
            log.info("Testing createNote with blank content");

            // Given
            NoteRequest invalidRequest = NoteRequest.builder()
                    .patientUuid(PATIENT_UUID)
                    .content("")
                    .build();

            setupMockAuthentication();

            // When & Then
            mockMvc.perform(post("/api/notes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest))
                            .principal(authentication))
                    .andExpect(status().isBadRequest());

            log.info("Test createNote_blankContent_returns400 passed");
        }
    }

    // GET NOTE BY UUID TESTS

    @Nested
    @DisplayName("GET /api/notes/{noteUuid} - Get Note by UUID")
    class GetNoteByUuidTests {

        @Test
        @DisplayName("Should return note with 200")
        void getNoteByUuid_noteExists_returns200() throws Exception {
            log.info("Testing getNoteByUuid with existing note");

            // Given
            when(noteService.getNoteByUuid(NOTE_UUID)).thenReturn(Mono.just(noteResponse));

            // When & Then
            mockMvc.perform(get("/api/notes/{noteUuid}", NOTE_UUID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.note.noteUuid", is(NOTE_UUID)))
                    .andExpect(jsonPath("$.data.note.content", is(noteResponse.getContent())))
                    .andExpect(jsonPath("$.data.note.practitionerName", is(PRACTITIONER_NAME)));

            verify(noteService).getNoteByUuid(NOTE_UUID);
            log.info("Test getNoteByUuid_noteExists_returns200 passed");
        }

        @Test
        @DisplayName("Should throw exception when note not found")
        void getNoteByUuid_noteNotFound_throwsException() throws Exception {
            log.info("Testing getNoteByUuid with non-existent note");

            // Given
            when(noteService.getNoteByUuid("unknown-uuid"))
                    .thenThrow(new ApiException("Note non trouvée"));

            // When & Then - L'exception est propagée (pas de HandleException configuré)
            try {
                mockMvc.perform(get("/api/notes/{noteUuid}", "unknown-uuid"));
            } catch (Exception e) {
                // Expected - ApiException is thrown
                log.info("Test getNoteByUuid_noteNotFound_throwsException passed - Exception caught as expected");
                return;
            }

            // Si on arrive ici, le test échoue
            throw new AssertionError("Expected ApiException to be thrown");
        }
    }

    // GET NOTES BY PATIENT UUID TESTS

    @Nested
    @DisplayName("GET /api/notes/patient/{patientUuid} - Get Notes by Patient")
    class GetNotesByPatientTests {

        @Test
        @DisplayName("Should return notes list with 200")
        void getNotesByPatientUuid_notesExist_returns200() throws Exception {
            log.info("Testing getNotesByPatientUuid with existing notes");

            // Given
            NoteResponse note2 = NoteResponse.builder()
                    .noteUuid("note-uuid-2")
                    .patientUuid(PATIENT_UUID)
                    .practitionerUuid(PRACTITIONER_UUID)
                    .content("Follow-up visit")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(noteService.getNotesByPatientUuid(PATIENT_UUID)).thenReturn((Flux<NoteResponse>) List.of(noteResponse, note2));

            // When & Then
            mockMvc.perform(get("/api/notes/patient/{patientUuid}", PATIENT_UUID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.notes", hasSize(2)))
                    .andExpect(jsonPath("$.data.count", is(2)))
                    .andExpect(jsonPath("$.message", is("Historique récupéré avec succès")));

            verify(noteService).getNotesByPatientUuid(PATIENT_UUID);
            log.info("Test getNotesByPatientUuid_notesExist_returns200 passed");
        }

        @Test
        @DisplayName("Should return empty list with 200")
        void getNotesByPatientUuid_noNotes_returns200WithEmptyList() throws Exception {
            log.info("Testing getNotesByPatientUuid with no notes");

            // Given
            when(noteService.getNotesByPatientUuid("patient-no-notes")).thenReturn((Flux<NoteResponse>) Collections.emptyList());

            // When & Then
            mockMvc.perform(get("/api/notes/patient/{patientUuid}", "patient-no-notes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.notes", hasSize(0)))
                    .andExpect(jsonPath("$.data.count", is(0)));

            log.info("Test getNotesByPatientUuid_noNotes_returns200WithEmptyList passed");
        }
    }

    // GET NOTES BY PRACTITIONER UUID TESTS

    @Nested
    @DisplayName("GET /api/notes/practitioner/{practitionerUuid} - Get Notes by Practitioner")
    class GetNotesByPractitionerTests {

        @Test
        @DisplayName("Should return notes by practitioner with 200")
        void getNotesByPractitionerUuid_notesExist_returns200() throws Exception {
            log.info("Testing getNotesByPractitionerUuid");

            // Given
            when(noteService.getNotesByPractitionerUuid(PRACTITIONER_UUID)).thenReturn((Flux<NoteResponse>) List.of(noteResponse));

            // When & Then
            mockMvc.perform(get("/api/notes/practitioner/{practitionerUuid}", PRACTITIONER_UUID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.notes", hasSize(1)))
                    .andExpect(jsonPath("$.data.count", is(1)))
                    .andExpect(jsonPath("$.data.notes[0].practitionerUuid", is(PRACTITIONER_UUID)));

            verify(noteService).getNotesByPractitionerUuid(PRACTITIONER_UUID);
            log.info("Test getNotesByPractitionerUuid_notesExist_returns200 passed");
        }
    }

    // GET MY NOTES TESTS

    @Nested
    @DisplayName("GET /api/notes/my-notes - Get Current Practitioner's Notes")
    class GetMyNotesTests {

        @Test
        @DisplayName("Should return current practitioner's notes with 200")
        void getMyNotes_notesExist_returns200() throws Exception {
            log.info("Testing getMyNotes");

            // Given
            when(authentication.getName()).thenReturn(PRACTITIONER_UUID);
            when(noteService.getNotesByPractitionerUuid(PRACTITIONER_UUID)).thenReturn((Flux<NoteResponse>) List.of(noteResponse));

            // When & Then
            mockMvc.perform(get("/api/notes/my-notes")
                            .principal(authentication))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.notes", hasSize(1)))
                    .andExpect(jsonPath("$.message", is("Mes notes récupérées avec succès")));

            verify(noteService).getNotesByPractitionerUuid(PRACTITIONER_UUID);
            log.info("Test getMyNotes_notesExist_returns200 passed");
        }
    }

    // UPDATE NOTE TESTS

    @Nested
    @DisplayName("PUT /api/notes/{noteUuid} - Update Note")
    class UpdateNoteTests {

        @Test
        @DisplayName("Should update note and return 200")
        void updateNote_validRequest_returns200() throws Exception {
            log.info("Testing updateNote with valid request");

            // Given
            NoteRequest updateRequest = NoteRequest.builder()
                    .patientUuid(PATIENT_UUID)
                    .content("Updated content with new observations")
                    .build();

            NoteResponse updatedResponse = NoteResponse.builder()
                    .noteUuid(NOTE_UUID)
                    .patientUuid(PATIENT_UUID)
                    .practitionerUuid(PRACTITIONER_UUID)
                    .content("Updated content with new observations")
                    .createdAt(LocalDateTime.now().minusDays(1))
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(authentication.getName()).thenReturn(PRACTITIONER_UUID);
            when(noteService.updateNote(eq(NOTE_UUID), any(NoteRequest.class), eq(PRACTITIONER_UUID))).thenReturn(Mono.just(updatedResponse));

            // When & Then
            mockMvc.perform(put("/api/notes/{noteUuid}", NOTE_UUID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest))
                            .principal(authentication))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.note.noteUuid", is(NOTE_UUID)))
                    .andExpect(jsonPath("$.data.note.content", is("Updated content with new observations")))
                    .andExpect(jsonPath("$.message", is("Note mise à jour avec succès")));

            verify(noteService).updateNote(eq(NOTE_UUID), any(NoteRequest.class), eq(PRACTITIONER_UUID));
            log.info("Test updateNote_validRequest_returns200 passed");
        }

        @Test
        @DisplayName("Should throw exception when note not found")
        void updateNote_noteNotFound_throwsException() throws Exception {
            log.info("Testing updateNote with non-existent note");

            // Given
            NoteRequest updateRequest = NoteRequest.builder()
                    .patientUuid(PATIENT_UUID)
                    .content("Updated content")
                    .build();

            when(authentication.getName()).thenReturn(PRACTITIONER_UUID);
            when(noteService.updateNote(eq("unknown-uuid"), any(NoteRequest.class), eq(PRACTITIONER_UUID)))
                    .thenThrow(new ApiException("Note non trouvée"));

            // When & Then
            try {
                mockMvc.perform(put("/api/notes/{noteUuid}", "unknown-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
                        .principal(authentication));
            } catch (Exception e) {
                log.info("Test updateNote_noteNotFound_throwsException passed - Exception caught as expected");
                return;
            }

            throw new AssertionError("Expected ApiException to be thrown");
        }

        @Test
        @DisplayName("Should throw exception when not author")
        void updateNote_notAuthor_throwsException() throws Exception {
            log.info("Testing updateNote by non-author");

            // Given
            NoteRequest updateRequest = NoteRequest.builder()
                    .patientUuid(PATIENT_UUID)
                    .content("Trying to update someone else's note")
                    .build();

            when(authentication.getName()).thenReturn("other-practitioner");
            when(noteService.updateNote(eq(NOTE_UUID), any(NoteRequest.class), eq("other-practitioner")))
                    .thenThrow(new ApiException("Vous n'êtes pas autorisé à modifier cette note"));

            // When & Then
            try {
                mockMvc.perform(put("/api/notes/{noteUuid}", NOTE_UUID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
                        .principal(authentication));
            } catch (Exception e) {
                log.info("Test updateNote_notAuthor_throwsException passed - Exception caught as expected");
                return;
            }

            throw new AssertionError("Expected ApiException to be thrown");
        }
    }

    // DELETE NOTE TESTS

    @Nested
    @DisplayName("DELETE /api/notes/{noteUuid} - Delete Note")
    class DeleteNoteTests {

        @Test
        @DisplayName("Should delete note and return 200")
        void deleteNote_noteExists_returns200() throws Exception {
            log.info("Testing deleteNote with existing note");

            // Given
            doNothing().when(noteService).deleteNote(NOTE_UUID);

            // When & Then
            mockMvc.perform(delete("/api/notes/{noteUuid}", NOTE_UUID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message", is("Note supprimée avec succès")));

            verify(noteService).deleteNote(NOTE_UUID);
            log.info("Test deleteNote_noteExists_returns200 passed");
        }

        @Test
        @DisplayName("Should throw exception when note not found")
        void deleteNote_noteNotFound_throwsException() throws Exception {
            log.info("Testing deleteNote with non-existent note");

            // Given
            doThrow(new ApiException("Note non trouvée"))
                    .when(noteService).deleteNote("unknown-uuid");

            // When & Then
            try {
                mockMvc.perform(delete("/api/notes/{noteUuid}", "unknown-uuid"));
            } catch (Exception e) {
                log.info("Test deleteNote_noteNotFound_throwsException passed - Exception caught as expected");
                return;
            }

            throw new AssertionError("Expected ApiException to be thrown");
        }
    }

    // COUNT NOTES TESTS

    @Nested
    @DisplayName("GET /api/notes/count/patient/{patientUuid} - Count Notes")
    class CountNotesTests {

        @Test
        @DisplayName("Should return count with 200")
        void countNotesByPatientUuid_returns200() throws Exception {
            log.info("Testing countNotesByPatientUuid");

            // Given
            when(noteService.countNotesByPatientUuid(PATIENT_UUID)).thenReturn(Mono.just(5L));

            // When & Then
            mockMvc.perform(get("/api/notes/count/patient/{patientUuid}", PATIENT_UUID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.patientUuid", is(PATIENT_UUID)))
                    .andExpect(jsonPath("$.data.noteCount", is(5)))
                    .andExpect(jsonPath("$.message", is("Comptage effectué")));

            verify(noteService).countNotesByPatientUuid(PATIENT_UUID);
            log.info("Test countNotesByPatientUuid_returns200 passed");
        }

        @Test
        @DisplayName("Should return 0 when no notes")
        void countNotesByPatientUuid_noNotes_returnsZero() throws Exception {
            log.info("Testing countNotesByPatientUuid with no notes");

            // Given
            when(noteService.countNotesByPatientUuid("patient-no-notes")).thenReturn(Mono.just(0L));

            // When & Then
            mockMvc.perform(get("/api/notes/count/patient/{patientUuid}", "patient-no-notes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.noteCount", is(0)));

            log.info("Test countNotesByPatientUuid_noNotes_returnsZero passed");
        }
    }

    // HELPER METHODS

    /**
     * Configure le mock Authentication avec un JWT simulé
     */
    private void setupMockAuthentication() {
        when(authentication.getName()).thenReturn(PRACTITIONER_UUID);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getClaimAsString("firstName")).thenReturn("Jean");
        when(jwt.getClaimAsString("lastName")).thenReturn("Dupont");
    }
}