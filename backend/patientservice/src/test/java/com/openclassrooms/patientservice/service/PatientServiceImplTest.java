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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour PatientServiceImpl.
 *
 * @author Kardigué MAGASSA
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PatientService Unit Tests")
class PatientServiceImplTest {

    @Mock
    private PatientRepository patientRepository;
    @Mock
    private PatientMapper patientMapper;
    @Mock
    private UserService userService;
    @InjectMocks
    private PatientServiceImpl patientService;

    // Test data
    private Patient testPatient;
    private PatientRequest testRequest;
    private PatientResponse testResponse;
    private UserRequest testUser;
    private UserRequest testUserRequest;

    @BeforeEach
    void setUp() {
        // Setup test patient
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

        // Setup test request
        testRequest = PatientRequest.builder()
                .userUuid("user-uuid-456")
                .dateOfBirth(LocalDate.of(1990, 5, 15))
                .gender("M")
                .bloodType("O+")
                .build();

        // Setup test response
        testResponse = PatientResponse.builder()
                .patientUuid("patient-uuid-123")
                .userUuid("user-uuid-456")
                .medicalRecordNumber("MED-2026-000001")
                .dateOfBirth(LocalDate.of(1990, 5, 15))
                .age(35)
                .gender("M")
                .bloodType("O+")
                .build();

        // Setup test user
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

    // CREATE TESTS

    @Nested
    @DisplayName("createPatient()")
    class CreatePatientTests {

        @Test
        @DisplayName("Should create patient successfully")
        void createPatient_validRequest_returnsPatientResponse() {
            // Given
            when(patientRepository.existsPatientByUserUuid(anyString())).thenReturn(false);
            when(userService.getUserByUuid(anyString())).thenReturn(testUser);
            when(patientRepository.existsPatientByMedicalRecordNumber(anyString())).thenReturn(false);
            when(patientMapper.toEntity(any(PatientRequest.class), anyString())).thenReturn(testPatient);
            when(patientRepository.savePatient(any(Patient.class))).thenReturn(testPatient);
            when(patientMapper.toResponse(any(Patient.class))).thenReturn(testResponse);

            // When
            PatientResponse result = patientService.createPatient(testRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getPatientUuid()).isEqualTo("patient-uuid-123");
            assertThat(result.getUserUuid()).isEqualTo("user-uuid-456");

            verify(patientRepository).existsPatientByUserUuid("user-uuid-456");
            verify(userService).getUserByUuid("user-uuid-456");
            verify(patientRepository).savePatient(any(Patient.class));
        }

        @Test
        @DisplayName("Should throw exception when patient already exists")
        void createPatient_patientAlreadyExists_throwsApiException() {
            // Given
            when(patientRepository.existsPatientByUserUuid(anyString())).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> patientService.createPatient(testRequest))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("existe déjà");

            verify(patientRepository).existsPatientByUserUuid("user-uuid-456");
            verify(patientRepository, never()).savePatient(any());
        }

        @Test
        @DisplayName("Should throw exception when user not found in auth server")
        void createPatient_userNotFound_throwsApiException() {
            // Given
            when(patientRepository.existsPatientByUserUuid(anyString())).thenReturn(false);
            when(userService.getUserByUuid(anyString())).thenThrow(new ApiException("User not found"));

            // When & Then
            assertThatThrownBy(() -> patientService.createPatient(testRequest))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("Utilisateur non trouvé");

            verify(patientRepository, never()).savePatient(any());
        }
    }

    // READ TESTS

    @Nested
    @DisplayName("getPatientByUuid()")
    class GetPatientByUuidTests {
        @Test
        @DisplayName("Should return patient with user info when found")
        void getPatientByUuid_patientExists_returnsPatientWithUserInfo() {
            // Given
            when(patientRepository.findByPatientUuid("patient-uuid-123")).thenReturn(Optional.of(testPatient));
            when(userService.getUserByUuid("user-uuid-456")).thenReturn(testUserRequest);
            when(patientMapper.toResponseWithUserInfo(testPatient, testUserRequest)).thenReturn(testResponse);

            // When
            PatientResponse result = patientService.getPatientByUuid("patient-uuid-123");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getPatientUuid()).isEqualTo("patient-uuid-123");
            verify(userService).getUserByUuid("user-uuid-456");
        }

