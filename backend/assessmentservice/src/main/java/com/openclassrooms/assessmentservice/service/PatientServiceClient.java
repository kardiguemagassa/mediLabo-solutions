package com.openclassrooms.assessmentservice.service;

import com.openclassrooms.assessmentservice.dtoresponse.PatientResponse;
import reactor.core.publisher.Mono;

/**
 * Client pour communiquer avec PatientService.
 *
 * @author Kardigué MAGASSA
 * @version 2.0
 * @since 2026-02-25
 */
public interface PatientServiceClient {

    /**
     * Récupère les informations d'un patient par son UUID.
     *
     * @param patientUuid UUID du patient
     * @return Mono<PatientResponse> ou Mono.empty() si non trouvé
     */
    Mono<PatientResponse> getPatientByUuid(String patientUuid);
}