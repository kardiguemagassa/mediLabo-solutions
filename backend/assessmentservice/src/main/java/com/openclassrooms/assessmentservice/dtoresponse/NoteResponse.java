package com.openclassrooms.assessmentservice.dtoresponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO représentant une note médicale du Notes Service.
 *
 * @author Kardigué MAGASSA
 * @version 1.1
 * @since 2026-02-09
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NoteResponse {
    private String noteUuid;
    private String patientUuid;
    private String practitionerUuid;
    private String practitionerName;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}