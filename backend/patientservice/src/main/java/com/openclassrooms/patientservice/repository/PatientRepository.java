package com.openclassrooms.patientservice.repository;

import com.openclassrooms.patientservice.model.Patient;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour les opérations Patient
 *
 * @author FirstName LastName
 * @version 1.0
 * @since 2026-01-09
 */
public interface PatientRepository {

    List<Patient> getAllPatients();
    Patient createPatient(Patient patient);
    Patient updatePatient(Patient patient);
    Optional<Patient> findByPatientUuid(String patientUuid);
    Optional<Patient> findByUserUuid(String userUuid);
    boolean existsByUserUuid(String userUuid);
    List<Patient> findAllByActiveTrue();
    Optional<Patient> findByMedicalRecordNumber(String medicalRecordNumber);
    List<Patient> findAllActivePatientsOrderByCreatedAtDesc();
    long countByActiveTrue();
    List<Patient> findByBloodTypeAndActiveTrue(String bloodType);
    void softDeletePatient(String patientUuid);


    /*
    Patient getPatientByUuid(String patientUuid);
    Patient getPatientByUserUuid(String userUuid);
    */
}