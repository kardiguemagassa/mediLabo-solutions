package com.openclassrooms.patientservice.controller;

import com.openclassrooms.patientservice.domain.Response;
import com.openclassrooms.patientservice.dtorequest.PatientRequest;
import com.openclassrooms.patientservice.dtoresponse.PatientResponse;
import com.openclassrooms.patientservice.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static com.openclassrooms.patientservice.constant.Role.*;
import static com.openclassrooms.patientservice.util.RequestUtils.getResponse;
import static org.springframework.http.HttpStatus.*;

/**
 * Controller REST pour la gestion des dossiers patients.
 * http://localhost:8081/swagger-ui/index.html
 * http://localhost:8081/v3/api-docs
 * http://localhost:8081/v3/api-docs.yaml
 * @author Kardigué MAGASSA
 * @version 2.0
 * @since 2026-01-09
 */
@Tag(name = "Patients", description = "API de gestion des dossiers patients")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    // CREATE

    @Operation(summary = "Créer un nouveau patient",
            description = "Crée un dossier patient associé à un utilisateur existant")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Patient créé avec succès"),
            @ApiResponse(responseCode = "400", description = "Requête invalide"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "403", description = "Accès refusé")
    })
    @PostMapping
    @PreAuthorize(ALL_STAFF)
    public ResponseEntity<Response> createPatient(@Parameter(description = "Données du patient", required = true) @Valid @RequestBody PatientRequest request, HttpServletRequest httpRequest) {
        log.info("Creating patient for user: {}", request.getUserUuid());
        PatientResponse patient = patientService.createPatient(request);
        URI location = URI.create("/api/patients/" + patient.getPatientUuid());
        return ResponseEntity.created(location).body(getResponse(httpRequest, Map.of("patient", patient), "Patient créé avec succès", CREATED));
    }

    // READ

    @Operation(summary = "Lister tous les patients actifs")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste récupérée"),
            @ApiResponse(responseCode = "403", description = "Accès refusé")
    })
    @GetMapping
    @PreAuthorize(ALL_STAFF)
    public ResponseEntity<Response> getAllPatients(HttpServletRequest request) {
        log.debug("Fetching all patients");
        List<PatientResponse> patients = patientService.getAllActivePatients();
        return ResponseEntity.ok(getResponse(request, Map.of("patients", patients, "count", patients.size()), "Patients récupérés avec succès", OK));
    }

    @Operation(summary = "Récupérer un patient par UUID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Patient trouvé"),
            @ApiResponse(responseCode = "404", description = "Patient introuvable")
    })
    @GetMapping("/{patientUuid}")
    @PreAuthorize(ALL_STAFF)
    public ResponseEntity<Response> getPatientByUuid(@Parameter(description = "UUID du patient", example = "550e8400-e29b-41d4-a716-446655440000") @PathVariable String patientUuid, HttpServletRequest request) {

        log.debug("Fetching patient: {}", patientUuid);
        PatientResponse patient = patientService.getPatientByUuid(patientUuid);
        return ResponseEntity.ok(getResponse(request, Map.of("patient", patient), "Patient récupéré avec succès", OK));
    }

    @Operation(summary = "Récupérer un patient via l'UUID utilisateur")
    @GetMapping("/user/{userUuid}")
    @PreAuthorize(ALL_STAFF)
    public ResponseEntity<Response> getPatientByUserUuid(@Parameter(description = "UUID de l'utilisateur") @PathVariable String userUuid, HttpServletRequest request) {
        log.debug("Fetching patient for user: {}", userUuid);
        PatientResponse patient = patientService.getPatientByUserUuid(userUuid);
        return ResponseEntity.ok(getResponse(request, Map.of("patient", patient), "Patient récupéré avec succès", OK));
    }

    @Operation(summary = "Récupérer un patient par email",
            description = "Recherche via Authorization Server puis récupère le dossier patient")
    @GetMapping("/email/{email}")
    @PreAuthorize(ALL_STAFF)
    public ResponseEntity<Response> getPatientByEmail(@Parameter(description = "Email de l'utilisateur", example = "patient@email.com") @PathVariable String email, HttpServletRequest request) {
        log.debug("Fetching patient by email: {}", email);
        PatientResponse patient = patientService.getPatientByEmail(email);
        return ResponseEntity.ok(getResponse(request, Map.of("patient", patient), "Patient récupéré avec succès", OK));
    }

    @Operation(summary = "Récupérer son propre dossier patient",
            description = "L'UUID est extrait du token d'authentification")
    @GetMapping("/me")
    @PreAuthorize(ALL_AUTHENTICATED)
    public ResponseEntity<Response> getMyPatientRecord(@Parameter(hidden = true) Authentication authentication, HttpServletRequest request) {
        String userUuid = authentication.getName();
        log.debug("Fetching own patient record for user: {}", userUuid);
        PatientResponse patient = patientService.getPatientByUserUuid(userUuid);
        return ResponseEntity.ok(getResponse(request, Map.of("patient", patient), "Dossier patient récupéré avec succès", OK));
    }

    @Operation(summary = "Récupérer un patient par numéro de dossier médical")
    @GetMapping("/medical-record/{medicalRecordNumber}")
    @PreAuthorize(ALL_STAFF)
    public ResponseEntity<Response> getPatientByMedicalRecordNumber(@Parameter(description = "Numéro de dossier médical", example = "MED-2026-123456") @PathVariable String medicalRecordNumber, HttpServletRequest request) {
        log.debug("Fetching patient by medical record: {}", medicalRecordNumber);
        PatientResponse patient = patientService.getPatientByMedicalRecordNumber(medicalRecordNumber);
        return ResponseEntity.ok(getResponse(request, Map.of("patient", patient), "Patient récupéré avec succès", OK));
    }

    @Operation(summary = "Rechercher des patients par groupe sanguin")
    @GetMapping("/blood-type/{bloodType}")
    @PreAuthorize(ALL_STAFF)
    public ResponseEntity<Response> getPatientsByBloodType(@Parameter(description = "Groupe sanguin", example = "O+") @PathVariable String bloodType, HttpServletRequest request) {
        log.debug("Fetching patients by blood type: {}", bloodType);
        List<PatientResponse> patients = patientService.getPatientsByBloodType(bloodType);
        return ResponseEntity.ok(getResponse(request, Map.of("patients", patients, "count", patients.size()), "Patients récupérés avec succès", OK));
    }

    // UPDATE

    @Operation(summary = "Mettre à jour un patient")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Patient mis à jour"),
            @ApiResponse(responseCode = "404", description = "Patient introuvable")
    })
    @PutMapping("/{patientUuid}")
    @PreAuthorize(ALL_STAFF)
    public ResponseEntity<Response> updatePatient(@Parameter(description = "UUID du patient") @PathVariable String patientUuid, @Valid @RequestBody PatientRequest request, HttpServletRequest httpRequest) {
        log.info("Updating patient: {}", patientUuid);
        PatientResponse patient = patientService.updatePatient(patientUuid, request);
        return ResponseEntity.ok(getResponse(httpRequest, Map.of("patient", patient), "Patient mis à jour avec succès", OK));
    }

    // DELETE

    @Operation(summary = "Supprimer un patient (soft delete)")
    @DeleteMapping("/{patientUuid}")
    @PreAuthorize(ADMIN_ONLY)
    public ResponseEntity<Response> deletePatient(@Parameter(description = "UUID du patient") @PathVariable String patientUuid, HttpServletRequest request) {
        log.info("Deleting patient: {}", patientUuid);
        patientService.deletePatient(patientUuid);
        return ResponseEntity.ok(getResponse(request, Map.of(), "Patient supprimé avec succès", OK));
    }

    // STATS & UTILS

    @Operation(summary = "Obtenir le nombre total de patients actifs")
    @GetMapping("/stats/count")
    @PreAuthorize(ADMIN_ONLY)
    public ResponseEntity<Response> getPatientCount(HttpServletRequest request) {
        long count = patientService.countActivePatients();
        return ResponseEntity.ok(getResponse(request, Map.of("totalPatients", count), "Statistiques récupérées", OK));
    }

    @Operation(summary = "Vérifier si un utilisateur a un dossier patient")
    @GetMapping("/exists/user/{userUuid}")
    @PreAuthorize(ALL_STAFF)
    public ResponseEntity<Response> checkPatientExists(@Parameter(description = "UUID de l'utilisateur") @PathVariable String userUuid, HttpServletRequest request) {
        boolean exists = patientService.hasPatientRecord(userUuid);
        return ResponseEntity.ok(getResponse(request, Map.of("exists", exists), exists ? "Dossier patient existant" : "Aucun dossier patient", OK));
    }
}