package com.openclassrooms.userservice.exception;

/**
 * Exception for l'API
 * Configuration KEYS
 * @author FirstName LastName
 * @version 1.0
 * @email magassa***REMOVED_USER***@gmail.com
 * @since 2026-05-01
 */
public class ApiException extends RuntimeException {
    public ApiException(String message){ super(message); }
}
