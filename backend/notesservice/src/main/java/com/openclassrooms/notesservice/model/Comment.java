package com.openclassrooms.notesservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Modèle embedded pour les commentaires sur une note.
 * Stocké directement dans le document Note (embedded document).
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-07
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Comment {

    private String commentUuid;
    private String content;

    // Informations sur l'auteur (dénormalisé)
    private String authorUuid;
    private String authorName;
    private String authorRole;
    private String authorImageUrl;

    private Boolean edited;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}