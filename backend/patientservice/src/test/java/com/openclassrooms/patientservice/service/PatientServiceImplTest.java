package com.openclassrooms.patientservice.service;

import com.openclassrooms.patientservice.dtorequest.PatientRequest;
import com.openclassrooms.patientservice.dtorequest.UserRequest;
import com.openclassrooms.patientservice.dtoresponse.PatientResponse;
import com.openclassrooms.patientservice.exception.ApiException;
import com.openclassrooms.patientservice.mapper.PatientMapper;
import com.openclassrooms.patientservice.model.Patient;
import com.openclassrooms.patientservice.repository.PatientRepository;
import com.openclassrooms.patientservice.service.implementation.PatientServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires réactifs pour PatientServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PatientServiceImpl Reactive Unit Tests")
class PatientServiceImplReactiveTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private PatientMapper patientMapper;

    @Mock
    private UserService userService;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PatientServiceImpl patientService;

    private Patient testPatient;
    private PatientRequest testRequest;
    private PatientResponse testResponse;
    private UserRequest testUser;
    private UserRequest testUserRequest;

    @BeforeEach
    void setUp() {
        testPatient = Patient.builder()
                .patientId(1L)
                .patientUuid("patient-uuid-123")
                .userUuid("user-uuid-456")
                .medicalRecordNumber("MED-2026-000001")
                .dateOfBirth(LocalDate.of(1990, 5, 15))
                .gender("M")
                .bloodType("O+")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

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

        testUser = UserRequest.builder()
                .userUuid("user-uuid-456")
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@email.com")
                .build();

        testUserRequest = UserRequest.builder()
                .userUuid("user-uuid-456")
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@email.com")
                .phone("+1234567890")
                .address("123 Main St")
                .build();
    }

    // ==================== CREATE ====================

    @Nested
    @DisplayName("createPatient()")
    class CreatePatientTests {

        @Test
        @DisplayName("Should create patient successfully")
        void createPatient_success() {
            when(patientRepository.existsPatientByUserUuid(anyString())).thenReturn(false);
            when(userService.getUserByUuid(anyString())).thenReturn(Mono.just(testUser));
            when(patientMapper.toEntity(any(PatientRequest.class), anyString())).thenReturn(testPatient);
            when(patientRepository.savePatient(any(Patient.class))).thenReturn(testPatient);
            when(patientMapper.toResponse(any(Patient.class))).thenReturn(testResponse);

            Mono<PatientResponse> resultMono = patientService.createPatient(testRequest);

            StepVerifier.create(resultMono)
                    .expectNextMatches(resp -> resp.getPatientUuid().equals("patient-uuid-123"))
                    .verifyComplete();

            verify(patientRepository).existsPatientByUserUuid("user-uuid-456");
            verify(userService).getUserByUuid("user-uuid-456");
            verify(patientRepository).savePatient(any(Patient.class));
        }

        @Test
        @DisplayName("Should fail if patient already exists")
        void createPatient_patientExists() {
            when(patientRepository.existsPatientByUserUuid(anyString())).thenReturn(true);

            Mono<PatientResponse> resultMono = patientService.createPatient(testRequest);

            StepVerifier.create(resultMono)
                    .expectErrorMatches(e -> e instanceof ApiException &&
                            e.getMessage().contains("existe déjà"))
                    .verify();

            verify(patientRepository).existsPatientByUserUuid("user-uuid-456");
            verify(patientRepository, never()).savePatient(any());
        }
    }

    // ==================== READ ====================

    @Nested
    @DisplayName("getPatientByUuid()")
    class GetPatientByUuidTests {

        @Test
        @DisplayName("Should return patient when exists")
        void getPatientByUuid_success() {
            when(patientRepository.findByPatientUuid("patient-uuid-123")).thenReturn(Optional.of(testPatient));
            when(userService.getUserByUuid("user-uuid-456")).thenReturn(Mono.just(testUserRequest));
            when(patientMapper.toResponseWithUserInfo(testPatient, testUserRequest)).thenReturn(testResponse);

            StepVerifier.create(patientService.getPatientByUuid("patient-uuid-123"))
                    .expectNextMatches(resp -> resp.getPatientUuid().equals("patient-uuid-123"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should error when patient not found")
        void getPatientByUuid_notFound() {
            when(patientRepository.findByPatientUuid("unknown-uuid")).thenReturn(Optional.empty());

            StepVerifier.create(patientService.getPatientByUuid("unknown-uuid"))
                    .expectErrorMatches(e -> e instanceof ApiException &&
                            e.getMessage().contains("Patient non trouvé"))
                    .verify();
        }
    }

    @Nested
    @DisplayName("getPatientByUserUuid()")
    class GetPatientByUserUuidTests {

        @Test
        void getPatientByUserUuid_success() {
            when(patientRepository.findByUserUuid("user-uuid-456")).thenReturn(Optional.of(testPatient));
            when(userService.getUserByUuid("user-uuid-456")).thenReturn(Mono.just(testUserRequest));
            when(patientMapper.toResponseWithUserInfo(testPatient, testUserRequest)).thenReturn(testResponse);

            StepVerifier.create(patientService.getPatientByUserUuid("user-uuid-456"))
                    .expectNextMatches(resp -> resp.getUserUuid().equals("user-uuid-456"))
                    .verifyComplete();
        }

        @Test
        void getPatientByUserUuid_notFound() {
            when(patientRepository.findByUserUuid("unknown-user")).thenReturn(Optional.empty());

            StepVerifier.create(patientService.getPatientByUserUuid("unknown-user"))
                    .expectErrorMatches(e -> e instanceof ApiException &&
                            e.getMessage().contains("Aucun dossier patient"))
                    .verify();
        }
    }

    @Nested
    @DisplayName("getPatientByEmail()")
    class GetPatientByEmailTests {

        @Test
        void getPatientByEmail_success() {
            when(userService.getUserByEmail("john.doe@email.com")).thenReturn(Mono.just(testUser));
            when(patientRepository.findByUserUuid("user-uuid-456")).thenReturn(Optional.of(testPatient));
            when(patientMapper.toResponseWithUserInfo(testPatient, testUser)).thenReturn(testResponse);

            StepVerifier.create(patientService.getPatientByEmail("john.doe@email.com"))
                    .expectNextMatches(resp -> resp.getUserUuid().equals("user-uuid-456"))
                    .verifyComplete();
        }

        @Test
        void getPatientByEmail_userNotFound() {
            when(userService.getUserByEmail("unknown@email.com")).thenReturn(Mono.empty());

            StepVerifier.create(patientService.getPatientByEmail("unknown@email.com"))
                    .expectErrorMatches(e -> e instanceof ApiException &&
                            e.getMessage().contains("Aucun utilisateur trouvé"))
                    .verify();
        }
    }

    @Nested
    @DisplayName("getAllActivePatients()")
    class GetAllActivePatientsTests {

        @Test
        void getAllActivePatients_success() {
            List<Patient> patients = List.of(testPatient);
            List<PatientResponse> responses = List.of(testResponse);

            when(patientRepository.findAllPatientByActiveTrue()).thenReturn(patients);
            when(userService.getUserByUuid("user-uuid-456")).thenReturn(Mono.just(testUserRequest));
            when(patientMapper.toResponseWithUserInfo(testPatient, testUserRequest)).thenReturn(testResponse);

            StepVerifier.create(patientService.getAllActivePatients())
                    .expectNextMatches(resp -> resp.getPatientUuid().equals("patient-uuid-123"))
                    .verifyComplete();
        }
    }

    // ==================== UPDATE ====================

    @Nested
    @DisplayName("updatePatient()")
    class UpdatePatientTests {

        @Test
        void updatePatient_success() {
            Patient updatedPatient = testPatient.toBuilder().bloodType("A+").build();

            when(patientRepository.findByPatientUuid("patient-uuid-123")).thenReturn(Optional.of(testPatient));
            when(patientMapper.updateEntity(testPatient, testRequest)).thenReturn(updatedPatient);
            when(patientRepository.updatePatient(updatedPatient)).thenReturn(updatedPatient);
            when(userService.getUserByUuid("user-uuid-456")).thenReturn(Mono.just(testUserRequest));
            when(patientMapper.toResponseWithUserInfo(updatedPatient, testUserRequest)).thenReturn(testResponse);

            StepVerifier.create(patientService.updatePatient("patient-uuid-123", testRequest))
                    .expectNextMatches(resp -> resp.getPatientUuid().equals("patient-uuid-123"))
                    .verifyComplete();
        }
    }

    // ==================== DELETE ====================

    @Nested
    @DisplayName("deletePatient()")
    class DeletePatientTests {

        @Test
        void deletePatient_success() {
            when(patientRepository.findByPatientUuid("patient-uuid-123")).thenReturn(Optional.of(testPatient));
            when(userService.getUserByUuid("user-uuid-456")).thenReturn(Mono.just(testUserRequest));
            when(patientRepository.softDeletePatientByPatientUuid("patient-uuid-123")).thenReturn(true);

            StepVerifier.create(patientService.deletePatient("patient-uuid-123"))
                    .verifyComplete();
        }
    }

    // ==================== QUERY ====================

    @Nested
    @DisplayName("getPatientByMedicalRecordNumber()")
    class GetPatientByMedicalRecordNumberTests {

        @Test
        void getPatientByMedicalRecordNumber_success() {
            when(patientRepository.findPatientByMedicalRecordNumber("MED-2026-000001"))
                    .thenReturn(Optional.of(testPatient));
            when(userService.getUserByUuid("user-uuid-456")).thenReturn(Mono.just(testUserRequest));
            when(patientMapper.toResponseWithUserInfo(testPatient, testUserRequest)).thenReturn(testResponse);

            StepVerifier.create(patientService.getPatientByMedicalRecordNumber("MED-2026-000001"))
                    .expectNextMatches(resp -> resp.getMedicalRecordNumber().equals("MED-2026-000001"))
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("getPatientsByBloodType()")
    class GetPatientsByBloodTypeTests {

        @Test
        void getPatientsByBloodType_success() {
            List<Patient> patients = List.of(testPatient);
            when(patientRepository.findPatientByBloodTypeAndActiveTrue("O+")).thenReturn(patients);
            when(patientMapper.toResponse(any(Patient.class))).thenReturn(testResponse);

            StepVerifier.create(patientService.getPatientsByBloodType("O+"))
                    .expectNextMatches(resp -> resp.getBloodType().equals("O+"))
                    .verifyComplete();
        }
    }

    // ==================== UTILITY ====================

    @Nested
    @DisplayName("Utility Methods")
    class UtilityMethodsTests {

        @Test
        void hasPatientRecord_true() {
            when(patientRepository.existsPatientByUserUuid("user-uuid-456")).thenReturn(true);

            StepVerifier.create(patientService.hasPatientRecord("user-uuid-456"))
                    .expectNext(true)
                    .verifyComplete();
        }

        @Test
        void countActivePatients() {
            when(patientRepository.countPatientByActiveTrue()).thenReturn(42L);

            StepVerifier.create(patientService.countActivePatients())
                    .expectNext(42L)
                    .verifyComplete();
        }
    }
}