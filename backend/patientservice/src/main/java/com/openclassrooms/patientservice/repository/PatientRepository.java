package com.openclassrooms.patientservice.repository;

import com.openclassrooms.patientservice.model.Patient;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour les opérations Patient.
 *
 * Note: Ce repository gère uniquement les données médicales.
 * Les informations utilisateur (email, nom, etc.) sont dans Authorization Server.
 *
 * Conventions de nommage Spring Data :
 * - findBy...  : recherche retournant Optional ou List
 * - existsBy... : vérification d'existence (boolean)
 * - countBy...  : comptage
 * - save        : création ou mise à jour
 * - deleteBy... : suppression
 *
 * @author Kardigué MAGASSA
 * @version 2.0
 * @since 2026-01-09
 */
public interface PatientRepository {

    // CREATE

    /**
     * Persiste un nouveau patient
     * @param patient entité à créer
     * @return patient créé avec ID généré
     */
    Patient savePatient(Patient patient);

    // READ

    /**
     * Recherche un patient par son UUID public
     * @param patientUuid UUID du patient
     * @return Optional contenant le patient si trouvé
     */
    Optional<Patient> findByPatientUuid(String patientUuid);

    /**
     * Recherche un patient par l'UUID utilisateur (Authorization Server)
     * @param userUuid UUID de l'utilisateur
     * @return Optional contenant le patient si trouvé
     */
    Optional<Patient> findByUserUuid(String userUuid);

    /**
     * Recherche un patient par son numéro de dossier médical
     * @param medicalRecordNumber numéro de dossier (format: MED-YYYY-XXXXXX)
     * @return Optional contenant le patient si trouvé
     */
    Optional<Patient> findPatientByMedicalRecordNumber(String medicalRecordNumber);

    /**
     * Récupère tous les patients actifs
     * @return liste des patients actifs triés par date de création (desc)
     */
    List<Patient> findAllPatientByActiveTrue();

    /**
     * Récupère les patients par groupe sanguin
     * @param bloodType groupe sanguin (A+, A-, B+, B-, AB+, AB-, O+, O-)
     * @return liste des patients actifs avec ce groupe sanguin
     */
    List<Patient> findPatientByBloodTypeAndActiveTrue(String bloodType);

    //EXISTS / COUNT

    /**
     * Vérifie si un patient existe pour cet utilisateur
     * @param userUuid UUID de l'utilisateur
     * @return true si un dossier patient existe
     */
    boolean existsPatientByUserUuid(String userUuid);

    /**
     * Vérifie si un numéro de dossier médical existe déjà
     * @param medicalRecordNumber numéro à vérifier
     * @return true si le numéro existe
     */
    boolean existsPatientByMedicalRecordNumber(String medicalRecordNumber);

    /**
     * Compte le nombre de patients actifs
     * @return nombre de patients actifs
     */
    long countPatientByActiveTrue();

    // UPDATE

    /**
     * Met à jour un patient existant
     * @param patient entité avec les modifications
     * @return patient mis à jour
     */
    Patient updatePatient(Patient patient);

    // DELETE

    /**
     * Soft delete : désactive un patient (active = false)
     * @param patientUuid UUID du patient à désactiver
     * @return true si la désactivation a réussi
     */
    boolean softDeletePatientByPatientUuid(String patientUuid);
}