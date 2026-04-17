package com.openclassrooms.assessmentservice.mapper;

import com.openclassrooms.assessmentservice.dtoresponse.AssessmentResponseDTO;
import com.openclassrooms.assessmentservice.model.Assessment;
import com.openclassrooms.assessmentservice.model.Gender;
import com.openclassrooms.assessmentservice.model.RiskLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AssessmentMapperTest {

    private AssessmentMapper assessmentMapper;

    @BeforeEach
    void setUp() {
        assessmentMapper = new AssessmentMapper();
    }

    @Test
    void toResponse_ShouldMapAllFieldsCorrectly() {
        // GIVEN
        LocalDateTime now = LocalDateTime.now();
        Assessment assessment = Assessment.builder()
                .patientUuid("uuid-123")
                .patientName("John Doe")
                .age(45)
                .gender(Gender.MALE) // Enum
                .riskLevel(RiskLevel.EARLY_ONSET)
                .triggerCount(5)
                .triggersFound(List.of("Hémoglobine A1C", "Microalbumine"))
                .assessedAt(now)
                .build();

        // WHEN
        AssessmentResponseDTO response = assessmentMapper.toResponse(assessment);

        // THEN
        assertThat(response).isNotNull();
        assertThat(response.getPatientUuid()).isEqualTo("uuid-123");
        assertThat(response.getPatientName()).isEqualTo("John Doe");
        assertThat(response.getAge()).isEqualTo(45);
        assertThat(response.getGender()).isEqualTo(Gender.MALE);
        assertThat(response.getRiskLevel()).isEqualTo(RiskLevel.EARLY_ONSET);
        assertThat(response.getRiskLevelDescription()).isEqualTo(RiskLevel.EARLY_ONSET.getDescription());
        assertThat(response.getTriggerCount()).isEqualTo(5);
        assertThat(response.getTriggersFound()).containsExactlyInAnyOrder("Hémoglobine A1C", "Microalbumine");
        assertThat(response.getAssessedAt()).isEqualTo(now);
    }

    @Test
    void toResponse_ShouldHandleFemaleGender() {
        // GIVEN
        Assessment assessment = Assessment.builder()
                .patientUuid("uuid-456")
                .patientName("Jane Doe")
                .age(30)
                .gender(Gender.FEMALE) // Enum
                .riskLevel(RiskLevel.BORDERLINE)
                .triggerCount(2)
                .triggersFound(List.of("Taille"))
                .assessedAt(LocalDateTime.now())
                .build();

        // WHEN
        AssessmentResponseDTO response = assessmentMapper.toResponse(assessment);

        // THEN
        assertThat(response).isNotNull();
        assertThat(response.getGender()).isEqualTo(Gender.FEMALE);
    }

    @Test
    void toResponse_ShouldHandleNullGender() {
        // GIVEN
        Assessment assessment = Assessment.builder()
                .patientUuid("uuid-789")
                .patientName("Unknown")
                .age(25)
                .gender(null) // null
                .riskLevel(RiskLevel.NONE)
                .triggerCount(0)
                .triggersFound(List.of())
                .assessedAt(LocalDateTime.now())
                .build();

        // WHEN
        AssessmentResponseDTO response = assessmentMapper.toResponse(assessment);

        // THEN
        assertThat(response).isNotNull();
        assertThat(response.getGender()).isNull(); // null
    }

    @Test
    void toResponse_ShouldHandleNullAssessment() {
        // WHEN
        AssessmentResponseDTO response = assessmentMapper.toResponse(null);

        // THEN
        assertThat(response).isNull();
    }

    @Test
    void toResponse_ShouldHandleNullRiskLevel() {
        // GIVEN
        Assessment assessment = Assessment.builder()
                .patientUuid("uuid-999")
                .patientName("Test Patient")
                .age(40)
                .gender(Gender.MALE)
                .riskLevel(null) // null
                .triggerCount(0)
                .triggersFound(List.of())
                .assessedAt(LocalDateTime.now())
                .build();

        // WHEN
        AssessmentResponseDTO response = assessmentMapper.toResponse(assessment);

        // THEN
        assertThat(response).isNotNull();
        assertThat(response.getRiskLevel()).isNull();
        assertThat(response.getRiskLevelDescription()).isNull();
    }

    @Test
    void toResponse_ShouldHandleNullTriggersFound() {
        // GIVEN
        Assessment assessment = Assessment.builder()
                .patientUuid("uuid-000")
                .patientName("Test Patient")
                .age(40)
                .gender(Gender.MALE)
                .riskLevel(RiskLevel.NONE)
                .triggerCount(0)
                .triggersFound(null) // null
                .assessedAt(LocalDateTime.now())
                .build();

        // WHEN
        AssessmentResponseDTO response = assessmentMapper.toResponse(assessment);

        // THEN
        assertThat(response).isNotNull();
        assertThat(response.getTriggersFound()).isNull();
        assertThat(response.getTriggerCount()).isEqualTo(0);
    }
}