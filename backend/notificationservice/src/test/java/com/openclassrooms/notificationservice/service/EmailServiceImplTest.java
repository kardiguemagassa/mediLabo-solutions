package com.openclassrooms.notificationservice.service;

import com.openclassrooms.notificationservice.service.implementation.EmailServiceImpl;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour EmailServiceImpl.
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-09
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmailServiceImpl Tests")
class EmailServiceImplTest {

    @Mock
    private JavaMailSender emailSender;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailServiceImpl emailService;

    @Captor
    private ArgumentCaptor<Context> contextCaptor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "host", "http://localhost:4200");
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@medilabo.fr");

        when(emailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>Test</html>");
    }

    @Nested
    @DisplayName("sendAccountVerificationEmail Tests")
    class AccountVerificationTests {

        @Test
        @DisplayName("Devrait envoyer un email de vérification de compte")
        void shouldSendAccountVerificationEmail() {
            // When
            emailService.sendAccountVerificationEmail("Jean Dupont", "jean@email.com", "token-123");

            // Then
            verify(templateEngine).process(eq("account-verification"), contextCaptor.capture());
            verify(emailSender).send(mimeMessage);

            Context context = contextCaptor.getValue();
            assertThat(context.getVariable("name")).isEqualTo("Jean Dupont");
            assertThat(context.getVariable("url").toString()).contains("token-123");
        }
    }

    @Nested
    @DisplayName("sendPasswordResetEmail Tests")
    class PasswordResetTests {

        @Test
        @DisplayName("Devrait envoyer un email de réinitialisation de mot de passe")
        void shouldSendPasswordResetEmail() {
            // When
            emailService.sendPasswordResetEmail("Marie Martin", "marie@email.com", "reset-token-456");

            // Then
            verify(templateEngine).process(eq("password-reset"), contextCaptor.capture());
            verify(emailSender).send(mimeMessage);

            Context context = contextCaptor.getValue();
            assertThat(context.getVariable("name")).isEqualTo("Marie Martin");
            assertThat(context.getVariable("url").toString()).contains("reset-token-456");
        }
    }

    @Nested
    @DisplayName("sendAppointmentConfirmationEmail Tests")
    class AppointmentConfirmationTests {

        @Test
        @DisplayName("Devrait envoyer un email de confirmation de rendez-vous")
        void shouldSendAppointmentConfirmationEmail() {
            // When
            emailService.sendAppointmentConfirmationEmail(
                    "Jean Dupont",
                    "jean@email.com",
                    "15 février 2026",
                    "14h30",
                    "Dr. Martin",
                    "Cardiologie",
                    "Bâtiment A, 2ème étage"
            );

            // Then
            verify(templateEngine).process(eq("appointment-confirmation"), contextCaptor.capture());
            verify(emailSender).send(mimeMessage);

            Context context = contextCaptor.getValue();
            assertThat(context.getVariable("name")).isEqualTo("Jean Dupont");
            assertThat(context.getVariable("appointmentDate")).isEqualTo("15 février 2026");
            assertThat(context.getVariable("appointmentTime")).isEqualTo("14h30");
            assertThat(context.getVariable("doctorName")).isEqualTo("Dr. Martin");
            assertThat(context.getVariable("department")).isEqualTo("Cardiologie");
            assertThat(context.getVariable("location")).isEqualTo("Bâtiment A, 2ème étage");
        }
    }

    @Nested
    @DisplayName("sendRiskAssessmentEmail Tests")
    class RiskAssessmentTests {

        @Test
        @DisplayName("Devrait envoyer un email d'évaluation de risque DANGER")
        void shouldSendDangerRiskAssessmentEmail() {
            // When
            emailService.sendRiskAssessmentEmail(
                    "Patient Test",
                    "patient@email.com",
                    "DANGER",
                    "09 février 2026",
                    5,
                    45,
                    List.of("Hémoglobine A1C", "Microalbumine", "Taille", "Poids", "Fumeur"),
                    null
            );

            // Then
            verify(templateEngine).process(eq("risk-assessment"), contextCaptor.capture());
            verify(emailSender).send(mimeMessage);

            Context context = contextCaptor.getValue();
            assertThat(context.getVariable("riskLevel")).isEqualTo("DANGER");
            assertThat(context.getVariable("triggerCount")).isEqualTo(5);
            assertThat(context.getVariable("patientAge")).isEqualTo(45);
            assertThat((List<?>) context.getVariable("triggers")).hasSize(5);
            assertThat(context.getVariable("recommendation").toString()).contains("urgente");
        }

        @Test
        @DisplayName("Devrait envoyer un email d'évaluation de risque NONE")
        void shouldSendNoneRiskAssessmentEmail() {
            // When
            emailService.sendRiskAssessmentEmail(
                    "Patient Test",
                    "patient@email.com",
                    "NONE",
                    "09 février 2026",
                    0,
                    30,
                    List.of(),
                    null
            );

            // Then
            verify(templateEngine).process(eq("risk-assessment"), contextCaptor.capture());

            Context context = contextCaptor.getValue();
            assertThat(context.getVariable("riskLevel")).isEqualTo("NONE");
            assertThat(context.getVariable("recommendation").toString()).contains("mode de vie sain");
        }
    }
}