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

    // ==================== HELPER METHODS ====================

    /**
     * JWT praticien par défaut avec autorités requises
     */
    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor mockJwt() {
        return jwt()
                .jwt(builder -> builder
                        .subject(PRACTITIONER_UUID)
                        .claim("firstName", "Jean")
                        .claim("lastName", "Dupont")
                        .claim("role", "practitioner"))
                .authorities(
                        new SimpleGrantedAuthority("ROLE_PRACTITIONER"),
                        new SimpleGrantedAuthority("note:create"),
                        new SimpleGrantedAuthority("note:read"),
                        new SimpleGrantedAuthority("note:update"),
                        new SimpleGrantedAuthority("note:delete")
                );
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
                .authorities(
                        new SimpleGrantedAuthority("ROLE_PRACTITIONER"),
                        new SimpleGrantedAuthority("note:create"),
                        new SimpleGrantedAuthority("note:read"),
                        new SimpleGrantedAuthority("note:update"),
                        new SimpleGrantedAuthority("note:delete")
                );
    }

    /**
     * JWT admin
     */
    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor mockAdmin() {
        return jwt()
                .jwt(builder -> builder
                        .subject("admin-uuid")
                        .claim("firstName", "Admin")
                        .claim("lastName", "System")
                        .claim("role", "admin"))
                .authorities(
                        new SimpleGrantedAuthority("ROLE_ADMIN"),
                        new SimpleGrantedAuthority("SUPER_ADMIN"),
                        new SimpleGrantedAuthority("note:create"),
                        new SimpleGrantedAuthority("note:read"),
                        new SimpleGrantedAuthority("note:update"),
                        new SimpleGrantedAuthority("note:delete")
                );
    }

    /**
     * Effectue une requête async et retourne le résultat après dispatch
     */
    private MvcResult performAsyncAndDispatch(MvcResult mvcResult) throws Exception {
        return mockMvc.perform(asyncDispatch(mvcResult)).andReturn();
    }

    // FULL CRUD SCENARIO

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

            MvcResult createAsyncResult = mockMvc.perform(post("/api/notes")
                            .with(mockJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            MvcResult createResult = mockMvc.perform(asyncDispatch(createAsyncResult))
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
            MvcResult readAsyncResult = mockMvc.perform(get("/api/notes/{noteUuid}", noteUuid)
                            .with(mockJwt()))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(readAsyncResult))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.note.noteUuid", is(noteUuid)))
                    .andExpect(jsonPath("$.data.note.content", is("Initial observation: patient stable")));

            // 3. UPDATE
            NoteRequest updateRequest = NoteRequest.builder()
                    .patientUuid(PATIENT_UUID)
                    .content("Updated observation: patient improving")
                    .build();

            MvcResult updateAsyncResult = mockMvc.perform(put("/api/notes/{noteUuid}", noteUuid)
                            .with(mockJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(updateAsyncResult))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.note.content", is("Updated observation: patient improving")));

            // 4. VERIFY UPDATE
            MvcResult verifyAsyncResult = mockMvc.perform(get("/api/notes/{noteUuid}", noteUuid)
                            .with(mockJwt()))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(verifyAsyncResult))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.note.content", is("Updated observation: patient improving")));

            // 5. DELETE (soft delete)
            MvcResult deleteAsyncResult = mockMvc.perform(delete("/api/notes/{noteUuid}", noteUuid)
                            .with(mockJwt()))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(deleteAsyncResult))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message", is("Note supprimée avec succès")));

            // 6. VERIFY DELETE - Note non trouvée
            MvcResult deletedAsyncResult = mockMvc.perform(get("/api/notes/{noteUuid}", noteUuid)
                            .with(mockJwt()))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(deletedAsyncResult))
                    .andExpect(status().isBadRequest());

            // 7. VERIFY DATABASE - La note existe toujours mais inactive
            assertThat(noteRepository.existsByNoteUuid(noteUuid)).isTrue();
        }
    }

    // ==================== MULTI-NOTE SCENARIOS ====================

    @Nested
    @DisplayName("Multi-Note Scenarios")
    class MultiNoteScenarioTests {

        @Test
        @DisplayName("Should manage multiple notes for same patient")
        void multipleNotesForPatient() throws Exception {
            // Créer 3 notes
            for (int i = 1; i <= 3; i++) {
                NoteRequest request = NoteRequest.builder()
                        .patientUuid(PATIENT_UUID)
                        .content("Note " + i + " for patient")
                        .build();

                MvcResult asyncResult = mockMvc.perform(post("/api/notes")
                                .with(mockJwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(request().asyncStarted())
                        .andReturn();

                mockMvc.perform(asyncDispatch(asyncResult))
                        .andExpect(status().isCreated());
            }

            // Vérifier la liste
            MvcResult listAsyncResult = mockMvc.perform(get("/api/notes/patient/{patientUuid}", PATIENT_UUID)
                            .with(mockJwt()))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(listAsyncResult))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.notes", hasSize(3)))
                    .andExpect(jsonPath("$.data.count", is(3)));

            // Vérifier le comptage
            MvcResult countAsyncResult = mockMvc.perform(get("/api/notes/count/patient/{patientUuid}", PATIENT_UUID)
                            .with(mockJwt()))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(countAsyncResult))
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

            MvcResult async1 = mockMvc.perform(post("/api/notes")
                            .with(mockJwtAs("practitioner-1", "Jean", "Dupont"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request1)))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(async1))
                    .andExpect(status().isCreated());

            // Praticien 2 crée une note
            NoteRequest request2 = NoteRequest.builder()
                    .patientUuid(PATIENT_UUID)
                    .content("Note from practitioner 2")
                    .build();

            MvcResult async2 = mockMvc.perform(post("/api/notes")
                            .with(mockJwtAs("practitioner-2", "Marie", "Martin"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request2)))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(async2))
                    .andExpect(status().isCreated());

            // Admin vérifie les notes du praticien 1
            MvcResult practitionerAsync = mockMvc.perform(get("/api/notes/practitioner/{uuid}", "practitioner-1")
                            .with(mockAdmin()))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(practitionerAsync))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.notes", hasSize(1)))
                    .andExpect(jsonPath("$.data.notes[0].content", is("Note from practitioner 1")));

            // Le patient a bien 2 notes au total
            MvcResult patientAsync = mockMvc.perform(get("/api/notes/patient/{patientUuid}", PATIENT_UUID)
                            .with(mockJwt()))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(patientAsync))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.notes", hasSize(2)));
        }
    }

    // ==================== AUTHORIZATION SCENARIOS ====================

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

            MvcResult createAsync = mockMvc.perform(post("/api/notes")
                            .with(mockJwtAs("author-uuid", "Jean", "Dupont"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            MvcResult createResult = mockMvc.perform(asyncDispatch(createAsync))
                    .andExpect(status().isCreated())
                    .andReturn();

            String noteUuid = objectMapper.readTree(createResult.getResponse().getContentAsString())
                    .path("data").path("note").path("noteUuid").asText();

            // Praticien 2 essaie de modifier
            NoteRequest updateRequest = NoteRequest.builder()
                    .patientUuid(PATIENT_UUID)
                    .content("Trying to hijack this note")
                    .build();

            MvcResult updateAsync = mockMvc.perform(put("/api/notes/{noteUuid}", noteUuid)
                            .with(mockJwtAs("other-uuid", "Autre", "Praticien"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(updateAsync))
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

    // ==================== EDGE CASES ====================

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should return empty list for unknown patient")
        void getNotes_unknownPatient_returnsEmptyList() throws Exception {
            MvcResult asyncResult = mockMvc.perform(get("/api/notes/patient/{patientUuid}", "unknown-patient")
                            .with(mockJwt()))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(asyncResult))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.notes", hasSize(0)))
                    .andExpect(jsonPath("$.data.count", is(0)));
        }

        @Test
        @DisplayName("Should return 0 count for unknown patient")
        void countNotes_unknownPatient_returnsZero() throws Exception {
            MvcResult asyncResult = mockMvc.perform(get("/api/notes/count/patient/{patientUuid}", "unknown-patient")
                            .with(mockJwt()))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(asyncResult))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.noteCount", is(0)));
        }
    }
}