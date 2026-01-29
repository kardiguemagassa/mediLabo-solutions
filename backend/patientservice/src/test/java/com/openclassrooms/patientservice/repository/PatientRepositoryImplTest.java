package com.openclassrooms.patientservice.repository;

import com.openclassrooms.patientservice.exception.ApiException;
import com.openclassrooms.patientservice.model.Patient;
import com.openclassrooms.patientservice.repository.implementation.PatientRepositoryImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.openclassrooms.patientservice.query.PatientQuery.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class PatientRepositoryImplTest {

    @Mock
    private JdbcClient jdbcClient;

    @Mock
    private JdbcClient.StatementSpec statementSpec;

    @Mock
    private JdbcClient.MappedQuerySpec<Patient> patientQuerySpec;

    @Mock
    private JdbcClient.MappedQuerySpec<Boolean> booleanQuerySpec;

    @Mock
    private JdbcClient.MappedQuerySpec<Long> longQuerySpec;

    @InjectMocks
    private PatientRepositoryImpl repository;

    private Patient patient;

    @BeforeEach
    void setUp() {
        patient = Patient.builder()
                .patientUuid("uuid-123")
                .userUuid("user-123")
                .medicalRecordNumber("MED-001")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender("M")
                .bloodType("O+")
                .heightCm(180)
                .weightKg(BigDecimal.valueOf(75))
                .build();

        lenient().when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        lenient().when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);
    }

    // CREATE

    @Test
    void savePatient_shouldReturnSavedPatient_whenSuccess() {
        when(statementSpec.query(Patient.class)).thenReturn(patientQuerySpec);
        when(patientQuerySpec.single()).thenReturn(patient);

        Patient result = repository.savePatient(patient);
        assertThat(result).isEqualTo(patient);
    }

    @Test
    void savePatient_whenJdbcFails_shouldThrowApiException() {
        when(statementSpec.query(Patient.class)).thenReturn(patientQuerySpec);
        when(patientQuerySpec.single()).thenThrow(new RuntimeException("DB error"));

        assertThatThrownBy(() -> repository.savePatient(patient))
                .isInstanceOf(ApiException.class)
                .hasMessage("Erreur lors de la création du patient");
    }

    // ================= READ =================

    @Test
    void findByPatientUuid_shouldReturnPatient_whenFound() {
        when(statementSpec.query(Patient.class)).thenReturn(patientQuerySpec);
        when(patientQuerySpec.single()).thenReturn(patient);

        Optional<Patient> result = repository.findByPatientUuid("uuid-123");
        assertThat(result).contains(patient);
    }

    @Test
    void findByPatientUuid_shouldReturnEmpty_whenNotFound() {
        when(statementSpec.query(Patient.class)).thenReturn(patientQuerySpec);
        when(patientQuerySpec.single()).thenThrow(new EmptyResultDataAccessException(1));

        Optional<Patient> result = repository.findByPatientUuid("non-existent");
        assertThat(result).isEmpty();
    }

    @Test
    void findByPatientUuid_whenJdbcFails_shouldThrowApiException() {
        when(statementSpec.query(Patient.class)).thenReturn(patientQuerySpec);
        when(patientQuerySpec.single()).thenThrow(new RuntimeException("DB error"));

        assertThatThrownBy(() -> repository.findByPatientUuid("uuid"))
                .isInstanceOf(ApiException.class)
                .hasMessage("Erreur lors de la recherche du patient");
    }

    @Test
    void findByUserUuid_shouldReturnPatient_whenFound() {
        when(statementSpec.query(Patient.class)).thenReturn(patientQuerySpec);
        when(patientQuerySpec.single()).thenReturn(patient);

        Optional<Patient> result = repository.findByUserUuid("user-123");
        assertThat(result).contains(patient);
    }

    @Test
    void findByUserUuid_shouldReturnEmpty_whenNotFound() {
        when(statementSpec.query(Patient.class)).thenReturn(patientQuerySpec);
        when(patientQuerySpec.single()).thenThrow(new EmptyResultDataAccessException(1));

        Optional<Patient> result = repository.findByUserUuid("non-existent");
        assertThat(result).isEmpty();
    }

    @Test
    void findByUserUuid_whenJdbcFails_shouldThrowApiException() {
        when(statementSpec.query(Patient.class)).thenReturn(patientQuerySpec);
        when(patientQuerySpec.single()).thenThrow(new RuntimeException("DB error"));

        assertThatThrownBy(() -> repository.findByUserUuid("user"))
                .isInstanceOf(ApiException.class)
                .hasMessage("Erreur lors de la recherche du patient");
    }

    @Test
    void findPatientByMedicalRecordNumber_shouldReturnPatient_whenFound() {
        when(statementSpec.query(Patient.class)).thenReturn(patientQuerySpec);
        when(patientQuerySpec.single()).thenReturn(patient);

        Optional<Patient> result = repository.findPatientByMedicalRecordNumber("MED-001");
        assertThat(result).contains(patient);
    }

    @Test
    void findPatientByMedicalRecordNumber_shouldReturnEmpty_whenNotFound() {
        when(statementSpec.query(Patient.class)).thenReturn(patientQuerySpec);
        when(patientQuerySpec.single()).thenThrow(new EmptyResultDataAccessException(1));

        Optional<Patient> result = repository.findPatientByMedicalRecordNumber("non-existent");
        assertThat(result).isEmpty();
    }

    @Test
    void findPatientByMedicalRecordNumber_whenJdbcFails_shouldThrowApiException() {
        when(statementSpec.query(Patient.class)).thenReturn(patientQuerySpec);
        when(patientQuerySpec.single()).thenThrow(new RuntimeException("DB error"));

        assertThatThrownBy(() -> repository.findPatientByMedicalRecordNumber("MED"))
                .isInstanceOf(ApiException.class)
                .hasMessage("Erreur lors de la recherche du patient");
    }

    @Test
    void findAllPatientByActiveTrue_shouldReturnListOfPatients() {
        List<Patient> patients = List.of(patient);
        when(statementSpec.query(Patient.class)).thenReturn(patientQuerySpec);
        when(patientQuerySpec.list()).thenReturn(patients);

        List<Patient> result = repository.findAllPatientByActiveTrue();
        assertThat(result).hasSize(1);
    }

    @Test
    void findAllPatientByActiveTrue_whenJdbcFails_shouldThrowApiException() {
        when(statementSpec.query(Patient.class)).thenReturn(patientQuerySpec);
        when(patientQuerySpec.list()).thenThrow(new RuntimeException("DB error"));

        assertThatThrownBy(() -> repository.findAllPatientByActiveTrue())
                .isInstanceOf(ApiException.class)
                .hasMessage("Erreur lors de la récupération des patients");
    }

    @Test
    void findPatientByBloodTypeAndActiveTrue_shouldReturnListOfPatients() {
        List<Patient> patients = List.of(patient);
        when(statementSpec.query(Patient.class)).thenReturn(patientQuerySpec);
        when(patientQuerySpec.list()).thenReturn(patients);

        List<Patient> result = repository.findPatientByBloodTypeAndActiveTrue("O+");
        assertThat(result).hasSize(1);
    }

    @Test
    void findPatientByBloodTypeAndActiveTrue_whenJdbcFails_shouldThrowApiException() {
        when(statementSpec.query(Patient.class)).thenReturn(patientQuerySpec);
        when(patientQuerySpec.list()).thenThrow(new RuntimeException("DB error"));

        assertThatThrownBy(() -> repository.findPatientByBloodTypeAndActiveTrue("O+"))
                .isInstanceOf(ApiException.class)
                .hasMessage("Erreur lors de la recherche par groupe sanguin");
    }

    // EXISTS

    @Test
    void existsPatientByUserUuid_shouldReturnTrue_whenExists() {
        when(statementSpec.query(Boolean.class)).thenReturn(booleanQuerySpec);
        when(booleanQuerySpec.single()).thenReturn(true);

        boolean result = repository.existsPatientByUserUuid("user-123");
        assertThat(result).isTrue();
    }

    @Test
    void existsPatientByUserUuid_shouldReturnFalse_whenNotExists() {
        when(statementSpec.query(Boolean.class)).thenReturn(booleanQuerySpec);
        when(booleanQuerySpec.single()).thenReturn(false);

        boolean result = repository.existsPatientByUserUuid("user-123");
        assertThat(result).isFalse();
    }

    @Test
    void existsPatientByUserUuid_whenJdbcFails_shouldReturnFalse() {
        when(statementSpec.query(Boolean.class)).thenReturn(booleanQuerySpec);
        when(booleanQuerySpec.single()).thenThrow(new RuntimeException("DB error"));

        boolean result = repository.existsPatientByUserUuid("user");
        assertThat(result).isFalse();
    }

    @Test
    void existsPatientByMedicalRecordNumber_shouldReturnTrue_whenExists() {
        when(statementSpec.query(Boolean.class)).thenReturn(booleanQuerySpec);
        when(booleanQuerySpec.single()).thenReturn(true);

        boolean result = repository.existsPatientByMedicalRecordNumber("MED-001");
        assertThat(result).isTrue();
    }

    @Test
    void existsPatientByMedicalRecordNumber_shouldReturnFalse_whenNotExists() {
        when(statementSpec.query(Boolean.class)).thenReturn(booleanQuerySpec);
        when(booleanQuerySpec.single()).thenReturn(false);

        boolean result = repository.existsPatientByMedicalRecordNumber("MED-001");
        assertThat(result).isFalse();
    }

    @Test
    void existsPatientByMedicalRecordNumber_whenJdbcFails_shouldReturnFalse() {
        when(statementSpec.query(Boolean.class)).thenReturn(booleanQuerySpec);
        when(booleanQuerySpec.single()).thenThrow(new RuntimeException("DB error"));

        boolean result = repository.existsPatientByMedicalRecordNumber("MED");
        assertThat(result).isFalse();
    }

    // COUNT

    @Test
    void countPatientByActiveTrue_shouldReturnCount() {
        when(statementSpec.query(Long.class)).thenReturn(longQuerySpec);
        when(longQuerySpec.single()).thenReturn(5L);

        long result = repository.countPatientByActiveTrue();
        assertThat(result).isEqualTo(5L);
    }

    @Test
    void countPatientByActiveTrue_whenJdbcFails_shouldReturnZero() {
        when(statementSpec.query(Long.class)).thenReturn(longQuerySpec);
        when(longQuerySpec.single()).thenThrow(new RuntimeException("DB error"));

        long result = repository.countPatientByActiveTrue();
        assertThat(result).isZero();
    }

    // UPDATE

    @Test
    void updatePatient_shouldReturnUpdatedPatient_whenSuccess() {
        when(statementSpec.query(Patient.class)).thenReturn(patientQuerySpec);
        when(patientQuerySpec.single()).thenReturn(patient);

        Patient result = repository.updatePatient(patient);
        assertThat(result).isEqualTo(patient);
    }

    @Test
    void updatePatient_shouldThrowApiException_whenPatientNotFound() {
        when(statementSpec.query(Patient.class)).thenReturn(patientQuerySpec);
        when(patientQuerySpec.single()).thenThrow(new EmptyResultDataAccessException(1));

        assertThatThrownBy(() -> repository.updatePatient(patient))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Patient non trouvé");
    }

    @Test
    void updatePatient_whenJdbcFails_shouldThrowApiException() {
        when(statementSpec.query(Patient.class)).thenReturn(patientQuerySpec);
        when(patientQuerySpec.single()).thenThrow(new RuntimeException("DB error"));

        assertThatThrownBy(() -> repository.updatePatient(patient))
                .isInstanceOf(ApiException.class)
                .hasMessage("Erreur lors de la mise à jour du patient");
    }

    // DELETE

    @Test
    void softDeletePatientByPatientUuid_shouldReturnTrue_whenDeleted() {
        when(statementSpec.update()).thenReturn(1);

        boolean result = repository.softDeletePatientByPatientUuid("uuid-123");
        assertThat(result).isTrue();
    }

    @Test
    void softDeletePatientByPatientUuid_shouldReturnFalse_whenNoPatientDeleted() {
        when(statementSpec.update()).thenReturn(0);

        boolean result = repository.softDeletePatientByPatientUuid("non-existent");
        assertThat(result).isFalse();
    }

    @Test
    void softDeletePatientByPatientUuid_whenJdbcFails_shouldThrowApiException() {
        when(statementSpec.update()).thenThrow(new RuntimeException("DB error"));

        assertThatThrownBy(() -> repository.softDeletePatientByPatientUuid("uuid"))
                .isInstanceOf(ApiException.class)
                .hasMessage("Erreur lors de la suppression du patient");
    }
}