package com.openclassrooms.patientservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openclassrooms.patientservice.dtorequest.PatientRequest;
import com.openclassrooms.patientservice.dtoresponse.PatientResponse;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;

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


            MvcResult mvcResult = mockMvc.perform(post("/api/patients")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testRequest)))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(mvcResult))
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

    // READ ALL

    @Nested
    @DisplayName("GET /api/patients")
    class GetAllPatientsEndpoint {

        @Test
        @DisplayName("Should return all patients with 200")
        void getAllPatients_patientsExist_returns200() throws Exception {
            when(patientService.getAllActivePatients()).thenReturn(Flux.just(testResponse));

            MvcResult mvcResult = mockMvc.perform(get("/api/patients"))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(mvcResult))
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

            MvcResult mvcResult = mockMvc.perform(get("/api/patients"))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.patients", hasSize(0)))
                    .andExpect(jsonPath("$.data.count", is(0)));

            verify(patientService).getAllActivePatients();
        }
    }

    // READ BY UUID

    @Nested
    @DisplayName("GET /api/patients/{patientUuid}")
    class GetPatientByUuidEndpoint {

        @Test
        @DisplayName("Should return patient with 200")
        void getPatientByUuid_patientExists_returns200() throws Exception {
            when(patientService.getPatientByUuid("patient-uuid-123")).thenReturn(Mono.just(testResponse));

            MvcResult mvcResult = mockMvc.perform(get("/api/patients/patient-uuid-123"))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.patient.patientUuid", is("patient-uuid-123")))
                    .andExpect(jsonPath("$.data.patient.bloodType", is("O+")));
        }

        @Test
        @DisplayName("Should return 404 when patient not found")
        void getPatientByUuid_patientNotFound_returns404() throws Exception {
            when(patientService.getPatientByUuid("unknown-uuid")).thenReturn(Mono.empty());

            MvcResult mvcResult = mockMvc.perform(get("/api/patients/unknown-uuid"))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message", containsString("introuvable")));
        }
    }

    //  READ BY EMAIL

    @Nested
    @DisplayName("GET /api/patients/email/{email}")
    class GetPatientByEmailEndpoint {

        @Test
        @DisplayName("Should return patient by email with 200")
        void getPatientByEmail_patientExists_returns200() throws Exception {
            when(patientService.getPatientByEmail("john@email.com")).thenReturn(Mono.just(testResponse));

            MvcResult mvcResult = mockMvc.perform(get("/api/patients/email/john@email.com"))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.patient.patientUuid", is("patient-uuid-123")))
                    .andExpect(jsonPath("$.data.patient.userUuid", is("user-uuid-456")));
        }

        @Test
        @DisplayName("Should return 404 when patient not found by email")
        void getPatientByEmail_patientNotFound_returns404() throws Exception {
            when(patientService.getPatientByEmail("unknown@email.com")).thenReturn(Mono.empty());

            MvcResult mvcResult = mockMvc.perform(get("/api/patients/email/unknown@email.com"))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message", containsString("introuvable")));
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

            MvcResult mvcResult = mockMvc.perform(put("/api/patients/patient-uuid-123")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testRequest)))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(mvcResult))
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

            MvcResult mvcResult = mockMvc.perform(delete("/api/patients/patient-uuid-123"))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(mvcResult))
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

            MvcResult mvcResult = mockMvc.perform(get("/api/patients/stats/count"))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalPatients", is(42)));
        }
    }

    // EXISTS

    @Nested
    @DisplayName("GET /api/patients/exists/user/{userUuid}")
    class CheckPatientExistsEndpoint {

        @Test
        @DisplayName("Should return true when patient exists")
        void checkPatientExists_patientExists_returnsTrue() throws Exception {
            when(patientService.hasPatientRecord("user-uuid-456")).thenReturn(Mono.just(true));

            MvcResult mvcResult = mockMvc.perform(get("/api/patients/exists/user/user-uuid-456"))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.exists", is(true)));
        }

        @Test
        @DisplayName("Should return false when patient not exists")
        void checkPatientExists_patientNotExists_returnsFalse() throws Exception {
            when(patientService.hasPatientRecord("unknown-user")).thenReturn(Mono.just(false));

            MvcResult mvcResult = mockMvc.perform(get("/api/patients/exists/user/unknown-user"))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.exists", is(false)));
        }
    }

    // READ BY USER UUID

    @Nested
    @DisplayName("GET /api/patients/user/{userUuid}")
    class GetPatientByUserUuidEndpoint {

        @Test
        @DisplayName("Should return patient by user UUID with 200")
        void getPatientByUserUuid_patientExists_returns200() throws Exception {
            when(patientService.getPatientByUserUuid("user-uuid-456")).thenReturn(Mono.just(testResponse));

            MvcResult mvcResult = mockMvc.perform(get("/api/patients/user/user-uuid-456"))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.patient.patientUuid", is("patient-uuid-123")))
                    .andExpect(jsonPath("$.data.patient.userUuid", is("user-uuid-456")))
                    .andExpect(jsonPath("$.message", containsString("récupéré")));

            verify(patientService).getPatientByUserUuid("user-uuid-456");
        }

        @Test
        @DisplayName("Should return 404 when patient not found by user UUID")
        void getPatientByUserUuid_patientNotFound_returns404() throws Exception {
            when(patientService.getPatientByUserUuid("unknown-user-uuid")).thenReturn(Mono.empty());

            MvcResult mvcResult = mockMvc.perform(get("/api/patients/user/unknown-user-uuid"))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message", containsString("introuvable")));

            verify(patientService).getPatientByUserUuid("unknown-user-uuid");
        }
    }

    // READ MY PATIENT RECORD

    @Nested
    @DisplayName("GET /api/patients/me")
    class GetMyPatientRecordEndpoint {

        @Test
        @DisplayName("Should return own patient record with 200")
        void getMyPatientRecord_patientExists_returns200() throws Exception {
            when(patientService.getPatientByUserUuid("user-uuid-456")).thenReturn(Mono.just(testResponse));

            // Mock Authentication
            org.springframework.security.core.Authentication authentication =
                    mock(org.springframework.security.core.Authentication.class);
            when(authentication.getName()).thenReturn("user-uuid-456");

            MvcResult mvcResult = mockMvc.perform(get("/api/patients/me")
                            .principal(authentication))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.patient.patientUuid", is("patient-uuid-123")))
                    .andExpect(jsonPath("$.message", containsString("récupéré")));

            verify(patientService).getPatientByUserUuid("user-uuid-456");
        }

        @Test
        @DisplayName("Should return 404 when own patient record not found")
        void getMyPatientRecord_patientNotFound_returns404() throws Exception {
            when(patientService.getPatientByUserUuid("user-uuid-789")).thenReturn(Mono.empty());

            org.springframework.security.core.Authentication authentication =
                    mock(org.springframework.security.core.Authentication.class);
            when(authentication.getName()).thenReturn("user-uuid-789");

            MvcResult mvcResult = mockMvc.perform(get("/api/patients/me")
                            .principal(authentication))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message", containsString("introuvable")));

            verify(patientService).getPatientByUserUuid("user-uuid-789");
        }
    }

    //  READ BY MEDICAL RECORD NUMBER

    @Nested
    @DisplayName("GET /api/patients/medical-record/{medicalRecordNumber}")
    class GetPatientByMedicalRecordNumberEndpoint {

        @Test
        @DisplayName("Should return patient by medical record number with 200")
        void getPatientByMedicalRecordNumber_patientExists_returns200() throws Exception {
            when(patientService.getPatientByMedicalRecordNumber("MED-2026-000001"))
                    .thenReturn(Mono.just(testResponse));

            MvcResult mvcResult = mockMvc.perform(get("/api/patients/medical-record/MED-2026-000001"))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.patient.patientUuid", is("patient-uuid-123")))
                    .andExpect(jsonPath("$.data.patient.medicalRecordNumber", is("MED-2026-000001")))
                    .andExpect(jsonPath("$.message", containsString("récupéré")));

            verify(patientService).getPatientByMedicalRecordNumber("MED-2026-000001");
        }

        @Test
        @DisplayName("Should return 404 when patient not found by medical record number")
        void getPatientByMedicalRecordNumber_patientNotFound_returns404() throws Exception {
            when(patientService.getPatientByMedicalRecordNumber("MED-9999-999999"))
                    .thenReturn(Mono.empty());

            MvcResult mvcResult = mockMvc.perform(get("/api/patients/medical-record/MED-9999-999999"))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message", containsString("introuvable")));

            verify(patientService).getPatientByMedicalRecordNumber("MED-9999-999999");
        }
    }

    // READ BY BLOOD TYPE

    @Nested
    @DisplayName("GET /api/patients/blood-type/{bloodType}")
    class GetPatientsByBloodTypeEndpoint {

        @Test
        @DisplayName("Should return patients by blood type with 200")
        void getPatientsByBloodType_patientsExist_returns200() throws Exception {
            when(patientService.getPatientsByBloodType("O+")).thenReturn(Flux.just(testResponse));

            MvcResult mvcResult = mockMvc.perform(get("/api/patients/blood-type/O+"))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.patients", hasSize(1)))
                    .andExpect(jsonPath("$.data.count", is(1)))
                    .andExpect(jsonPath("$.data.patients[0].bloodType", is("O+")))
                    .andExpect(jsonPath("$.message", containsString("récupérés")));

            verify(patientService).getPatientsByBloodType("O+");
        }

        @Test
        @DisplayName("Should return empty list when no patients with blood type")
        void getPatientsByBloodType_noPatients_returns200WithEmptyList() throws Exception {
            when(patientService.getPatientsByBloodType("AB-")).thenReturn(Flux.empty());

            MvcResult mvcResult = mockMvc.perform(get("/api/patients/blood-type/AB-"))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.patients", hasSize(0)))
                    .andExpect(jsonPath("$.data.count", is(0)));

            verify(patientService).getPatientsByBloodType("AB-");
        }
    }

    // RESTORE

    @Nested
    @DisplayName("PATCH /api/patients/{patientUuid}/restore")
    class RestorePatientEndpoint {

        @Test
        @DisplayName("Should restore patient and return 200")
        void restorePatient_patientExists_returns200() throws Exception {
            when(patientService.restorePatient("patient-uuid-123")).thenReturn(Mono.just(testResponse));

            MvcResult mvcResult = mockMvc.perform(patch("/api/patients/patient-uuid-123/restore"))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.patient.patientUuid", is("patient-uuid-123")))
                    .andExpect(jsonPath("$.message", containsString("restauré")));

            verify(patientService).restorePatient("patient-uuid-123");
        }

        @Test
        @DisplayName("Should return 404 when patient to restore not found")
        void restorePatient_patientNotFound_returns404() throws Exception {
            when(patientService.restorePatient("unknown-uuid")).thenReturn(Mono.empty());

            MvcResult mvcResult = mockMvc.perform(patch("/api/patients/unknown-uuid/restore"))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message", containsString("introuvable")));

            verify(patientService).restorePatient("unknown-uuid");
        }
    }


}