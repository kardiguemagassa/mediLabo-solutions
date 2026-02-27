package com.openclassrooms.notesservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openclassrooms.notesservice.dto.NoteRequest;
import com.openclassrooms.notesservice.dto.NoteResponse;
import com.openclassrooms.notesservice.exception.ApiException;
import com.openclassrooms.notesservice.service.NoteService;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("NoteController Unit Tests")
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

    private static final String PRACTITIONER_UUID = "practitioner-uuid-123";
    private static final String PATIENT_UUID = "patient-uuid-456";
    private static final String NOTE_UUID = "note-uuid-789";

    private NoteRequest validNoteRequest;
    private NoteResponse noteResponse;

    @BeforeEach
    void setUp() {
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
                .practitionerName("Dr. Jean Dupont")
                .content("Patient shows signs of improvement. Blood pressure normalized.")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private void setupMockAuthentication() {
        when(authentication.getName()).thenReturn(PRACTITIONER_UUID);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getClaimAsString("firstName")).thenReturn("Jean");
        when(jwt.getClaimAsString("lastName")).thenReturn("Dupont");
    }

    // CREATE NOTE TESTS

    @Nested
    @DisplayName("POST /api/notes - Create Note")
    class CreateNoteTests {

        @Test
        @DisplayName("Should create note and return 201")
        void createNote_validRequest_returns201() throws Exception {
            setupMockAuthentication();
            when(noteService.createNote(any(NoteRequest.class), eq(PRACTITIONER_UUID), any(String.class)))
                    .thenReturn(Mono.just(noteResponse));

            MvcResult asyncResult = mockMvc.perform(post("/api/notes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validNoteRequest))
                            .principal(authentication))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(asyncResult))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "/api/notes/" + NOTE_UUID))
                    .andExpect(jsonPath("$.data.note.noteUuid", is(NOTE_UUID)))
                    .andExpect(jsonPath("$.data.note.patientUuid", is(PATIENT_UUID)))
                    .andExpect(jsonPath("$.message", is("Note créée avec succès")));

            verify(noteService).createNote(any(NoteRequest.class), eq(PRACTITIONER_UUID), any(String.class));
        }

        @Test
        @DisplayName("Should return 400 when patientUuid is missing")
        void createNote_missingPatientUuid_returns400() throws Exception {
            NoteRequest invalidRequest = NoteRequest.builder()
                    .content("Some content")
                    .build();

            setupMockAuthentication();

            mockMvc.perform(post("/api/notes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest))
                            .principal(authentication))
                    .andExpect(status().isBadRequest());

            verify(noteService, never()).createNote(any(), any(), any());
        }
    }

    // GET NOTE BY UUID TESTS

    @Nested
    @DisplayName("GET /api/notes/{noteUuid} - Get Note by UUID")
    class GetNoteByUuidTests {

        @Test
        @DisplayName("Should return note with 200")
        void getNoteByUuid_noteExists_returns200() throws Exception {
            when(noteService.getNoteByUuid(NOTE_UUID)).thenReturn(Mono.just(noteResponse));

            MvcResult asyncResult = mockMvc.perform(get("/api/notes/{noteUuid}", NOTE_UUID))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(asyncResult))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.note.noteUuid", is(NOTE_UUID)))
                    .andExpect(jsonPath("$.data.note.content", is(noteResponse.getContent())));

            verify(noteService).getNoteByUuid(NOTE_UUID);
        }

        @Test
        @DisplayName("Should propagate error when note not found")
        void getNoteByUuid_noteNotFound_propagatesError() throws Exception {
            // Mono.error() - l'erreur sera dans le résultat async
            when(noteService.getNoteByUuid("unknown-uuid"))
                    .thenReturn(Mono.error(new ApiException("Note non trouvée")));

            MvcResult asyncResult = mockMvc.perform(get("/api/notes/{noteUuid}", "unknown-uuid"))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            // L'exception est dans le résultat async
            Exception resolvedException = asyncResult.getResolvedException();

            // Si pas d'exception handler, vérifier que le service a été appelé
            verify(noteService).getNoteByUuid("unknown-uuid");

            // Vérifier que l'erreur async est présente
            assertThat(asyncResult.getAsyncResult()).isInstanceOf(Throwable.class);
        }
    }

    // GET NOTES BY PATIENT TESTS

    @Nested
    @DisplayName("GET /api/notes/patient/{patientUuid} - Get Notes by Patient")
    class GetNotesByPatientTests {

        @Test
        @DisplayName("Should return notes list with 200")
        void getNotesByPatientUuid_notesExist_returns200() throws Exception {
            NoteResponse note2 = NoteResponse.builder()
                    .noteUuid("note-uuid-2")
                    .patientUuid(PATIENT_UUID)
                    .practitionerUuid(PRACTITIONER_UUID)
                    .content("Follow-up visit")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(noteService.getNotesByPatientUuid(PATIENT_UUID))
                    .thenReturn(Flux.just(noteResponse, note2));

            MvcResult asyncResult = mockMvc.perform(get("/api/notes/patient/{patientUuid}", PATIENT_UUID))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(asyncResult))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.notes", hasSize(2)))
                    .andExpect(jsonPath("$.data.count", is(2)))
                    .andExpect(jsonPath("$.message", is("Historique récupéré avec succès")));

            verify(noteService).getNotesByPatientUuid(PATIENT_UUID);
        }

        @Test
        @DisplayName("Should return empty list with 200")
        void getNotesByPatientUuid_noNotes_returns200WithEmptyList() throws Exception {
            when(noteService.getNotesByPatientUuid("patient-no-notes"))
                    .thenReturn(Flux.empty());

            MvcResult asyncResult = mockMvc.perform(get("/api/notes/patient/{patientUuid}", "patient-no-notes"))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(asyncResult))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.notes", hasSize(0)))
                    .andExpect(jsonPath("$.data.count", is(0)));
        }
    }

    // GET NOTES BY PRACTITIONER TESTS

    @Nested
    @DisplayName("GET /api/notes/practitioner/{practitionerUuid}")
    class GetNotesByPractitionerTests {

        @Test
        @DisplayName("Should return notes by practitioner with 200")
        void getNotesByPractitionerUuid_notesExist_returns200() throws Exception {
            when(noteService.getNotesByPractitionerUuid(PRACTITIONER_UUID))
                    .thenReturn(Flux.just(noteResponse));

            MvcResult asyncResult = mockMvc.perform(
                            get("/api/notes/practitioner/{practitionerUuid}", PRACTITIONER_UUID))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(asyncResult))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.notes", hasSize(1)))
                    .andExpect(jsonPath("$.data.notes[0].practitionerUuid", is(PRACTITIONER_UUID)));

            verify(noteService).getNotesByPractitionerUuid(PRACTITIONER_UUID);
        }
    }

    // GET MY NOTES TESTS

    @Nested
    @DisplayName("GET /api/notes/my-notes")
    class GetMyNotesTests {

        @Test
        @DisplayName("Should return current practitioner's notes with 200")
        void getMyNotes_notesExist_returns200() throws Exception {
            when(authentication.getName()).thenReturn(PRACTITIONER_UUID);
            when(noteService.getNotesByPractitionerUuid(PRACTITIONER_UUID))
                    .thenReturn(Flux.just(noteResponse));

            MvcResult asyncResult = mockMvc.perform(get("/api/notes/my-notes")
                            .principal(authentication))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(asyncResult))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.notes", hasSize(1)))
                    .andExpect(jsonPath("$.message", is("Mes notes récupérées avec succès")));

            verify(noteService).getNotesByPractitionerUuid(PRACTITIONER_UUID);
        }
    }

    // UPDATE NOTE TESTS

    @Nested
    @DisplayName("PUT /api/notes/{noteUuid}")
    class UpdateNoteTests {

        @Test
        @DisplayName("Should update note and return 200")
        void updateNote_validRequest_returns200() throws Exception {
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
            when(noteService.updateNote(eq(NOTE_UUID), any(NoteRequest.class), eq(PRACTITIONER_UUID)))
                    .thenReturn(Mono.just(updatedResponse));

            MvcResult asyncResult = mockMvc.perform(put("/api/notes/{noteUuid}", NOTE_UUID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest))
                            .principal(authentication))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(asyncResult))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.note.noteUuid", is(NOTE_UUID)))
                    .andExpect(jsonPath("$.data.note.content", is("Updated content with new observations")))
                    .andExpect(jsonPath("$.message", is("Note mise à jour avec succès")));

            verify(noteService).updateNote(eq(NOTE_UUID), any(NoteRequest.class), eq(PRACTITIONER_UUID));
        }

        @Test
        @DisplayName("Should propagate error when not author")
        void updateNote_notAuthor_propagatesError() throws Exception {
            NoteRequest updateRequest = NoteRequest.builder()
                    .patientUuid(PATIENT_UUID)
                    .content("Trying to update")
                    .build();

            when(authentication.getName()).thenReturn("other-practitioner");
            when(noteService.updateNote(eq(NOTE_UUID), any(NoteRequest.class), eq("other-practitioner")))
                    .thenReturn(Mono.error(new ApiException("Vous n'êtes pas autorisé à modifier cette note")));

            MvcResult asyncResult = mockMvc.perform(put("/api/notes/{noteUuid}", NOTE_UUID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest))
                            .principal(authentication))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            // Vérifier que le service a été appelé
            verify(noteService).updateNote(eq(NOTE_UUID), any(NoteRequest.class), eq("other-practitioner"));

            // Vérifier que l'erreur async est présente (ApiException)
            Object asyncResultValue = asyncResult.getAsyncResult();
            assertThat(asyncResultValue).isInstanceOf(ApiException.class);
            assertThat(((ApiException) asyncResultValue).getMessage())
                    .contains("Vous n'êtes pas autorisé");
        }
    }

    // DELETE NOTE TESTS

    @Nested
    @DisplayName("DELETE /api/notes/{noteUuid}")
    class DeleteNoteTests {

        @Test
        @DisplayName("Should delete note and return 200")
        void deleteNote_noteExists_returns200() throws Exception {
            when(noteService.deleteNote(NOTE_UUID)).thenReturn(Mono.empty());

            MvcResult asyncResult = mockMvc.perform(delete("/api/notes/{noteUuid}", NOTE_UUID))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(asyncResult))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message", is("Note supprimée avec succès")));

            verify(noteService).deleteNote(NOTE_UUID);
        }

        @Test
        @DisplayName("Should propagate error when note not found")
        void deleteNote_noteNotFound_propagatesError() throws Exception {
            when(noteService.deleteNote("unknown-uuid"))
                    .thenReturn(Mono.error(new ApiException("Note non trouvée")));

            MvcResult asyncResult = mockMvc.perform(delete("/api/notes/{noteUuid}", "unknown-uuid"))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            // Vérifier que le service a été appelé
            verify(noteService).deleteNote("unknown-uuid");

            // Vérifier que l'erreur async est présente
            Object asyncResultValue = asyncResult.getAsyncResult();
            assertThat(asyncResultValue).isInstanceOf(ApiException.class);
            assertThat(((ApiException) asyncResultValue).getMessage())
                    .contains("Note non trouvée");
        }
    }

    // COUNT NOTES TESTS

    @Nested
    @DisplayName("GET /api/notes/count/patient/{patientUuid}")
    class CountNotesTests {

        @Test
        @DisplayName("Should return count with 200")
        void countNotesByPatientUuid_returns200() throws Exception {
            when(noteService.countNotesByPatientUuid(PATIENT_UUID)).thenReturn(Mono.just(5L));

            MvcResult asyncResult = mockMvc.perform(
                            get("/api/notes/count/patient/{patientUuid}", PATIENT_UUID))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(asyncResult))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.patientUuid", is(PATIENT_UUID)))
                    .andExpect(jsonPath("$.data.noteCount", is(5)))
                    .andExpect(jsonPath("$.message", is("Comptage effectué")));

            verify(noteService).countNotesByPatientUuid(PATIENT_UUID);
        }

        @Test
        @DisplayName("Should return 0 when no notes")
        void countNotesByPatientUuid_noNotes_returnsZero() throws Exception {
            when(noteService.countNotesByPatientUuid("patient-no-notes")).thenReturn(Mono.just(0L));

            MvcResult asyncResult = mockMvc.perform(
                            get("/api/notes/count/patient/{patientUuid}", "patient-no-notes"))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(asyncResult))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.noteCount", is(0)));
        }
    }
}