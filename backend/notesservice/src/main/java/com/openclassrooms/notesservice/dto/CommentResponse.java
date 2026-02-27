package com.openclassrooms.notesservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO pour la réponse d'un commentaire.
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-07
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommentResponse {

    private String commentUuid;
    private String content;

    // Author info
    private String authorUuid;
    private String authorName;
    private String authorRole;
    private String authorImageUrl;

    private Boolean edited;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}