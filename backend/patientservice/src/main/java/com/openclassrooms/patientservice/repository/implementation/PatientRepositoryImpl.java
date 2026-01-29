package com.openclassrooms.patientservice.repository.implementation;

import com.openclassrooms.patientservice.exception.ApiException;
import com.openclassrooms.patientservice.model.Patient;
import com.openclassrooms.patientservice.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static com.openclassrooms.patientservice.query.PatientQuery.*;

/**
 * Implémentation JdbcClient du repository Patient.
 * Utilise SQL natif pour un contrôle précis des requêtes.
 *
 * @author Kardigué MAGASSA
 * @version 2.0
 * @since 2026-01-09
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class PatientRepositoryImpl implements PatientRepository {

    private final JdbcClient jdbc;

    // CREATE

    @Override
    public Patient savePatient(Patient patient) {
        log.debug("Saving patient: {}", patient.getPatientUuid());
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
        } catch (Exception exception) {
            log.error("Error saving patient: {}", exception.getMessage(), exception);
            throw new ApiException("Erreur lors de la création du patient");
        }
    }

    // READ

    @Override
    public Optional<Patient> findByPatientUuid(String patientUuid) {
        log.debug("Finding patient by UUID: {}", patientUuid);
        try {
            Patient patient = jdbc.sql(SELECT_PATIENT_BY_UUID_QUERY).param("patientUuid", patientUuid).query(Patient.class).single();
            return Optional.of(patient);
        } catch (EmptyResultDataAccessException exception) {
            log.debug("Patient not found: {}", patientUuid);
            return Optional.empty();
        } catch (Exception exception) {
            log.error("Error finding patient: {}", exception.getMessage(), exception);
            throw new ApiException("Erreur lors de la recherche du patient");
        }
    }

    @Override
    public Optional<Patient> findByUserUuid(String userUuid) {
        log.debug("Finding patient by user UUID: {}", userUuid);
        try {
            Patient patient = jdbc.sql(SELECT_PATIENT_BY_USER_UUID_QUERY).param("userUuid", userUuid).query(Patient.class).single();
            return Optional.of(patient);
        } catch (EmptyResultDataAccessException exception) {
            log.debug("Patient not found for user: {}", userUuid);
            return Optional.empty();
        } catch (Exception exception) {
            log.error("Error finding patient by user: {}", exception.getMessage(), exception);
            throw new ApiException("Erreur lors de la recherche du patient");
        }
    }

    @Override
    public Optional<Patient> findPatientByMedicalRecordNumber(String medicalRecordNumber) {
        log.debug("Finding patient by medical record: {}", medicalRecordNumber);
        try {
            Patient patient = jdbc.sql(SELECT_PATIENT_BY_MEDICAL_RECORD_NUMBER_QUERY)
                    .param("medicalRecordNumber", medicalRecordNumber).query(Patient.class).single();
            return Optional.of(patient);
        } catch (EmptyResultDataAccessException exception) {
            log.debug("Patient not found for medical record: {}", medicalRecordNumber);
            return Optional.empty();
        } catch (Exception exception) {
            log.error("Error finding patient by medical record: {}", exception.getMessage(), exception);
            throw new ApiException("Erreur lors de la recherche du patient");
        }
    }

    @Override
    public List<Patient> findAllPatientByActiveTrue() {
        log.debug("Finding all active patients");
        try {
            return jdbc.sql(SELECT_ALL_ACTIVE_PATIENTS_QUERY).query(Patient.class).list();
        } catch (Exception exception) {
            log.error("Error finding all patients: {}", exception.getMessage(), exception);
            throw new ApiException("Erreur lors de la récupération des patients");
        }
    }

    @Override
    public List<Patient> findPatientByBloodTypeAndActiveTrue(String bloodType) {
        log.debug("Finding patients by blood type: {}", bloodType);
        try {
            return jdbc.sql(SELECT_PATIENTS_BY_BLOOD_TYPE_QUERY).param("bloodType", bloodType).query(Patient.class).list();
        } catch (Exception exception) {
            log.error("Error finding patients by blood type: {}", exception.getMessage(), exception);
            throw new ApiException("Erreur lors de la recherche par groupe sanguin");
        }
    }

    // EXISTS / COUNT

    @Override
    public boolean existsPatientByUserUuid(String userUuid) {
        log.debug("Checking if patient exists for user: {}", userUuid);
        try {
            return jdbc.sql(EXISTS_BY_USER_UUID_QUERY).param("userUuid", userUuid).query(Boolean.class).single();
        } catch (Exception exception) {
            log.error("Error checking patient existence: {}", exception.getMessage(), exception);
            return false;
        }
    }

    @Override
    public boolean existsPatientByMedicalRecordNumber(String medicalRecordNumber) {
        log.debug("Checking if medical record exists: {}", medicalRecordNumber);
        try {
            return jdbc.sql(EXISTS_BY_MEDICAL_RECORD_NUMBER_QUERY).param("medicalRecordNumber", medicalRecordNumber)
                    .query(Boolean.class).single();
        } catch (Exception exception) {
            log.error("Error checking medical record existence: {}", exception.getMessage(), exception);
            return false;
        }
    }

    @Override
    public long countPatientByActiveTrue() {
        log.debug("Counting active patients");
        try {
            return jdbc.sql(COUNT_ACTIVE_PATIENTS_QUERY).query(Long.class).single();
        } catch (Exception exception) {
            log.error("Error counting patients: {}", exception.getMessage(), exception);
            return 0L;
        }
    }

    // UPDATE

    @Override
    public Patient updatePatient(Patient patient) {
        log.debug("Updating patient: {}", patient.getPatientUuid());
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
        } catch (EmptyResultDataAccessException exception) {
            log.error("Patient not found for update: {}", patient.getPatientUuid());
            throw new ApiException("Patient non trouvé: " + patient.getPatientUuid());
        } catch (Exception exception) {
            log.error("Error updating patient: {}", exception.getMessage(), exception);
            throw new ApiException("Erreur lors de la mise à jour du patient");
        }
    }

    // DELETE

    @Override
    public boolean softDeletePatientByPatientUuid(String patientUuid) {
        log.debug("Soft deleting patient: {}", patientUuid);
        try {
            int updated = jdbc.sql(SOFT_DELETE_PATIENT_QUERY).param("patientUuid", patientUuid).update();

            if (updated > 0) {
                log.info("Patient soft deleted: {}", patientUuid);return true;
            }
            return false;
        } catch (Exception exception) {
            log.error("Error soft deleting patient: {}", exception.getMessage(), exception);
            throw new ApiException("Erreur lors de la suppression du patient");
        }
    }
}