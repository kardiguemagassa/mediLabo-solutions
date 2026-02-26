package com.openclassrooms.patientservice.service;

import com.openclassrooms.patientservice.dtorequest.PatientRequest;
import com.openclassrooms.patientservice.dtoresponse.PatientResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service de gestion des patients.
 *
 * @author Kardigué MAGASSA
 * @version 2.1
 * @since 2026-01-09
 */
public interface PatientService {

    // CRUD
    Mono<PatientResponse> createPatient(PatientRequest request);
    Mono<PatientResponse> getPatientByUuid(String patientUuid);
    Mono<PatientResponse> getPatientByUserUuid(String userUuid);
    Mono<PatientResponse> getPatientByEmail(String email);
    Flux<PatientResponse> getAllActivePatients();
    Mono<PatientResponse> updatePatient(String patientUuid, PatientRequest request);
    Mono<Void> deletePatient(String patientUuid);
    Mono<PatientResponse> restorePatient(String patientUuid);

    // QUERY
    Mono<PatientResponse> getPatientByMedicalRecordNumber(String medicalRecordNumber);
    Flux<PatientResponse> getPatientsByBloodType(String bloodType);

    // UTILITY
    Mono<Boolean> hasPatientRecord(String userUuid);
    Mono<Long> countActivePatients();
}