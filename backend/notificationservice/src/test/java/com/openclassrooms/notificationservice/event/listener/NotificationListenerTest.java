package com.openclassrooms.notificationservice.event.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassrooms.notificationservice.domain.Notification;
import com.openclassrooms.notificationservice.enumeration.EventType;
import com.openclassrooms.notificationservice.event.Event;
import com.openclassrooms.notificationservice.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour NotificationListener.
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-09
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationListener Tests")
class NotificationListenerTest {

    @Mock
    private EmailService emailService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private NotificationListener notificationListener;

    private Notification notification;

    @BeforeEach
    void setUp() {
        notification = new Notification();
        notification.setHeaders(Map.of("type", "notification"));
    }

    // ÉVÉNEMENTS D'AUTHENTIFICATION

    @Test
    @DisplayName("Devrait traiter USER_CREATED et envoyer email de vérification")
    void shouldHandleUserCreatedEvent() {
        // Given
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Jean Dupont");
        data.put("email", "jean@email.com");
        data.put("token", "verification-token-123");

        Event event = Event.builder()
                .eventType(EventType.USER_CREATED)
                .data(data)
                .build();
        notification.setPayload(event);

        // When
        notificationListener.handleNotification(notification);

        // Then
        verify(emailService).sendAccountVerificationEmail(
                "Jean Dupont",
                "jean@email.com",
                "verification-token-123"
        );
    }

    @Test
    @DisplayName("Devrait traiter PASSWORD_RESET et envoyer email de réinitialisation")
    void shouldHandlePasswordResetEvent() {
        // Given
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Marie Martin");
        data.put("email", "marie@email.com");
        data.put("token", "reset-token-456");

        Event event = Event.builder()
                .eventType(EventType.PASSWORD_RESET)
                .data(data)
                .build();
        notification.setPayload(event);

        // When
        notificationListener.handleNotification(notification);

        // Then
        verify(emailService).sendPasswordResetEmail(
                "Marie Martin",
                "marie@email.com",
                "reset-token-456"
        );
    }

    // ÉVÉNEMENTS PATIENT

    @Test
    @DisplayName("Devrait traiter PATIENT_CREATED et envoyer email de bienvenue patient")
    void shouldHandlePatientCreatedEvent() {
        // Given
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Patient Test");
        data.put("email", "patient@email.com");
        data.put("recordNumber", "P-2026-001");

        Event event = Event.builder()
                .eventType(EventType.PATIENT_CREATED)
                .data(data)
                .build();
        notification.setPayload(event);

        // When
        notificationListener.handleNotification(notification);

        // Then
        verify(emailService).sendWelcomePatientEmail(
                "Patient Test",
                "patient@email.com",
                "P-2026-001"
        );
    }

    @Test
    @DisplayName("Devrait traiter PATIENT_UPDATED et envoyer email de mise à jour patient")
    void shouldHandlePatientUpdatedEvent() {
        // Given
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Patient Test");
        data.put("email", "patient@email.com");
        data.put("recordNumber", "P-2026-001");
        data.put("date", "27 février 2026");

        Event event = Event.builder()
                .eventType(EventType.PATIENT_UPDATED)
                .data(data)
                .build();
        notification.setPayload(event);

        // When
        notificationListener.handleNotification(notification);

        // Then
        verify(emailService).sendPatientUpdatedEmail(
                "Patient Test",
                "patient@email.com",
                "P-2026-001",
                "27 février 2026"
        );
    }

    @Test
    @DisplayName("Devrait traiter PATIENT_DELETED et envoyer email de suppression patient")
    void shouldHandlePatientDeletedEvent() {
        // Given
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Patient Test");
        data.put("email", "patient@email.com");
        data.put("recordNumber", "P-2026-001");
        data.put("date", "27 février 2026");

        Event event = Event.builder()
                .eventType(EventType.PATIENT_DELETED)
                .data(data)
                .build();
        notification.setPayload(event);

        // When
        notificationListener.handleNotification(notification);

        // Then
        verify(emailService).sendPatientDeletedEmail(
                "Patient Test",
                "patient@email.com",
                "P-2026-001",
                "27 février 2026"
        );
    }

