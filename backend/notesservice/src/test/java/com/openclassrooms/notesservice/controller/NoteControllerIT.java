package com.openclassrooms.notesservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassrooms.notesservice.dto.NoteRequest;
import com.openclassrooms.notesservice.repository.NoteRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@DisplayName("NoteController Integration Tests")
class NoteControllerIT {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("spring.data.mongodb.database", () -> "test");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NoteRepository noteRepository;

    private static final String PRACTITIONER_UUID = "practitioner-uuid-123";
    private static final String PATIENT_UUID = "patient-uuid-456";

    @BeforeEach
    void setUp() {
        noteRepository.deleteAll();
    }

    // HELPER METHODS

    /**
     * JWT praticien par défaut
     */
    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor mockJwt() {
        return jwt()
                .jwt(builder -> builder
                        .subject(PRACTITIONER_UUID)
                        .claim("firstName", "Jean")
                        .claim("lastName", "Dupont")
                        .claim("role", "practitioner"))
                .authorities(new SimpleGrantedAuthority("ROLE_PRACTITIONER"));
    }

    /**
     * JWT praticien avec UUID personnalisé
     */
    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor mockJwtAs(String uuid, String firstName, String lastName) {
        return jwt()
                .jwt(builder -> builder
                        .subject(uuid)
                        .claim("firstName", firstName)
                        .claim("lastName", lastName)
                        .claim("role", "practitioner"))
                .authorities(new SimpleGrantedAuthority("ROLE_PRACTITIONER"));
    }

