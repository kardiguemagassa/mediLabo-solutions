package com.openclassrooms.notificationservice.enumeration;

/**
 * Types d'événements pour les notifications MediLabo.
 *
 * @author Kardigué MAGASSA
 * @version 2.1
 * @since 2026-02-09
 */
public enum EventType {

    USER_CREATED,
    PASSWORD_RESET,

    PATIENT_CREATED,
    PATIENT_UPDATED,
    PATIENT_DELETED,

    APPOINTMENT_CREATED,
    APPOINTMENT_REMINDER,
    APPOINTMENT_CANCELLED,

    NOTE_CREATED,
    NOTE_UPDATED,
    COMMENT_CREATED,
    COMMENT_UPDATED,
    COMMENT_DELETED,
    FILE_DELETED,
    FILE_UPLOADED,

    RESULTS_AVAILABLE,
    RISK_ASSESSMENT,
    ASSESSMENT_COMPLETED,

    NEW_MESSAGE

}