    // ÉVÉNEMENTS DE RENDEZ-VOUS

    @Test
    @DisplayName("Devrait traiter APPOINTMENT_CREATED et envoyer email de confirmation rendez-vous")
    void shouldHandleAppointmentCreatedEvent() {
        // Given
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Patient Test");
        data.put("email", "patient@email.com");
        data.put("appointmentDate", "15 mars 2026");
        data.put("appointmentTime", "14:30");
        data.put("doctorName", "Dr. Martin");
        data.put("department", "Cardiologie");
        data.put("location", "Salle 301, Bâtiment A");

        Event event = Event.builder()
                .eventType(EventType.APPOINTMENT_CREATED)
                .data(data)
                .build();
        notification.setPayload(event);

        // When
        notificationListener.handleNotification(notification);

        // Then
        verify(emailService).sendAppointmentConfirmationEmail(
                "Patient Test",
                "patient@email.com",
                "15 mars 2026",
                "14:30",
                "Dr. Martin",
                "Cardiologie",
                "Salle 301, Bâtiment A"
        );
    }

    @Test
    @DisplayName("Devrait traiter APPOINTMENT_REMINDER et envoyer email de rappel rendez-vous")
    void shouldHandleAppointmentReminderEvent() {
        // Given
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Patient Test");
        data.put("email", "patient@email.com");
        data.put("appointmentDate", "15 mars 2026");
        data.put("appointmentTime", "14:30");
        data.put("doctorName", "Dr. Martin");
        data.put("location", "Salle 301, Bâtiment A");

        Event event = Event.builder()
                .eventType(EventType.APPOINTMENT_REMINDER)
                .data(data)
                .build();
        notification.setPayload(event);

        // When
        notificationListener.handleNotification(notification);

        // Then
        verify(emailService).sendAppointmentReminderEmail(
                "Patient Test",
                "patient@email.com",
                "15 mars 2026",
                "14:30",
                "Dr. Martin",
                "Salle 301, Bâtiment A"
        );
    }

    // ÉVÉNEMENTS DE NOTES MÉDICALES

    @Test
    @DisplayName("Devrait traiter NOTE_CREATED et envoyer email de nouvelle note médicale")
    void shouldHandleNoteCreatedEvent() {
        // Given
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Patient Test");
        data.put("email", "patient@email.com");
        data.put("patientNumber", "PAT-001");
        data.put("doctorName", "Dr. Martin");
        data.put("department", "Cardiologie");
        data.put("date", "27 février 2026");
        data.put("notePreview", "Résultats de l'examen cardiaque...");

        Event event = Event.builder()
                .eventType(EventType.NOTE_CREATED)
                .data(data)
                .build();
        notification.setPayload(event);

        // When
        notificationListener.handleNotification(notification);

        // Then
        verify(emailService).sendNewMedicalNoteEmail(
                "Patient Test",
                "patient@email.com",
                "PAT-001",
                "Dr. Martin",
                "Cardiologie",
                "27 février 2026",
                "Résultats de l'examen cardiaque..."
        );
    }

    @Test
    @DisplayName("Devrait traiter NOTE_UPDATED et envoyer email de note médicale mise à jour")
    void shouldHandleNoteUpdatedEvent() {
        // Given
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Patient Test");
        data.put("email", "patient@email.com");
        data.put("patientNumber", "PAT-001");
        data.put("doctorName", "Dr. Martin");
        data.put("date", "27 février 2026");
        data.put("notePreview", "Résultats mis à jour...");

        Event event = Event.builder()
                .eventType(EventType.NOTE_UPDATED)
                .data(data)
                .build();
        notification.setPayload(event);

        // When
        notificationListener.handleNotification(notification);

        // Then
        verify(emailService).sendNoteUpdatedEmail(
                "Patient Test",
                "patient@email.com",
                "PAT-001",
                "Dr. Martin",
                "27 février 2026",
                "Résultats mis à jour..."
        );
    }

