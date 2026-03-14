package com.openclassrooms.assessmentservice.service;

import com.openclassrooms.assessmentservice.model.Assessment;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Service d'évaluation du risque de diabète Full Réactif
 *
 * @author Kardigué MAGASSA
 * @version 2.0
 * @since 2026-02-25
 */
public interface AssessmentService {

    List<Assessment> getAllAssessments();

    /**
     * Évalue le risque de diabète pour un patient de manière réactive.
     *
     * @param patientUuid UUID du patient
     * @return Mono<Assessment> contenant l'évaluation complète du risque
     */
    Mono<Assessment> assessDiabetesRisk(String patientUuid, String token);
}