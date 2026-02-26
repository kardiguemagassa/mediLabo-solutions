package com.openclassrooms.notificationservice.enumeration;

/**
 * Types d'événements pour les notifications MediLabo.
 *
 * @author Kardigué MAGASSA
 * @version 2.1
 * @since 2026-02-09
 */
public enum EventType {

    // AUTHENTIFICATION
    USER_CREATED,
    PASSWORD_RESET,
    // PATIENTS
    PATIENT_CREATED,
    PATIENT_UPDATED,
    PATIENT_DELETED,
    // RENDEZ-VOUS
    APPOINTMENT_CREATED,
    APPOINTMENT_REMINDER,
    APPOINTMENT_CANCELLED,
    // NOTES MÉDICALES
    NOTE_CREATED,
    NOTE_UPDATED,
    COMMENT_CREATED,
    COMMENT_UPDATED,
    COMMENT_DELETED,
    FILE_DELETED,
    FILE_UPLOADED,
    // RÉSULTATS & ÉVALUATIONS
    RESULTS_AVAILABLE,
    RISK_ASSESSMENT,
    ASSESSMENT_COMPLETED,  // Nouveau
    // MESSAGES
    NEW_MESSAGE

}