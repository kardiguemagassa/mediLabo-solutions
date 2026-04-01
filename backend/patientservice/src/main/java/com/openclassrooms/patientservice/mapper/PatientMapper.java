package com.openclassrooms.patientservice.mapper;

import com.openclassrooms.patientservice.dtorequest.PatientRequest;
import com.openclassrooms.patientservice.dtorequest.UserRequest;
import com.openclassrooms.patientservice.dtoresponse.PatientResponse;
import com.openclassrooms.patientservice.model.Patient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Mapper pour les conversions Patient Entity ↔ DTOs.
 * Centralise toute la logique de transformation pour :
 * Garantir la cohérence des transformations
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-01-09
 */
@Component
public class PatientMapper {

    /**
     * Convertit une requête de création en entité Patient
     * @param request DTO de création
     * @param medicalRecordNumber numéro de dossier généré
     * @return entité Patient
     */
    public Patient toEntity(PatientRequest request, String medicalRecordNumber) {
        return Patient.builder()
                .patientUuid(UUID.randomUUID().toString())
                .userUuid(request.getUserUuid())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .bloodType(request.getBloodType())
                .heightCm(request.getHeightCm())
                .weightKg(request.getWeightKg())
                .allergies(request.getAllergies())
                .chronicConditions(request.getChronicConditions())
                .currentMedications(request.getCurrentMedications())
                .emergencyContactName(request.getEmergencyContactName())
                .emergencyContactPhone(request.getEmergencyContactPhone())
                .emergencyContactRelationship(request.getEmergencyContactRelationship())
                .medicalRecordNumber(medicalRecordNumber)
                .insuranceNumber(request.getInsuranceNumber())
                .insuranceProvider(request.getInsuranceProvider())
                .active(true)
                .build();
    }

    /**
     * Met à jour une entité existante avec les données de la requête
     * @param existing entité existante
     * @param request nouvelles données
     * @return entité mise à jour
     */
    public Patient updateEntity(Patient existing, PatientRequest request) {
        if (request.getGender() !=null) existing.setGender(request.getGender());
        existing.setDateOfBirth(request.getDateOfBirth());
        existing.setGender(request.getGender());
        existing.setBloodType(request.getBloodType());
        existing.setHeightCm(request.getHeightCm());
        existing.setWeightKg(request.getWeightKg());
        existing.setAllergies(request.getAllergies());
        existing.setChronicConditions(request.getChronicConditions());
        existing.setCurrentMedications(request.getCurrentMedications());
        existing.setEmergencyContactName(request.getEmergencyContactName());
        existing.setEmergencyContactPhone(request.getEmergencyContactPhone());
        existing.setEmergencyContactRelationship(request.getEmergencyContactRelationship());
        existing.setInsuranceNumber(request.getInsuranceNumber());
        existing.setInsuranceProvider(request.getInsuranceProvider());
        return existing;
    }

    /**
     * Convertit une entité Patient en DTO de réponse
     * @param patient entité
     * @return DTO de réponse
     */
    public PatientResponse toResponse(Patient patient) {
        return PatientResponse.builder()
                .patientUuid(patient.getPatientUuid())
                .userUuid(patient.getUserUuid())
                .medicalRecordNumber(patient.getMedicalRecordNumber())
                .dateOfBirth(patient.getDateOfBirth())
                .age(patient.getAge())
                .gender(patient.getGender())
                .bloodType(patient.getBloodType())
                .heightCm(patient.getHeightCm())
                .weightKg(patient.getWeightKg())
                .bmi(patient.getBMI())
                .allergies(patient.getAllergies())
                .chronicConditions(patient.getChronicConditions())
                .currentMedications(patient.getCurrentMedications())
                .emergencyContactName(patient.getEmergencyContactName())
                .emergencyContactPhone(patient.getEmergencyContactPhone())
                .emergencyContactRelationship(patient.getEmergencyContactRelationship())
                .insuranceNumber(patient.getInsuranceNumber())
                .insuranceProvider(patient.getInsuranceProvider())
                .createdAt(patient.getCreatedAt())
                .updatedAt(patient.getUpdatedAt())
                .build();
    }

    /**
     * Convertit une entité Patient en DTO avec informations utilisateur
     * @param patient entité
     * @param userRequest données utilisateur depuis Auth Server
     * @return DTO de réponse enrichi
     */
    public PatientResponse toResponseWithUserInfo(Patient patient, UserRequest userRequest) {
        PatientResponse response = toResponse(patient);

        if (userRequest != null) {
            response.setUserInfo(PatientResponse.UserInfo.builder()
                    .firstName(userRequest.getFirstName())
                    .lastName(userRequest.getLastName())
                    .email(userRequest.getEmail())
                    .phone(userRequest.getPhone())
                    .address(userRequest.getAddress())
                    .imageUrl(userRequest.getImageUrl())
                    .build());
        }

        return response;
    }

    /**
     * Convertit une liste d'entités en liste de DTOs
     * @param patients liste d'entités
     * @return liste de DTOs
     */
    public List<PatientResponse> toResponseList(List<Patient> patients) {
        return patients.stream().map(this::toResponse).toList();
    }
}