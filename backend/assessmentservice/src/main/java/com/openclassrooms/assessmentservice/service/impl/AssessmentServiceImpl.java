package com.openclassrooms.assessmentservice.service.impl;

import com.openclassrooms.assessmentservice.model.Assessment;
import com.openclassrooms.assessmentservice.model.RiskLevel;
import com.openclassrooms.assessmentservice.exception.ApiException;
import com.openclassrooms.assessmentservice.client.NoteClient;
import com.openclassrooms.assessmentservice.client.PatientClient;
import com.openclassrooms.assessmentservice.dtoresponse.NoteResponse;
import com.openclassrooms.assessmentservice.dtoresponse.PatientResponse;
import com.openclassrooms.assessmentservice.service.AssessmentService;
import com.openclassrooms.assessmentservice.service.RiskLevelCalculator;
import com.openclassrooms.assessmentservice.service.TriggerTerms;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;


/**
 * Implémentation du service d'évaluation du risque de diabète.
 *
 * Récupère les informations du patient et ses notes de manière asynchrone,
 * puis calcule le niveau de risque selon les règles métier.
 *
 * @author Kardigué MAGASSA
 * @version 1.1
 * @since 2026-02-09
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class AssessmentServiceImpl implements AssessmentService {

    private final PatientClient patientClient;
    private final NoteClient noteClient;
    private final RiskLevelCalculator riskLevelCalculator;

    /**
     * Évalue le risque de diabète pour un patient de manière asynchrone.
     *
     * @param patientUuid UUID du patient
     * @return CompletableFuture contenant l'Assessment
     */
    @Override
    public CompletableFuture<Assessment> assessDiabetesRisk(String patientUuid) {
        log.info("Starting async diabetes risk assessment for patient: {}", patientUuid);

        // Récupération asynchrone des données patient et notes
        CompletableFuture<Optional<PatientResponse>> patientFuture = patientClient.getPatientByUuid(patientUuid);
        CompletableFuture<List<NoteResponse>> notesFuture = noteClient.getNotesByPatientUuid(patientUuid);

        return patientFuture.thenCombine(notesFuture, (patientOptional, notes) -> {
            // Vérification de l'existence du patient
            PatientResponse patient = patientOptional.orElseThrow(() -> new ApiException("Patient non trouvé: " + patientUuid));

            // Extraire le contenu des notes
            List<String> noteContents = notes.stream().map(NoteResponse::getContent).toList();

            // Comptage des termes déclencheurs
            Set<String> triggersFound = TriggerTerms.findTriggersInMultipleTexts(noteContents);
            int triggerCount = triggersFound.size();

            // Calcul du niveau de risque
            RiskLevel riskLevel = riskLevelCalculator.calculate(patient.getAge(), patient.getGender(), triggerCount);

            log.info("Assessment complete for patient {} - Risk Level: {}, Triggers found: {}", patientUuid, riskLevel, triggerCount);

            // Retourner l'objet Assessment
            return new Assessment(patient.getPatientUuid(), patient.getFullName(), patient.getAge(), patient.getGender(),riskLevel, triggerCount, new ArrayList<>(triggersFound));
        }).exceptionally(ex -> {
            log.error("Error assessing patient {}: {}", patientUuid, ex.getMessage(), ex);
            throw new ApiException("Impossible d'évaluer le risque pour le patient: " + patientUuid);
        });
    }

}