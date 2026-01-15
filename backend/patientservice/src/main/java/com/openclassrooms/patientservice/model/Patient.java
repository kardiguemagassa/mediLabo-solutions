package com.openclassrooms.patientservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
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
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Patient {

    // IDs
    private Long patientId;
    private String patientUuid;
    private String userUuid;  // Référence Authorization Server

    // Données médicales
    private LocalDate dateOfBirth;
    private String gender;
    private String bloodType;
    private Integer heightCm;
    private BigDecimal weightKg;
    private String allergies;
    private String chronicConditions;
    private String currentMedications;

    // Contact d'urgence
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelationship;

    // Assurance et dossier
    private String medicalRecordNumber;
    private String insuranceNumber;
    private String insuranceProvider;

    // Métadonnées
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Calcule l'âge du patient
     */
    public int getAge() {
        if (dateOfBirth == null) {
            return 0;
        }
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    /**
     * Calcule l'IMC (Indice de Masse Corporelle)
     */
    public BigDecimal getBMI() {
        if (heightCm == null || weightKg == null || heightCm == 0) {
            return null;
        }
        double heightM = heightCm / 100.0;
        double bmi = weightKg.doubleValue() / (heightM * heightM);
        return BigDecimal.valueOf(bmi).setScale(2, BigDecimal.ROUND_HALF_UP);
    }
}