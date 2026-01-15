package com.openclassrooms.patientservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO pour mettre à jour un patient
 * Tous les champs sont optionnels
 *
 * @author FirstName LastName
 * @version 1.0
 * @since 2026-01-09
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePatientRequest {

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

    // Assurance
    private String insuranceNumber;
    private String insuranceProvider;
}