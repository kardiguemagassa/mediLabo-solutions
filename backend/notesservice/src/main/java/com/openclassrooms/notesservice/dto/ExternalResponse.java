package com.openclassrooms.notesservice.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * DTO pour désérialiser les réponses des services externes (PatientService, etc.)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExternalResponse(
        @JsonAlias({"time", "timestamp"}) String timestamp,
        @JsonAlias({"code", "statusCode"}) int statusCode,
        String path,
        String status,
        String message,
        String developerMessage,
        Map<String, Object> data

) {}