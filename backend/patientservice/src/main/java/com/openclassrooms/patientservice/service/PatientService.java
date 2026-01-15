package com.openclassrooms.patientservice.service;

import com.openclassrooms.patientservice.dto.CreatePatientRequest;
import com.openclassrooms.patientservice.dto.PatientDTO;
import com.openclassrooms.patientservice.dto.UpdatePatientRequest;

import java.util.List;

/**
 * Service pour la logique métier Patient
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-01-09
 */
public interface PatientService {

    /**
     * Créer un nouveau patient
     */
    PatientDTO createPatient(CreatePatientRequest request);

    /**
     * Récupérer un patient par UUID
     */
    PatientDTO getPatientByUuid(String patientUuid);

    /**
     * Récupérer un patient par user UUID
     */
    PatientDTO getPatientByUserUuid(String userUuid);

    /**
     * Récupérer tous les patients actifs
     */
    List<PatientDTO> getAllPatients();

    /**
     * Mettre à jour un patient
     */
    PatientDTO updatePatient(String patientUuid, UpdatePatientRequest request);

    /**
     * Supprimer un patient (soft delete)
     */
    void deletePatient(String patientUuid);
}