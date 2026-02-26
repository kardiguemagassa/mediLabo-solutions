package com.openclassrooms.gatewayserverservice.controller;

import com.openclassrooms.gatewayserverservice.domain.Response;
import com.openclassrooms.gatewayserverservice.utils.RequestUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Collections;

/**
 * http://localhost:8080/swagger-ui.html
 */

@Tag(name = "Resilience", description = "Endpoints de secours déclenchés par le Circuit Breaker")
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @Operation(summary = "Fallback Patient", description = "Réponse de secours pour Patient-Service")
    @ApiResponses(value = { @ApiResponse(responseCode = "503", content = @Content(schema = @Schema(implementation = Response.class))) })
    @RequestMapping(value = "/patients")
    public Mono<ResponseEntity<Response>> patientFallback(ServerWebExchange exchange) {
        return buildFallbackResponse(exchange, "Le service Patient est momentanément indisponible.");
    }

    @Operation(summary = "Fallback Notes", description = "Réponse de secours pour Notes-Service")
    @ApiResponses(value = { @ApiResponse(responseCode = "503", content = @Content(schema = @Schema(implementation = Response.class))) })
    @RequestMapping(value = "/notes")
    public Mono<ResponseEntity<Response>> notesFallback(ServerWebExchange exchange) {
        return buildFallbackResponse(exchange, "Le service des Notes est indisponible pour le moment.");
    }

    @Operation(summary = "Fallback Assessments", description = "Réponse de secours pour Assessment-Service")
    @ApiResponses(value = { @ApiResponse(responseCode = "503", content = @Content(schema = @Schema(implementation = Response.class))) })
    @RequestMapping(value = "/assessments")
    public Mono<ResponseEntity<Response>> assessmentFallback(ServerWebExchange exchange) {
        return buildFallbackResponse(exchange, "Le service d'évaluation santé est indisponible.");
    }

    @Operation(summary = "Fallback Notifications", description = "Réponse de secours pour Notification-Service")
    @ApiResponses(value = { @ApiResponse(responseCode = "503", content = @Content(schema = @Schema(implementation = Response.class))) })
    @RequestMapping(value = "/notifications")
    public Mono<ResponseEntity<Response>> notificationFallback(ServerWebExchange exchange) {
        return buildFallbackResponse(exchange, "Le service de notification est indisponible pour le moment.");
    }

    private Mono<ResponseEntity<Response>> buildFallbackResponse(ServerWebExchange exchange, String message) {
        HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
        Response apiResponse = RequestUtils.getResponse(exchange, Collections.emptyMap(), message, status);
        return Mono.just(ResponseEntity.status(status).body(apiResponse));
    }
}