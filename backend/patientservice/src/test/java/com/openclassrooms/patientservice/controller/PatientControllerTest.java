package com.openclassrooms.patientservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openclassrooms.patientservice.dtorequest.PatientRequest;
import com.openclassrooms.patientservice.dtoresponse.PatientResponse;
import com.openclassrooms.patientservice.exception.ApiException;
import com.openclassrooms.patientservice.exception.HandleException;
import com.openclassrooms.patientservice.service.PatientService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests unitaires pour PatientController (réactif avec Mono/Flux)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PatientController Unit Tests - Reactive")
@Slf4j
class PatientControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PatientService patientService;

    @InjectMocks
    private PatientController patientController;

    private ObjectMapper objectMapper;
    private PatientRequest testRequest;
    private PatientResponse testResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(patientController)
                .setControllerAdvice(new HandleException())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        testRequest = PatientRequest.builder()
                .userUuid("user-uuid-456")
                .dateOfBirth(LocalDate.of(1990, 5, 15))
                .gender("M")
                .bloodType("O+")
                .build();

        testResponse = PatientResponse.builder()
                .patientUuid("patient-uuid-123")
                .userUuid("user-uuid-456")
                .medicalRecordNumber("MED-2026-000001")
                .dateOfBirth(LocalDate.of(1990, 5, 15))
                .age(35)
                .gender("M")
                .bloodType("O+")
                .build();
    }

    // CREATE
    @Nested
    @DisplayName("POST /api/patients")
    class CreatePatientEndpoint {

        @Test
        @DisplayName("Should create patient and return 201")
        void createPatient_validRequest_returns201() throws Exception {
            when(patientService.createPatient(any(PatientRequest.class))).thenReturn(Mono.just(testResponse));

            mockMvc.perform(post("/api/patients")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "/api/patients/patient-uuid-123"))
                    .andExpect(jsonPath("$.data.patient.patientUuid", is("patient-uuid-123")))
                    .andExpect(jsonPath("$.message", containsString("créé")));

            verify(patientService).createPatient(any(PatientRequest.class));
        }

        @Test
        @DisplayName("Should return 400 when request is invalid")
        void createPatient_invalidRequest_returns400() throws Exception {
            PatientRequest invalidRequest = PatientRequest.builder().gender("M").build();

            mockMvc.perform(post("/api/patients")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    // READ
    @Nested
    @DisplayName("GET /api/patients")
    class GetAllPatientsEndpoint {

        @Test
        @DisplayName("Should return all patients with 200")
        void getAllPatients_patientsExist_returns200() throws Exception {
            when(patientService.getAllActivePatients()).thenReturn(Flux.just(testResponse));

            mockMvc.perform(get("/api/patients"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.patients", hasSize(1)))
                    .andExpect(jsonPath("$.data.count", is(1)))
                    .andExpect(jsonPath("$.data.patients[0].patientUuid", is("patient-uuid-123")));

            verify(patientService).getAllActivePatients();
        }

        @Test
        @DisplayName("Should return empty list with 200")
        void getAllPatients_noPatients_returns200WithEmptyList() throws Exception {
            when(patientService.getAllActivePatients()).thenReturn(Flux.empty());

            mockMvc.perform(get("/api/patients"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.patients", hasSize(0)))
                    .andExpect(jsonPath("$.data.count", is(0)));

            verify(patientService).getAllActivePatients();
        }
    }

    @Nested
    @DisplayName("GET /api/patients/{patientUuid}")
    class GetPatientByUuidEndpoint {

        @Test
        @DisplayName("Should return patient with 200")
        void getPatientByUuid_patientExists_returns200() throws Exception {
            when(patientService.getPatientByUuid("patient-uuid-123")).thenReturn(Mono.just(testResponse));

            mockMvc.perform(get("/api/patients/patient-uuid-123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.patient.patientUuid", is("patient-uuid-123")))
                    .andExpect(jsonPath("$.data.patient.bloodType", is("O+")));
        }

        @Test
        @DisplayName("Should return 404 when patient not found")
        void getPatientByUuid_patientNotFound_returns404() throws Exception {
            when(patientService.getPatientByUuid("unknown-uuid")).thenReturn(Mono.empty());

            mockMvc.perform(get("/api/patients/unknown-uuid"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/patients/email/{email}")
    class GetPatientByEmailEndpoint {

        @Test
        @DisplayName("Should return patient by email with 200")
        void getPatientByEmail_patientExists_returns200() throws Exception {
            when(patientService.getPatientByEmail("john@email.com")).thenReturn(Mono.just(testResponse));

            mockMvc.perform(get("/api/patients/email/john@email.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.patient.patientUuid", is("patient-uuid-123")))
                    .andExpect(jsonPath("$.data.patient.userUuid", is("user-uuid-456")));
        }

        @Test
        @DisplayName("Should return 404 when patient not found")
        void getPatientByEmail_patientNotFound_returns404() throws Exception {
            when(patientService.getPatientByEmail("unknown@email.com")).thenReturn(Mono.empty());

            mockMvc.perform(get("/api/patients/email/unknown@email.com"))
                    .andExpect(status().isNotFound());
        }
    }

    // UPDATE
    @Nested
    @DisplayName("PUT /api/patients/{patientUuid}")
    class UpdatePatientEndpoint {

        @Test
        @DisplayName("Should update patient and return 200")
        void updatePatient_validRequest_returns200() throws Exception {
            when(patientService.updatePatient(eq("patient-uuid-123"), any(PatientRequest.class)))
                    .thenReturn(Mono.just(testResponse));

            mockMvc.perform(put("/api/patients/patient-uuid-123")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.patient.patientUuid", is("patient-uuid-123")))
                    .andExpect(jsonPath("$.message", containsString("mis à jour")));
        }
    }

    // DELETE
    @Nested
    @DisplayName("DELETE /api/patients/{patientUuid}")
    class DeletePatientEndpoint {

        @Test
        @DisplayName("Should delete patient and return 200")
        void deletePatient_patientExists_returns200() throws Exception {
            when(patientService.deletePatient("patient-uuid-123")).thenReturn(Mono.empty());

            mockMvc.perform(delete("/api/patients/patient-uuid-123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message", containsString("supprimé")));
        }
    }

    // STATS
    @Nested
    @DisplayName("GET /api/patients/stats/count")
    class GetPatientCountEndpoint {

        @Test
        @DisplayName("Should return patient count with 200")
        void getPatientCount_returns200() throws Exception {
            when(patientService.countActivePatients()).thenReturn(Mono.just(42L));

            mockMvc.perform(get("/api/patients/stats/count"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalPatients", is(42)));
        }
    }

    @Nested
    @DisplayName("GET /api/patients/exists/user/{userUuid}")
    class CheckPatientExistsEndpoint {

        @Test
        @DisplayName("Should return true when patient exists")
        void checkPatientExists_patientExists_returnsTrue() throws Exception {
            when(patientService.hasPatientRecord("user-uuid-456")).thenReturn(Mono.just(true));

            mockMvc.perform(get("/api/patients/exists/user/user-uuid-456"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.exists", is(true)));
        }

        @Test
        @DisplayName("Should return false when patient not exists")
        void checkPatientExists_patientNotExists_returnsFalse() throws Exception {
            when(patientService.hasPatientRecord("unknown-user")).thenReturn(Mono.just(false));

            mockMvc.perform(get("/api/patients/exists/user/unknown-user"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.exists", is(false)));
        }
    }
}