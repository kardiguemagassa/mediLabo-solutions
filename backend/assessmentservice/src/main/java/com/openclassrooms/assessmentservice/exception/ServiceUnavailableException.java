package com.openclassrooms.assessmentservice.exception;

/**
 * Exception levée quand un service externe est indisponible.
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-09
 */
public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String message) {
        super(message);
    }

}