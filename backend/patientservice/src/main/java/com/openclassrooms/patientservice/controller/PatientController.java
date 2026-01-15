package com.openclassrooms.patientservice.controller;

import com.openclassrooms.patientservice.dto.ApiResponse;
import com.openclassrooms.patientservice.dto.CreatePatientRequest;
import com.openclassrooms.patientservice.dto.PatientDTO;
import com.openclassrooms.patientservice.dto.UpdatePatientRequest;
import com.openclassrooms.patientservice.service.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST pour les patients
 *
 * @author FirstName LastName
 * @version 1.0
 * @since 2026-01-09
 */
@Slf4j
@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    /**
     * Créer un nouveau patient
     * Accessible par: ORGANIZER, ADMIN, PRACTITIONER
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ORGANIZER', 'ADMIN', 'PRACTITIONER')")
    public ResponseEntity<ApiResponse<PatientDTO>> createPatient(
            @Valid @RequestBody CreatePatientRequest request) {

        log.info("Creating patient for user: {}", request.getUserUuid());

        PatientDTO patient = patientService.createPatient(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Patient created successfully", patient));
    }

    /**
     * Récupérer un patient par UUID
     * Accessible par: ORGANIZER, ADMIN, PRACTITIONER
     */
    @GetMapping("/{patientUuid}")
    @PreAuthorize("hasAnyAuthority('ORGANIZER', 'ADMIN', 'PRACTITIONER')")
    public ResponseEntity<ApiResponse<PatientDTO>> getPatientByUuid(
            @PathVariable String patientUuid) {

        log.debug("Fetching patient: {}", patientUuid);

        PatientDTO patient = patientService.getPatientByUuid(patientUuid);

        return ResponseEntity.ok(ApiResponse.success(patient));
    }

    /**
     * Récupérer un patient par user UUID
     * Accessible par: ORGANIZER, ADMIN, PRACTITIONER
     */
    @GetMapping("/user/{userUuid}")
    @PreAuthorize("hasAnyAuthority('ORGANIZER', 'ADMIN', 'PRACTITIONER')")
    public ResponseEntity<ApiResponse<PatientDTO>> getPatientByUserUuid(
            @PathVariable String userUuid) {

        log.debug("Fetching patient for user: {}", userUuid);

        PatientDTO patient = patientService.getPatientByUserUuid(userUuid);

        return ResponseEntity.ok(ApiResponse.success(patient));
    }

    /**
     * Récupérer son propre dossier patient
     * Accessible par: USER (authentifié)
     */
    @GetMapping("/me")
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<ApiResponse<PatientDTO>> getMyPatient(Authentication authentication) {

        String userUuid = authentication.getName();
        log.debug("Fetching own patient record for user: {}", userUuid);

        PatientDTO patient = patientService.getPatientByUserUuid(userUuid);

        return ResponseEntity.ok(ApiResponse.success(patient));
    }

    /**
     * Récupérer tous les patients
     * Accessible par: ORGANIZER, ADMIN, PRACTITIONER
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ORGANIZER', 'ADMIN', 'PRACTITIONER')")
    public ResponseEntity<ApiResponse<List<PatientDTO>>> getAllPatients() {

        log.debug("Fetching all patients");

        List<PatientDTO> patients = patientService.getAllPatients();

        return ResponseEntity.ok(ApiResponse.success(patients));
    }

    /**
     * Mettre à jour un patient
     * Accessible par: ORGANIZER, ADMIN, PRACTITIONER
     */
    @PutMapping("/{patientUuid}")
    @PreAuthorize("hasAnyAuthority('ORGANIZER', 'ADMIN', 'PRACTITIONER')")
    public ResponseEntity<ApiResponse<PatientDTO>> updatePatient(
            @PathVariable String patientUuid,
            @Valid @RequestBody UpdatePatientRequest request) {

        log.info("Updating patient: {}", patientUuid);

        PatientDTO patient = patientService.updatePatient(patientUuid, request);

        return ResponseEntity.ok(ApiResponse.success("Patient updated successfully", patient));
    }

    /**
     * Supprimer un patient (soft delete)
     * Accessible par: ORGANIZER, ADMIN
     */
    @DeleteMapping("/{patientUuid}")
    @PreAuthorize("hasAnyAuthority('ORGANIZER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deletePatient(@PathVariable String patientUuid) {

        log.info("Deleting patient: {}", patientUuid);

        patientService.deletePatient(patientUuid);

        return ResponseEntity.ok(ApiResponse.success("Patient deleted successfully", null));
    }
}