    // ÉVÉNEMENTS DE COMMENTAIRES

    @Test
    @DisplayName("Devrait traiter COMMENT_CREATED et envoyer email de nouveau commentaire")
    void shouldHandleCommentCreatedEvent() {
        // Given
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Patient Test");
        data.put("email", "patient@email.com");
        data.put("recordNumber", "PAT-001");
        data.put("subject", "Suivi post-opératoire");
        data.put("senderName", "Dr. Martin");
        data.put("date", "27 février 2026");
        data.put("comment", "Le patient se remet bien de l'opération.");

        Event event = Event.builder()
                .eventType(EventType.COMMENT_CREATED)
                .data(data)
                .build();
        notification.setPayload(event);

        // When
        notificationListener.handleNotification(notification);

        // Then
        verify(emailService).sendNewCommentEmail(
                "Patient Test",
                "patient@email.com",
                "PAT-001",
                "Suivi post-opératoire",
                "Dr. Martin",
                "27 février 2026",
                "Le patient se remet bien de l'opération."
        );
    }

    @Test
    @DisplayName("Devrait traiter COMMENT_UPDATED et envoyer email de commentaire mis à jour")
    void shouldHandleCommentUpdatedEvent() {
        // Given
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Patient Test");
        data.put("email", "patient@email.com");
        data.put("recordNumber", "PAT-001");
        data.put("senderName", "Dr. Martin");
        data.put("date", "27 février 2026");
        data.put("comment", "Commentaire mis à jour...");

        Event event = Event.builder()
                .eventType(EventType.COMMENT_UPDATED)
                .data(data)
                .build();
        notification.setPayload(event);

        // When
        notificationListener.handleNotification(notification);

        // Then
        verify(emailService).sendCommentUpdatedEmail(
                "Patient Test",
                "patient@email.com",
                "PAT-001",
                "Dr. Martin",
                "27 février 2026",
                "Commentaire mis à jour..."
        );
    }

    @Test
    @DisplayName("Devrait traiter COMMENT_DELETED et envoyer email de commentaire supprimé")
    void shouldHandleCommentDeletedEvent() {
        // Given
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Patient Test");
        data.put("email", "patient@email.com");
        data.put("recordNumber", "PAT-001");
        data.put("senderName", "Dr. Martin");
        data.put("date", "27 février 2026");

        Event event = Event.builder()
                .eventType(EventType.COMMENT_DELETED)
                .data(data)
                .build();
        notification.setPayload(event);

        // When
        notificationListener.handleNotification(notification);

        // Then
        verify(emailService).sendCommentDeletedEmail(
                "Patient Test",
                "patient@email.com",
                "PAT-001",
                "Dr. Martin",
                "27 février 2026"
        );
    }

    // ÉVÉNEMENTS D'ÉVALUATION DE RISQUE

    @Test
    @DisplayName("Devrait traiter RISK_ASSESSMENT et envoyer email d'évaluation de risque")
    void shouldHandleRiskAssessmentEvent() {
        // Given
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Patient Test");
        data.put("email", "patient@email.com");
        data.put("riskLevel", "DANGER");
        data.put("assessmentDate", "27 février 2026");
        data.put("triggerCount", 5);
        data.put("patientAge", 45);
        data.put("triggers", List.of("Hémoglobine A1C élevée", "Microalbumine", "Tabagisme"));
        data.put("recommendation", "Consultation urgente recommandée");

        Event event = Event.builder()
                .eventType(EventType.RISK_ASSESSMENT)
                .data(data)
                .build();
        notification.setPayload(event);

        // When
        notificationListener.handleNotification(notification);

        // Then
        verify(emailService).sendRiskAssessmentEmail(
                "Patient Test",
                "patient@email.com",
                "DANGER",
                "27 février 2026",
                5,
                45,
                List.of("Hémoglobine A1C élevée", "Microalbumine", "Tabagisme"),
                "Consultation urgente recommandée"
        );
    }

