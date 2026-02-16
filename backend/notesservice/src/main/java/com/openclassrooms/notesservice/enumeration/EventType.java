package com.openclassrooms.notesservice.enumeration;

/**
 * Types d'événements émis par le NotesService.
 *
 * @author Kardigué MAGASSA
 * @version 2.0
 * @since 2026-02-02
 */
public enum EventType {

    /**
     * Une nouvelle note médicale a été créée.
     */
    NOTE_CREATED,

    /**
     * Une note médicale a été mise à jour.
     */
    NOTE_UPDATED,

    /**
     * Un commentaire a été ajouté à une note.
     */
    COMMENT_CREATED,

    /**
     * Un commentaire a été modifié.
     */
    COMMENT_UPDATED,

    /**
     * Un commentaire a été supprimé.
     */
    COMMENT_DELETED,

    /**
     * Un fichier a été uploadé sur une note.
     */
    FILE_UPLOADED,

    /**
     * Un fichier a été supprimé d'une note.
     */
    FILE_DELETED,

    /**
     * Compte utilisateur vérifié (pour compatibilité).
     */
    ACCOUNT_VERIFIED
}