package com.openclassrooms.patientservice.controller;

import com.openclassrooms.patientservice.domain.Response;
import com.openclassrooms.patientservice.dtorequest.PatientRequest;
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
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Map;

import static com.openclassrooms.patientservice.constant.Role.*;
import static com.openclassrooms.patientservice.util.RequestUtils.getResponse;
import static org.springframework.http.HttpStatus.*;

/**
 * Controller REST pour la gestion des dossiers patients (Full Réactif).
 *
 * @author Kardigué MAGASSA
 * @version 2.1
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
    public Mono<ResponseEntity<Response>> createPatient(@Parameter(description = "Données du patient", required = true)
                                                            @Valid @RequestBody PatientRequest request, HttpServletRequest httpRequest) {

        log.info("Creating patient for user: {}", request.getUserUuid());

        return patientService.createPatient(request)
                .map(patient -> {URI location = URI.create("/api/patients/" + patient.getPatientUuid());
                    return ResponseEntity.created(location).body(getResponse(httpRequest, Map.of("patient", patient), "Patient créé avec succès", CREATED));});
    }

    // READ

    @Operation(summary = "Lister tous les patients actifs")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste récupérée"),
            @ApiResponse(responseCode = "403", description = "Accès refusé")
    })
    @GetMapping
    @PreAuthorize(ALL_STAFF)
    public Mono<ResponseEntity<Response>> getAllPatients(HttpServletRequest request) {
        log.debug("Fetching all patients");

        return patientService.getAllActivePatients().collectList().map(patients -> ResponseEntity.ok(
                getResponse(request, Map.of("patients", patients, "count", patients.size()), "Patients récupérés avec succès", OK)));
    }

    @Operation(summary = "Récupérer un patient par UUID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Patient trouvé"),
            @ApiResponse(responseCode = "404", description = "Patient introuvable")
    })
    @GetMapping("/{patientUuid}")
    @PreAuthorize(ALL_STAFF)
    public Mono<ResponseEntity<Response>> getPatientByUuid(@Parameter(description = "UUID du patient", example = "550e8400-e29b-41d4-a716-446655440000")
                                                               @PathVariable String patientUuid, HttpServletRequest request) {

        log.debug("Fetching patient: {}", patientUuid);

        return patientService.getPatientByUuid(patientUuid)
                .map(patient -> ResponseEntity.ok(
                        getResponse(request, Map.of("patient", patient), "Patient récupéré avec succès", OK)))
                // switchIfEmpty pour retourner 404
                .switchIfEmpty(Mono.just(ResponseEntity.status(NOT_FOUND)
                        .body(getResponse(request, Map.of(), "Patient introuvable", NOT_FOUND))));
    }

    @Operation(summary = "Récupérer un patient via l'UUID utilisateur")
    @GetMapping("/user/{userUuid}")
    @PreAuthorize(ALL_STAFF)
    public Mono<ResponseEntity<Response>> getPatientByUserUuid(@Parameter(description = "UUID de l'utilisateur") @PathVariable String userUuid, HttpServletRequest request) {

        log.debug("Fetching patient for user: {}", userUuid);

        return patientService.getPatientByUserUuid(userUuid).map(patient -> ResponseEntity.ok(
                        getResponse(request, Map.of("patient", patient), "Patient récupéré avec succès", OK)))
                .switchIfEmpty(Mono.just(ResponseEntity.status(NOT_FOUND)
                        .body(getResponse(request, Map.of(), "Patient introuvable", NOT_FOUND))));
    }

    @Operation(summary = "Récupérer un patient par email",
            description = "Recherche via Authorization Server puis récupère le dossier patient")
    @GetMapping("/email/{email}")
    @PreAuthorize(ALL_STAFF)
    public Mono<ResponseEntity<Response>> getPatientByEmail(@Parameter(description = "Email de l'utilisateur", example = "patient@email.com")
                                                                @PathVariable String email, HttpServletRequest request) {

        log.debug("Fetching patient by email: {}", email);

        return patientService.getPatientByEmail(email)
                .map(patient -> ResponseEntity.ok(
                        getResponse(request, Map.of("patient", patient), "Patient récupéré avec succès", OK)))
                // switchIfEmpty pour retourner 404
                .switchIfEmpty(Mono.just(ResponseEntity.status(NOT_FOUND)
                        .body(getResponse(request, Map.of(), "Patient introuvable", NOT_FOUND))));
    }

    @Operation(summary = "Récupérer son propre dossier patient",
            description = "L'UUID est extrait du token d'authentification")
    @GetMapping("/me")
    @PreAuthorize(ALL_AUTHENTICATED)
    public Mono<ResponseEntity<Response>> getMyPatientRecord(@Parameter(hidden = true) Authentication authentication, HttpServletRequest request) {

        String userUuid = authentication.getName();
        log.debug("Fetching own patient record for user: {}", userUuid);

        return patientService.getPatientByUserUuid(userUuid)
                .map(patient -> ResponseEntity.ok(
                        getResponse(request, Map.of("patient", patient), "Patient récupéré avec succès", OK)))
                .switchIfEmpty(Mono.just(ResponseEntity.status(NOT_FOUND)
                        .body(getResponse(request, Map.of(), "Patient introuvable", NOT_FOUND))));
    }

    @Operation(summary = "Récupérer un patient par numéro de dossier médical")
    @GetMapping("/medical-record/{medicalRecordNumber}")
    @PreAuthorize(ALL_STAFF)
    public Mono<ResponseEntity<Response>> getPatientByMedicalRecordNumber(@Parameter(description = "Numéro de dossier médical", example = "MED-2026-123456")
            @PathVariable String medicalRecordNumber, HttpServletRequest request) {

        log.debug("Fetching patient by medical record: {}", medicalRecordNumber);

        return patientService.getPatientByMedicalRecordNumber(medicalRecordNumber)
                .map(patient -> ResponseEntity.ok(
                        getResponse(request, Map.of("patient", patient), "Patient récupéré avec succès", OK)))
                .switchIfEmpty(Mono.just(ResponseEntity.status(NOT_FOUND)
                        .body(getResponse(request, Map.of(), "Patient introuvable", NOT_FOUND))));
    }

    @Operation(summary = "Rechercher des patients par groupe sanguin")
    @GetMapping("/blood-type/{bloodType}")
    @PreAuthorize(ALL_STAFF)
    public Mono<ResponseEntity<Response>> getPatientsByBloodType(@Parameter(description = "Groupe sanguin", example = "O+")
                                                                     @PathVariable String bloodType, HttpServletRequest request) {

        log.debug("Fetching patients by blood type: {}", bloodType);

        return patientService.getPatientsByBloodType(bloodType).collectList().map(patients -> ResponseEntity.ok(
                        getResponse(request, Map.of("patients", patients, "count", patients.size()), "Patients récupérés avec succès", OK)));
    }

    // UPDATE

    @Operation(summary = "Mettre à jour un patient")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Patient mis à jour"),
            @ApiResponse(responseCode = "404", description = "Patient introuvable")
    })
    @PutMapping("/{patientUuid}")
    @PreAuthorize(ALL_STAFF)
    public Mono<ResponseEntity<Response>> updatePatient(@Parameter(description = "UUID du patient") @PathVariable String patientUuid,
            @Valid @RequestBody PatientRequest patientRequest, HttpServletRequest request) {

        log.info("Updating patient: {}", patientUuid);

        return patientService.updatePatient(patientUuid, patientRequest).map(patient -> ResponseEntity.ok(
                        getResponse(request, Map.of("patient", patient), "Patient mis à jour avec succès", OK)))
                .switchIfEmpty(Mono.just(ResponseEntity.status(NOT_FOUND)
                        .body(getResponse(request, Map.of(), "Patient introuvable", NOT_FOUND))));
    }

    // DELETE

    @Operation(summary = "Supprimer un patient (soft delete)")
    @DeleteMapping("/{patientUuid}")
    @PreAuthorize(ADMIN_ONLY)
    public Mono<ResponseEntity<Response>> deletePatient(@Parameter(description = "UUID du patient") @PathVariable String patientUuid, HttpServletRequest request) {

        log.info("Deleting patient: {}", patientUuid);

        return patientService.deletePatient(patientUuid).then(Mono.just(ResponseEntity.ok(
                        getResponse(request, Map.of(), "Patient supprimé avec succès", OK))));
    }

    // RESTORE

    @Operation(summary = "Restaurer un patient supprimé")
    @PatchMapping("/{patientUuid}/restore")
    @PreAuthorize("hasAnyAuthority('patient:update', 'ADMIN', 'SUPER_ADMIN')")
    public Mono<ResponseEntity<Response>> restorePatient(@PathVariable String patientUuid, HttpServletRequest request) {

        log.info("Restoring patient: {}", patientUuid);

        return patientService.restorePatient(patientUuid).map(patient -> ResponseEntity.ok(
                        getResponse(request, Map.of("patient", patient), "Patient restauré avec succès", OK)))
                .switchIfEmpty(Mono.just(ResponseEntity.status(NOT_FOUND)
                        .body(getResponse(request, Map.of(), "Patient introuvable", NOT_FOUND))));
    }

    // STATS & UTILS

    @Operation(summary = "Obtenir le nombre total de patients actifs")
    @GetMapping("/stats/count")
    @PreAuthorize(ADMIN_ONLY)
    public Mono<ResponseEntity<Response>> getPatientCount(HttpServletRequest request) {
        return patientService.countActivePatients().map(count -> ResponseEntity.ok(
                        getResponse(request, Map.of("totalPatients", count), "Statistiques récupérées", OK)));
    }

    @Operation(summary = "Vérifier si un utilisateur a un dossier patient")
    @GetMapping("/exists/user/{userUuid}")
    @PreAuthorize(ALL_STAFF)
    public Mono<ResponseEntity<Response>> checkPatientExists(@Parameter(description = "UUID de l'utilisateur") @PathVariable String userUuid, HttpServletRequest request) {

        return patientService.hasPatientRecord(userUuid).map(exists -> ResponseEntity.ok(getResponse(request, Map.of("exists", exists),
                                exists ? "Dossier patient existant" : "Aucun dossier patient", OK)));
    }
}