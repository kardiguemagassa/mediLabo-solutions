package com.openclassrooms.notificationservice.event;

import com.openclassrooms.notificationservice.enumeration.EventType;
import lombok.*;

import java.util.Map;

/**
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-09
 */

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Event {
    private EventType eventType;
    private Map<String, ?> data;
}