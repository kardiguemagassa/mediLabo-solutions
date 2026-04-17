package com.openclassrooms.assessmentservice.dtoresponse;

import com.openclassrooms.assessmentservice.model.Gender;
import com.openclassrooms.assessmentservice.model.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de réponse pour l'évaluation du risque de diabète.
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-09
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentResponseDTO {
    private String patientUuid;
    private String patientName;
    private int age;
    private Gender gender;
    private RiskLevel riskLevel;
    private String riskLevelDescription;
    private int triggerCount;
    private List<String> triggersFound;
    private LocalDateTime assessedAt;
}