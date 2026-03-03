package com.openclassrooms.assessmentservice.controller;

import com.openclassrooms.assessmentservice.domain.Response;
import com.openclassrooms.assessmentservice.mapper.AssessmentMapper;
import com.openclassrooms.assessmentservice.service.AssessmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

import static com.openclassrooms.assessmentservice.util.RequestUtils.getResponse;
import static org.springframework.http.HttpStatus.OK;

/**
 * Controller REST réactif pour l'évaluation du risque de diabète.
 * ARCHITECTURE RÉACTIVE:
 * - Retourne Mono<ResponseEntity<Response>>
 * - Le JWT est propagé automatiquement via WebClientInterceptor
 *
 * @author Kardigué MAGASSA
 * @version 2.0
 * @since 2026-02-25
 */
@Tag(name = "Assessment", description = "API d'évaluation du risque de diabète")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
@RestController
@RequestMapping("/api/assessments")
@RequiredArgsConstructor
public class AssessmentController {

    private final AssessmentService assessmentService;
    private final AssessmentMapper assessmentMapper;

    /**
     * Évalue le risque de diabète pour un patient.
     * FLUX:
     * 1. Réception de la requête avec JWT dans le header, Capturer le token AVANT d'entrer dans le contexte réactif
     * 2. Spring Security extrait le JWT et le met dans SecurityContext
     * 3. AssessmentService appelle PatientService et NotesService
     * 4. WebClientInterceptor propage automatiquement le JWT à chaque appel
     * 5. Résultat retourné au client
     */

    @Operation(
            summary = "Évaluer le risque de diabète",
            description = "Calcule le niveau de risque de diabète pour un patient " +
                    "basé sur son âge, son genre et les termes déclencheurs dans ses notes médicales"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Évaluation réussie"),
            @ApiResponse(responseCode = "404", description = "Patient non trouvé"),
            @ApiResponse(responseCode = "503", description = "Service externe indisponible")
    })
    @GetMapping("/diabetes/{patientUuid}")
    @PreAuthorize("isAuthenticated()")
    public Mono<ResponseEntity<Response>> assessDiabetesRisk(@Parameter(description = "UUID du patient", required = true) @PathVariable String patientUuid, HttpServletRequest request) {

        log.info("Received diabetes assessment request for patient: {}", patientUuid);

        String authHeader = request.getHeader("Authorization");
        String token = (authHeader != null && authHeader.startsWith("Bearer ")) ? authHeader.substring(7) : null;

        return assessmentService.assessDiabetesRisk(patientUuid, token)
                .map(assessmentMapper::toResponse)
                .map(assessmentResponse -> ResponseEntity.ok(getResponse(request,
                        Map.of("assessment", assessmentResponse),
                        "Évaluation du risque effectuée avec succès", OK)))
                .doOnError(error -> log.error("Assessment failed for patient {}: {}", patientUuid, error.getMessage()));
    }
}