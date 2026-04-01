package com.openclassrooms.notesservice.service;

import com.openclassrooms.notesservice.dto.PatientInfo;
import reactor.core.publisher.Mono;

/**
 * Client pour communiquer avec le PatientService.
 *
 * @author Kardigué MAGASSA
 * @version 2.0
 * @since 2026-02-25
 */
public interface PatientServiceClient {
    Mono<PatientInfo> getPatientByUuid(String patientUuid);
    Mono<PatientInfo> getPatientContactInfo(String patientUuid);
    Mono<PatientInfo> getMyPatient();
}