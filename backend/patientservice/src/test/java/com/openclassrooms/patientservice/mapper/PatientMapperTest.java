package com.openclassrooms.patientservice.mapper;

import com.openclassrooms.patientservice.dtorequest.PatientRequest;
import com.openclassrooms.patientservice.dtorequest.UserRequest;
import com.openclassrooms.patientservice.dtoresponse.PatientResponse;
import com.openclassrooms.patientservice.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires pour PatientMapper.
 *
 * @author Kardigué MAGASSA
 */
@DisplayName("PatientMapper Unit Tests")
class PatientMapperTest {

    private PatientMapper patientMapper;
    private Patient testPatient;
    private PatientRequest testRequest;
    private UserRequest testUser;

    @BeforeEach
    void setUp() {
        patientMapper = new PatientMapper();

        testPatient = Patient.builder()
                .patientId(1L)
                .patientUuid("patient-uuid-123")
                .userUuid("user-uuid-456")
                .medicalRecordNumber("MED-2026-000001")
                .dateOfBirth(LocalDate.of(1990, 5, 15))
                .gender("M")
                .bloodType("O+")
                .heightCm(180)
                .weightKg(BigDecimal.valueOf(75))
                .allergies("Penicillin, Pollen")
                .chronicConditions("Asthma")
                .currentMedications("Ventolin")
                .emergencyContactName("Jane Doe")
                .emergencyContactPhone("+33612345678")
                .emergencyContactRelationship("Spouse")
                .insuranceProvider("CPAM")
                .insuranceNumber("123456789")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testRequest = PatientRequest.builder()
                .userUuid("user-uuid-456")
                .dateOfBirth(LocalDate.of(1990, 5, 15))
                .gender("M")
                .bloodType("O+")
                .heightCm(180)
                .weightKg(BigDecimal.valueOf(75))
                .allergies("Penicillin, Pollen")
                .chronicConditions("Asthma")
                .currentMedications("Ventolin")
                .emergencyContactName("Jane Doe")
                .emergencyContactPhone("+33612345678")
                .emergencyContactRelationship("Spouse")
                .insuranceProvider("CPAM")
                .insuranceNumber("123456789")
                .build();

        testUser = UserRequest.builder()
                .userUuid("user-uuid-456")
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@email.com")
                .phone("+33698765432")
                .imageUrl("https://example.com/image.jpg")
                .build();
    }

    // toEntity() TESTS

    @Nested
    @DisplayName("toEntity()")
    class ToEntityTests {
        @Test
        @DisplayName("Should create entity from request with generated UUID")
        void toEntity_validRequest_createsEntityWithUuid() {
            // Given - request avec tous les champs nécessaires
            PatientRequest fullRequest = PatientRequest.builder()
                    .userUuid("user-uuid-456")
                    .dateOfBirth(LocalDate.of(1990, 5, 15))
                    .gender("M")
                    .bloodType("O+")
                    .build();

            // When
            Patient result = patientMapper.toEntity(fullRequest, "MED-2026-123456");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getPatientUuid()).isNotNull().isNotEmpty();
            assertThat(result.getUserUuid()).isEqualTo("user-uuid-456");
            assertThat(result.getMedicalRecordNumber()).isEqualTo("MED-2026-123456");
            assertThat(result.getDateOfBirth()).isEqualTo(LocalDate.of(1990, 5, 15));
            assertThat(result.getGender()).isEqualTo("M");
            assertThat(result.getBloodType()).isEqualTo("O+");
            assertThat(result.getActive()).isTrue();
        }

