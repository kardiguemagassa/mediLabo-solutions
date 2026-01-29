package com.openclassrooms.patientservice.dtoresponse;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO de réponse pour Patient.
 * Contient uniquement les données exposées via l'API.
 *
 * Note: Les informations personnelles (nom, prénom, email)
 * sont gérées par Authorization Server et enrichies si nécessaire.
 *
 * @author FirstName LastName
 * @version 1.0
 * @since 2026-01-09
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PatientResponse {

    // Identifiants (jamais exposer patientId interne)
    private String patientUuid;
    private String userUuid;
    private String medicalRecordNumber;

    // Données démographiques
    private LocalDate dateOfBirth;
    private Integer age;  // Calculé
    private String gender;

    // Données médicales
    private String bloodType;
    private Integer heightCm;
    private BigDecimal weightKg;
    private BigDecimal bmi;  // Calculé
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Informations utilisateur enrichies (optionnel, depuis Auth Server)
    private UserInfo userInfo;

    /**
     * Informations utilisateur depuis Authorization Server
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UserInfo {
        private String firstName;
        private String lastName;
        private String email;
        private String phone;
        private String address;
        private String imageUrl;
    }
}