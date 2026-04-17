package com.openclassrooms.assessmentservice.model;

import lombok.Getter;

/**
 * Niveaux de risque de diabète selon les règles métier.
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-09
 */
@Getter
public enum RiskLevel {
    NONE("Aucun risque"),
    BORDERLINE("Risque limité"),
    IN_DANGER("Danger"),
    EARLY_ONSET("Apparition précoce"),
    UNKNOWN("Inconnu");

    private final String description;

    RiskLevel(String description) {this.description = description;}

}