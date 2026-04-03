package com.openclassrooms.patientservice.mapper;

import com.openclassrooms.patientservice.dto.PatientRequestDTO;
import com.openclassrooms.patientservice.dto.PatientResponseDTO;
import com.openclassrooms.patientservice.dto.UserRequestDTO;
import com.openclassrooms.patientservice.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PatientMapper Unit Tests")
class PatientMapperTest {

    private final PatientMapper patientMapper = Mappers.getMapper(PatientMapper.class);

    private Patient testPatient;
    private PatientRequestDTO testRequest;
    private UserRequestDTO testUser;

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

        testRequest = PatientRequestDTO.builder()
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

        testUser = UserRequestDTO.builder()
                .userUuid("user-uuid-456")
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@email.com")
                .phone("+33698765432")
                .address("123 Rue de Paris")
                .imageUrl("https://example.com/image.jpg")
                .build();
    }

    @Nested
    @DisplayName("toEntity()")
    class ToEntityTests {

        @Test
        @DisplayName("Should create entity from request with generated UUID")
        void toEntity_validRequest_createsEntityWithUuid() {
            PatientRequestDTO fullRequest = PatientRequestDTO.builder()
                    .userUuid("user-uuid-456")
                    .dateOfBirth(LocalDate.of(1990, 5, 15))
                    .gender("M")
                    .bloodType("O+")
                    .build();

            Patient result = patientMapper.toEntity(fullRequest, "MED-2026-123456");

            assertThat(result).isNotNull();
            assertThat(result.getPatientUuid()).isNotNull().isNotEmpty();
            assertThat(result.getUserUuid()).isEqualTo("user-uuid-456");
            assertThat(result.getMedicalRecordNumber()).isEqualTo("MED-2026-123456");
            assertThat(result.getDateOfBirth()).isEqualTo(LocalDate.of(1990, 5, 15));
            assertThat(result.getGender()).isEqualTo("M");
            assertThat(result.getBloodType()).isEqualTo("O+");
            assertThat(result.getActive()).isTrue();
            // patientId non initialisé (généré par DB)
            assertThat(result.getPatientId()).isNull();
        }

        @Test
        @DisplayName("Should copy all fields from request")
        void toEntity_requestWithAllFields_copiesAllFields() {
            Patient result = patientMapper.toEntity(testRequest, "MED-2026-123456");

            assertThat(result.getAllergies()).isEqualTo("Penicillin, Pollen");
            assertThat(result.getChronicConditions()).isEqualTo("Asthma");
            assertThat(result.getCurrentMedications()).isEqualTo("Ventolin");
            assertThat(result.getEmergencyContactName()).isEqualTo("Jane Doe");
            assertThat(result.getEmergencyContactPhone()).isEqualTo("+33612345678");
            assertThat(result.getInsuranceProvider()).isEqualTo("CPAM");
        }

        @Test
        @DisplayName("Should generate unique UUIDs for different calls")
        void toEntity_multipleCalls_generatesDifferentUuids() {
            Patient result1 = patientMapper.toEntity(testRequest, "MED-001");
            Patient result2 = patientMapper.toEntity(testRequest, "MED-002");

            assertThat(result1.getPatientUuid()).isNotEqualTo(result2.getPatientUuid());
        }
    }

    @Nested
    @DisplayName("updateEntity()")
    class UpdateEntityTests {

        @Test
        @DisplayName("Should update all provided fields")
        void updateEntity_fullUpdate_updatesAllFields() {
            PatientRequestDTO updateRequest = PatientRequestDTO.builder()
                    .dateOfBirth(LocalDate.of(1985, 3, 20))
                    .gender("F")
                    .bloodType("A+")
                    .weightKg(BigDecimal.valueOf(80))
                    .heightCm(170)
                    .build();

            Patient result = patientMapper.updateEntity(testPatient, updateRequest);

            assertThat(result.getBloodType()).isEqualTo("A+");
            assertThat(result.getWeightKg()).isEqualTo(BigDecimal.valueOf(80));
            assertThat(result.getGender()).isEqualTo("F");
            assertThat(result.getHeightCm()).isEqualTo(170);
            // UUID et medicalRecordNumber ignorés par MapStruct
            assertThat(result.getPatientUuid()).isEqualTo("patient-uuid-123");
            assertThat(result.getMedicalRecordNumber()).isEqualTo("MED-2026-000001");
        }

        @Test
        @DisplayName("Should not overwrite fields when request has null values (IGNORE strategy)")
        void updateEntity_nullFields_keepsExistingValues() {
            // Request avec seulement bloodType, tout le reste null
            PatientRequestDTO partialRequest = PatientRequestDTO.builder()
                    .bloodType("AB-")
                    .build();

            Patient result = patientMapper.updateEntity(testPatient, partialRequest);

            // bloodType mis à jour
            assertThat(result.getBloodType()).isEqualTo("AB-");
            // Les champs null dans la request ne sont PAS écrasés (IGNORE strategy)
            assertThat(result.getGender()).isEqualTo("M");
            assertThat(result.getHeightCm()).isEqualTo(180);
            assertThat(result.getWeightKg()).isEqualTo(BigDecimal.valueOf(75));
            assertThat(result.getAllergies()).isEqualTo("Penicillin, Pollen");
            assertThat(result.getEmergencyContactName()).isEqualTo("Jane Doe");
        }

        @Test
        @DisplayName("Should return the same object reference (in-place update)")
        void updateEntity_anyUpdate_returnsSameReference() {
            PatientRequestDTO updateRequest = PatientRequestDTO.builder()
                    .bloodType("A+")
                    .build();

            Patient result = patientMapper.updateEntity(testPatient, updateRequest);

            // MapStruct avec @MappingTarget modifie l'objet existant
            assertThat(result).isSameAs(testPatient);
        }
    }

    @Nested
    @DisplayName("toResponse()")
    class ToResponseTests {

        @Test
        @DisplayName("Should create response from entity")
        void toResponse_validEntity_createsResponse() {
            PatientResponseDTO result = patientMapper.toResponse(testPatient);

            assertThat(result).isNotNull();
            assertThat(result.getPatientUuid()).isEqualTo("patient-uuid-123");
            assertThat(result.getUserUuid()).isEqualTo("user-uuid-456");
            assertThat(result.getMedicalRecordNumber()).isEqualTo("MED-2026-000001");
            assertThat(result.getGender()).isEqualTo("M");
            assertThat(result.getBloodType()).isEqualTo("O+");
            assertThat(result.getUserInfo()).isNull();
        }

        @Test
        @DisplayName("Should calculate age correctly")
        void toResponse_withDateOfBirth_calculatesAge() {
            PatientResponseDTO result = patientMapper.toResponse(testPatient);

            assertThat(result.getAge()).isGreaterThan(0);
            assertThat(result.getAge()).isBetween(34, 37);
        }

        @Test
        @DisplayName("Should calculate BMI correctly")
        void toResponse_withHeightAndWeight_calculatesBmi() {
            PatientResponseDTO result = patientMapper.toResponse(testPatient);

            // BMI = 75 / (1.80 * 1.80) ≈ 23.15
            assertThat(result.getBmi()).isNotNull();
            assertThat(result.getBmi().doubleValue()).isBetween(23.0, 24.0);
        }

        @Test
        @DisplayName("Should return null BMI when height is missing")
        void toResponse_missingHeight_returnsNullBmi() {
            testPatient.setHeightCm(null);

            PatientResponseDTO result = patientMapper.toResponse(testPatient);

            assertThat(result.getBmi()).isNull();
        }

        @Test
        @DisplayName("Should return null BMI when weight is missing")
        void toResponse_missingWeight_returnsNullBmi() {
            testPatient.setWeightKg(null);

            PatientResponseDTO result = patientMapper.toResponse(testPatient);

            assertThat(result.getBmi()).isNull();
        }
    }

    @Nested
    @DisplayName("toResponseWithUserInfo()")
    class ToResponseWithUserInfoTests {

        @Test
        @DisplayName("Should include user info in response")
        void toResponseWithUserInfo_withUser_includesUserInfo() {
            PatientResponseDTO result = patientMapper.toResponseWithUserInfo(testPatient, testUser);

            assertThat(result).isNotNull();
            assertThat(result.getPatientUuid()).isEqualTo("patient-uuid-123");
            assertThat(result.getUserInfo()).isNotNull();
            assertThat(result.getUserInfo().getFirstName()).isEqualTo("John");
            assertThat(result.getUserInfo().getLastName()).isEqualTo("Doe");
            assertThat(result.getUserInfo().getEmail()).isEqualTo("john.doe@email.com");
            assertThat(result.getUserInfo().getPhone()).isEqualTo("+33698765432");
            assertThat(result.getUserInfo().getAddress()).isEqualTo("123 Rue de Paris");
            assertThat(result.getUserInfo().getImageUrl()).isEqualTo("https://example.com/image.jpg");
        }

        @Test
        @DisplayName("Should return response without user info when user is null")
        void toResponseWithUserInfo_nullUser_noUserInfo() {
            PatientResponseDTO result = patientMapper.toResponseWithUserInfo(testPatient, null);

            assertThat(result).isNotNull();
            assertThat(result.getPatientUuid()).isEqualTo("patient-uuid-123");
            assertThat(result.getUserInfo()).isNull();
        }
    }

    @Nested
    @DisplayName("toResponseList()")
    class ToResponseListTests {

        @Test
        @DisplayName("Should convert list of entities to responses")
        void toResponseList_multipleEntities_convertsAll() {
            Patient patient2 = testPatient.toBuilder()
                    .patientUuid("patient-uuid-456")
                    .medicalRecordNumber("MED-2026-000002")
                    .build();
            List<Patient> patients = List.of(testPatient, patient2);

            List<PatientResponseDTO> result = patientMapper.toResponseList(patients);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getPatientUuid()).isEqualTo("patient-uuid-123");
            assertThat(result.get(1).getPatientUuid()).isEqualTo("patient-uuid-456");
        }

        @Test
        @DisplayName("Should return empty list for empty input")
        void toResponseList_emptyList_returnsEmptyList() {
            List<PatientResponseDTO> result = patientMapper.toResponseList(List.of());

            assertThat(result).isEmpty();
        }
    }
}