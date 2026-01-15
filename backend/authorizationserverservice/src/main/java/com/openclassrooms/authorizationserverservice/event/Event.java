package com.openclassrooms.patientservice.event;

import com.openclassrooms.patientservice.enumeration.EventType;
import lombok.*;

import java.util.Map;

/**
 * @author Kardigué MAGASSA
 * @version 1.0
 * @email magassakara@gmail.com
 * @since 2026-05-01
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