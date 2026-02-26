package com.openclassrooms.assessmentservice.controller;

import com.openclassrooms.assessmentservice.domain.Response;
import com.openclassrooms.assessmentservice.model.Assessment;
import com.openclassrooms.assessmentservice.dtoresponse.AssessmentResponse;
import com.openclassrooms.assessmentservice.mapper.AssessmentMapper;
import com.openclassrooms.assessmentservice.service.AssessmentService;
import com.openclassrooms.assessmentservice.model.Gender;
import com.openclassrooms.assessmentservice.model.RiskLevel;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AssessmentController - Tests unitaires")
class AssessmentControllerTest {

    @Mock
    private AssessmentService assessmentService;

    @Mock
    private AssessmentMapper assessmentMapper;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private AssessmentController assessmentController;

    private static final String PATIENT_UUID = "patient-uuid-123";

    private Assessment assessment;
    private AssessmentResponse assessmentResponse;

    @BeforeEach
    void setUp() {
        assessment = new Assessment(PATIENT_UUID, "Jean Dupont", 45, Gender.MALE, RiskLevel.NONE, 0, List.of());

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
    @DisplayName("Devrait retourner ResponseEntity<Response> correctement")
    void shouldReturnResponseEntity_whenServiceSucceeds() {

        // Given
        when(assessmentService.assessDiabetesRisk(PATIENT_UUID))
                .thenReturn(Mono.just(assessment));

        when(assessmentMapper.toResponse(assessment))
                .thenReturn(assessmentResponse);

        when(request.getRequestURI()).thenReturn("/api/assess/diabetes/" + PATIENT_UUID);

        // When
        Mono<ResponseEntity<Response>> resultMono = assessmentController.assessDiabetesRisk(PATIENT_UUID, request);

        ResponseEntity<Response> responseEntity = resultMono.block();

        // Then
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(200);
        assertThat(responseEntity.getBody()).isNotNull();

        verify(assessmentService).assessDiabetesRisk(PATIENT_UUID);
        verify(assessmentMapper).toResponse(assessment);
    }

    @Test
    @DisplayName("Devrait propager une erreur si le service échoue")
    void shouldReturnError_whenServiceFails() {

        // Given
        when(assessmentService.assessDiabetesRisk(PATIENT_UUID))
                .thenReturn(Mono.error(new RuntimeException("Service indisponible")));

        // When
        Mono<ResponseEntity<Response>> resultMono =
                assessmentController.assessDiabetesRisk(PATIENT_UUID, request);

        // Then
        StepVerifier.create(resultMono).expectError(RuntimeException.class).verify();

        verify(assessmentService).assessDiabetesRisk(PATIENT_UUID);
        verifyNoInteractions(assessmentMapper);
    }


}