    @Test
    @DisplayName("Devrait traiter ASSESSMENT_COMPLETED et envoyer email d'évaluation terminée")
    void shouldHandleAssessmentCompletedEvent() {
        // Given
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Patient Test");
        data.put("email", "patient@email.com");
        data.put("riskLevel", "MODERATE");
        data.put("riskLevelDescription", "Risque modéré de diabète");
        data.put("assessedAt", "27 février 2026");
        data.put("triggerCount", 3);
        data.put("age", 45);
        data.put("triggersFound", List.of("Antécédents familiaux", "IMC > 30"));

        Event event = Event.builder()
                .eventType(EventType.ASSESSMENT_COMPLETED)
                .data(data)
                .build();
        notification.setPayload(event);

        // When
        notificationListener.handleNotification(notification);

        // Then
        verify(emailService).sendAssessmentCompletedEmail(
                "Patient Test",
                "patient@email.com",
                "MODERATE",
                "Risque modéré de diabète",
                "27 février 2026",
                3,
                45,
                List.of("Antécédents familiaux", "IMC > 30")
        );
    }

    // ÉVÉNEMENTS DE MESSAGES

    @Test
    @DisplayName("Devrait traiter NEW_MESSAGE et envoyer email de nouveau message")
    void shouldHandleNewMessageEvent() {
        // Given
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Patient Test");
        data.put("email", "patient@email.com");
        data.put("senderName", "Dr. Martin");
        data.put("senderRole", "Médecin traitant");
        data.put("subject", "Question concernant votre traitement");
        data.put("messagePreview", "Bonjour, je souhaitais faire le point sur votre traitement...");

        Event event = Event.builder()
                .eventType(EventType.NEW_MESSAGE)
                .data(data)
                .build();
        notification.setPayload(event);

        // When
        notificationListener.handleNotification(notification);

        // Then
        verify(emailService).sendNewMessageEmail(
                "Patient Test",
                "patient@email.com",
                "Dr. Martin",
                "Médecin traitant",
                "Question concernant votre traitement",
                "Bonjour, je souhaitais faire le point sur votre traitement..."
        );
    }

