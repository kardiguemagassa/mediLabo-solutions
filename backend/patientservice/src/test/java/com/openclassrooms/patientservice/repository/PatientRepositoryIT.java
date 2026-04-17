package com.openclassrooms.patientservice.repository;

import com.openclassrooms.patientservice.exception.ApiException;
import com.openclassrooms.patientservice.model.Patient;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.transaction.annotation.Transactional;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
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
@ActiveProfiles("test")
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("PatientRepository Integration Tests")
class PatientRepositoryIT {

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private JdbcClient jdbcClient;

    private Patient testPatient;

    @PersistenceContext
    private EntityManager entityManager;

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

    @Test
    @Order(1)
    @DisplayName("Should save patient successfully")
    void save_validPatient_savesAndReturnsWithId() {
        // When
        Patient savedPatient = patientRepository.save(testPatient);

        // Then
        assertThat(savedPatient).isNotNull();
        assertThat(savedPatient.getPatientId()).isNotNull().isPositive();
        assertThat(savedPatient.getPatientUuid()).isEqualTo(testPatient.getPatientUuid());
        assertThat(savedPatient.getMedicalRecordNumber()).isEqualTo(testPatient.getMedicalRecordNumber());
    }

    @Test
    @Order(2)
    @DisplayName("Should find patient by UUID")
    void findByPatientUuid_patientExists_returnsPatient() {
        // Given
        Patient saved = patientRepository.save(testPatient);

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
        Patient saved = patientRepository.save(testPatient);

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
        Patient saved = patientRepository.save(testPatient);

        // When
        Optional<Patient> result = patientRepository.findByMedicalRecordNumber(saved.getMedicalRecordNumber());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getMedicalRecordNumber()).isEqualTo(saved.getMedicalRecordNumber());
    }

    @Test
    @Order(6)
    @DisplayName("findByActiveTrue with pagination")
    void findByActiveTrue_ShouldReturnPagedResults() {
        Patient saved = patientRepository.save(testPatient);
        Page<Patient> result = patientRepository.findByActiveTrue(PageRequest.of(0, 10, Sort.by("createdAt").descending()));
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().getPatientUuid()).isEqualTo(saved.getPatientUuid());
    }

    @Test
    @Order(7)
    @DisplayName("Should find patients by blood type")
    void findByBloodTypeAndActiveTrue_patientsExist_returnsList() {
        // Given
        patientRepository.save(testPatient);

        // When
        List<Patient> result = patientRepository.findByBloodTypeAndActiveTrue("O+"); //findByBloodTypeAndActiveTrue

        // Then
        assertThat(result).isNotEmpty();
        assertThat(result).allMatch(p -> p.getBloodType().equals("O+"));
    }

    @Test
    @Order(8)
    @DisplayName("Should return true when patient exists by user UUID")
    void existsByUserUuid_patientExists_returnsTrue() {
        // Given
        Patient saved = patientRepository.save(testPatient);

        // When existsPatientByUserUuid
        boolean exists = patientRepository.existsByUserUuid(saved.getUserUuid());

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @Order(9)
    @DisplayName("Should return false when patient not exists by user UUID")
    void existsByUserUuid_patientNotExists_returnsFalse() {
        // When
        boolean exists = patientRepository.existsByUserUuid("non-existent-user");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @Order(10)
    @DisplayName("Should return true when medical record number exists")
    void existsByMedicalRecordNumber_exists_returnsTrue() {
        // Given
        Patient saved = patientRepository.save(testPatient);

        // When
        boolean exists = patientRepository.existsByMedicalRecordNumber(saved.getMedicalRecordNumber());

        // Then
        assertThat(exists).isTrue();
    }


    @Test
    @Order(11)
    @DisplayName("Should count active patients")
    void countByActiveTrue_activePatients_returnsCount() {
        // Given
        patientRepository.save(testPatient);

        // When countPatientByActiveTrue
        long count = patientRepository.countByActiveTrue();

        // Then
        assertThat(count).isGreaterThanOrEqualTo(1);
    }

    @Test
    @Order(12)
    @DisplayName("Should update patient successfully")
    void update_validPatient_updatesAndReturns() {
        // Given
        Patient saved = patientRepository.save(testPatient);
        saved.setBloodType("A+");
        saved.setWeightKg(BigDecimal.valueOf(80));

        // When updatePatient
        Patient updated = patientRepository.save(saved);

        // Then
        assertThat(updated.getBloodType()).isEqualTo("A+");
        assertThat(updated.getWeightKg()).isEqualByComparingTo(BigDecimal.valueOf(80));
    }

    @Test
    @Order(13)
    @DisplayName("Should soft delete patient successfully")
    void softDeleteByPatientUuid_patientExists_deactivates() {
        // Given
        Patient saved = patientRepository.save(testPatient);
        assertThat(saved.getActive()).isTrue();

        // When
        int deleted = patientRepository.softDeleteByPatientUuid(saved.getPatientUuid());

        // Then
        assertThat(deleted).isEqualTo(1);

        // Recharger depuis la base avec une nouvelle requête
        entityManager.flush();
        entityManager.clear();

        // Chercher directement avec une requête JPQL qui ignore le cache
        TypedQuery<Patient> query = entityManager.createQuery(
                "SELECT p FROM Patient p WHERE p.patientUuid = :uuid", Patient.class);
        query.setParameter("uuid", saved.getPatientUuid());
        Patient reloaded = query.getSingleResult();

        assertThat(reloaded.getActive()).isFalse();
    }

    @Test
    @Order(14)
    @DisplayName("Should return 0 when trying to delete non-existent patient")
    void softDeleteByPatientUuid_patientNotExists_returnsZero() {
        int deleted = patientRepository.softDeleteByPatientUuid("non-existent-uuid");
        assertThat(deleted).isZero();
    }

    @Test
    @DisplayName("Should throw DataIntegrityViolationException when saving invalid patient")
    void savePatient_whenDatabaseError_throwsException() {
        Patient invalidPatient = testPatient.toBuilder().patientUuid(null).build();

        assertThatThrownBy(() -> patientRepository.save(invalidPatient))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

}