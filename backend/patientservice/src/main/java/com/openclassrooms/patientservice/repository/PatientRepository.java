package com.openclassrooms.patientservice.repository;

import com.openclassrooms.patientservice.model.Patient;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour les opérations Patient
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-01-09
 */
public interface PatientRepository {

    /**
     * Créer un nouveau patient
     */
    Patient save(Patient patient);

    /**
     * Mettre à jour un patient existant
     */
    Patient update(Patient patient);

    /**
     * Trouver un patient par UUID
     */
    Optional<Patient> findByPatientUuid(String patientUuid);

    /**
     * Trouver un patient par user UUID
     */
    Optional<Patient> findByUserUuid(String userUuid);

    /**
     * Vérifier si un patient existe par user UUID
     */
    boolean existsByUserUuid(String userUuid);

    /**
     * Trouver tous les patients actifs
     */
    List<Patient> findAllByActiveTrue();

    /**
     * Trouver un patient par numéro de dossier médical
     */
    Optional<Patient> findByMedicalRecordNumber(String medicalRecordNumber);

    /**
     * Trouver tous les patients actifs par date de création (desc)
     */
    List<Patient> findAllActivePatientsOrderByCreatedAtDesc();

    /**
     * Compter les patients actifs
     */
    long countByActiveTrue();

    /**
     * Trouver les patients par groupe sanguin
     */
    List<Patient> findByBloodTypeAndActiveTrue(String bloodType);

    /**
     * Soft delete d'un patient
     */
    void softDelete(String patientUuid);
}