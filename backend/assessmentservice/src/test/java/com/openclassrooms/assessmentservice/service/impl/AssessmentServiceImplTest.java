package com.openclassrooms.assessmentservice.service.impl;

import com.openclassrooms.assessmentservice.model.Assessment;
import com.openclassrooms.assessmentservice.model.Gender;
import com.openclassrooms.assessmentservice.model.RiskLevel;
import com.openclassrooms.assessmentservice.exception.ApiException;
import com.openclassrooms.assessmentservice.client.NoteClient;
import com.openclassrooms.assessmentservice.client.PatientClient;
import com.openclassrooms.assessmentservice.dtoresponse.NoteResponse;
import com.openclassrooms.assessmentservice.dtoresponse.PatientResponse;
import com.openclassrooms.assessmentservice.service.RiskLevelCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AssessmentServiceImpl - Tests unitaires")
class AssessmentServiceImplTest {

    @Mock
    private PatientClient patientClient;

    @Mock
    private NoteClient noteClient;

    @Mock
    private RiskLevelCalculator riskLevelCalculator;

    @InjectMocks
    private AssessmentServiceImpl assessmentService;

    private static final String PATIENT_UUID = "patient-uuid-123";

    private PatientResponse createPatient(int age, Gender gender) {
        return PatientResponse.builder()
                .patientUuid(PATIENT_UUID)
                .firstName("Jean")
                .lastName("Dupont")
                .dateOfBirth(LocalDate.now().minusYears(age))
                .gender(gender)
                .build();
    }

