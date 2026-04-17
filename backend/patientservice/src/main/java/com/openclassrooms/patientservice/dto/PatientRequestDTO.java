package com.openclassrooms.patientservice.dto;

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
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-01-09
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientRequestDTO {

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
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelationship;
    private String insuranceNumber;
    private String insuranceProvider;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String insurancePolicyNumber;

    private String phone;
    private String address;
    private String firstName;
    private String lastName;
    private String email;
}