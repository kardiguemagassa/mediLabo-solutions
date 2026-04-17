package com.openclassrooms.notesservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la requête d'ajout d'un commentaire.
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-07
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentRequest {

    @NotBlank(message = "Le contenu du commentaire est obligatoire")
    @Size(min = 1, max = 2000, message = "Le commentaire doit contenir entre 1 et 2000 caractères")
    private String content;
}