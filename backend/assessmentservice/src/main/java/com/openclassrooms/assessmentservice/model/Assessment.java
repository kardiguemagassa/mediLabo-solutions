package com.openclassrooms.assessmentservice.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Résultat d'une évaluation du risque de diabète.
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-09
 */

@Builder
public record Assessment(String patientUuid, String patientName, int age, Gender gender, RiskLevel riskLevel, int triggerCount, List<String> triggersFound, LocalDateTime assessedAt) {
    public Assessment(String patientUuid, String patientName, int age, Gender gender, RiskLevel riskLevel, int triggerCount, List<String> triggersFound) {
        this(patientUuid, patientName, age, gender, riskLevel, triggerCount, triggersFound, LocalDateTime.now());
    }
}