    private NoteResponse createNote(String content) {
        return NoteResponse.builder()
                .noteUuid("note-uuid-" + System.nanoTime())
                .patientUuid(PATIENT_UUID)
                .content(content)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // SUCCESS

    @Nested
    @DisplayName("Cas de succès")
    class SuccessTests {

        @Test
        @DisplayName("Devrait évaluer NONE quand aucune note")
        void shouldReturnNone_whenNoNotes() {
            // Given
            PatientResponse patient = createPatient(45, Gender.MALE);

            when(patientClient.getPatientByUuid(PATIENT_UUID)).thenReturn(CompletableFuture.completedFuture(Optional.of(patient)));
            when(noteClient.getNotesByPatientUuid(PATIENT_UUID)).thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
            when(riskLevelCalculator.calculate(eq(45), eq(Gender.MALE), eq(0))).thenReturn(RiskLevel.NONE);

            // When
            Assessment result = assessmentService.assessDiabetesRisk(PATIENT_UUID).join();

            // Then
            assertThat(result.patientUuid()).isEqualTo(PATIENT_UUID);
            assertThat(result.patientName()).isEqualTo("Jean Dupont");
            assertThat(result.age()).isEqualTo(45);
            assertThat(result.gender()).isEqualTo(Gender.MALE);
            assertThat(result.riskLevel()).isEqualTo(RiskLevel.NONE);
            assertThat(result.triggerCount()).isZero();
            assertThat(result.triggersFound()).isEmpty();

            verify(patientClient).getPatientByUuid(PATIENT_UUID);
            verify(noteClient).getNotesByPatientUuid(PATIENT_UUID);
            verify(riskLevelCalculator).calculate(45, Gender.MALE, 0);
        }

        @Test
        @DisplayName("Devrait évaluer BORDERLINE avec 3 déclencheurs pour patient > 30 ans")
        void shouldReturnBorderline_whenOver30With3Triggers() {
            // Given
            PatientResponse patient = createPatient(45, Gender.FEMALE);
            List<NoteResponse> notes = List.of(
                    createNote("Patient fumeur, cholestérol élevé"),
                    createNote("Poids stable")
            );

            when(patientClient.getPatientByUuid(PATIENT_UUID)).thenReturn(CompletableFuture.completedFuture(Optional.of(patient)));
            when(noteClient.getNotesByPatientUuid(PATIENT_UUID)).thenReturn(CompletableFuture.completedFuture((notes)));
            when(riskLevelCalculator.calculate(eq(45), eq(Gender.FEMALE), eq(3))).thenReturn(RiskLevel.BORDERLINE);

            // When
            Assessment result = assessmentService.assessDiabetesRisk(PATIENT_UUID).join();

            // Then
            assertThat(result.riskLevel()).isEqualTo(RiskLevel.BORDERLINE);
            assertThat(result.triggerCount()).isEqualTo(3);
            assertThat(result.triggersFound()).containsExactlyInAnyOrder("Fumeur", "Cholestérol", "Poids");
        }

        @Test
        @DisplayName("Devrait évaluer IN_DANGER pour homme < 30 ans avec 3 déclencheurs")
        void shouldReturnInDanger_whenYoungMaleWith3Triggers() {
            // Given
            PatientResponse patient = createPatient(25, Gender.MALE);
            List<NoteResponse> notes = List.of(
                    createNote("Fumeur, taille normale, poids élevé")
            );

            when(patientClient.getPatientByUuid(PATIENT_UUID)).thenReturn(CompletableFuture.completedFuture(Optional.of(patient)));
            when(noteClient.getNotesByPatientUuid(PATIENT_UUID)).thenReturn(CompletableFuture.completedFuture(notes));
            when(riskLevelCalculator.calculate(eq(25), eq(Gender.MALE), eq(3))).thenReturn(RiskLevel.IN_DANGER);

            // When
            Assessment result = assessmentService.assessDiabetesRisk(PATIENT_UUID).join();

            // Then
            assertThat(result.riskLevel()).isEqualTo(RiskLevel.IN_DANGER);
            assertThat(result.triggerCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("Devrait évaluer EARLY_ONSET pour homme < 30 ans avec 5+ déclencheurs")
        void shouldReturnEarlyOnset_whenYoungMaleWith5Triggers() {
            // Given
            PatientResponse patient = createPatient(28, Gender.MALE);
            List<NoteResponse> notes = List.of(
                    createNote("Fumeur, cholestérol anormal, vertiges fréquents"),
                    createNote("Anticorps détectés, réaction allergique")
            );

            when(patientClient.getPatientByUuid(PATIENT_UUID)).thenReturn(CompletableFuture.completedFuture(Optional.of(patient)));
            when(noteClient.getNotesByPatientUuid(PATIENT_UUID)).thenReturn(CompletableFuture.completedFuture(notes));
            when(riskLevelCalculator.calculate(eq(28), eq(Gender.MALE), eq(6))).thenReturn(RiskLevel.EARLY_ONSET);

            // When
            Assessment result = assessmentService.assessDiabetesRisk(PATIENT_UUID).join();

            // Then
            assertThat(result.riskLevel()).isEqualTo(RiskLevel.EARLY_ONSET);
            assertThat(result.triggerCount()).isEqualTo(6);
        }

        @Test
        @DisplayName("Devrait ignorer les doublons de déclencheurs entre notes")
        void shouldCountUniqueTriggersAcrossNotes() {
            // Given
            PatientResponse patient = createPatient(40, Gender.MALE);
            List<NoteResponse> notes = List.of(
                    createNote("Patient fumeur"),
                    createNote("Toujours fumeur, cholestérol élevé"),
                    createNote("Fumeur confirmé")
            );

            when(patientClient.getPatientByUuid(PATIENT_UUID)).thenReturn(CompletableFuture.completedFuture(Optional.of(patient)));
            when(noteClient.getNotesByPatientUuid(PATIENT_UUID)).thenReturn(CompletableFuture.completedFuture(notes));
            when(riskLevelCalculator.calculate(eq(40), eq(Gender.MALE), eq(2))).thenReturn(RiskLevel.BORDERLINE);

            // When
            Assessment result = assessmentService.assessDiabetesRisk(PATIENT_UUID).join();

            // Then
            assertThat(result.triggerCount()).isEqualTo(2); // Fumeur + Cholestérol (pas 3x Fumeur)
            assertThat(result.triggersFound()).containsExactlyInAnyOrder("Fumeur", "Cholestérol");
        }
    }

    // ERROR

    @Nested
    @DisplayName("Cas d'erreur")
    class ErrorTests {

        @Test
        @DisplayName("Devrait lever ApiException si patient introuvable")
        void shouldThrowPatientNotFound_whenPatientDoesNotExist() {
            // GIVEN
            when(patientClient.getPatientByUuid(PATIENT_UUID)).thenReturn(CompletableFuture.completedFuture(Optional.empty()));

            // OBLIGATOIRE sinon NPE dans thenCombine
            when(noteClient.getNotesByPatientUuid(PATIENT_UUID)).thenReturn(CompletableFuture.completedFuture(List.of()));

            // WHEN
            CompletableFuture<Assessment> future = assessmentService.assessDiabetesRisk(PATIENT_UUID);

            // THEN
            assertThatThrownBy(future::join).hasCauseInstanceOf(ApiException.class).hasMessageContaining(PATIENT_UUID);

            verify(patientClient).getPatientByUuid(PATIENT_UUID);
            verify(noteClient).getNotesByPatientUuid(PATIENT_UUID);
            verify(riskLevelCalculator, never()).calculate(anyInt(), any(), anyInt());
        }


        @Test
        @DisplayName("Devrait continuer avec 0 déclencheurs si Notes Service indisponible (fallback)")
        void shouldContinueWithZeroTriggers_whenNotesServiceUnavailable() {
            // Given
            PatientResponse patient = createPatient(50, Gender.FEMALE);
            when(patientClient.getPatientByUuid(PATIENT_UUID)).thenReturn(CompletableFuture.completedFuture(Optional.of(patient)));
            when(noteClient.getNotesByPatientUuid(PATIENT_UUID)).thenReturn(CompletableFuture.completedFuture(Collections.emptyList())); // Fallback
            when(riskLevelCalculator.calculate(eq(50), eq(Gender.FEMALE), eq(0))).thenReturn(RiskLevel.NONE);

            // When
            Assessment result = assessmentService.assessDiabetesRisk(PATIENT_UUID).join();

            // Then
            assertThat(result.riskLevel()).isEqualTo(RiskLevel.NONE);
            assertThat(result.triggerCount()).isZero();
        }
    }

    // ==================== CAS LIMITES ====================

    @Nested
    @DisplayName("Cas limites")
    class EdgeCasesTests {

        @Test
        @DisplayName("Devrait gérer les notes avec contenu null")
        void shouldHandleNotesWithNullContent() {
            // Given
            PatientResponse patient = createPatient(35, Gender.MALE);
            NoteResponse noteWithNullContent = NoteResponse.builder()
                    .noteUuid("note-1")
                    .patientUuid(PATIENT_UUID)
                    .content(null)
                    .build();

            when(patientClient.getPatientByUuid(PATIENT_UUID)).thenReturn(CompletableFuture.completedFuture(Optional.of(patient)));
            when(noteClient.getNotesByPatientUuid(PATIENT_UUID)).thenReturn(CompletableFuture.completedFuture(List.of(noteWithNullContent)));
            when(riskLevelCalculator.calculate(eq(35), eq(Gender.MALE), eq(0))).thenReturn(RiskLevel.NONE);

            // When
            Assessment result = assessmentService.assessDiabetesRisk(PATIENT_UUID).join();

            // Then
            assertThat(result.triggerCount()).isZero();
        }

        @Test
        @DisplayName("Devrait gérer les notes avec contenu vide")
        void shouldHandleNotesWithEmptyContent() {
            // Given
            PatientResponse patient = createPatient(35, Gender.FEMALE);
            NoteResponse emptyNote = createNote("");

            when(patientClient.getPatientByUuid(PATIENT_UUID)).thenReturn(CompletableFuture.completedFuture(Optional.of(patient)));
            when(noteClient.getNotesByPatientUuid(PATIENT_UUID)).thenReturn(CompletableFuture.completedFuture(List.of(emptyNote)));
            when(riskLevelCalculator.calculate(eq(35), eq(Gender.FEMALE), eq(0))).thenReturn(RiskLevel.NONE);

            // When
            Assessment result = assessmentService.assessDiabetesRisk(PATIENT_UUID).join();

            // Then
            assertThat(result.triggerCount()).isZero();
        }
    }
}