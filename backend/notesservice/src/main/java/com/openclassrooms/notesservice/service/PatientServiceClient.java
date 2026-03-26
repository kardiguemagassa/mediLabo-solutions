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

    /**
     * Récupère les informations d'un patient par son UUID.
     *
     * @param patientUuid UUID du patient
     * @return Mono<PatientInfo> ou Mono.empty() si non trouvé
     */
    Mono<PatientInfo> getPatientByUuid(String patientUuid);

    /**
     * Récupère les infos de contact d'un patient (email, nom).
     * Retourne Mono.empty() si le patient n'a pas d'email configuré.
     *
     * @param patientUuid UUID du patient
     * @return Mono<PatientInfo> ou Mono.empty()
     */
    Mono<PatientInfo> getPatientContactInfo(String patientUuid);

    Mono<PatientInfo> getMyPatient();
}