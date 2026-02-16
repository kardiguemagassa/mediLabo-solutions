package com.openclassrooms.notesservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Modèle embedded pour les fichiers attachés à une note.
 * Stocké directement dans le document Note (embedded document).
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-07
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileAttachment {

    private String fileUuid;
    private String originalName;
    private String storedName;
    private String extension;
    private String contentType;
    private Long size;
    private String formattedSize;
    private String uri;

    // Informations sur l'uploader (dénormalisé)
    private String uploadedByUuid;
    private String uploadedByName;
    private String uploadedByRole;

    private LocalDateTime uploadedAt;
}