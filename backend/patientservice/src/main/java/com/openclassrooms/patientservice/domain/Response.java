package com.openclassrooms.patientservice.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-05-01
 */

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public record Response(String time, int code, String path, HttpStatus status, String message, String exception, Map<?, ?> data) {}
