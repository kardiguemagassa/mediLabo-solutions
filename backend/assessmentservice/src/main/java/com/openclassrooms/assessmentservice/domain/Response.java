package com.openclassrooms.assessmentservice.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Format de réponse standardisé pour l'API
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-09
 */

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Response(String timestamp, int statusCode, String path, HttpStatus status, String message, String developerMessage, Map<?, ?> data) {

}