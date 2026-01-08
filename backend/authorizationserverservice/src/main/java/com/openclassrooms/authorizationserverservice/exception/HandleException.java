package com.openclassrooms.authorizationserverservice.exception;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.web.bind.annotation.ControllerAdvice;

/**
 * Exception for l'API
 * Configuration KEYS
 * @author Kardigué MAGASSA
 * @version 1.0
 * @email magassakara@gmail.com
 * @since 2026-05-01
 */

@ControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class HandleException implements ErrorController {
}