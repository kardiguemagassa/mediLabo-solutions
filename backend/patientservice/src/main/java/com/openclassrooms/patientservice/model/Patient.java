package com.openclassrooms.patientservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

/**
 * Entité Patient - Données médicales uniquement
 * Les infos utilisateur (nom, prénom, etc.) sont dans Authorization Server
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-01-09
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Patient {

    private Long patientId;
    private String patientUuid;
    private String userUuid;
    /** Données démographiques & médicales*/
    private LocalDate dateOfBirth;
    private String gender;
    private String bloodType;
    private Integer heightCm;
    private BigDecimal weightKg;
    private String allergies;
    private String chronicConditions;
    private String currentMedications;
    /**Contact d'urgence */
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelationship;
    // Assurance et dossier
    private String medicalRecordNumber;
    private String insuranceNumber;
    private String insuranceProvider;
    private String insurancePolicyNumber;
    /** Métadonnées */
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** MÉTHODES MÉTIER */

    public int getAge() {
        if (dateOfBirth == null) return 0;
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    public BigDecimal getBMI() {
        if (heightCm == null || weightKg == null || heightCm == 0) {
            return null;
        }
        // Conversion cm -> m (ex: 180 -> 1.8)
        BigDecimal heightM = BigDecimal.valueOf(heightCm).divide(BigDecimal.valueOf(100));
        // Formule : poids / (taille * taille)
        return weightKg.divide(heightM.multiply(heightM), 2, RoundingMode.HALF_UP);
    }
}