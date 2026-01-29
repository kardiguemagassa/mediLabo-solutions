package com.openclassrooms.patientservice.service.implementation;

import com.openclassrooms.patientservice.dtorequest.PatientRequest;
import com.openclassrooms.patientservice.dtorequest.UserRequest;
import com.openclassrooms.patientservice.dtoresponse.PatientResponse;
import com.openclassrooms.patientservice.exception.ApiException;
import com.openclassrooms.patientservice.mapper.PatientMapper;
import com.openclassrooms.patientservice.model.Patient;
import com.openclassrooms.patientservice.repository.PatientRepository;
import com.openclassrooms.patientservice.service.PatientService;
import com.openclassrooms.patientservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.List;
import java.util.Optional;

import static com.openclassrooms.patientservice.util.UserUtils.hasElevatedPermissions;

/**
 * Implémentation du service Patient.
 *
 * Responsabilités :
 * - Validation métier
 * - Orchestration des appels repository et services externes
 * - Transformation Entity <-> DTO
 * - Gestion des erreurs métier
 *
 * @author Kardigué MAGASSA
 * @version 2.0
 * @since 2026-01-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;
    private final UserService userService;


    // CRUD OPERATIONS

    @Override
    @Transactional
    public PatientResponse createPatient(PatientRequest request) {
        log.info("Creating patient for user: {}", request.getUserUuid());

        // Vérifier qu'un dossier n'existe pas déjà pour cet utilisateur
        if (patientRepository.existsPatientByUserUuid(request.getUserUuid())) {
            throw new ApiException("Un dossier patient existe déjà pour cet utilisateur");
        }

        // Vérifier que l'utilisateur existe dans Authorization Server
        try {
            userService.getUserByUuid(request.getUserUuid());
        } catch (ApiException e) {
            throw new ApiException("Utilisateur non trouvé dans le système");
        }

        // Générer le numéro de dossier médical
        String medicalRecordNumber = generateUniqueMedicalRecordNumber();

        // Créer l'entité
        Patient patient = patientMapper.toEntity(request, medicalRecordNumber);

        // Persister
        Patient savedPatient = patientRepository.savePatient(patient);
        log.info("Patient created successfully: {}", savedPatient.getPatientUuid());

        return patientMapper.toResponse(savedPatient);
    }

    @Override
    public PatientResponse getPatientByUuid(String patientUuid) {
        log.debug("Fetching patient by UUID: {}", patientUuid);

        Patient patient = patientRepository.findByPatientUuid(patientUuid)
                .orElseThrow(() -> new ApiException("Patient non trouvé: " + patientUuid));

        // Enrichir avec les infos utilisateur
        return enrichWithUserInfo(patient);
    }

    @Override
    public PatientResponse getPatientByUserUuid(String userUuid) {
        log.debug("Fetching patient for user: {}", userUuid);

        Patient patient = patientRepository.findByUserUuid(userUuid)
                .orElseThrow(() -> new ApiException("Aucun dossier patient pour cet utilisateur"));

        // Enrichir avec les infos utilisateur
        return enrichWithUserInfo(patient);
    }

    // Méthode utilitaire privée
    private PatientResponse enrichWithUserInfo(Patient patient) {
        try {
            UserRequest user = userService.getUserByUuid(patient.getUserUuid());
            return patientMapper.toResponseWithUserInfo(patient, user);
        } catch (ApiException e) {
            log.warn("Could not fetch user info for patient: {}", patient.getPatientUuid());
            return patientMapper.toResponse(patient);
        }
    }

    /**
     * Récupère un patient par email.
     *
     * Flow:
     * 1. Appeler Authorization Server pour obtenir l'utilisateur par email
     * 2. Récupérer le userUuid
     * 3. Chercher le patient par userUuid
     */
    @Override
    public PatientResponse getPatientByEmail(String email) {
        log.debug("Fetching patient by email: {}", email);

        // 1. Appeler Authorization Server pour obtenir l'utilisateur
        Optional<UserRequest> userOptional = userService.getUserByEmail(email);

        if (userOptional.isEmpty()) {
            throw new ApiException("Aucun utilisateur trouvé avec cet email: " + email);
        }

        UserRequest user = userOptional.get();
        String userUuid = user.getUserUuid();

        // 2. Chercher le patient par userUuid
        Patient patient = patientRepository.findByUserUuid(userUuid)
                .orElseThrow(() -> new ApiException("Aucun dossier patient pour l'email: " + email));

        // 3. Retourner avec les infos utilisateur enrichies
        return patientMapper.toResponseWithUserInfo(patient, user);
    }

    @Override
    public List<PatientResponse> getAllActivePatients() {
        log.debug("Fetching all active patients");

        List<Patient> patients = patientRepository.findAllPatientByActiveTrue();

        return patientMapper.toResponseList(patients);
    }

    @Override
    @Transactional
    public PatientResponse updatePatient(String patientUuid, PatientRequest request) {
        log.info("Updating patient: {}", patientUuid);

        // Récupérer le patient existant
        Patient existingPatient = patientRepository.findByPatientUuid(patientUuid)
                .orElseThrow(() -> new ApiException("Patient non trouvé: " + patientUuid));

        // Mettre à jour les champs
        Patient updatedPatient = patientMapper.updateEntity(existingPatient, request);

        // Persister
        Patient savedPatient = patientRepository.updatePatient(updatedPatient);
        log.info("Patient updated successfully: {}", patientUuid);

        log.info("Patient updated successfully: {}", patientUuid);

        return enrichWithUserInfo(savedPatient);
    }

    @Override
    @Transactional
    public void deletePatient(String patientUuid) {
        log.info("Soft deleting patient: {}", patientUuid);

        // Vérifier que le patient existe
        if (patientRepository.findByPatientUuid(patientUuid).isEmpty()) {
            throw new ApiException("Patient non trouvé: " + patientUuid);
        }

        boolean deleted = patientRepository.softDeletePatientByPatientUuid(patientUuid);

        if (!deleted) {
            throw new ApiException("Erreur lors de la suppression du patient");
        }

        log.info("Patient deleted successfully: {}", patientUuid);
    }

    // QUERY OPERATIONS

    @Override
    public PatientResponse getPatientByMedicalRecordNumber(String medicalRecordNumber) {
        log.debug("Fetching patient by medical record: {}", medicalRecordNumber);

        Patient patient = patientRepository.findPatientByMedicalRecordNumber(medicalRecordNumber)
                .orElseThrow(() -> new ApiException("Dossier médical non trouvé: " + medicalRecordNumber));

        return enrichWithUserInfo(patient);
    }

    @Override
    public List<PatientResponse> getPatientsByBloodType(String bloodType) {
        log.debug("Fetching patients by blood type: {}", bloodType);

        List<Patient> patients = patientRepository.findPatientByBloodTypeAndActiveTrue(bloodType);

        return patientMapper.toResponseList(patients);
    }

    // UTILITY OPERATIONS

    @Override
    public boolean hasPatientRecord(String userUuid) {
        return patientRepository.existsPatientByUserUuid(userUuid);
    }

    @Override
    public long countActivePatients() {
        return patientRepository.countPatientByActiveTrue();
    }

    // PRIVATE METHODS

    /**
     * Génère un numéro de dossier médical unique.
     * Format: MED-YYYY-XXXXXX
     *
     * @return numéro de dossier unique
     */
    private String generateUniqueMedicalRecordNumber() {
        String medicalRecordNumber;
        int attempts = 0;
        final int maxAttempts = 10;

        do {
            medicalRecordNumber = generateMedicalRecordNumber();
            attempts++;

            if (attempts >= maxAttempts) {
                throw new ApiException("Impossible de générer un numéro de dossier unique");
            }
        } while (patientRepository.existsPatientByMedicalRecordNumber(medicalRecordNumber));

        return medicalRecordNumber;
    }

    private String generateMedicalRecordNumber() {
        int year = Year.now().getValue();
        int random = (int) (Math.random() * 999999);
        return String.format("MED-%d-%06d", year, random);
    }
}