package com.openclassrooms.patientservice.service;

import com.openclassrooms.patientservice.dto.PatientRequestDTO;
import com.openclassrooms.patientservice.dto.PatientResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    Mono<Page<PatientResponseDTO>> getAllPatientsPageable(Pageable pageable);
    Mono<Page<PatientResponseDTO>> getAllActivePatientsPageable(Pageable pageable);
    Mono<PatientResponseDTO> createPatient(PatientRequestDTO request);
    Mono<PatientResponseDTO> getPatientByUuid(String patientUuid);
    Mono<PatientResponseDTO> getPatientByUserUuid(String userUuid);
    Mono<PatientResponseDTO> getPatientByEmail(String email);
    Flux<PatientResponseDTO> getAllActivePatients();
    Mono<PatientResponseDTO> updatePatient(String patientUuid, PatientRequestDTO request);
    Mono<Void> deletePatient(String patientUuid);
    Mono<PatientResponseDTO> restorePatient(String patientUuid);

    // QUERY
    Mono<PatientResponseDTO> getPatientByMedicalRecordNumber(String medicalRecordNumber);
    Flux<PatientResponseDTO> getPatientsByBloodType(String bloodType);

    // UTILITY
    Mono<Boolean> hasPatientRecord(String userUuid);
    Mono<Long> countActivePatients();
}