package com.openclassrooms.notificationservice.service;

import com.openclassrooms.notificationservice.exception.ApiException;
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
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour EmailServiceImpl.
 * Couvre tous les types d'emails envoyés par le système MediLabo.
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

    private static final String TEST_HOST = "http://localhost:4200";
    private static final String TEST_FROM_EMAIL = "noreply@medilabo.fr";
    private static final String TEST_EMAIL = "test@medilabo.fr";
    private static final String TEST_NAME = "Jean Dupont";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "host", TEST_HOST);
        ReflectionTestUtils.setField(emailService, "fromEmail", TEST_FROM_EMAIL);

        when(emailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>Test Email</html>");
    }

    // AUTHENTIFICATION

    @Nested
    @DisplayName("sendAccountVerificationEmail Tests")
    class AccountVerificationTests {

        @Test
        @DisplayName("Devrait envoyer un email de vérification de compte")
        void shouldSendAccountVerificationEmail() {
            // When
            emailService.sendAccountVerificationEmail(TEST_NAME, TEST_EMAIL, "token-123");

            // Then
            verify(templateEngine).process(eq("account-verification"), contextCaptor.capture());
            verify(emailSender).send(mimeMessage);

            Context context = contextCaptor.getValue();
            assertThat(context.getVariable("name")).isEqualTo(TEST_NAME);
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

    // PATIENTS

    @Nested
    @DisplayName("Patient Email Tests")
    class PatientEmailTests {

        @Test
        @DisplayName("Devrait envoyer un email de bienvenue patient")
        void shouldSendWelcomePatientEmail() {
            // When
            emailService.sendWelcomePatientEmail(TEST_NAME, TEST_EMAIL, "PAT-2026-001");

            // Then
            verify(templateEngine).process(eq("welcome-patient"), contextCaptor.capture());
            verify(emailSender).send(mimeMessage);

            Context context = contextCaptor.getValue();
            assertThat(context.getVariable("name")).isEqualTo(TEST_NAME);
            assertThat(context.getVariable("recordNumber")).isEqualTo("PAT-2026-001");
            assertThat(context.getVariable("url")).isNotNull();
        }

        @Test
        @DisplayName("Devrait envoyer un email de mise à jour dossier patient")
        void shouldSendPatientUpdatedEmail() {
            // When
            emailService.sendPatientUpdatedEmail(TEST_NAME, TEST_EMAIL, "PAT-2026-001", "09/02/2026");

            // Then
            verify(templateEngine).process(eq("patient-updated"), contextCaptor.capture());
            verify(emailSender).send(mimeMessage);

            Context context = contextCaptor.getValue();
            assertThat(context.getVariable("name")).isEqualTo(TEST_NAME);
            assertThat(context.getVariable("recordNumber")).isEqualTo("PAT-2026-001");
            assertThat(context.getVariable("date")).isEqualTo("09/02/2026");
        }

        @Test
        @DisplayName("Devrait envoyer un email de suppression dossier patient")
        void shouldSendPatientDeletedEmail() {
            // When
            emailService.sendPatientDeletedEmail(TEST_NAME, TEST_EMAIL, "PAT-2026-001", "09/02/2026");

            // Then
            verify(templateEngine).process(eq("patient-deleted"), contextCaptor.capture());
            verify(emailSender).send(mimeMessage);

            Context context = contextCaptor.getValue();
            assertThat(context.getVariable("name")).isEqualTo(TEST_NAME);
            assertThat(context.getVariable("recordNumber")).isEqualTo("PAT-2026-001");
            assertThat(context.getVariable("date")).isEqualTo("09/02/2026");
        }
    }

    // RENDEZ-VOUS

    @Nested
    @DisplayName("Appointment Email Tests")
    class AppointmentEmailTests {

        @Test
        @DisplayName("Devrait envoyer un email de confirmation de rendez-vous")
        void shouldSendAppointmentConfirmationEmail() {
            // When
            emailService.sendAppointmentConfirmationEmail(
                    TEST_NAME,
                    TEST_EMAIL,
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
            assertThat(context.getVariable("name")).isEqualTo(TEST_NAME);
            assertThat(context.getVariable("appointmentDate")).isEqualTo("15 février 2026");
            assertThat(context.getVariable("appointmentTime")).isEqualTo("14h30");
            assertThat(context.getVariable("doctorName")).isEqualTo("Dr. Martin");
            assertThat(context.getVariable("department")).isEqualTo("Cardiologie");
            assertThat(context.getVariable("location")).isEqualTo("Bâtiment A, 2ème étage");
        }

        @Test
        @DisplayName("Devrait envoyer un email de rappel de rendez-vous")
        void shouldSendAppointmentReminderEmail() {
            // When
            emailService.sendAppointmentReminderEmail(
                    TEST_NAME,
                    TEST_EMAIL,
                    "16 février 2026",
                    "09h00",
                    "Dr. Dupont",
                    "Bâtiment B, RDC"
            );

            // Then
            verify(templateEngine).process(eq("appointment-reminder"), contextCaptor.capture());
            verify(emailSender).send(mimeMessage);

            Context context = contextCaptor.getValue();
            assertThat(context.getVariable("name")).isEqualTo(TEST_NAME);
            assertThat(context.getVariable("appointmentDate")).isEqualTo("16 février 2026");
            assertThat(context.getVariable("appointmentTime")).isEqualTo("09h00");
            assertThat(context.getVariable("doctorName")).isEqualTo("Dr. Dupont");
            assertThat(context.getVariable("location")).isEqualTo("Bâtiment B, RDC");
        }
    }

    // NOTES MÉDICALES

    @Nested
    @DisplayName("Medical Notes Email Tests")
    class MedicalNotesEmailTests {

        @Test
        @DisplayName("Devrait envoyer un email de nouvelle note médicale")
        void shouldSendNewMedicalNoteEmail() {
            // When
            emailService.sendNewMedicalNoteEmail(
                    TEST_NAME,
                    TEST_EMAIL,
                    "PAT-2026-001",
                    "Dr. Martin",
                    "Endocrinologie",
                    "09/02/2026",
                    "Le patient présente une amélioration significative..."
            );


            verify(templateEngine).process(eq("new-medical-note"), contextCaptor.capture());
            verify(emailSender).send(mimeMessage);

            Context context = contextCaptor.getValue();
            assertThat(context.getVariable("name")).isEqualTo(TEST_NAME);
            assertThat(context.getVariable("patientNumber")).isEqualTo("PAT-2026-001");
            assertThat(context.getVariable("doctorName")).isEqualTo("Dr. Martin");
            assertThat(context.getVariable("department")).isEqualTo("Endocrinologie");
            assertThat(context.getVariable("date")).isEqualTo("09/02/2026");
            assertThat(context.getVariable("notePreview")).isEqualTo("Le patient présente une amélioration significative...");
        }

        @Test
        @DisplayName("Devrait envoyer un email de mise à jour de note")
        void shouldSendNoteUpdatedEmail() {
            // When
            emailService.sendNoteUpdatedEmail(
                    TEST_NAME,
                    TEST_EMAIL,
                    "PAT-2026-001",
                    "Dr. Martin",
                    "10/02/2026",
                    "Mise à jour du traitement..."
            );

            // Then
            verify(templateEngine).process(eq("note-updated"), contextCaptor.capture());
            verify(emailSender).send(mimeMessage);

            Context context = contextCaptor.getValue();
            assertThat(context.getVariable("name")).isEqualTo(TEST_NAME);
            assertThat(context.getVariable("patientNumber")).isEqualTo("PAT-2026-001");
            assertThat(context.getVariable("doctorName")).isEqualTo("Dr. Martin");
            assertThat(context.getVariable("notePreview")).isEqualTo("Mise à jour du traitement...");
        }
    }

    // COMMENTAIRES

    @Nested
    @DisplayName("Comment Email Tests")
    class CommentEmailTests {

        @Test
        @DisplayName("Devrait envoyer un email de nouveau commentaire")
        void shouldSendNewCommentEmail() {
            // When
            emailService.sendNewCommentEmail(
                    TEST_NAME,
                    TEST_EMAIL,
                    "PAT-2026-001",
                    "Résultats d'analyses",
                    "Dr. Martin",
                    "09/02/2026",
                    "Veuillez noter que les résultats sont normaux."
            );

            // Then
            verify(templateEngine).process(eq("new-comment"), contextCaptor.capture());
            verify(emailSender).send(mimeMessage);

            Context context = contextCaptor.getValue();
            assertThat(context.getVariable("name")).isEqualTo(TEST_NAME);
            assertThat(context.getVariable("recordNumber")).isEqualTo("PAT-2026-001");
            assertThat(context.getVariable("subject")).isEqualTo("Résultats d'analyses");
            assertThat(context.getVariable("senderName")).isEqualTo("Dr. Martin");
            assertThat(context.getVariable("message")).isEqualTo("Veuillez noter que les résultats sont normaux.");
        }

        @Test
        @DisplayName("Devrait envoyer un email de commentaire modifié")
        void shouldSendCommentUpdatedEmail() {
            // When
            emailService.sendCommentUpdatedEmail(
                    TEST_NAME,
                    TEST_EMAIL,
                    "PAT-2026-001",
                    "Dr. Martin",
                    "10/02/2026",
                    "Commentaire modifié avec précisions."
            );

            // Then
            verify(templateEngine).process(eq("comment-updated"), contextCaptor.capture());
            verify(emailSender).send(mimeMessage);

            Context context = contextCaptor.getValue();
            assertThat(context.getVariable("name")).isEqualTo(TEST_NAME);
            assertThat(context.getVariable("senderName")).isEqualTo("Dr. Martin");
            assertThat(context.getVariable("comment")).isEqualTo("Commentaire modifié avec précisions.");
        }

        @Test
        @DisplayName("Devrait envoyer un email de commentaire supprimé")
        void shouldSendCommentDeletedEmail() {
            // When
            emailService.sendCommentDeletedEmail(
                    TEST_NAME,
                    TEST_EMAIL,
                    "PAT-2026-001",
                    "Dr. Martin",
                    "10/02/2026"
            );

            // Then
            verify(templateEngine).process(eq("comment-deleted"), contextCaptor.capture());
            verify(emailSender).send(mimeMessage);

            Context context = contextCaptor.getValue();
            assertThat(context.getVariable("name")).isEqualTo(TEST_NAME);
            assertThat(context.getVariable("recordNumber")).isEqualTo("PAT-2026-001");
            assertThat(context.getVariable("senderName")).isEqualTo("Dr. Martin");
            assertThat(context.getVariable("date")).isEqualTo("10/02/2026");
        }
    }

    // FICHIERS

    @Nested
    @DisplayName("File Email Tests")
    class FileEmailTests {

        @Test
        @DisplayName("Devrait envoyer un email de nouveaux fichiers")
        void shouldSendNewFilesEmail() {
            // When
            emailService.sendNewFilesEmail(
                    TEST_NAME,
                    TEST_EMAIL,
                    "PAT-2026-001",
                    "Résultats IRM",
                    "Dr. Martin",
                    "09/02/2026",
                    "irm_cerveau.pdf,rapport_radiologie.pdf"
            );

            verify(templateEngine).process(eq("new-files-uploaded"), contextCaptor.capture());
            verify(emailSender).send(mimeMessage);

            Context context = contextCaptor.getValue();
            assertThat(context.getVariable("name")).isEqualTo(TEST_NAME);
            assertThat(context.getVariable("recordNumber")).isEqualTo("PAT-2026-001");
            assertThat(context.getVariable("subject")).isEqualTo("Résultats IRM");
            assertThat(context.getVariable("uploaderName")).isEqualTo("Dr. Martin");
            assertThat((String[]) context.getVariable("files")).hasSize(2);
            assertThat((String[]) context.getVariable("files")).contains("irm_cerveau.pdf", "rapport_radiologie.pdf");
        }

        @Test
        @DisplayName("Devrait gérer les fichiers null")
        void shouldHandleNullFiles() {
            // When
            emailService.sendNewFilesEmail(
                    TEST_NAME,
                    TEST_EMAIL,
                    "PAT-2026-001",
                    "Documents",
                    "Dr. Martin",
                    "09/02/2026",
                    null
            );


            verify(templateEngine).process(eq("new-files-uploaded"), contextCaptor.capture());

            Context context = contextCaptor.getValue();
            assertThat((String[]) context.getVariable("files")).isEmpty();
        }

        @Test
        @DisplayName("Devrait envoyer un email de fichier supprimé")
        void shouldSendFileDeletedEmail() {
            // When
            emailService.sendFileDeletedEmail(
                    TEST_NAME,
                    TEST_EMAIL,
                    "PAT-2026-001",
                    "Dr. Martin",
                    "10/02/2026",
                    "ancien_rapport.pdf"
            );

            // Then
            verify(templateEngine).process(eq("file-deleted"), contextCaptor.capture());
            verify(emailSender).send(mimeMessage);

            Context context = contextCaptor.getValue();
            assertThat(context.getVariable("name")).isEqualTo(TEST_NAME);
            assertThat(context.getVariable("fileName")).isEqualTo("ancien_rapport.pdf");
        }
    }

    // RÉSULTATS & ÉVALUATIONS

    @Nested
    @DisplayName("Results Email Tests")
    class ResultsEmailTests {

        @Test
        @DisplayName("Devrait envoyer un email de résultats disponibles")
        void shouldSendResultsAvailableEmail() {
            // When
            emailService.sendResultsAvailableEmail(
                    TEST_NAME,
                    TEST_EMAIL,
                    "PAT-2026-001",
                    "Bilan sanguin complet",
                    "Laboratoire Central",
                    "08/02/2026",
                    "bilan_sanguin.pdf,hemogramme.pdf"
            );

            // Then
            verify(templateEngine).process(eq("results-available"), contextCaptor.capture());
            verify(emailSender).send(mimeMessage);

            Context context = contextCaptor.getValue();
            assertThat(context.getVariable("name")).isEqualTo(TEST_NAME);
            assertThat(context.getVariable("patientNumber")).isEqualTo("PAT-2026-001");
            assertThat(context.getVariable("analysisType")).isEqualTo("Bilan sanguin complet");
            assertThat(context.getVariable("laboratory")).isEqualTo("Laboratoire Central");
            assertThat(context.getVariable("sampleDate")).isEqualTo("08/02/2026");
            assertThat((String[]) context.getVariable("files")).hasSize(2);
        }

        @Test
        @DisplayName("Devrait gérer les fichiers null pour résultats")
        void shouldHandleNullFilesInResults() {
            // When
            emailService.sendResultsAvailableEmail(
                    TEST_NAME,
                    TEST_EMAIL,
                    "PAT-2026-001",
                    "Analyse",
                    "Labo",
                    "08/02/2026",
                    null
            );

            // Then
            verify(templateEngine).process(eq("results-available"), contextCaptor.capture());

            Context context = contextCaptor.getValue();
            assertThat((String[]) context.getVariable("files")).isEmpty();
        }
    }

    @Nested
    @DisplayName("Risk Assessment Email Tests")
    class RiskAssessmentTests {

        @Test
        @DisplayName("Devrait envoyer un email d'évaluation de risque DANGER")
        void shouldSendDangerRiskAssessmentEmail() {
            // When
            emailService.sendRiskAssessmentEmail(
                    TEST_NAME,
                    TEST_EMAIL,
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
        @DisplayName("Devrait envoyer un email d'évaluation de risque EARLY_ONSET")
        void shouldSendEarlyOnsetRiskAssessmentEmail() {
            // When
            emailService.sendRiskAssessmentEmail(
                    TEST_NAME,
                    TEST_EMAIL,
                    "EARLY_ONSET",
                    "09 février 2026",
                    3,
                    28,
                    List.of("Hémoglobine A1C", "Taille", "Poids"),
                    null
            );

            // Then
            verify(templateEngine).process(eq("risk-assessment"), contextCaptor.capture());

            Context context = contextCaptor.getValue();
            assertThat(context.getVariable("riskLevel")).isEqualTo("EARLY_ONSET");
            assertThat(context.getVariable("recommendation").toString()).contains("fortement recommandée");
        }

        @Test
        @DisplayName("Devrait envoyer un email d'évaluation de risque BORDERLINE")
        void shouldSendBorderlineRiskAssessmentEmail() {
            // When
            emailService.sendRiskAssessmentEmail(
                    TEST_NAME,
                    TEST_EMAIL,
                    "BORDERLINE",
                    "09 février 2026",
                    2,
                    35,
                    List.of("Taille", "Poids"),
                    null
            );

            // Then
            verify(templateEngine).process(eq("risk-assessment"), contextCaptor.capture());

            Context context = contextCaptor.getValue();
            assertThat(context.getVariable("riskLevel")).isEqualTo("BORDERLINE");
            assertThat(context.getVariable("recommendation").toString()).contains("alimentation");
        }

        @Test
        @DisplayName("Devrait envoyer un email d'évaluation de risque NONE")
        void shouldSendNoneRiskAssessmentEmail() {
            // When
            emailService.sendRiskAssessmentEmail(
                    TEST_NAME,
                    TEST_EMAIL,
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

        @Test
        @DisplayName("Devrait utiliser la recommandation personnalisée si fournie")
        void shouldUseCustomRecommendation() {
            String customRecommendation = "Recommandation personnalisée par le médecin.";

            // When
            emailService.sendRiskAssessmentEmail(
                    TEST_NAME,
                    TEST_EMAIL,
                    "DANGER",
                    "09 février 2026",
                    5,
                    45,
                    List.of("Trigger1"),
                    customRecommendation
            );

            // Then
            verify(templateEngine).process(eq("risk-assessment"), contextCaptor.capture());

            Context context = contextCaptor.getValue();
            assertThat(context.getVariable("recommendation")).isEqualTo(customRecommendation);
        }

        @Test
        @DisplayName("Devrait gérer les triggers null")
        void shouldHandleNullTriggers() {
            // When
            emailService.sendRiskAssessmentEmail(
                    TEST_NAME,
                    TEST_EMAIL,
                    "NONE",
                    "09 février 2026",
                    0,
                    30,
                    null,
                    null
            );

            // Then
            verify(templateEngine).process(eq("risk-assessment"), contextCaptor.capture());

            Context context = contextCaptor.getValue();
            assertThat((List<?>) context.getVariable("triggers")).isEmpty();
        }
    }

    @Nested
    @DisplayName("Assessment Completed Email Tests")
    class AssessmentCompletedTests {

        @Test
        @DisplayName("Devrait envoyer un email d'évaluation diabète complétée")
        void shouldSendAssessmentCompletedEmail() {
            // When
            emailService.sendAssessmentCompletedEmail(
                    TEST_NAME,
                    TEST_EMAIL,
                    "DANGER",
                    "Risque élevé de diabète",
                    "09/02/2026 14:30",
                    5,
                    45,
                    List.of("Hémoglobine A1C", "Microalbumine", "Fumeur")
            );

            // Then
            verify(templateEngine).process(eq("assessment-completed"), contextCaptor.capture());
            verify(emailSender).send(mimeMessage);

            Context context = contextCaptor.getValue();
            assertThat(context.getVariable("name")).isEqualTo(TEST_NAME);
            assertThat(context.getVariable("riskLevel")).isEqualTo("DANGER");
            assertThat(context.getVariable("riskLevelDescription")).isEqualTo("Risque élevé de diabète");
            assertThat(context.getVariable("assessedAt")).isEqualTo("09/02/2026 14:30");
            assertThat(context.getVariable("triggerCount")).isEqualTo(5);
            assertThat(context.getVariable("age")).isEqualTo(45);
            assertThat((List<?>) context.getVariable("triggersFound")).hasSize(3);
        }

        @Test
        @DisplayName("Devrait gérer les valeurs null dans assessment completed")
        void shouldHandleNullValuesInAssessmentCompleted() {
            // When
            emailService.sendAssessmentCompletedEmail(
                    TEST_NAME,
                    TEST_EMAIL,
                    "NONE",
                    "Aucun risque",
                    "09/02/2026",
                    null,
                    null,
                    null
            );

            // Then
            verify(templateEngine).process(eq("assessment-completed"), contextCaptor.capture());

            Context context = contextCaptor.getValue();
            assertThat(context.getVariable("triggerCount")).isEqualTo(0);
            assertThat(context.getVariable("age")).isEqualTo(0);
            assertThat((List<?>) context.getVariable("triggersFound")).isEmpty();
        }
    }

    // MESSAGES

    @Nested
    @DisplayName("Message Email Tests")
    class MessageEmailTests {

        @Test
        @DisplayName("Devrait envoyer un email de nouveau message")
        void shouldSendNewMessageEmail() {
            // When
            emailService.sendNewMessageEmail(
                    TEST_NAME,
                    TEST_EMAIL,
                    "Dr. Martin",
                    "DOCTOR",
                    "Résultats de consultation",
                    "Bonjour, suite à notre consultation d'hier..."
            );

            // Then
            verify(templateEngine).process(eq("new-message"), contextCaptor.capture());
            verify(emailSender).send(mimeMessage);

            Context context = contextCaptor.getValue();
            assertThat(context.getVariable("name")).isEqualTo(TEST_NAME);
            assertThat(context.getVariable("senderName")).isEqualTo("Dr. Martin");
            assertThat(context.getVariable("senderRole")).isEqualTo("Médecin");
            assertThat(context.getVariable("subject")).isEqualTo("Résultats de consultation");
            assertThat(context.getVariable("messagePreview")).isEqualTo("Bonjour, suite à notre consultation d'hier...");
        }

        @Test
        @DisplayName("Devrait formater le rôle PATIENT")
        void shouldFormatPatientRole() {
            // When
            emailService.sendNewMessageEmail(
                    TEST_NAME,
                    TEST_EMAIL,
                    "Marie Martin",
                    "PATIENT",
                    "Question",
                    "Message"
            );

            // Then
            verify(templateEngine).process(eq("new-message"), contextCaptor.capture());

            Context context = contextCaptor.getValue();
            assertThat(context.getVariable("senderRole")).isEqualTo("Patient");
        }

        @Test
        @DisplayName("Devrait formater le rôle ADMIN")
        void shouldFormatAdminRole() {
            // When
            emailService.sendNewMessageEmail(
                    TEST_NAME,
                    TEST_EMAIL,
                    "Admin System",
                    "ADMIN",
                    "Notification",
                    "Message"
            );

            // Then
            verify(templateEngine).process(eq("new-message"), contextCaptor.capture());

            Context context = contextCaptor.getValue();
            assertThat(context.getVariable("senderRole")).isEqualTo("Administrateur");
        }

        @Test
        @DisplayName("Devrait formater le rôle ROLE_DOCTOR")
        void shouldFormatRoleDoctorWithPrefix() {
            // When
            emailService.sendNewMessageEmail(
                    TEST_NAME,
                    TEST_EMAIL,
                    "Dr. Test",
                    "ROLE_DOCTOR",
                    "Sujet",
                    "Message"
            );

            // Then
            verify(templateEngine).process(eq("new-message"), contextCaptor.capture());

            Context context = contextCaptor.getValue();
            assertThat(context.getVariable("senderRole")).isEqualTo("Médecin");
        }

        @Test
        @DisplayName("Devrait gérer un rôle null")
        void shouldHandleNullRole() {
            // When
            emailService.sendNewMessageEmail(
                    TEST_NAME,
                    TEST_EMAIL,
                    "Utilisateur",
                    null,
                    "Sujet",
                    "Message"
            );

            // Then
            verify(templateEngine).process(eq("new-message"), contextCaptor.capture());

            Context context = contextCaptor.getValue();
            assertThat(context.getVariable("senderRole")).isEqualTo("");
        }

        @Test
        @DisplayName("Devrait tronquer un message long")
        void shouldTruncateLongMessage() {
            String longMessage = "A".repeat(300);

            // When
            emailService.sendNewMessageEmail(
                    TEST_NAME,
                    TEST_EMAIL,
                    "Dr. Martin",
                    "DOCTOR",
                    "Sujet",
                    longMessage
            );

            // Then
            verify(templateEngine).process(eq("new-message"), contextCaptor.capture());

            Context context = contextCaptor.getValue();
            String preview = (String) context.getVariable("messagePreview");
            assertThat(preview).hasSize(203); // 200 + "..."
            assertThat(preview).endsWith("...");
        }

        @Test
        @DisplayName("Devrait gérer un message null")
        void shouldHandleNullMessage() {
            // When
            emailService.sendNewMessageEmail(
                    TEST_NAME,
                    TEST_EMAIL,
                    "Dr. Martin",
                    "DOCTOR",
                    "Sujet",
                    null
            );

            // Then
            verify(templateEngine).process(eq("new-message"), contextCaptor.capture());

            Context context = contextCaptor.getValue();
            assertThat(context.getVariable("messagePreview")).isEqualTo("");
        }
    }

    // ERROR HANDLING

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Devrait lancer ApiException en cas d'erreur d'envoi")
        void shouldThrowApiExceptionOnSendError() {
            // Given
            doThrow(new MailSendException("SMTP server unavailable"))
                    .when(emailSender).send(any(MimeMessage.class));

            // When & Then
            assertThatThrownBy(() ->
                    emailService.sendAccountVerificationEmail(TEST_NAME, TEST_EMAIL, "token"))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("Impossible d'envoyer l'e-mail");
        }
    }
}