package com.openclassrooms.authorizationserverservice.exception;

/**
 * Exception for l'API
 * Configuration KEYS
 * @author Kardigué MAGASSA
 * @version 1.0
 * @email magassakara@gmail.com
 * @since 2026-05-01
 */
public class ApiException extends RuntimeException {
    public ApiException(String message){ super(message); }
}
