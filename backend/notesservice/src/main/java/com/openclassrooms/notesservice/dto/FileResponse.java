package com.openclassrooms.notesservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO pour la réponse d'un fichier attaché.
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-07
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileResponse {

    private String fileUuid;
    private String name;
    private String extension;
    private String contentType;
    private Long size;
    private String formattedSize;
    private String downloadUrl;

    // Uploader info
    private String uploadedByUuid;
    private String uploadedByName;
    private String uploadedByRole;

    private LocalDateTime uploadedAt;
}