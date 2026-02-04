package com.openclassrooms.assessmentservice.controller;

import com.openclassrooms.assessmentservice.model.Assessment;
import com.openclassrooms.assessmentservice.dtoresponse.AssessmentResponse;
import com.openclassrooms.assessmentservice.mapper.AssessmentMapper;
import com.openclassrooms.assessmentservice.service.AssessmentService;
import com.openclassrooms.assessmentservice.model.Gender;
import com.openclassrooms.assessmentservice.model.RiskLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AssessmentController - Tests unitaires")
class AssessmentControllerTest {

    @Mock
    private AssessmentService assessmentService;

    @Mock
    private AssessmentMapper assessmentMapper;

    @InjectMocks
    private AssessmentController assessmentController;

    private static final String PATIENT_UUID = "patient-uuid-123";
    private Assessment assessment;
    private AssessmentResponse assessmentResponse;

    @BeforeEach
    void setUp() {
        assessment = new Assessment(
                PATIENT_UUID,
                "Jean Dupont",
                45,
                Gender.MALE,
                RiskLevel.NONE,
                0,
                List.of()
        );

        assessmentResponse = AssessmentResponse.builder()
                .patientUuid(PATIENT_UUID)
                .patientName("Jean Dupont")
                .age(45)
                .gender(Gender.MALE)
                .riskLevel(RiskLevel.NONE)
                .riskLevelDescription("Aucun risque détecté")
                .triggerCount(0)
                .triggersFound(List.of())
                .assessedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Devrait retourner AssessmentResponse correctement mappée")
    void shouldReturnAssessmentResponse_whenServiceSucceeds() throws Exception {
        // Given
        CompletableFuture<Assessment> futureAssessment = CompletableFuture.completedFuture(assessment);
        when(assessmentService.assessDiabetesRisk(PATIENT_UUID)).thenReturn(futureAssessment);
        when(assessmentMapper.toResponse(assessment)).thenReturn(assessmentResponse);

        // When
        CompletableFuture<AssessmentResponse> resultFuture = assessmentController.assessDiabetesRisk(PATIENT_UUID);
        AssessmentResponse result = resultFuture.get(); // Bloque juste pour le test

        // Then
        assertThat(result.getPatientUuid()).isEqualTo(PATIENT_UUID);
        assertThat(result.getPatientName()).isEqualTo("Jean Dupont");
        assertThat(result.getAge()).isEqualTo(45);
        assertThat(result.getGender()).isEqualTo(Gender.MALE);
        assertThat(result.getRiskLevel()).isEqualTo(RiskLevel.NONE);
        assertThat(result.getTriggerCount()).isZero();
        assertThat(result.getTriggersFound()).isEmpty();

        verify(assessmentService).assessDiabetesRisk(PATIENT_UUID);
        verify(assessmentMapper).toResponse(assessment);
    }

    @Test
    @DisplayName("Devrait lancer une exception si le service échoue")
    void shouldThrowException_whenServiceFails() {
        // Given
        CompletableFuture<Assessment> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Service indisponible"));
        when(assessmentService.assessDiabetesRisk(PATIENT_UUID)).thenReturn(failedFuture);

        // When / Then
        CompletableFuture<AssessmentResponse> resultFuture = assessmentController.assessDiabetesRisk(PATIENT_UUID);
        assertThat(resultFuture).isCompletedExceptionally();
        verify(assessmentService).assessDiabetesRisk(PATIENT_UUID);
        verifyNoInteractions(assessmentMapper);
    }
}
