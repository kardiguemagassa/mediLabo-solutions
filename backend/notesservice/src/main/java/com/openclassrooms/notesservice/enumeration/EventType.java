package com.openclassrooms.notesservice.enumeration;

/**
 * Types d'événements émis par le NotesService.
 *
 * @author Kardigué MAGASSA
 * @version 2.0
 * @since 2026-02-02
 */
public enum EventType {
    NOTE_CREATED,
    NOTE_UPDATED,
    COMMENT_CREATED,
    COMMENT_UPDATED,
    COMMENT_DELETED,
    FILE_UPLOADED,
    FILE_DELETED,
    ACCOUNT_VERIFIED
}