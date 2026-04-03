package com.openclassrooms.patientservice.repository;

import com.openclassrooms.patientservice.model.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Repository JPA pour la gestion des patients.
 * Spring Data génère l'implémentation automatiquement à partir des noms de méthodes.
 * {@code @SQLRestriction("active = true")} sur l'entité filtre automatiquement les patients supprimés.
 * Méthodes héritées de JpaRepository :
 * save(Patient) : création ET mise à jour (merge si l'ID existe)
 * findById(Long) : recherche par ID technique
 * deleteById(Long) : suppression physique (non utilisée, je préfère le soft delete)
 *
 * @author Kardigué MAGASSA
 * @version 3.0
 * @since 2026-04-01
 */

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByPatientUuid(String patientUuid);
    Optional<Patient> findByUserUuid(String userUuid);
    Optional<Patient> findByMedicalRecordNumber(String medicalRecordNumber);
    List<Patient> findAllByActiveTrueOrderByCreatedAtDesc();
    Page<Patient> findByActiveTrue(Pageable pageable);
    // Tous les patients actifs + inactifs pour la vue admin
    Page<Patient> findAllByOrderByCreatedAtDesc(Pageable pageable);
    List<Patient> findAllByOrderByCreatedAtDesc();
    List<Patient> findByBloodTypeAndActiveTrue(String bloodType);
    Page<Patient> findByBloodTypeAndActiveTrue(String bloodType, Pageable pageable);
    boolean existsByUserUuid(String userUuid);
    boolean existsByMedicalRecordNumber(String medicalRecordNumber);
    long countByActiveTrue();


    /**
     * Soft delete : marque le patient comme inactif.
     * Utilise JPQL — @SQLRestriction ne s'applique pas aux UPDATE JPQL.
     * @return nombre de lignes modifiées (0 ou 1)
     */
    @Transactional
    @Modifying
    @Query("UPDATE Patient p SET p.active = false WHERE p.patientUuid = :patientUuid AND p.active = true")
    int softDeleteByPatientUuid(@Param("patientUuid") String patientUuid);

    Optional<Patient> findByPatientUuidAndActiveFalse(String patientUuid);

    /**
     * Restore : réactive un patient supprimé.
     * @return nombre de lignes modifiées (0 ou 1)
     */
    @Transactional
    @Modifying
    @Query("UPDATE Patient p SET p.active = true WHERE p.patientUuid = :patientUuid AND p.active = false")
    int restoreByPatientUuid(@Param("patientUuid") String patientUuid);

    /**Recherche un patient supprimé soft deleted.*/
    @Query(value = "SELECT * FROM patients WHERE patient_uuid = :patientUuid AND active = false", nativeQuery = true)
    Optional<Patient> findSoftDeletedByPatientUuid(@Param("patientUuid") String patientUuid);
}