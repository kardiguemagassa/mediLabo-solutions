package com.openclassrooms.notesservice.event;

import com.openclassrooms.notesservice.enumeration.EventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Événement émis par le NotesService.
 * Envoyé via Kafka au NotificationService.
 *
 * Structure alignée avec NotificationService.
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
     * Type d'événement (aligné avec NotificationService).
     */
    private EventType eventType;
    private Map<String, Object> data;
}