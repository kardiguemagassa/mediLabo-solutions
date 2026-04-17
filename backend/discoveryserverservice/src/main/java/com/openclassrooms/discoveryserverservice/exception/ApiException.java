package com.openclassrooms.discoveryserverservice.exception;

/**
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-05-01
 */

public class ApiException extends RuntimeException {
    public ApiException(String message) {
        super(message);
    }
}
