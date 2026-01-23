package com.openclassrooms.patientservice.service.implementation;


import com.openclassrooms.patientservice.dtorequest.*;
import com.openclassrooms.patientservice.exception.ApiException;
import com.openclassrooms.patientservice.model.Patient;
import com.openclassrooms.patientservice.repository.PatientRepository;
import com.openclassrooms.patientservice.service.PatientService;
import com.openclassrooms.patientservice.service.UserService;
import jakarta.ws.rs.core.SecurityContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implémentation du service Patient
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-01-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;
    private final UserService userService;


    @Override
    public void getAllPatients() {

    }

    @Override
    public void createPatient(PatientRequest request) {

    }

    @Override
    public void getPatientByUuid(String patientUuid) {

    }

    @Override
    public void getPatientByUserUuid(String userUuid) {

    }

    @Override
    public void updatePatient(String patientUuid, Patient request) {

    }

    @Override
    @Transactional
    public void deletePatient(String patientUuid) {
        log.info("Deleting patient (soft delete): {}", patientUuid);
        patientRepository.softDeletePatient(patientUuid);
        log.info("Patient deleted successfully: {}", patientUuid);
    }


    @Override
    public Optional<Patient> findByPatientUuid(String patientUuid) {
        return Optional.empty();
    }

    @Override
    public Optional<Patient> findByUserUuid(String userUuid) {
        return Optional.empty();
    }

    @Override
    public boolean existsByUserUuid(String userUuid) {
        return false;
    }

    @Override
    public List<Patient> findAllByActiveTrue() {
        return List.of();
    }

    @Override
    public Optional<Patient> findByMedicalRecordNumber(String medicalRecordNumber) {
        return Optional.empty();
    }

    @Override
    public List<Patient> findAllActivePatientsOrderByCreatedAtDesc() {
        return List.of();
    }

    @Override
    public long countByActiveTrue() {
        return 0;
    }

    @Override
    public List<Patient> findByBloodTypeAndActiveTrue(String bloodType) {
        return List.of();
    }

    @Override
    public void softDeletePatient(String patientUuid) {

    }

    public void someBusinessMethod(String userUuid) {
        String token = extractBearerToken();
        userService.getUserByUuid(userUuid, token);
    }

    private String extractBearerToken() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            return "Bearer " + jwtAuthenticationToken.getToken().getTokenValue();
        }
        throw new ApiException("Utilisateur non anthentifié");
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