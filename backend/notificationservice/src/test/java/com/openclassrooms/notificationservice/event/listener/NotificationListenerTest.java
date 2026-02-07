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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        verify(emailService).sendAccountVerificationEmail("Jean Dupont", "jean@email.com", "verification-token-123");
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
        verify(emailService).sendPasswordResetEmail("Marie Martin", "marie@email.com", "reset-token-456");
    }

    @Test
    @DisplayName("Devrait traiter RISK_ASSESSMENT et envoyer email d'évaluation")
    void shouldHandleRiskAssessmentEvent() {
        // Given
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Patient Test");
        data.put("email", "patient@email.com");
        data.put("riskLevel", "DANGER");
        data.put("assessmentDate", "09 février 2026");
        data.put("triggerCount", 5);
        data.put("patientAge", 45);
        data.put("triggers", List.of("Hémoglobine A1C", "Microalbumine"));
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
                eq("Patient Test"),
                eq("patient@email.com"),
                eq("DANGER"),
                eq("09 février 2026"),
                eq(5),
                eq(45),
                anyList(),
                eq("Consultation urgente recommandée")
        );
    }

    @Test
    @DisplayName("Devrait traiter NOTE_CREATED et envoyer email de nouvelle note")
    void shouldHandleNoteCreatedEvent() {
        // Given
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Patient Test");
        data.put("email", "patient@email.com");
        data.put("patientNumber", "PAT-001");
        data.put("doctorName", "Dr. Martin");
        data.put("department", "Cardiologie");
        data.put("date", "09 février 2026");
        data.put("notePreview", "Résultats de l'examen...");

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
                "09 février 2026",
                "Résultats de l'examen..."
        );
    }
}