package com.openclassrooms.notificationservice.service;

import java.util.List;

/**
 * Service d'envoi d'emails pour MediLabo.
 *
 * @author Kardigué MAGASSA
 * @version 2.1
 * @since 2026-02-09
 */
public interface EmailService {

    /** AUTHENTIFICATION */
    void sendAccountVerificationEmail(String name, String email, String token);
    void sendPasswordResetEmail(String name, String email, String token);

    /**  PATIENTS */
    void sendWelcomePatientEmail(String name, String email, String recordNumber);
    void sendPatientUpdatedEmail(String name, String email, String recordNumber, String date);
    void sendPatientDeletedEmail(String name, String email, String recordNumber, String date);

    /** RENDEZ-VOUS */
    void sendAppointmentConfirmationEmail(String name, String email, String appointmentDate, String appointmentTime, String doctorName, String department, String location);
    void sendAppointmentReminderEmail(String name, String email, String appointmentDate, String appointmentTime, String doctorName, String location);

    void sendNewMedicalNoteEmail(String name, String email, String patientNumber, String doctorName, String department, String date, String notePreview);
    void sendNoteUpdatedEmail(String name, String email, String patientNumber, String doctorName, String date, String notePreview);  // Nouveau
    void sendNewCommentEmail(String name, String email, String recordNumber, String subject, String senderName, String date, String comment);
    void sendCommentUpdatedEmail(String name, String email, String recordNumber, String senderName, String date, String comment);  // Nouveau
    void sendCommentDeletedEmail(String name, String email, String recordNumber, String senderName, String date);  // Nouveau
    void sendNewFilesEmail(String name, String email, String recordNumber, String subject, String uploaderName, String date, String files);
    void sendFileDeletedEmail(String name, String email, String recordNumber, String uploaderName, String date, String fileName);  // Nouveau

    /** RÉSULTATS & ÉVALUATIONS */
    void sendResultsAvailableEmail(String name, String email, String patientNumber, String analysisType, String laboratory, String sampleDate, String files);
    void sendRiskAssessmentEmail(String name, String email, String riskLevel, String assessmentDate, Integer triggerCount, Integer patientAge, List<String> triggers, String recommendation);
    void sendAssessmentCompletedEmail(String name, String email, String riskLevel, String riskLevelDescription, String assessedAt, Integer triggerCount, Integer age, List<String> triggersFound);

    /** MESSAGES */
    void sendNewMessageEmail(String name, String email, String senderName, String senderRole, String subject, String messagePreview);
}