package com.openclassrooms.patientservice.exception;

/**
 * Exception générique API
 *
 * @author FirstName LastName
 * @version 1.0
 * @since 2026-01-09
 */
public class ApiException extends RuntimeException {
    public ApiException(String message) {
        super(message);
    }
}