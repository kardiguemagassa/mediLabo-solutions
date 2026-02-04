package com.openclassrooms.assessmentservice.service;

import com.openclassrooms.assessmentservice.model.Assessment;

import java.util.concurrent.CompletableFuture;

/**
 * Service d'évaluation du risque de diabète.
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-09
 */
public interface AssessmentService {

    /**
     * Évalue le risque de diabète pour un patient de manière asynchrone.
     *
     * @param patientUuid UUID du patient
     * @return CompletableFuture contenant l'évaluation complète du risque
     */
    CompletableFuture<Assessment> assessDiabetesRisk(String patientUuid);
}