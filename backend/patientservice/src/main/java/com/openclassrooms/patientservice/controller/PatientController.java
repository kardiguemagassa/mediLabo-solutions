package com.openclassrooms.patientservice.controller;

import com.openclassrooms.patientservice.domain.Response;
import com.openclassrooms.patientservice.dtorequest.PatientRequest;
import com.openclassrooms.patientservice.model.Patient;
import com.openclassrooms.patientservice.service.PatientService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.net.URI;

import static com.openclassrooms.patientservice.util.RequestUtils.getResponse;
import static java.util.Collections.emptyMap;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.ResponseEntity.created;
import static org.springframework.http.ResponseEntity.ok;


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
    public ResponseEntity<Response> register(@RequestBody PatientRequest patient, HttpServletRequest request) {
        patientService.createPatient((PatientRequest) request);

        return created(getUri()).body(getResponse(request, emptyMap(), "Compte créé avec succès. Veuillez consulter votre boîte email pour activer votre compte.", CREATED));
    }


    /**
     * Récupérer un patient par UUID
     * Accessible par: ORGANIZER, ADMIN, PRACTITIONER
     */
    @GetMapping("/{patientUuid}")
    @PreAuthorize("hasAnyAuthority('ORGANIZER', 'ADMIN', 'PRACTITIONER')")
    public ResponseEntity<Response> getPatientByUuid(@PathVariable String patientUuid, HttpServletRequest request) {

        log.debug("Fetching patient: {}", patientUuid);

         patientService.getPatientByUuid(patientUuid);

        return ok(getResponse(request, emptyMap(), "un patient par UUID", OK));
    }

    /**
     * Récupérer un patient par user UUID
     * Accessible par: ORGANIZER, ADMIN, PRACTITIONER
     */
    @GetMapping("/user/{userUuid}")
    @PreAuthorize("hasAnyAuthority('ORGANIZER', 'ADMIN', 'PRACTITIONER')")
    public ResponseEntity<Response> getPatientByUserUuid(@PathVariable String userUuid, HttpServletRequest request) {

        log.debug("Fetching patient for user: {}", userUuid);

        patientService.getPatientByUserUuid(userUuid);

        return ok(getResponse(request, emptyMap(), "patient par user UUID",OK));
    }

    /**
     * Récupérer son propre dossier patient
     * Accessible par: USER (authentifié)
     */
    @GetMapping("/me")
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<Response> getPatient(Authentication authentication, HttpServletRequest request) {

        String userUuid = authentication.getName();
        log.debug("Fetching own patient record for user: {}", userUuid);

        patientService.getPatientByUserUuid(userUuid);

        return ok(getResponse(request, emptyMap(), "patient par user UUID",OK));
    }

    /**
     * Récupérer tous les patients
     * Accessible par: ORGANIZER, ADMIN, PRACTITIONER
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ORGANIZER', 'ADMIN', 'PRACTITIONER')")
    public ResponseEntity<Response> getAllPatients(HttpServletRequest request) {

        log.debug("Fetching all patients");

        patientService.getAllPatients();

        return ok(getResponse(request, emptyMap(), "Récupérer tous les patients",OK));
    }

    /**
     * Mettre à jour un patient
     * Accessible par: ORGANIZER, ADMIN, PRACTITIONER
     */
    @PutMapping("/{patientUuid}")
    @PreAuthorize("hasAnyAuthority('ORGANIZER', 'ADMIN', 'PRACTITIONER')")
    public ResponseEntity<Response> updatePatient(@PathVariable String patientUuid, @Valid @RequestBody Patient request) {

        log.info("Updating patient: {}", patientUuid);

        patientService.updatePatient(patientUuid, request);

        return ok(getResponse((HttpServletRequest) request, emptyMap(),"Patient updated successfully", OK));
    }

    /**
     * Supprimer un patient (soft delete)
     * Accessible par: ORGANIZER, ADMIN
     */
    @DeleteMapping("/{patientUuid}")
    @PreAuthorize("hasAnyAuthority('ORGANIZER', 'ADMIN')")
    public ResponseEntity<Response> deletePatient(@PathVariable String patientUuid, HttpServletRequest request) {

        log.info("Deleting patient: {}", patientUuid);

        patientService.deletePatient(patientUuid);

        return ok(getResponse(request, emptyMap(),"Patient deleted successfully", NO_CONTENT));
    }

    private URI getUri() {
        return URI.create("/api/patients/profile/userId");
    }
}