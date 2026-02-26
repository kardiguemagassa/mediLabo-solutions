package com.openclassrooms.assessmentservice.service;

import com.openclassrooms.assessmentservice.dtoresponse.NoteResponse;
import reactor.core.publisher.Flux;

/**
 * Client pour communiquer avec NotesService.
 *
 * @author Kardigué MAGASSA
 * @version 2.0
 * @since 2026-02-25
 */
public interface NoteServiceClient {

    /**
     * Récupère toutes les notes d'un patient.
     *
     * @param patientUuid UUID du patient
     * @return Flux<NoteResponse> ou Flux.empty() si aucune note
     */
    Flux<NoteResponse> getNotesByPatientUuid(String patientUuid);
}