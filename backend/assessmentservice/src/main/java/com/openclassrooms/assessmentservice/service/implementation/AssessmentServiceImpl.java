package com.openclassrooms.assessmentservice.service.implementation;

import com.openclassrooms.assessmentservice.cache.AssessmentCache;
import com.openclassrooms.assessmentservice.dtoresponse.NoteResponseDTO;
import com.openclassrooms.assessmentservice.dtoresponse.PatientResponseDTO;
import com.openclassrooms.assessmentservice.exception.ApiException;
import com.openclassrooms.assessmentservice.model.Assessment;
import com.openclassrooms.assessmentservice.model.RiskLevel;
import com.openclassrooms.assessmentservice.service.AssessmentService;
import com.openclassrooms.assessmentservice.service.NoteServiceClient;
import com.openclassrooms.assessmentservice.service.PatientServiceClient;
import com.openclassrooms.assessmentservice.service.RiskLevelCalculator;
import com.openclassrooms.assessmentservice.service.TriggerTerms;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.openclassrooms.assessmentservice.enumeration.EventType;
import com.openclassrooms.assessmentservice.event.Event;
import java.util.HashMap;
import java.util.Map;

/**
 * Implémentation réactive du service d'évaluation du risque de diabète.
 * ARCHITECTURE RÉACTIVE:
 * - Mono.zip() : Combine les appels PatientService et NotesService en parallèle
 * - Flux.collectList() : Convertit Flux<Note> en Mono<List<Note>>
 * - switchIfEmpty() : Gestion des patients non trouvés
 *
 * @author Kardigué MAGASSA
 * @version 2.0
 * @since 2026-02-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssessmentServiceImpl implements AssessmentService {

    private final PatientServiceClient patientServiceClient;
    private final NoteServiceClient noteServiceClient;
    private final RiskLevelCalculator riskLevelCalculator;
    private final ApplicationEventPublisher eventPublisher;
    private final AssessmentCache assessmentCache;

    // 3. Ajouter cette nouvelle méthode
    @Override
    public List<Assessment> getAllAssessments() {
        log.info("Retrieving all cached assessments - {} entries", assessmentCache.size());
        return assessmentCache.getAll();
    }

    /**
     * Évalue le risque de diabète pour un patient.
     * FLUX:
     * 1. Appels parallèles vers PatientService et NotesService
     * 2. Combinaison des résultats avec Mono.zip()
     * 3. Analyse des termes déclencheurs dans les notes
     * 4. Calcul du niveau de risque
     * 5. Construction de l'Assessment
     */
    @Override
    public Mono<Assessment> assessDiabetesRisk(String patientUuid, String token) {
        Mono<PatientResponseDTO> patientMono = patientServiceClient
                .getPatientByUuid(patientUuid, token)
                .switchIfEmpty(Mono.error(new ApiException("Patient non trouvé: " + patientUuid)));

        Mono<List<NoteResponseDTO>> notesMono = noteServiceClient
                .getNotesByPatientUuid(patientUuid, token)
                .collectList()
                .defaultIfEmpty(List.of());

        return Mono.zip(patientMono, notesMono)
                .map(tuple -> {
                    Assessment assessment = buildAssessment(tuple.getT1(), tuple.getT2());
                    assessmentCache.save(assessment);
                    publishAssessmentCompletedEvent(tuple.getT1(), assessment);
                    return assessment;
                })
                .doOnSuccess(a -> log.info("Assessment complete for patient {} - Risk: {}", patientUuid, a.riskLevel()))
                .doOnError(error -> log.error("Error assessing patient {}: {}", patientUuid, error.getMessage()));
    }

    //  PRIVATE METHODS

    /**
     * Construit l'Assessment à partir des données patient et notes.
     */
    private Assessment buildAssessment(PatientResponseDTO patient, List<NoteResponseDTO> notes) {
        // Extraire le contenu des notes
        List<String> noteContents = notes.stream().map(NoteResponseDTO::getContent).toList();

        // Analyser les termes déclencheurs
        Set<String> triggersFound = TriggerTerms.findTriggersInMultipleTexts(noteContents);
        int triggerCount = triggersFound.size();

        // Calculer le niveau de risque
        RiskLevel riskLevel = riskLevelCalculator.calculate(patient.getAge(), patient.getGender(), triggerCount);

        // Construire l'Assessment
        return new Assessment(patient.getPatientUuid(), patient.getFullName(), patient.getAge(), patient.getGender(), riskLevel, triggerCount, new ArrayList<>(triggersFound));
    }

    /**
     * Publie un événement ASSESSMENT_COMPLETED pour notification email.
     */
    private void publishAssessmentCompletedEvent(PatientResponseDTO patient, Assessment assessment) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("patientUuid", assessment.patientUuid());
            data.put("name", assessment.patientName());
            data.put("email", patient.getUserInfo() != null ? patient.getUserInfo().getEmail() : null);
            data.put("age", assessment.age());
            data.put("gender", assessment.gender().getCode());
            data.put("riskLevel", assessment.riskLevel().name());
            data.put("riskLevelDescription", assessment.riskLevel().getDescription());
            data.put("triggerCount", assessment.triggerCount());
            data.put("triggersFound", assessment.triggersFound());
            data.put("assessedAt", assessment.assessedAt().toString());

            Event event = Event.builder()
                    .eventType(EventType.ASSESSMENT_COMPLETED)
                    .data(data)
                    .build();

            eventPublisher.publishEvent(event);
            log.debug("ASSESSMENT_COMPLETED event published for patient: {}", assessment.patientUuid());

        } catch (Exception e) {
            log.error("Failed to publish ASSESSMENT_COMPLETED event: {}", e.getMessage());
        }
    }
}