package com.openclassrooms.assessmentservice.controller;

import com.openclassrooms.assessmentservice.dtoresponse.AssessmentResponse;
import com.openclassrooms.assessmentservice.mapper.AssessmentMapper;
import com.openclassrooms.assessmentservice.service.AssessmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import java.util.concurrent.CompletableFuture;

/**
 * Controller REST pour l'évaluation du risque de diabète.
 * Utilise l'API asynchrone et délègue la gestion des exceptions au HandleException.
 * http://localhost:8083/swagger-ui/index.html#/
 * http://localhost:8083/v3/api-docs
 * http://localhost:8083/v3/api-docs.yaml
 * http://localhost:8083/actuator/health
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-09
 */
@Tag(name = "Assessment", description = "API d'évaluation du risque de diabète")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
@RestController
@RequestMapping("/api/assess")
@RequiredArgsConstructor
public class AssessmentController {

    private final AssessmentService assessmentService;
    private final AssessmentMapper assessmentMapper;

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
    public CompletableFuture<AssessmentResponse> assessDiabetesRisk(@Parameter(description = "UUID du patient", required = true) @PathVariable String patientUuid) {

        log.info("Received diabetes assessment request for patient: {}", patientUuid);

        // Appel asynchrone du service
        return assessmentService.assessDiabetesRisk(patientUuid).thenApply(assessmentMapper::toResponse);
    }
}