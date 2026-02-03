package com.openclassrooms.notesservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la création/modification d'une note
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-02
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteRequest {

    @NotBlank(message = "L'UUID du patient est obligatoire")
    private String patientUuid;

    @NotBlank(message = "Le contenu de la note est obligatoire")
    @Size(min = 1, max = 10000, message = "Le contenu doit faire entre 1 et 10000 caractères")
    private String content;
}
