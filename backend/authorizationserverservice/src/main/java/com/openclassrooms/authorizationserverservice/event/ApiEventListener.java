package com.openclassrooms.patientservice.event;


import com.openclassrooms.patientservice.domain.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import static org.springframework.kafka.support.KafkaHeaders.TOPIC;

/**
 * @author Kardigué MAGASSA
 * @version 1.0
 * @email magassakara@gmail.com
 * @since 2026-05-01
 */

@Component
@RequiredArgsConstructor
public class ApiEventListener {
    private final KafkaTemplate<String, Notification> kafkaTemplate;
    private static final String NOTIFICATION_TOPIC = "NOTIFICATION_TOPIC";

    @EventListener
    public void onApiEvent(Event event) {
        var message = MessageBuilder.withPayload(new Notification(event)).setHeader(TOPIC, NOTIFICATION_TOPIC).build();
        kafkaTemplate.send(message);
    }
}