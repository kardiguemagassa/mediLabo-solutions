package com.openclassrooms.patientservice.repository.implementation;

import com.openclassrooms.patientservice.exception.ApiException;
import com.openclassrooms.patientservice.model.Patient;
import com.openclassrooms.patientservice.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static com.openclassrooms.patientservice.query.PatientQuery.*;
import static java.lang.String.format;

/**
 * Implémentation JdbcClient du repository Patient
 * Utilise SQL natif au lieu de JPA
 *
 * @author FirstName LastName
 * @version 1.0
 * @since 2026-01-09
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class PatientRepositoryImpl implements PatientRepository {

    private final JdbcClient jdbc;

    @Override
    public Patient save(Patient patient) {
        try {
            return jdbc.sql(INSERT_PATIENT_QUERY)
                    .param("patientUuid", patient.getPatientUuid())
                    .param("userUuid", patient.getUserUuid())
                    .param("dateOfBirth", patient.getDateOfBirth())
                    .param("gender", patient.getGender())
                    .param("bloodType", patient.getBloodType())
                    .param("heightCm", patient.getHeightCm())
                    .param("weightKg", patient.getWeightKg())
                    .param("allergies", patient.getAllergies())
                    .param("chronicConditions", patient.getChronicConditions())
                    .param("currentMedications", patient.getCurrentMedications())
                    .param("emergencyContactName", patient.getEmergencyContactName())
                    .param("emergencyContactPhone", patient.getEmergencyContactPhone())
                    .param("emergencyContactRelationship", patient.getEmergencyContactRelationship())
                    .param("medicalRecordNumber", patient.getMedicalRecordNumber())
                    .param("insuranceNumber", patient.getInsuranceNumber())
                    .param("insuranceProvider", patient.getInsuranceProvider())
                    .param("active", true)
                    .query(Patient.class)
                    .single();
        } catch (Exception e) {
            log.error("Error saving patient: {}", e.getMessage(), e);
            throw new ApiException("Failed to create patient: " + e.getMessage());
        }
    }

    @Override
    public Patient update(Patient patient) {
        try {
            return jdbc.sql(UPDATE_PATIENT_QUERY)
                    .param("patientUuid", patient.getPatientUuid())
                    .param("dateOfBirth", patient.getDateOfBirth())
                    .param("gender", patient.getGender())
                    .param("bloodType", patient.getBloodType())
                    .param("heightCm", patient.getHeightCm())
                    .param("weightKg", patient.getWeightKg())
                    .param("allergies", patient.getAllergies())
                    .param("chronicConditions", patient.getChronicConditions())
                    .param("currentMedications", patient.getCurrentMedications())
                    .param("emergencyContactName", patient.getEmergencyContactName())
                    .param("emergencyContactPhone", patient.getEmergencyContactPhone())
                    .param("emergencyContactRelationship", patient.getEmergencyContactRelationship())
                    .param("insuranceNumber", patient.getInsuranceNumber())
                    .param("insuranceProvider", patient.getInsuranceProvider())
                    .query(Patient.class)
                    .single();
        } catch (EmptyResultDataAccessException e) {
            log.error("Patient not found for update: {}", patient.getPatientUuid());
            throw new ApiException(format("Patient not found: %s", patient.getPatientUuid()));
        } catch (Exception e) {
            log.error("Error updating patient: {}", e.getMessage(), e);
            throw new ApiException("Failed to update patient: " + e.getMessage());
        }
    }

    @Override
    public Optional<Patient> findByPatientUuid(String patientUuid) {
        try {
            Patient patient = jdbc.sql(SELECT_PATIENT_BY_UUID_QUERY)
                    .param("patientUuid", patientUuid)
                    .query(Patient.class)
                    .single();
            return Optional.of(patient);
        } catch (EmptyResultDataAccessException e) {
            log.debug("Patient not found with UUID: {}", patientUuid);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error finding patient by UUID: {}", e.getMessage(), e);
            throw new ApiException("Failed to find patient: " + e.getMessage());
        }
    }

    @Override
    public Optional<Patient> findByUserUuid(String userUuid) {
        try {
            Patient patient = jdbc.sql(SELECT_PATIENT_BY_USER_UUID_QUERY)
                    .param("userUuid", userUuid)
                    .query(Patient.class)
                    .single();
            return Optional.of(patient);
        } catch (EmptyResultDataAccessException e) {
            log.debug("Patient not found for user UUID: {}", userUuid);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error finding patient by user UUID: {}", e.getMessage(), e);
            throw new ApiException("Failed to find patient: " + e.getMessage());
        }
    }

    @Override
    public boolean existsByUserUuid(String userUuid) {
        try {
            Boolean exists = jdbc.sql(EXISTS_BY_USER_UUID_QUERY)
                    .param("userUuid", userUuid)
                    .query(Boolean.class)
                    .single();
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.error("Error checking patient existence: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public List<Patient> findAllByActiveTrue() {
        try {
            return jdbc.sql(SELECT_ALL_ACTIVE_PATIENTS_QUERY)
                    .query(Patient.class)
                    .list();
        } catch (Exception e) {
            log.error("Error finding all active patients: {}", e.getMessage(), e);
            throw new ApiException("Failed to retrieve patients: " + e.getMessage());
        }
    }

    @Override
    public Optional<Patient> findByMedicalRecordNumber(String medicalRecordNumber) {
        try {
            Patient patient = jdbc.sql(SELECT_PATIENT_BY_MEDICAL_RECORD_NUMBER_QUERY)
                    .param("medicalRecordNumber", medicalRecordNumber)
                    .query(Patient.class)
                    .single();
            return Optional.of(patient);
        } catch (EmptyResultDataAccessException e) {
            log.debug("Patient not found with medical record number: {}", medicalRecordNumber);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error finding patient by medical record number: {}", e.getMessage(), e);
            throw new ApiException("Failed to find patient: " + e.getMessage());
        }
    }

    @Override
    public List<Patient> findAllActivePatientsOrderByCreatedAtDesc() {
        return findAllByActiveTrue();
    }

    @Override
    public long countByActiveTrue() {
        try {
            Long count = jdbc.sql(COUNT_ACTIVE_PATIENTS_QUERY)
                    .query(Long.class)
                    .single();
            return count != null ? count : 0L;
        } catch (Exception e) {
            log.error("Error counting active patients: {}", e.getMessage(), e);
            return 0L;
        }
    }

    @Override
    public List<Patient> findByBloodTypeAndActiveTrue(String bloodType) {
        try {
            return jdbc.sql(SELECT_PATIENTS_BY_BLOOD_TYPE_QUERY)
                    .param("bloodType", bloodType)
                    .query(Patient.class)
                    .list();
        } catch (Exception e) {
            log.error("Error finding patients by blood type: {}", e.getMessage(), e);
            throw new ApiException("Failed to retrieve patients: " + e.getMessage());
        }
    }

    @Override
    public void softDelete(String patientUuid) {
        try {
            int updated = jdbc.sql(SOFT_DELETE_PATIENT_QUERY)
                    .param("patientUuid", patientUuid)
                    .update();

            if (updated == 0) {
                throw new ApiException(format("Patient not found: %s", patientUuid));
            }

            log.info("Patient soft deleted: {}", patientUuid);
        } catch (Exception e) {
            log.error("Error soft deleting patient: {}", e.getMessage(), e);
            throw new ApiException("Failed to delete patient: " + e.getMessage());
        }
    }
}