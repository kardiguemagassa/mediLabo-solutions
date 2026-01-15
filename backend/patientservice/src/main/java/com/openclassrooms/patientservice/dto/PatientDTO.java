package com.openclassrooms.patientservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO Patient enrichi avec informations utilisateur
 * Combine données Patient + User depuis Authorization Server
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-01-09
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientDTO {

    // Patient info
    private String patientUuid;
    private String userUuid;

    // User info (depuis Authorization Server)
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String address;
    private String imageUrl;
    private String memberId;

    // Medical data
    private LocalDate dateOfBirth;
    private Integer age;
    private String gender;
    private String bloodType;
    private Integer heightCm;
    private BigDecimal weightKg;
    private BigDecimal bmi;
    private String allergies;
    private String chronicConditions;
    private String currentMedications;

    // Emergency contact
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelationship;

    // Insurance
    private String medicalRecordNumber;
    private String insuranceNumber;
    private String insuranceProvider;

    // Metadata
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}