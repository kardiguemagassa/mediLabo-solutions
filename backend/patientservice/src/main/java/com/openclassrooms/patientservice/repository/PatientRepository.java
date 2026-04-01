package com.openclassrooms.patientservice.repository;

import com.openclassrooms.patientservice.model.Patient;

import java.util.List;
import java.util.Optional;

/**
 * @author Kardigué MAGASSA
 * @version 2.0
 * @since 2026-01-09
 */
public interface PatientRepository {
    Patient savePatient(Patient patient);
    Optional<Patient> findByPatientUuid(String patientUuid);
    Optional<Patient> findByUserUuid(String userUuid);
    Optional<Patient> findPatientByMedicalRecordNumber(String medicalRecordNumber);
    List<Patient> findAllPatientByActiveTrue();
    List<Patient> findPatientByBloodTypeAndActiveTrue(String bloodType);
    Optional<Patient> findSoftDeletedByPatientUuid(String patientUuid);
    boolean existsPatientByUserUuid(String userUuid);
    boolean existsPatientByMedicalRecordNumber(String medicalRecordNumber);
    long countPatientByActiveTrue();
    Patient updatePatient(Patient patient);
    boolean softDeletePatientByPatientUuid(String patientUuid);
    boolean restorePatientByPatientUuid(String patientUuid);
}