    /**
     * JWT admin (pour les endpoints @PreAuthorize(ADMIN_ONLY))
     */
    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor mockAdmin() {
        return jwt()
                .jwt(builder -> builder
                        .subject("admin-uuid")
                        .claim("firstName", "Admin")
                        .claim("lastName", "System")
                        .claim("role", "admin"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    // SCÉNARIO CRUD COMPLETE

    @Nested
    @DisplayName("Full CRUD Scenario")
    class FullCrudScenarioTests {

        @Test
        @DisplayName("Should complete full lifecycle: create → read → update → delete")
        void fullNoteLifecycle() throws Exception {
            // 1. CREATE
            NoteRequest createRequest = NoteRequest.builder()
                    .patientUuid(PATIENT_UUID)
                    .content("Initial observation: patient stable")
                    .build();

            MvcResult createResult = mockMvc.perform(post("/api/notes")
                            .with(mockJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.note.noteUuid").exists())
                    .andExpect(jsonPath("$.data.note.patientUuid", is(PATIENT_UUID)))
                    .andExpect(jsonPath("$.data.note.content", is("Initial observation: patient stable")))
                    .andReturn();

            String responseJson = createResult.getResponse().getContentAsString();
            String noteUuid = objectMapper.readTree(responseJson)
                    .path("data").path("note").path("noteUuid").asText();
            assertThat(noteUuid).isNotBlank();

            // 2. READ
            mockMvc.perform(get("/api/notes/{noteUuid}", noteUuid)
                            .with(mockJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.note.noteUuid", is(noteUuid)))
                    .andExpect(jsonPath("$.data.note.content", is("Initial observation: patient stable")));

            // 3. UPDATE
            NoteRequest updateRequest = NoteRequest.builder()
                    .patientUuid(PATIENT_UUID)
                    .content("Updated observation: patient improving")
                    .build();

            mockMvc.perform(put("/api/notes/{noteUuid}", noteUuid)
                            .with(mockJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.note.content", is("Updated observation: patient improving")));

            // 4. VERIFY UPDATE
            mockMvc.perform(get("/api/notes/{noteUuid}", noteUuid)
                            .with(mockJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.note.content", is("Updated observation: patient improving")));

            // 5. DELETE (soft delete)
            mockMvc.perform(delete("/api/notes/{noteUuid}", noteUuid)
                            .with(mockJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message", is("Note supprimée avec succès")));

            // 6. VERIFY DELETE - ApiException → 400 BAD_REQUEST
            mockMvc.perform(get("/api/notes/{noteUuid}", noteUuid)
                            .with(mockJwt()))
                    .andExpect(status().isBadRequest());

            // 7. VERIFY DATABASE - La note existe toujours mais inactive
            assertThat(noteRepository.existsByNoteUuid(noteUuid)).isTrue();
        }
    }

    // SCÉNARIOS MULTI-NOTES

    @Nested
    @DisplayName("Multi-Note Scenarios")
    class MultiNoteScenarioTests {

        @Test
        @DisplayName("Should manage multiple notes for same patient")
        void multipleNotesForPatient() throws Exception {
            for (int i = 1; i <= 3; i++) {
                NoteRequest request = NoteRequest.builder()
                        .patientUuid(PATIENT_UUID)
                        .content("Note " + i + " for patient")
                        .build();

                mockMvc.perform(post("/api/notes")
                                .with(mockJwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isCreated());
            }

            mockMvc.perform(get("/api/notes/patient/{patientUuid}", PATIENT_UUID)
                            .with(mockJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.notes", hasSize(3)))
                    .andExpect(jsonPath("$.data.count", is(3)));

            mockMvc.perform(get("/api/notes/count/patient/{patientUuid}", PATIENT_UUID)
                            .with(mockJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.noteCount", is(3)));
        }

        @Test
        @DisplayName("Should separate notes by practitioner")
        void notesSeparatedByPractitioner() throws Exception {
            // Praticien 1 crée une note
            NoteRequest request1 = NoteRequest.builder()
                    .patientUuid(PATIENT_UUID)
                    .content("Note from practitioner 1")
                    .build();

            mockMvc.perform(post("/api/notes")
                            .with(mockJwtAs("practitioner-1", "Jean", "Dupont"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request1)))
                    .andExpect(status().isCreated());

            // Praticien 2 crée une note
            NoteRequest request2 = NoteRequest.builder()
                    .patientUuid(PATIENT_UUID)
                    .content("Note from practitioner 2")
                    .build();

            mockMvc.perform(post("/api/notes")
                            .with(mockJwtAs("practitioner-2", "Marie", "Martin"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request2)))
                    .andExpect(status().isCreated());

            // Admin vérifie les notes du praticien 1 (ADMIN_ONLY)
            mockMvc.perform(get("/api/notes/practitioner/{uuid}", "practitioner-1")
                            .with(mockAdmin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.notes", hasSize(1)))
                    .andExpect(jsonPath("$.data.notes[0].content", is("Note from practitioner 1")));

            // Le patient a bien 2 notes au total (ALL_STAFF)
            mockMvc.perform(get("/api/notes/patient/{patientUuid}", PATIENT_UUID)
                            .with(mockJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.notes", hasSize(2)));
        }
    }

    // SCÉNARIOS D'AUTORISATION

    @Nested
    @DisplayName("Authorization Scenarios")
    class AuthorizationScenarioTests {

        @Test
        @DisplayName("Should prevent non-author from updating note")
        void updateNote_byNonAuthor_shouldFail() throws Exception {
            // Praticien 1 crée une note
            NoteRequest createRequest = NoteRequest.builder()
                    .patientUuid(PATIENT_UUID)
                    .content("Original content")
                    .build();

            MvcResult createResult = mockMvc.perform(post("/api/notes")
                            .with(mockJwtAs("author-uuid", "Jean", "Dupont"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isCreated())
                    .andReturn();

            String noteUuid = objectMapper.readTree(createResult.getResponse().getContentAsString())
                    .path("data").path("note").path("noteUuid").asText();

            // Praticien 2 essaie de modifier → ApiException → 400
            NoteRequest updateRequest = NoteRequest.builder()
                    .patientUuid(PATIENT_UUID)
                    .content("Trying to hijack this note")
                    .build();

            mockMvc.perform(put("/api/notes/{noteUuid}", noteUuid)
                            .with(mockJwtAs("other-uuid", "Autre", "Praticien"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject unauthenticated requests")
        void unauthenticatedRequest_shouldReturn401() throws Exception {
            mockMvc.perform(get("/api/notes/patient/{patientUuid}", PATIENT_UUID))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should reject practitioner accessing admin-only endpoint")
        void practitionerAccessingAdminEndpoint_shouldReturn403() throws Exception {
            mockMvc.perform(get("/api/notes/practitioner/{uuid}", "some-uuid")
                            .with(mockJwt()))
                    .andExpect(status().isForbidden());
        }
    }

    // SCÉNARIOS EDGE CASES

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should return empty list for unknown patient")
        void getNotes_unknownPatient_returnsEmptyList() throws Exception {
            mockMvc.perform(get("/api/notes/patient/{patientUuid}", "unknown-patient")
                            .with(mockJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.notes", hasSize(0)))
                    .andExpect(jsonPath("$.data.count", is(0)));
        }

        @Test
        @DisplayName("Should return 0 count for unknown patient")
        void countNotes_unknownPatient_returnsZero() throws Exception {
            mockMvc.perform(get("/api/notes/count/patient/{patientUuid}", "unknown-patient")
                            .with(mockJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.noteCount", is(0)));
        }
    }
}