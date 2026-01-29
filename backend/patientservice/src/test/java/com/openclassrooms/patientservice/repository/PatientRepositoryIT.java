package com.openclassrooms.patientservice.repository;

import com.openclassrooms.patientservice.exception.ApiException;
import com.openclassrooms.patientservice.model.Patient;

import com.openclassrooms.patientservice.repository.implementation.PatientRepositoryImpl;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

/**
 * Tests d'intégration pour PatientRepository avec PostgreSQL (TestContainers).
 *
 * @author Kardigué MAGASSA
 */
@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Import({PatientRepositoryImpl.class})
@DisplayName("PatientRepository Integration Tests")
class PatientRepositoryIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("patientdb_test")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("schema.sql");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> "http://localhost:9001");
        registry.add("eureka.client.enabled", () -> false);
    }

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private JdbcClient jdbcClient;

    private Patient testPatient;

    @BeforeEach
    void setUp() {
        // Clean up before each test
        jdbcClient.sql("DELETE FROM patients").update();

        testPatient = Patient.builder()
                .patientUuid("test-patient-uuid-" + System.currentTimeMillis())
                .userUuid("test-user-uuid-123")
                .medicalRecordNumber("MED-2026-" + String.format("%06d", (int)(Math.random() * 999999)))
                .dateOfBirth(LocalDate.of(1990, 5, 15))
                .gender("M")
                .bloodType("O+")
                .heightCm(180)
                .weightKg(BigDecimal.valueOf(75))
                .allergies("Penicillin")
                .chronicConditions("None")
                .currentMedications("None")
                .emergencyContactName("Emergency Contact")
                .emergencyContactPhone("+33600000000")
                .emergencyContactRelationship("Family")
                .insuranceProvider("CPAM")
                .insuranceNumber("123456")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // SAVE TESTS

    @Test
    @Order(1)
    @DisplayName("Should save patient successfully")
    void save_validPatient_savesAndReturnsWithId() {
        // When
        Patient savedPatient = patientRepository.savePatient(testPatient);

        // Then
        assertThat(savedPatient).isNotNull();
        assertThat(savedPatient.getPatientId()).isNotNull().isPositive();
        assertThat(savedPatient.getPatientUuid()).isEqualTo(testPatient.getPatientUuid());
        assertThat(savedPatient.getMedicalRecordNumber()).isEqualTo(testPatient.getMedicalRecordNumber());
    }

    // FIND TESTS

    @Test
    @Order(2)
    @DisplayName("Should find patient by UUID")
    void findByPatientUuid_patientExists_returnsPatient() {
        // Given
        Patient saved = patientRepository.savePatient(testPatient);

        // When
        Optional<Patient> result = patientRepository.findByPatientUuid(saved.getPatientUuid());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getPatientUuid()).isEqualTo(saved.getPatientUuid());
        assertThat(result.get().getBloodType()).isEqualTo("O+");
    }

    @Test
    @Order(3)
    @DisplayName("Should return empty when patient not found by UUID")
    void findByPatientUuid_patientNotExists_returnsEmpty() {
        // When
        Optional<Patient> result = patientRepository.findByPatientUuid("non-existent-uuid");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @Order(4)
    @DisplayName("Should find patient by user UUID")
    void findByUserUuid_patientExists_returnsPatient() {
        // Given
        Patient saved = patientRepository.savePatient(testPatient);

        // When
        Optional<Patient> result = patientRepository.findByUserUuid(saved.getUserUuid());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getUserUuid()).isEqualTo(saved.getUserUuid());
    }

    @Test
    @Order(5)
    @DisplayName("Should find patient by medical record number")
    void findByMedicalRecordNumber_patientExists_returnsPatient() {
        // Given
        Patient saved = patientRepository.savePatient(testPatient);

        // When
        Optional<Patient> result = patientRepository.findPatientByMedicalRecordNumber(saved.getMedicalRecordNumber());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getMedicalRecordNumber()).isEqualTo(saved.getMedicalRecordNumber());
    }

    @Test
    @Order(6)
    @DisplayName("Should find all active patients")
    void findAllByActiveTrue_activePatients_returnsList() {
        // Given
        patientRepository.savePatient(testPatient);

        Patient patient2 = testPatient.toBuilder()
                .patientUuid("patient-uuid-2")
                .userUuid("user-uuid-2")
                .medicalRecordNumber("MED-2026-000002")
                .build();
        patientRepository.savePatient(patient2);

        // When
        List<Patient> result = patientRepository.findAllPatientByActiveTrue(); //findAllByActiveTrue

        // Then
        assertThat(result).hasSize(2);
    }

    @Test
    @Order(7)
    @DisplayName("Should find patients by blood type")
    void findByBloodTypeAndActiveTrue_patientsExist_returnsList() {
        // Given
        patientRepository.savePatient(testPatient);

        // When
        List<Patient> result = patientRepository.findPatientByBloodTypeAndActiveTrue("O+"); //findByBloodTypeAndActiveTrue

        // Then
        assertThat(result).isNotEmpty();
        assertThat(result).allMatch(p -> p.getBloodType().equals("O+"));
    }

    // EXISTS TESTS

    @Test
    @Order(8)
    @DisplayName("Should return true when patient exists by user UUID")
    void existsByUserUuid_patientExists_returnsTrue() {
        // Given
        Patient saved = patientRepository.savePatient(testPatient);

        // When
        boolean exists = patientRepository.existsPatientByUserUuid(saved.getUserUuid());

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @Order(9)
    @DisplayName("Should return false when patient not exists by user UUID")
    void existsByUserUuid_patientNotExists_returnsFalse() {
        // When
        boolean exists = patientRepository.existsPatientByUserUuid("non-existent-user");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @Order(10)
    @DisplayName("Should return true when medical record number exists")
    void existsByMedicalRecordNumber_exists_returnsTrue() {
        // Given
        Patient saved = patientRepository.savePatient(testPatient);

        // When
        boolean exists = patientRepository.existsPatientByMedicalRecordNumber(saved.getMedicalRecordNumber());

        // Then
        assertThat(exists).isTrue();
    }

    // COUNT TESTS

    @Test
    @Order(11)
    @DisplayName("Should count active patients")
    void countByActiveTrue_activePatients_returnsCount() {
        // Given
        patientRepository.savePatient(testPatient);

        // When
        long count = patientRepository.countPatientByActiveTrue();

        // Then
        assertThat(count).isGreaterThanOrEqualTo(1);
    }

    // UPDATE TESTS

    @Test
    @Order(12)
    @DisplayName("Should update patient successfully")
    void update_validPatient_updatesAndReturns() {
        // Given
        Patient saved = patientRepository.savePatient(testPatient);
        saved.setBloodType("A+");
        saved.setWeightKg(BigDecimal.valueOf(80));

        // When
        Patient updated = patientRepository.updatePatient(saved);

        // Then
        assertThat(updated.getBloodType()).isEqualTo("A+");
        assertThat(updated.getWeightKg()).isEqualByComparingTo(BigDecimal.valueOf(80));
    }

    // DELETE TESTS

    @Test
    @Order(13)
    @DisplayName("Should soft delete patient successfully")
    void softDeleteByPatientUuid_patientExists_deactivates() {
        // Given
        Patient saved = patientRepository.savePatient(testPatient);

        // When
        boolean deleted = patientRepository.softDeletePatientByPatientUuid(saved.getPatientUuid());

        // Then
        assertThat(deleted).isTrue();

        // Verify patient is deactivated
        Optional<Patient> result = patientRepository.findByPatientUuid(saved.getPatientUuid());
        assertThat(result).isEmpty(); // findByPatientUuid only returns active patients
    }

    @Test
    @Order(14)
    @DisplayName("Should return false when trying to delete non-existent patient")
    void softDeleteByPatientUuid_patientNotExists_returnsFalse() {
        // When
        boolean deleted = patientRepository.softDeletePatientByPatientUuid("non-existent-uuid");

        // Then
        assertThat(deleted).isFalse();
    }


    @Test
    @DisplayName("Should throw ApiException when saving patient fails")
    void savePatient_whenDatabaseError_throwsApiException() {
        // Given : patient invalide (UUID null → erreur SQL)
        Patient invalidPatient = testPatient.toBuilder().patientUuid(null).build(); // Provoque une erreur NOT NULL;

        // When / Then
        assertThatThrownBy(() -> patientRepository.savePatient(invalidPatient))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Erreur lors de la création du patient");
    }

}