    @Test
    @DisplayName("Ne devrait pas planter si l'eventType est null")
    void shouldNotCrashWhenEventTypeIsNull() {
        // Given
        Map<String, Object> data = new HashMap<>();
        Event event = Event.builder()
                .eventType(null)
                .data(data)
                .build();
        notification.setPayload(event);

        // When
        notificationListener.handleNotification(notification);

        // Then
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("Ne devrait pas planter si les données sont incomplètes")
    void shouldNotCrashWhenDataIsIncomplete() {
        // Given
        Map<String, Object> data = new HashMap<>();
        Event event = Event.builder()
                .eventType(EventType.USER_CREATED)
                .data(data)
                .build();
        notification.setPayload(event);

        // When
        notificationListener.handleNotification(notification);

        // Then
        // Vérifier que le service est appelé avec des valeurs null
        verify(emailService).sendAccountVerificationEmail(
                null, null, null
        );
    }

    @Test
    @DisplayName("Devrait logger une erreur en cas d'exception de conversion")
    void shouldLogErrorWhenConversionFails() {
        // Given
        // On mock le objectMapper pour qu'il lance une exception
        ObjectMapper mockMapper = mock(ObjectMapper.class);
        NotificationListener listenerWithMockMapper = new NotificationListener(emailService, mockMapper);

        Map<String, Object> data = new HashMap<>();
        data.put("name", "Jean Dupont");

        Event event = Event.builder()
                .eventType(EventType.USER_CREATED)
                .data(data)
                .build();
        notification.setPayload(event);

        when(mockMapper.convertValue(any(), eq(com.openclassrooms.notificationservice.domain.Data.class)))
                .thenThrow(new RuntimeException("Erreur de conversion simulée"));

        // When
        listenerWithMockMapper.handleNotification(notification);

        // Then
        verify(emailService, never()).sendAccountVerificationEmail(any(), any(), any());
    }

    //  TEST POUR LES TYPES D'ÉVÉNEMENTS NON GÉRÉS

    @ParameterizedTest
    @EnumSource(value = EventType.class, mode = EnumSource.Mode.EXCLUDE, names = {
            "USER_CREATED", "PASSWORD_RESET", "PATIENT_CREATED", "PATIENT_UPDATED",
            "PATIENT_DELETED", "APPOINTMENT_CREATED", "APPOINTMENT_REMINDER",
            "NOTE_CREATED", "NOTE_UPDATED", "COMMENT_CREATED", "COMMENT_UPDATED",
            "COMMENT_DELETED", "FILE_UPLOADED", "FILE_DELETED", "RESULTS_AVAILABLE",
            "RISK_ASSESSMENT", "ASSESSMENT_COMPLETED", "NEW_MESSAGE"
    })
    @DisplayName("Devrait logger un avertissement pour les types d'événements non gérés")
    void shouldLogWarningForUnhandledEventTypes(EventType unhandledType) {
        // Given
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Test");
        data.put("email", "test@email.com");

        Event event = Event.builder()
                .eventType(unhandledType)
                .data(data)
                .build();
        notification.setPayload(event);

        // When
        notificationListener.handleNotification(notification);

        // Then - Aucun appel à emailService
        verifyNoInteractions(emailService);
    }

    // TESTS DE VÉRIFICATION DES PARAMÈTRES

    @Test
    @DisplayName("Devrait appeler sendNewMedicalNoteEmail avec les bons paramètres")
    void shouldCallSendNewMedicalNoteEmailWithCorrectParameters() {
        // Given
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Patient Nom");
        data.put("email", "patient@email.com");
        data.put("patientNumber", "NUM-123");
        data.put("doctorName", "Dr Docteur");
        data.put("department", "Service");
        data.put("date", "2026-02-27");
        data.put("notePreview", "Aperçu note");

        Event event = Event.builder()
                .eventType(EventType.NOTE_CREATED)
                .data(data)
                .build();
        notification.setPayload(event);

        // When
        notificationListener.handleNotification(notification);

        // Then
        verify(emailService).sendNewMedicalNoteEmail(
                "Patient Nom",
                "patient@email.com",
                "NUM-123",
                "Dr Docteur",
                "Service",
                "2026-02-27",
                "Aperçu note"
        );
    }

    @Test
    @DisplayName("Devrait appeler sendRiskAssessmentEmail avec les bons paramètres")
    void shouldCallSendRiskAssessmentEmailWithCorrectParameters() {
        // Given
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Patient Nom");
        data.put("email", "patient@email.com");
        data.put("riskLevel", "ELEVÉ");
        data.put("assessmentDate", "2026-02-27");
        data.put("triggerCount", 3);
        data.put("patientAge", 50);
        data.put("triggers", List.of("Trigger1", "Trigger2"));
        data.put("recommendation", "Recommandation test");

        Event event = Event.builder()
                .eventType(EventType.RISK_ASSESSMENT)
                .data(data)
                .build();
        notification.setPayload(event);

        // When
        notificationListener.handleNotification(notification);

        // Then
        verify(emailService).sendRiskAssessmentEmail(
                "Patient Nom",
                "patient@email.com",
                "ELEVÉ",
                "2026-02-27",
                3,
                50,
                List.of("Trigger1", "Trigger2"),
                "Recommandation test"
        );
    }
}