package com.openclassrooms.patientservice.exception;

/**
 * Exception Patient Not Found
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-01-09
 */
public class PatientNotFoundException extends RuntimeException {

    public PatientNotFoundException(String message) {
        super(message);
    }

    public PatientNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}