        @Test
        @DisplayName("Should throw exception when patient not found")
        void getPatientByUuid_patientNotFound_throwsApiException() {
            // Given
            when(patientRepository.findByPatientUuid("unknown-uuid")).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> patientService.getPatientByUuid("unknown-uuid"))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("Patient non trouvé");
        }
    }

    @Nested
    @DisplayName("getPatientByUserUuid()")
    class GetPatientByUserUuidTests {

        @Test
        @DisplayName("Should return patient with user info when found by user UUID")
        void getPatientByUserUuid_patientExists_returnsPatientWithUserInfo() {
            // Given
            when(patientRepository.findByUserUuid("user-uuid-456")).thenReturn(Optional.of(testPatient));
            when(userService.getUserByUuid("user-uuid-456")).thenReturn(testUserRequest);
            when(patientMapper.toResponseWithUserInfo(testPatient, testUserRequest)).thenReturn(testResponse);

            // When
            PatientResponse result = patientService.getPatientByUserUuid("user-uuid-456");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUserUuid()).isEqualTo("user-uuid-456");
            verify(userService).getUserByUuid("user-uuid-456");
        }

        @Test
        @DisplayName("Should throw exception when no patient for user")
        void getPatientByUserUuid_noPatient_throwsApiException() {
            // Given
            when(patientRepository.findByUserUuid("unknown-user")).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> patientService.getPatientByUserUuid("unknown-user"))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("Aucun dossier patient");
        }

        @Test
        @DisplayName("Should return patient without user info when userService fails")
        void getPatientByUuid_userServiceFails_returnsPatientWithoutUserInfo() {
            // Given
            when(patientRepository.findByPatientUuid("patient-uuid-123")).thenReturn(Optional.of(testPatient));
            when(userService.getUserByUuid("user-uuid-456")).thenThrow(new ApiException("Service unavailable"));
            when(patientMapper.toResponse(testPatient)).thenReturn(testResponse);

            // When
            PatientResponse result = patientService.getPatientByUuid("patient-uuid-123");

            // Then
            assertThat(result).isNotNull();
            verify(patientMapper).toResponse(testPatient); // fallback sans userInfo
        }
    }

    @Nested
    @DisplayName("getPatientByEmail()")
    class GetPatientByEmailTests {

        @Test
        @DisplayName("Should return patient when found by email")
        void getPatientByEmail_patientExists_returnsPatientWithUserInfo() {
            // Given
            when(userService.getUserByEmail("john.doe@email.com")).thenReturn(Optional.of(testUser));
            when(patientRepository.findByUserUuid("user-uuid-456")).thenReturn(Optional.of(testPatient));
            when(patientMapper.toResponseWithUserInfo(testPatient, testUser)).thenReturn(testResponse);

            // When
            PatientResponse result = patientService.getPatientByEmail("john.doe@email.com");

            // Then
            assertThat(result).isNotNull();
            verify(userService).getUserByEmail("john.doe@email.com");
            verify(patientRepository).findByUserUuid("user-uuid-456");
        }

        @Test
        @DisplayName("Should throw exception when user not found by email")
        void getPatientByEmail_userNotFound_throwsApiException() {
            // Given
            when(userService.getUserByEmail("unknown@email.com")).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> patientService.getPatientByEmail("unknown@email.com"))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("Aucun utilisateur trouvé");
        }

        @Test
        @DisplayName("Should throw exception when user exists but no patient record")
        void getPatientByEmail_userExistsNoPatient_throwsApiException() {
            // Given
            when(userService.getUserByEmail("john.doe@email.com")).thenReturn(Optional.of(testUser));
            when(patientRepository.findByUserUuid("user-uuid-456")).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> patientService.getPatientByEmail("john.doe@email.com"))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("Aucun dossier patient pour l'email");
        }
    }

    @Nested
    @DisplayName("getAllActivePatients()")
    class GetAllActivePatientsTests {

        @Test
        @DisplayName("Should return all active patients")
        void getAllActivePatients_patientsExist_returnsList() {
            // Given
            List<Patient> patients = List.of(testPatient);
            List<PatientResponse> responses = List.of(testResponse);

            when(patientRepository.findAllPatientByActiveTrue()).thenReturn(patients);
            when(patientMapper.toResponseList(patients)).thenReturn(responses);

            // When
            List<PatientResponse> result = patientService.getAllActivePatients();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getPatientUuid()).isEqualTo("patient-uuid-123");
        }

        @Test
        @DisplayName("Should return empty list when no patients")
        void getAllActivePatients_noPatients_returnsEmptyList() {
            // Given
            when(patientRepository.findAllPatientByActiveTrue()).thenReturn(List.of());
            when(patientMapper.toResponseList(List.of())).thenReturn(List.of());

            // When
            List<PatientResponse> result = patientService.getAllActivePatients();

            // Then
            assertThat(result).isEmpty();
        }
    }

    // UPDATE TESTS

    @Nested
    @DisplayName("updatePatient()")
    class UpdatePatientTests {

        @Test
        @DisplayName("Should update patient successfully with user info")
        void updatePatient_validRequest_returnsUpdatedPatientWithUserInfo() {
            // Given
            Patient updatedPatient = testPatient.toBuilder().bloodType("A+").build();

            when(patientRepository.findByPatientUuid("patient-uuid-123")).thenReturn(Optional.of(testPatient));
            when(patientMapper.updateEntity(testPatient, testRequest)).thenReturn(updatedPatient);
            when(patientRepository.updatePatient(updatedPatient)).thenReturn(updatedPatient);
            when(userService.getUserByUuid("user-uuid-456")).thenReturn(testUserRequest);
            when(patientMapper.toResponseWithUserInfo(updatedPatient, testUserRequest)).thenReturn(testResponse);

            // When
            PatientResponse result = patientService.updatePatient("patient-uuid-123", testRequest);

            // Then
            assertThat(result).isNotNull();
            verify(patientRepository).updatePatient(updatedPatient);
            verify(userService).getUserByUuid("user-uuid-456");
        }

        @Test
        @DisplayName("Should throw exception when patient not found for update")
        void updatePatient_patientNotFound_throwsApiException() {
            // Given
            when(patientRepository.findByPatientUuid("unknown-uuid")).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> patientService.updatePatient("unknown-uuid", testRequest))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("Patient non trouvé");

            verify(patientRepository, never()).updatePatient(any());
        }
    }

    // DELETE TESTS

    @Nested
    @DisplayName("deletePatient()")
    class DeletePatientTests {

        @Test
        @DisplayName("Should soft delete patient successfully")
        void deletePatient_patientExists_deletesSuccessfully() {
            // Given
            when(patientRepository.findByPatientUuid("patient-uuid-123")).thenReturn(Optional.of(testPatient));
            when(patientRepository.softDeletePatientByPatientUuid("patient-uuid-123")).thenReturn(true);

            // When
            patientService.deletePatient("patient-uuid-123");

            // Then
            verify(patientRepository).softDeletePatientByPatientUuid("patient-uuid-123");
        }

        @Test
        @DisplayName("Should throw exception when patient not found for delete")
        void deletePatient_patientNotFound_throwsApiException() {
            // Given
            when(patientRepository.findByPatientUuid("unknown-uuid")).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> patientService.deletePatient("unknown-uuid"))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("Patient non trouvé");

            verify(patientRepository, never()).softDeletePatientByPatientUuid(any());
        }

        @Test
        @DisplayName("Should throw exception when delete fails")
        void deletePatient_deleteFails_throwsApiException() {
            // Given
            when(patientRepository.findByPatientUuid("patient-uuid-123")).thenReturn(Optional.of(testPatient));
            when(patientRepository.softDeletePatientByPatientUuid("patient-uuid-123")).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> patientService.deletePatient("patient-uuid-123"))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("Erreur lors de la suppression");
        }
    }

    // UTILITY TESTS

    @Nested
    @DisplayName("Utility Methods")
    class UtilityMethodsTests {

        @Test
        @DisplayName("hasPatientRecord should return true when patient exists")
        void hasPatientRecord_patientExists_returnsTrue() {
            // Given
            when(patientRepository.existsPatientByUserUuid("user-uuid-456")).thenReturn(true);

            // When
            boolean result = patientService.hasPatientRecord("user-uuid-456");

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("hasPatientRecord should return false when patient not exists")
        void hasPatientRecord_patientNotExists_returnsFalse() {
            // Given
            when(patientRepository.existsPatientByUserUuid("unknown-user")).thenReturn(false);

            // When
            boolean result = patientService.hasPatientRecord("unknown-user");

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("countActivePatients should return count")
        void countActivePatients_returnsCount() {
            // Given
            when(patientRepository.countPatientByActiveTrue()).thenReturn(42L);

            // When
            long result = patientService.countActivePatients();

            // Then
            assertThat(result).isEqualTo(42L);
        }
    }

    @Nested
    @DisplayName("getPatientByMedicalRecordNumber()")
    class GetPatientByMedicalRecordNumberTests {

        @Test
        @DisplayName("Should return patient with user info when found by medical record number")
        void getPatientByMedicalRecordNumber_patientExists_returnsPatientWithUserInfo() {
            // Given
            String medicalRecordNumber = "MED-2026-000001";
            when(patientRepository.findPatientByMedicalRecordNumber(medicalRecordNumber))
                    .thenReturn(Optional.of(testPatient));
            when(userService.getUserByUuid("user-uuid-456")).thenReturn(testUserRequest);
            when(patientMapper.toResponseWithUserInfo(testPatient, testUserRequest)).thenReturn(testResponse);

            // When
            PatientResponse result = patientService.getPatientByMedicalRecordNumber(medicalRecordNumber);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getMedicalRecordNumber()).isEqualTo(medicalRecordNumber);
            verify(patientRepository).findPatientByMedicalRecordNumber(medicalRecordNumber);
            verify(userService).getUserByUuid("user-uuid-456");
        }

        @Test
        @DisplayName("Should throw exception when medical record not found")
        void getPatientByMedicalRecordNumber_notFound_throwsApiException() {
            // Given
            String medicalRecordNumber = "MED-9999-999999";
            when(patientRepository.findPatientByMedicalRecordNumber(medicalRecordNumber))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> patientService.getPatientByMedicalRecordNumber(medicalRecordNumber))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("Dossier médical non trouvé");

            verify(patientRepository).findPatientByMedicalRecordNumber(medicalRecordNumber);
        }

        @Test
        @DisplayName("Should handle null medical record number")
        void getPatientByMedicalRecordNumber_nullInput_throwsApiException() {
            // When & Then
            assertThatThrownBy(() -> patientService.getPatientByMedicalRecordNumber(null))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("Dossier médical non trouvé");

            verify(patientRepository).findPatientByMedicalRecordNumber(null);
        }

        @Test
        @DisplayName("Should handle empty medical record number")
        void getPatientByMedicalRecordNumber_emptyInput_throwsApiException() {
            // When & Then
            assertThatThrownBy(() -> patientService.getPatientByMedicalRecordNumber(""))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("Dossier médical non trouvé");

            verify(patientRepository).findPatientByMedicalRecordNumber("");
        }
    }

    @Nested
    @DisplayName("getPatientsByBloodType()")
    class GetPatientsByBloodTypeTests {

        @Test
        @DisplayName("Should return patients when found by blood type")
        void getPatientsByBloodType_patientsExist_returnsList() {
            // Given
            String bloodType = "O+";
            List<Patient> patients = List.of(testPatient);
            List<PatientResponse> responses = List.of(testResponse);

            when(patientRepository.findPatientByBloodTypeAndActiveTrue(bloodType)).thenReturn(patients);
            when(patientMapper.toResponseList(patients)).thenReturn(responses);

            // When
            List<PatientResponse> result = patientService.getPatientsByBloodType(bloodType);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getBloodType()).isEqualTo(bloodType);
            verify(patientRepository).findPatientByBloodTypeAndActiveTrue(bloodType);
        }

        @Test
        @DisplayName("Should return empty list when no patients with blood type")
        void getPatientsByBloodType_noPatients_returnsEmptyList() {
            // Given
            String bloodType = "AB-";
            when(patientRepository.findPatientByBloodTypeAndActiveTrue(bloodType)).thenReturn(List.of());
            when(patientMapper.toResponseList(List.of())).thenReturn(List.of());

            // When
            List<PatientResponse> result = patientService.getPatientsByBloodType(bloodType);

            // Then
            assertThat(result).isEmpty();
            verify(patientRepository).findPatientByBloodTypeAndActiveTrue(bloodType);
        }

        @Test
        @DisplayName("Should handle null blood type")
        void getPatientsByBloodType_nullInput_returnsList() {
            // Given
            when(patientRepository.findPatientByBloodTypeAndActiveTrue(null)).thenReturn(List.of(testPatient));
            when(patientMapper.toResponseList(anyList())).thenReturn(List.of(testResponse));

            // When
            List<PatientResponse> result = patientService.getPatientsByBloodType(null);

            // Then
            assertThat(result).isNotEmpty();
            verify(patientRepository).findPatientByBloodTypeAndActiveTrue(null);
        }

        @Test
        @DisplayName("Should handle empty blood type")
        void getPatientsByBloodType_emptyInput_returnsList() {
            // Given
            when(patientRepository.findPatientByBloodTypeAndActiveTrue("")).thenReturn(List.of(testPatient));
            when(patientMapper.toResponseList(anyList())).thenReturn(List.of(testResponse));

            // When
            List<PatientResponse> result = patientService.getPatientsByBloodType("");

            // Then
            assertThat(result).isNotEmpty();
            verify(patientRepository).findPatientByBloodTypeAndActiveTrue("");
        }
    }

    @Nested
    @DisplayName("Medical Record Number Generation")
    class MedicalRecordNumberGenerationTests {

        @Test
        @DisplayName("Should generate medical record number with correct format")
        void generateMedicalRecordNumber_returnsCorrectFormat() {
            // Given
            PatientServiceImpl service = new PatientServiceImpl(patientRepository, patientMapper, userService);

            // Use reflection to test private method
            try {
                java.lang.reflect.Method method = PatientServiceImpl.class.getDeclaredMethod("generateMedicalRecordNumber");
                method.setAccessible(true);

                // When
                String result = (String) method.invoke(service);

                // Then
                assertThat(result).matches("MED-\\d{4}-\\d{6}");

                // Extract year
                String yearPart = result.substring(4, 8);
                int year = Integer.parseInt(yearPart);
                int currentYear = Year.now().getValue();

                assertThat(year).isEqualTo(currentYear);

            } catch (Exception e) {
                fail("Failed to test private method", e);
            }
        }
    }

}