        @Test
        @DisplayName("Should copy all fields from request")
        void toEntity_requestWithAllFields_copiesAllFields() {
            // When
            Patient result = patientMapper.toEntity(testRequest, "MED-2026-123456");

            // Then
            assertThat(result.getAllergies()).isEqualTo("Penicillin, Pollen");
            assertThat(result.getChronicConditions()).isEqualTo("Asthma");
            assertThat(result.getCurrentMedications()).isEqualTo("Ventolin");
            assertThat(result.getEmergencyContactName()).isEqualTo("Jane Doe");
            assertThat(result.getEmergencyContactPhone()).isEqualTo("+33612345678");
            assertThat(result.getInsuranceProvider()).isEqualTo("CPAM");
        }
    }

    // UPDATE_ENTITY TESTS

    @Nested
    @DisplayName("updateEntity()")
    class UpdateEntityTests {

        @Test
        @DisplayName("Should update all fields from request")
        void updateEntity_fullUpdate_updatesAllFields() {
            // Given - request avec TOUS les champs
            PatientRequest updateRequest = PatientRequest.builder()
                    .dateOfBirth(LocalDate.of(1985, 3, 20))
                    .gender("F")
                    .bloodType("A+")
                    .weightKg(BigDecimal.valueOf(80))
                    .heightCm((170))
                    .build();

            // When
            Patient result = patientMapper.updateEntity(testPatient, updateRequest);

            // Then
            assertThat(result.getBloodType()).isEqualTo("A+");
            assertThat(result.getWeightKg()).isEqualTo(BigDecimal.valueOf(80));
            assertThat(result.getGender()).isEqualTo("F");
            // UUID et medicalRecordNumber ne changent jamais
            assertThat(result.getPatientUuid()).isEqualTo("patient-uuid-123");
            assertThat(result.getMedicalRecordNumber()).isEqualTo("MED-2026-000001");
        }

        @Test
        @DisplayName("Should update timestamp")
        void updateEntity_anyUpdate_updatesTimestamp() {
            // Given
            LocalDateTime originalUpdatedAt = testPatient.getUpdatedAt();
            PatientRequest updateRequest = PatientRequest.builder()
                    .bloodType("A+")
                    .build();

            // When
            Patient result = patientMapper.updateEntity(testPatient, updateRequest);

            // Then
            assertThat(result.getUpdatedAt()).isAfterOrEqualTo(originalUpdatedAt);
        }
    }

    // toResponse() TESTS

    @Nested
    @DisplayName("toResponse()")
    class ToResponseTests {

        @Test
        @DisplayName("Should create response from entity")
        void toResponse_validEntity_createsResponse() {
            // When
            PatientResponse result = patientMapper.toResponse(testPatient);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getPatientUuid()).isEqualTo("patient-uuid-123");
            assertThat(result.getUserUuid()).isEqualTo("user-uuid-456");
            assertThat(result.getMedicalRecordNumber()).isEqualTo("MED-2026-000001");
            assertThat(result.getGender()).isEqualTo("M");
            assertThat(result.getBloodType()).isEqualTo("O+");
        }

        @Test
        @DisplayName("Should calculate age correctly")
        void toResponse_withDateOfBirth_calculatesAge() {
            // When
            PatientResponse result = patientMapper.toResponse(testPatient);

            // Then
            assertThat(result.getAge()).isGreaterThan(0);
            // Born in 1990, should be around 35-36 years old
            assertThat(result.getAge()).isBetween(34, 37);
        }

        @Test
        @DisplayName("Should calculate BMI correctly")
        void toResponse_withHeightAndWeight_calculatesBmi() {
            // When
            PatientResponse result = patientMapper.toResponse(testPatient);

            // Then
            // BMI = 75 / (1.80 * 1.80) = 23.15
            assertThat(result.getBmi()).isNotNull();
            assertThat(result.getBmi().doubleValue()).isBetween(23.0, 24.0);
        }

        @Test
        @DisplayName("Should return null BMI when height or weight is missing")
        void toResponse_missingHeightOrWeight_returnsNullBmi() {
            // Given
            testPatient.setHeightCm(null);

            // When
            PatientResponse result = patientMapper.toResponse(testPatient);

            // Then
            assertThat(result.getBmi()).isNull();
        }
    }

    // toResponseWithUserInfo() TESTS

    @Nested
    @DisplayName("toResponseWithUserInfo()")
    class ToResponseWithUserInfoTests {

        @Test
        @DisplayName("Should include user info in response")
        void toResponseWithUserInfo_withUser_includesUserInfo() {
            // When
            PatientResponse result = patientMapper.toResponseWithUserInfo(testPatient, testUser);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getPatientUuid()).isEqualTo("patient-uuid-123");

            assertThat(result.getUserInfo()).isNotNull();
            assertThat(result.getUserInfo().getFirstName()).isEqualTo("John");
            assertThat(result.getUserInfo().getLastName()).isEqualTo("Doe");
            assertThat(result.getUserInfo().getEmail()).isEqualTo("john.doe@email.com");
        }
    }

    // toResponseList() TESTS

    @Nested
    @DisplayName("toResponseList()")
    class ToResponseListTests {

        @Test
        @DisplayName("Should convert list of entities to responses")
        void toResponseList_multipleEntities_convertsAll() {
            // Given
            Patient patient2 = testPatient.toBuilder()
                    .patientUuid("patient-uuid-456")
                    .medicalRecordNumber("MED-2026-000002")
                    .build();
            List<Patient> patients = List.of(testPatient, patient2);

            // When
            List<PatientResponse> result = patientMapper.toResponseList(patients);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getPatientUuid()).isEqualTo("patient-uuid-123");
            assertThat(result.get(1).getPatientUuid()).isEqualTo("patient-uuid-456");
        }

        @Test
        @DisplayName("Should return empty list for empty input")
        void toResponseList_emptyList_returnsEmptyList() {
            // When
            List<PatientResponse> result = patientMapper.toResponseList(List.of());

            // Then
            assertThat(result).isEmpty();
        }
    }
}