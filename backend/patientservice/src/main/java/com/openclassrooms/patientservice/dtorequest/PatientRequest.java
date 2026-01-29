package com.openclassrooms.patientservice.dtorequest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO pour créer un patient
 *
 * @author FirstName LastName
 * @version 1.0
 * @since 2026-01-09
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientRequest {

    @NotBlank(message = "L'UUID utilisateur est obligatoire")
    private String userUuid;
    @NotNull(message = "La date de naissance est obligatoire")
    @Past(message = "La date de naissance doit être dans le passé")
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

    // Métadonnées
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String insurancePolicyNumber;
}