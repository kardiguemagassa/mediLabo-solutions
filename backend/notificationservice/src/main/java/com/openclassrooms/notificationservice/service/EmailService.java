package com.openclassrooms.notificationservice.service;

import java.util.List;

/**
 * Service d'envoi d'emails pour MediLabo.
 *
 * @author Kardigué MAGASSA
 * @version 2.0
 * @since 2026-02-09
 */
public interface EmailService {

    // AUTHENTIFICATION
    void sendAccountVerificationEmail(String name, String email, String token);
    void sendPasswordResetEmail(String name, String email, String token);
    // RENDEZ-VOUS
    void sendAppointmentConfirmationEmail(String name, String email, String appointmentDate, String appointmentTime, String doctorName, String department, String location);
    void sendAppointmentReminderEmail(String name, String email, String appointmentDate, String appointmentTime, String doctorName, String location);
    // NOTES MÉDICALES
    void sendNewMedicalNoteEmail(String name, String email, String patientNumber, String doctorName, String department, String date, String notePreview);
    void sendNewCommentEmail(String name, String email, String recordNumber, String subject, String senderName, String date, String comment);
    void sendNewFilesEmail(String name, String email, String recordNumber, String subject, String uploaderName, String date, String files);
    // RÉSULTATS & ÉVALUATIONS
    void sendResultsAvailableEmail(String name, String email, String patientNumber, String analysisType, String laboratory, String sampleDate, String files);
    void sendRiskAssessmentEmail(String name, String email, String riskLevel, String assessmentDate, Integer triggerCount, Integer patientAge, List<String> triggers, String recommendation);
    // MESSAGES
    void sendNewMessageEmail(String name, String email, String senderName, String senderRole, String subject, String messagePreview);
}