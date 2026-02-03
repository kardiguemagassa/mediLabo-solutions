package com.openclassrooms.notesservice.exception;

/**
 * Exception personnalisée pour les erreurs métier
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-02
 */

public class ApiException extends RuntimeException {
    public ApiException(String message) {super(message);}
}