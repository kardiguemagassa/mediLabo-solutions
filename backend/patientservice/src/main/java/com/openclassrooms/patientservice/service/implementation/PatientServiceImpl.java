package com.openclassrooms.patientservice.service.impl;

import com.openclassrooms.patientservice.client.AuthServerClient;
import com.openclassrooms.patientservice.dto.*;
import com.openclassrooms.patientservice.exception.ApiException;
import com.openclassrooms.patientservice.exception.PatientNotFoundException;
import com.openclassrooms.patientservice.model.Patient;
import com.openclassrooms.patientservice.repository.PatientRepository;
import com.openclassrooms.patientservice.service.PatientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implémentation du service Patient
 *
 * @author FirstName LastName
 * @version 1.0
 * @since 2026-01-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;
    private final AuthServerClient authServerClient;

    @Override
    @Transactional
    public PatientDTO createPatient(CreatePatientRequest request) {
        log.info("Creating patient for user UUID: {}", request.getUserUuid());

        // Vérifier si l'utilisateur existe
        if (!authServerClient.userExists(request.getUserUuid())) {
            throw new ApiException("User not found: " + request.getUserUuid());
        }

        // Vérifier si un patient existe déjà pour cet utilisateur
        if (patientRepository.existsByUserUuid(request.getUserUuid())) {
            throw new ApiException("Patient already exists for this user");
        }

        // Créer le patient
        Patient patient = Patient.builder()
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
                .medicalRecordNumber(generateMedicalRecordNumber())
                .insuranceNumber(request.getInsuranceNumber())
                .insuranceProvider(request.getInsuranceProvider())
                .active(true)
                .build();

        Patient savedPatient = patientRepository.save(patient);
        log.info("Patient created successfully: {}", savedPatient.getPatientUuid());

        return enrichPatientWithUserInfo(savedPatient);
    }

    @Override
    public PatientDTO getPatientByUuid(String patientUuid) {
        log.debug("Fetching patient by UUID: {}", patientUuid);

        Patient patient = patientRepository.findByPatientUuid(patientUuid)
                .orElseThrow(() -> new PatientNotFoundException("Patient not found: " + patientUuid));

        return enrichPatientWithUserInfo(patient);
    }

    @Override
    public PatientDTO getPatientByUserUuid(String userUuid) {
        log.debug("Fetching patient by user UUID: {}", userUuid);

        Patient patient = patientRepository.findByUserUuid(userUuid)
                .orElseThrow(() -> new PatientNotFoundException("Patient not found for user: " + userUuid));

        return enrichPatientWithUserInfo(patient);
    }

    @Override
    public List<PatientDTO> getAllPatients() {
        log.debug("Fetching all active patients");

        List<Patient> patients = patientRepository.findAllByActiveTrue();

        return patients.stream()
                .map(this::enrichPatientWithUserInfo)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PatientDTO updatePatient(String patientUuid, UpdatePatientRequest request) {
        log.info("Updating patient: {}", patientUuid);

        Patient existingPatient = patientRepository.findByPatientUuid(patientUuid)
                .orElseThrow(() -> new PatientNotFoundException("Patient not found: " + patientUuid));

        // Mettre à jour uniquement les champs fournis
        if (request.getDateOfBirth() != null) {
            existingPatient.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getGender() != null) {
            existingPatient.setGender(request.getGender());
        }
        if (request.getBloodType() != null) {
            existingPatient.setBloodType(request.getBloodType());
        }
        if (request.getHeightCm() != null) {
            existingPatient.setHeightCm(request.getHeightCm());
        }
        if (request.getWeightKg() != null) {
            existingPatient.setWeightKg(request.getWeightKg());
        }
        if (request.getAllergies() != null) {
            existingPatient.setAllergies(request.getAllergies());
        }
        if (request.getChronicConditions() != null) {
            existingPatient.setChronicConditions(request.getChronicConditions());
        }
        if (request.getCurrentMedications() != null) {
            existingPatient.setCurrentMedications(request.getCurrentMedications());
        }
        if (request.getEmergencyContactName() != null) {
            existingPatient.setEmergencyContactName(request.getEmergencyContactName());
        }
        if (request.getEmergencyContactPhone() != null) {
            existingPatient.setEmergencyContactPhone(request.getEmergencyContactPhone());
        }
        if (request.getEmergencyContactRelationship() != null) {
            existingPatient.setEmergencyContactRelationship(request.getEmergencyContactRelationship());
        }
        if (request.getInsuranceNumber() != null) {
            existingPatient.setInsuranceNumber(request.getInsuranceNumber());
        }
        if (request.getInsuranceProvider() != null) {
            existingPatient.setInsuranceProvider(request.getInsuranceProvider());
        }

        Patient updatedPatient = patientRepository.update(existingPatient);
        log.info("Patient updated successfully: {}", patientUuid);

        return enrichPatientWithUserInfo(updatedPatient);
    }

    @Override
    @Transactional
    public void deletePatient(String patientUuid) {
        log.info("Deleting patient (soft delete): {}", patientUuid);

        patientRepository.softDelete(patientUuid);

        log.info("Patient deleted successfully: {}", patientUuid);
    }

    /**
     * Enrichir les données patient avec les informations utilisateur
     */
    private PatientDTO enrichPatientWithUserInfo(Patient patient) {
        UserInfoDTO userInfo = authServerClient.getUserInfo(patient.getUserUuid());

        return PatientDTO.builder()
                .patientUuid(patient.getPatientUuid())
                .userUuid(patient.getUserUuid())
                // User info
                .firstName(userInfo.getFirstName())
                .lastName(userInfo.getLastName())
                .email(userInfo.getEmail())
                .phone(userInfo.getPhone())
                .address(userInfo.getAddress())
                .imageUrl(userInfo.getImageUrl())
                .memberId(userInfo.getMemberId())
                // Medical data
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
                // Emergency contact
                .emergencyContactName(patient.getEmergencyContactName())
                .emergencyContactPhone(patient.getEmergencyContactPhone())
                .emergencyContactRelationship(patient.getEmergencyContactRelationship())
                // Insurance
                .medicalRecordNumber(patient.getMedicalRecordNumber())
                .insuranceNumber(patient.getInsuranceNumber())
                .insuranceProvider(patient.getInsuranceProvider())
                // Metadata
                .active(patient.getActive())
                .createdAt(patient.getCreatedAt())
                .updatedAt(patient.getUpdatedAt())
                .build();
    }

    /**
     * Générer un numéro de dossier médical unique
     * Format: MED-YYYY-XXXXXX
     */
    private String generateMedicalRecordNumber() {
        int year = Year.now().getValue();
        int random = (int) (Math.random() * 999999);
        return String.format("MED-%d-%06d", year, random);
    }
}