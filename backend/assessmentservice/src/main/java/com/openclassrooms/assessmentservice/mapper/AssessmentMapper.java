package com.openclassrooms.assessmentservice.mapper;

import com.openclassrooms.assessmentservice.model.Assessment;
import com.openclassrooms.assessmentservice.dtoresponse.AssessmentResponse;
import com.openclassrooms.assessmentservice.model.Gender;
import com.openclassrooms.assessmentservice.model.RiskLevel;
import org.springframework.stereotype.Component;

/**
 * Mapper pour convertir Assessment domain vers DTO.
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-09
 */
@Component
public class AssessmentMapper {

    public AssessmentResponse toResponse(Assessment assessment) {
        if (assessment == null) {
            return null;
        }

        return AssessmentResponse.builder()
                .patientUuid(assessment.patientUuid())
                .patientName(assessment.patientName())
                .age(assessment.age())
                .gender(assessment.gender())
                .riskLevel(assessment.riskLevel())
                .riskLevelDescription(getRiskDescription(assessment.riskLevel()))
                .triggerCount(assessment.triggerCount())
                .triggersFound(assessment.triggersFound())
                .assessedAt(assessment.assessedAt())
                .build();
    }

    private String getRiskDescription(RiskLevel riskLevel) {
        return riskLevel != null ? riskLevel.getDescription() : null;
    }
}