package com.openclassrooms.notificationservice.exception;

/**
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-09
 */

public class ApiException extends RuntimeException {
    public ApiException(String message) { super(message); }
}