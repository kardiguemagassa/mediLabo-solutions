package com.openclassrooms.gatewayserverservice.domain;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Réponse standard pour les erreurs du Gateway.
 * @author Kardigué MAGASSA
 * @version 1.0
 * @email magassakara@gmail.com
 * @since 2026-05-01
 */

public record Response(
        String time,
        int code,
        String path,
        HttpStatus status,
        String message,
        String exception,
        Map<?, ?> data
) {}
