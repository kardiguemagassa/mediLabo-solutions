package com.openclassrooms.assessmentservice.dtoresponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO représentant une note médicale du Notes Service.
 *
 * @author Kardigué MAGASSA
 * @version 1.0
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
    @JsonProperty("note")
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}