package com.openclassrooms.notificationservice.event.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassrooms.notificationservice.domain.Data;
import com.openclassrooms.notificationservice.domain.Notification;
import com.openclassrooms.notificationservice.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

/**
 * Listener Kafka pour les notifications MediLabo.
 * Écoute les événements et déclenche l'envoi des emails appropriés.
 *
 * @author Kardigué MAGASSA
 * @version 2.0
 * @since 2026-02-09
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationListener {

    private static final String MEDILABO_NOTIFICATIONS = "MEDILABO_NOTIFICATIONS";

    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = MEDILABO_NOTIFICATIONS, groupId = "${spring.kafka.consumer.group-id:notification-group}")
    public void handleNotification(Notification notification) {
        log.info("Notification reçue: type={}", notification.getPayload().getEventType());

        try {
            objectMapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
            var data = objectMapper.convertValue(notification.getPayload().getData(), Data.class);

            processNotification(notification, data);

        } catch (Exception e) {
            log.error("Erreur traitement notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Traite la notification selon son type.
     */
    private void processNotification(Notification notification, Data data) {
        switch (notification.getPayload().getEventType()) {

            case USER_CREATED -> {
                log.info("Traitement USER_CREATED pour: {}", data.getEmail());
                emailService.sendAccountVerificationEmail(
                        data.getName(),
                        data.getEmail(),
                        data.getToken()
                );
            }

            case PASSWORD_RESET -> {
                log.info("Traitement PASSWORD_RESET pour: {}", data.getEmail());
                emailService.sendPasswordResetEmail(
                        data.getName(),
                        data.getEmail(),
                        data.getToken()
                );
            }

            case PATIENT_CREATED -> {
                log.info("Traitement PATIENT_CREATED pour: {}", data.getEmail());
                emailService.sendWelcomePatientEmail(
                        data.getName(),
                        data.getEmail(),
                        data.getRecordNumber()
                );
            }

            case PATIENT_UPDATED -> {
                log.info("Traitement PATIENT_UPDATED pour: {}", data.getEmail());
                emailService.sendPatientUpdatedEmail(
                        data.getName(),
                        data.getEmail(),
                        data.getRecordNumber(),
                        data.getDate()
                );
            }

            case PATIENT_DELETED -> {
                log.info("Traitement PATIENT_DELETED pour: {}", data.getEmail());
                emailService.sendPatientDeletedEmail(
                        data.getName(),
                        data.getEmail(),
                        data.getRecordNumber(),
                        data.getDate()
                );
            }

            case APPOINTMENT_CREATED -> {
                log.info("Traitement APPOINTMENT_CREATED pour: {}", data.getEmail());
                emailService.sendAppointmentConfirmationEmail(
                        data.getName(),
                        data.getEmail(),
                        data.getAppointmentDate(),
                        data.getAppointmentTime(),
                        data.getDoctorName(),
                        data.getDepartment(),
                        data.getLocation()
                );
            }

            case APPOINTMENT_REMINDER -> {
                log.info("Traitement APPOINTMENT_REMINDER pour: {}", data.getEmail());
                emailService.sendAppointmentReminderEmail(
                        data.getName(),
                        data.getEmail(),
                        data.getAppointmentDate(),
                        data.getAppointmentTime(),
                        data.getDoctorName(),
                        data.getLocation()
                );
            }

            case NOTE_CREATED -> {
                log.info("Traitement NOTE_CREATED pour: {}", data.getEmail());
                emailService.sendNewMedicalNoteEmail(
                        data.getName(),
                        data.getEmail(),
                        data.getPatientNumber(),
                        data.getDoctorName(),
                        data.getDepartment(),
                        data.getDate(),
                        data.getNotePreview()
                );
            }

            case NOTE_UPDATED -> {
                log.info("Traitement NOTE_UPDATED pour: {}", data.getEmail());
                emailService.sendNoteUpdatedEmail(
                        data.getName(),
                        data.getEmail(),
                        data.getPatientNumber(),
                        data.getDoctorName(),
                        data.getDate(),
                        data.getNotePreview()
                );
            }

            case COMMENT_UPDATED -> {
                log.info("Traitement COMMENT_UPDATED pour: {}", data.getEmail());
                emailService.sendCommentUpdatedEmail(
                        data.getName(),
                        data.getEmail(),
                        data.getRecordNumber(),
                        data.getSenderName(),
                        data.getDate(),
                        data.getComment()
                );
            }

            case COMMENT_DELETED -> {
                log.info("Traitement COMMENT_DELETED pour: {}", data.getEmail());
                emailService.sendCommentDeletedEmail(
                        data.getName(),
                        data.getEmail(),
                        data.getRecordNumber(),
                        data.getSenderName(),
                        data.getDate()
                );
            }

            case FILE_DELETED -> {
                log.info("Traitement FILE_DELETED pour: {}", data.getEmail());
                emailService.sendFileDeletedEmail(
                        data.getName(),
                        data.getEmail(),
                        data.getRecordNumber(),
                        data.getUploaderName(),
                        data.getDate(),
                        data.getFiles()
                );
            }

            case COMMENT_CREATED -> {
                log.info("Traitement NOTE_COMMENT_ADDED pour: {}", data.getEmail());
                emailService.sendNewCommentEmail(
                        data.getName(),
                        data.getEmail(),
                        data.getRecordNumber(),
                        data.getSubject(),
                        data.getSenderName(),
                        data.getDate(),
                        data.getComment()
                );
            }

            case FILE_UPLOADED -> {
                log.info("Traitement NOTE_FILE_UPLOADED pour: {}", data.getEmail());
                emailService.sendNewFilesEmail(
                        data.getName(),
                        data.getEmail(),
                        data.getRecordNumber(),
                        data.getSubject(),
                        data.getUploaderName(),
                        data.getDate(),
                        data.getFiles()
                );
            }

            case RESULTS_AVAILABLE -> {
                log.info("Traitement RESULTS_AVAILABLE pour: {}", data.getEmail());
                emailService.sendResultsAvailableEmail(
                        data.getName(),
                        data.getEmail(),
                        data.getPatientNumber(),
                        data.getAnalysisType(),
                        data.getLaboratory(),
                        data.getSampleDate(),
                        data.getFiles()
                );
            }

            case RISK_ASSESSMENT -> {
                log.info("Traitement RISK_ASSESSMENT pour: {} - Niveau: {}", data.getEmail(), data.getRiskLevel());
                emailService.sendRiskAssessmentEmail(
                        data.getName(),
                        data.getEmail(),
                        data.getRiskLevel(),
                        data.getAssessmentDate(),
                        data.getTriggerCount(),
                        data.getPatientAge(),
                        data.getTriggers(),
                        data.getRecommendation()
                );
            }

            case ASSESSMENT_COMPLETED -> {
                log.info("Traitement ASSESSMENT_COMPLETED pour: {} - Niveau: {}", data.getEmail(), data.getRiskLevel());
                emailService.sendAssessmentCompletedEmail(
                        data.getName(),
                        data.getEmail(),
                        data.getRiskLevel(),
                        data.getRiskLevelDescription(),
                        data.getAssessedAt(),
                        data.getTriggerCount(),
                        data.getAge(),
                        data.getTriggersFound()
                );
            }

            case NEW_MESSAGE -> {
                log.info("Traitement NEW_MESSAGE pour: {}", data.getEmail());
                emailService.sendNewMessageEmail(
                        data.getName(),
                        data.getEmail(),
                        data.getSenderName(),
                        data.getSenderRole(),
                        data.getSubject(),
                        data.getMessagePreview()
                );
            }

            default -> log.warn("Type d'événement non géré: {}", notification.getPayload().getEventType());
        }
    }
}