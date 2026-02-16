package com.openclassrooms.notesservice.event;

import com.openclassrooms.notesservice.enumeration.EventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Événement émis par le NotesService.
 * Envoyé via Kafka au NotificationService.
 *
 * @author Kardigué MAGASSA
 * @version 2.0
 * @since 2026-02-07
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    /**
     * Type d'événement.
     */
    private EventType type;

    /**
     * UUID de la note concernée.
     */
    private String noteUuid;

    /**
     * UUID du patient concerné.
     */
    private String patientUuid;

    /**
     * Email du patient (pour notification directe).
     */
    private String patientEmail;

    /**
     * Nom du patient (pour affichage).
     */
    private String patientName;

    /**
     * UUID du praticien qui a effectué l'action.
     */
    private String practitionerUuid;

    /**
     * Nom du praticien.
     */
    private String practitionerName;

    /**
     * Rôle du praticien.
     */
    private String practitionerRole;

    /**
     * Données spécifiques selon le type d'event.
     *
     * Pour FILE_UPLOADED:
     *   - fileUuid: UUID du fichier
     *   - fileName: Nom du fichier
     *   - fileSize: Taille formatée
     *
     * Pour COMMENT_CREATED:
     *   - commentUuid: UUID du commentaire
     *   - commentPreview: Contenu (tronqué)
     *   - authorName: Nom de l'auteur
     */
    private Map<String, Object> data;

    /**
     * Timestamp de l'event.
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}