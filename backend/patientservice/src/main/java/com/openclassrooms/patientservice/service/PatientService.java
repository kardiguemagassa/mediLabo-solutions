package com.openclassrooms.patientservice.service;

import com.openclassrooms.patientservice.dtorequest.PatientRequest;
import com.openclassrooms.patientservice.model.Patient;

import java.util.List;
import java.util.Optional;

/**
 * Service pour la logique métier Patient
 *
 * @author FirstName LastName
 * @version 1.0
 * @since 2026-01-09
 */
public interface PatientService {

    void getAllPatients();
    void createPatient(PatientRequest request);
    void getPatientByUuid(String patientUuid);
    void getPatientByUserUuid(String userUuid);
    void updatePatient(String patientUuid, Patient request);
    void deletePatient(String patientUuid);


    Optional<Patient> findByPatientUuid(String patientUuid);
    Optional<Patient> findByUserUuid(String userUuid);
    boolean existsByUserUuid(String userUuid);
    List<Patient> findAllByActiveTrue();
    Optional<Patient> findByMedicalRecordNumber(String medicalRecordNumber);
    List<Patient> findAllActivePatientsOrderByCreatedAtDesc();
    long countByActiveTrue();
    List<Patient> findByBloodTypeAndActiveTrue(String bloodType);
    void softDeletePatient(String patientUuid);
}