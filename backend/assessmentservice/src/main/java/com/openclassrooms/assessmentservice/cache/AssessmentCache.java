package com.openclassrooms.assessmentservice.cache;

import com.openclassrooms.assessmentservice.model.Assessment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache en mémoire pour les résultats d'évaluation de risque de diabète.
 * Les résultats sont stockés après chaque calcul et disponibles pour lecture rapide.
 * Le cache se vide au redémarrage du service.
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-03-07
 */
@Slf4j
@Component
public class AssessmentCache {

    private final Map<String, Assessment> cache = new ConcurrentHashMap<>();

    public void save(Assessment assessment) {
        cache.put(assessment.patientUuid(), assessment);
        log.debug("Assessment cached for patient: {}", assessment.patientUuid());
    }

    public List<Assessment> getAll() {
        return new ArrayList<>(cache.values());
    }

    public Assessment getByPatientUuid(String patientUuid) {
        return cache.get(patientUuid);
    }

    public int size() {
        return cache.size();
    }
}