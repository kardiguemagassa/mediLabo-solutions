package com.openclassrooms.patientservice.event;

import com.openclassrooms.patientservice.domain.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import static org.springframework.kafka.support.KafkaHeaders.TOPIC;


@Slf4j
@Component
@RequiredArgsConstructor
public class ApiEventListener {
    private final KafkaTemplate<String, Notification> kafkaTemplate;
    private static final String NOTIFICATION_TOPIC = "MEDILABO_NOTIFICATIONS";

    /**
     * Écoute les événements et les publie sur Kafka.
     * @param event L'événement à publier
     */
    @Async
    @EventListener
    public void onApiEvent(Event event) {
        try {
            log.info("Publishing event: eventType={}", event.getEventType());

            var notification = new Notification(event);
            var message = MessageBuilder
                    .withPayload(notification)
                    .setHeader(TOPIC, NOTIFICATION_TOPIC)
                    .build();

            kafkaTemplate.send(message)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish event: eventType={}",
                                    event.getEventType(), ex);
                        } else {
                            log.debug("Event published successfully: eventType={}",
                                    event.getEventType());
                        }
                    });

        } catch (Exception e) {
            log.error("Error publishing event to Kafka", e);
        }
    }
}
