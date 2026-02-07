package com.openclassrooms.notificationservice.service.implementation;

import com.openclassrooms.notificationservice.exception.ApiException;
import com.openclassrooms.notificationservice.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.openclassrooms.notificationservice.constant.Constant.*;
import static com.openclassrooms.notificationservice.utils.NotificationUtils.*;

/**
 * Implémentation du service d'envoi d'emails pour MediLabo.
 * Utilise Thymeleaf pour les templates HTML.
 *
 * @author Kardigué MAGASSA
 * @version 2.0
 * @since 2026-02-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender emailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.verify.host}")
    private String host;

    @Value("${spring.mail.username}")
    private String fromEmail;


    // AUTHENTIFICATION
    @Override
    @Async
    public void sendAccountVerificationEmail(String name, String email, String token) {
        log.info("Envoi email vérification compte à: {}", email);

        var variables = new HashMap<String, Object>();
        variables.put("name", name);
        variables.put("url", getVerificationUrl(host, token));

        sendEmail(email, SUBJECT_ACCOUNT_VERIFICATION, TEMPLATE_ACCOUNT_VERIFICATION, variables);
    }

    @Override
    @Async
    public void sendPasswordResetEmail(String name, String email, String token) {
        log.info("Envoi email réinitialisation mot de passe à: {}", email);

        var variables = new HashMap<String, Object>();
        variables.put("name", name);
        variables.put("url", getResetPasswordUrl(host, token));

        sendEmail(email, SUBJECT_PASSWORD_RESET, TEMPLATE_PASSWORD_RESET, variables);
    }

    // RENDEZ-VOUS
    @Override
    @Async
    public void sendAppointmentConfirmationEmail(String name, String email, String appointmentDate, String appointmentTime, String doctorName, String department, String location) {
        log.info("Envoi confirmation RDV à: {}", email);

        var variables = new HashMap<String, Object>();
        variables.put("name", name);
        variables.put("appointmentDate", appointmentDate);
        variables.put("appointmentTime", appointmentTime);
        variables.put("doctorName", doctorName);
        variables.put("department", department);
        variables.put("location", location);
        variables.put("url", getPatientDashboardUrl(host));

        sendEmail(email, SUBJECT_APPOINTMENT_CONFIRMATION, TEMPLATE_APPOINTMENT_CONFIRMATION, variables);
    }

    @Override
    @Async
    public void sendAppointmentReminderEmail(String name, String email, String appointmentDate, String appointmentTime, String doctorName, String location) {
        log.info("Envoi rappel RDV à: {}", email);

        var variables = new HashMap<String, Object>();
        variables.put("name", name);
        variables.put("appointmentDate", appointmentDate);
        variables.put("appointmentTime", appointmentTime);
        variables.put("doctorName", doctorName);
        variables.put("location", location);
        variables.put("url", getPatientDashboardUrl(host));

        sendEmail(email, SUBJECT_APPOINTMENT_REMINDER, TEMPLATE_APPOINTMENT_REMINDER, variables);
    }

    // NOTES MÉDICALES
    @Override
    @Async
    public void sendNewMedicalNoteEmail(String name, String email, String patientNumber, String doctorName, String department, String date, String notePreview) {
        log.info("Envoi notification nouvelle note à: {}", email);

        var variables = new HashMap<String, Object>();
        variables.put("name", name);
        variables.put("patientNumber", patientNumber);
        variables.put("doctorName", doctorName);
        variables.put("department", department);
        variables.put("date", date);
        variables.put("notePreview", notePreview);
        variables.put("url", getPatientDashboardUrl(host));

        sendEmail(email, SUBJECT_NEW_NOTE, TEMPLATE_NEW_NOTE, variables);
    }

    @Override
    @Async
    public void sendNewCommentEmail(String name, String email, String recordNumber, String subject, String senderName, String date, String comment) {
        log.info("Envoi notification nouveau commentaire à: {}", email);

        var variables = new HashMap<String, Object>();
        variables.put("name", name);
        variables.put("recordNumber", recordNumber);
        variables.put("subject", subject);
        variables.put("senderName", senderName);
        variables.put("date", date);
        variables.put("message", comment);
        variables.put("url", getMessagesUrl(host));

        sendEmail(email, SUBJECT_NEW_COMMENT, TEMPLATE_NEW_COMMENT, variables);
    }

    @Override
    @Async
    public void sendNewFilesEmail(String name, String email, String recordNumber, String subject, String uploaderName, String date, String files) {
        log.info("Envoi notification nouveaux fichiers à: {}", email);

        var variables = new HashMap<String, Object>();
        variables.put("name", name);
        variables.put("recordNumber", recordNumber);
        variables.put("subject", subject);
        variables.put("uploaderName", uploaderName);
        variables.put("date", date);
        variables.put("files", files != null ? files.split(",") : new String[0]);
        variables.put("url", getPatientDashboardUrl(host));

        sendEmail(email, SUBJECT_NEW_FILES, TEMPLATE_NEW_FILES, variables);
    }

    // RÉSULTATS & ÉVALUATIONS
    @Override
    @Async
    public void sendResultsAvailableEmail(String name, String email, String patientNumber, String analysisType, String laboratory, String sampleDate, String files) {
        log.info("Envoi notification résultats disponibles à: {}", email);

        var variables = new HashMap<String, Object>();
        variables.put("name", name);
        variables.put("patientNumber", patientNumber);
        variables.put("analysisType", analysisType);
        variables.put("laboratory", laboratory);
        variables.put("sampleDate", sampleDate);
        variables.put("files", files != null ? files.split(",") : new String[0]);
        variables.put("url", getResultsUrl(host));

        sendEmail(email, SUBJECT_RESULTS_AVAILABLE, TEMPLATE_RESULTS_AVAILABLE, variables);
    }

    @Override
    @Async
    public void sendRiskAssessmentEmail(String name, String email, String riskLevel, String assessmentDate, Integer triggerCount, Integer patientAge, List<String> triggers, String recommendation) {
        log.info("Envoi notification évaluation risque à: {} - Niveau: {}", email, riskLevel);

        var variables = new HashMap<String, Object>();
        variables.put("name", name);
        variables.put("riskLevel", riskLevel);
        variables.put("assessmentDate", assessmentDate);
        variables.put("triggerCount", triggerCount);
        variables.put("patientAge", patientAge);
        variables.put("triggers", triggers != null ? triggers : List.of());
        variables.put("recommendation", recommendation != null ? recommendation : getDefaultRecommendation(riskLevel));
        variables.put("url", getRiskAssessmentUrl(host));

        sendEmail(email, SUBJECT_RISK_ASSESSMENT, TEMPLATE_RISK_ASSESSMENT, variables);
    }

    // MESSAGES
    @Override
    @Async
    public void sendNewMessageEmail(String name, String email, String senderName, String senderRole, String subject, String messagePreview) {
        log.info("Envoi notification nouveau message à: {}", email);

        var variables = new HashMap<String, Object>();
        variables.put("name", name);
        variables.put("senderName", senderName);
        variables.put("senderRole", formatRole(senderRole));
        variables.put("subject", subject);
        variables.put("messagePreview", truncateMessage(messagePreview, 200));
        variables.put("url", getMessagesUrl(host));

        sendEmail(email, SUBJECT_NEW_MESSAGE + " de " + senderName, TEMPLATE_NEW_MESSAGE, variables);
    }

    // MÉTHODES UTILITAIRES PRIVÉES

    /**
     * Envoie un email avec le template Thymeleaf spécifié.
     */
    private void sendEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        try {
            var context = new Context();
            context.setVariables(variables);

            var htmlContent = templateEngine.process(templateName, context);

            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, UTF_8_ENCODING);

            helper.setPriority(1);
            helper.setSubject(subject);
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setText(htmlContent, true);

            emailSender.send(message);
            log.info("Email envoyé avec succès à: {} - Sujet: {}", to, subject);

        } catch (Exception exception) {
            log.error("Erreur envoi email à {}: {}", to, exception.getMessage(), exception);
            throw new ApiException("Impossible d'envoyer l'e-mail à " + to);
        }
    }

    /**
     * Retourne la recommandation par défaut selon le niveau de risque.
     */
    private String getDefaultRecommendation(String riskLevel) {
        return switch (riskLevel) {
            case "DANGER" -> "Consultation urgente recommandée. Veuillez prendre rendez-vous avec votre médecin dans les plus brefs délais.";
            case "EARLY_ONSET" -> "Une consultation médicale est fortement recommandée pour un suivi approfondi.";
            case "BORDERLINE" -> "Nous vous recommandons de surveiller votre alimentation et de consulter votre médecin lors de votre prochain rendez-vous.";
            default -> "Continuez à maintenir un mode de vie sain. Aucune action immédiate n'est requise.";
        };
    }

    /**
     * Formate le rôle pour l'affichage.
     */
    private String formatRole(String role) {
        if (role == null) return "";
        return switch (role.toUpperCase()) {
            case "DOCTOR", "ROLE_DOCTOR" -> "Médecin";
            case "PATIENT", "ROLE_PATIENT" -> "Patient";
            case "ADMIN", "ROLE_ADMIN" -> "Administrateur";
            default -> role;
        };
    }

    /**
     * Tronque un message à la longueur spécifiée.
     */
    private String truncateMessage(String message, int maxLength) {
        if (message == null) return "";
        if (message.length() <= maxLength) return message;
        return message.substring(0, maxLength) + "...";
    }
}