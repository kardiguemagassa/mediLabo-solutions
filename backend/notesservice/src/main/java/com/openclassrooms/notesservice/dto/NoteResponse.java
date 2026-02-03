package com.openclassrooms.notesservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de réponse pour une note
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-02
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NoteResponse {

    private String noteUuid;
    private String patientUuid;
    private String practitionerUuid;
    private String practitionerName;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}