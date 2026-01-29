package com.openclassrooms.patientservice.service;

import com.openclassrooms.patientservice.dtorequest.PatientRequest;
import com.openclassrooms.patientservice.dtoresponse.PatientResponse;

import java.util.List;

/**
 * Service pour la logique métier Patient.
 *
 * Architecture:
 * - Les données médicales sont dans PatientService (PostgreSQL)
 * - Les données utilisateur (email, nom) sont dans Authorization Server
 * - La recherche par email nécessite un appel inter-service
 *
 * Conventions de nommage :
 * - get...    : récupération (lève exception si non trouvé)
 * - create... : création
 * - update... : mise à jour
 * - delete... : suppression
 * - has/exists... : vérification
 * - count...  : comptage
 *
 * @author Kardigué MAGASSA
 * @version 2.0
 * @since 2026-01-09
 */
public interface PatientService {

    // CRUD OPERATIONS

    /**
     * Crée un nouveau dossier patient
     * @param request données du patient
     * @return patient créé
     */
    PatientResponse createPatient(PatientRequest request);

    /**
     * Récupère un patient par son UUID
     * @param patientUuid UUID du patient
     * @return patient trouvé
     */
    PatientResponse getPatientByUuid(String patientUuid);

    /**
     * Récupère un patient par l'UUID utilisateur
     * @param userUuid UUID de l'utilisateur (Authorization Server)
     * @return patient trouvé
     */
    PatientResponse getPatientByUserUuid(String userUuid);

    /**
     * Récupère un patient par email.
     *
     * Flow:
     * 1. Appel Authorization Server pour obtenir userUuid depuis email
     * 2. Recherche du patient par userUuid
     *
     * @param email email de l'utilisateur
     * @return patient trouvé
     */
    PatientResponse getPatientByEmail(String email);

    /**
     * Récupère tous les patients actifs
     * @return liste des patients
     */
    List<PatientResponse> getAllActivePatients();

    /**
     * Met à jour un patient existant
     * @param patientUuid UUID du patient
     * @param request nouvelles données
     * @return patient mis à jour
     */
    PatientResponse updatePatient(String patientUuid, PatientRequest request);

    /**
     * Supprime un patient (soft delete)
     * @param patientUuid UUID du patient
     */
    void deletePatient(String patientUuid);

    // QUERY OPERATIONS

    /**
     * Récupère un patient par numéro de dossier médical
     * @param medicalRecordNumber numéro de dossier
     * @return patient trouvé
     */
    PatientResponse getPatientByMedicalRecordNumber(String medicalRecordNumber);

    /**
     * Récupère les patients par groupe sanguin
     * @param bloodType groupe sanguin
     * @return liste des patients
     */
    List<PatientResponse> getPatientsByBloodType(String bloodType);

    // UTILITY OPERATIONS

    /**
     * Vérifie si un utilisateur a déjà un dossier patient
     * @param userUuid UUID de l'utilisateur
     * @return true si un dossier existe
     */
    boolean hasPatientRecord(String userUuid);

    /**
     * Compte le nombre de patients actifs
     * @return nombre de patients
     */
    long countActivePatients();
}