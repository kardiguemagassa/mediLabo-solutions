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

    @Override
    public CompletableFuture<Assessment> assessDiabetesRisk(String patientUuid) {
        log.info("Starting async diabetes risk assessment for patient: {}", patientUuid);

        // 1. Lancement des appels en parallèle (Non-bloquant)
        // Note: On utilise bien les nouveaux noms de méthodes Async
        CompletableFuture<Optional<PatientResponse>> patientFuture = patientClient.getPatientByUuid(patientUuid);
        CompletableFuture<List<NoteResponse>> notesFuture = noteClient.getNotesByPatientUuidAsync(patientUuid);

        // 2. Combinaison des deux futurs quand ils sont prêts
        return patientFuture.thenCombine(notesFuture, (patientOptional, notes) -> {

            // Extraction du patient ou exception si absent
            PatientResponse patient = patientOptional.orElseThrow(() -> new ApiException("Patient non trouvé: " + patientUuid));

            // Extraction du contenu des notes
            List<String> noteContents = notes.stream().map(NoteResponse::getContent).toList();

            // Analyse des termes déclencheurs
            Set<String> triggersFound = TriggerTerms.findTriggersInMultipleTexts(noteContents);
            int triggerCount = triggersFound.size();

            // Calcul du niveau de risque via le calculateur métier
            RiskLevel riskLevel = riskLevelCalculator.calculate(patient.getAge(), patient.getGender(), triggerCount);

            log.info("Assessment complete for patient {} - Risk Level: {}, Triggers: {}",
                    patientUuid, riskLevel, triggerCount);

            return new Assessment(patient.getPatientUuid(), patient.getFullName(), patient.getAge(), patient.getGender(), riskLevel, triggerCount, new ArrayList<>(triggersFound));
        }).exceptionally(ex -> {
            // Gestion propre des erreurs de la chaîne asynchrone
            log.error("Error assessing patient {}: {}", patientUuid, ex.getMessage());
            if (ex.getCause() instanceof ApiException) {
                throw (ApiException) ex.getCause();
            }
            throw new ApiException("Erreur lors de l'évaluation du risque");
        });
    }
}