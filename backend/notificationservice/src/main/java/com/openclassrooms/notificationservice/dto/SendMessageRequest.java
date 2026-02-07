package com.openclassrooms.notificationservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * DTO pour la requête d'envoi de message.
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-09
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SendMessageRequest {

    @NotBlank(message = "L'email du destinataire est obligatoire")
    @Email(message = "L'email du destinataire doit être valide")
    private String receiverEmail;

    @NotBlank(message = "Le sujet est obligatoire")
    @Size(max = 255, message = "Le sujet ne peut pas dépasser 255 caractères")
    private String subject;

    @NotBlank(message = "Le message est obligatoire")
    @Size(max = 5000, message = "Le message ne peut pas dépasser 5000 caractères")
    private String message;
}