package com.openclassrooms.userservice.event;


import com.openclassrooms.userservice.domain.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import static org.springframework.kafka.support.KafkaHeaders.TOPIC;

/**
 * Écouteur central des événements métier de l’application.
 * Cette classe est responsable de la publication des événements applicatifs
 * vers l’infrastructure de messagerie Kafka.
 * Son rôle est de :
 * Recevoir les événements métiers générés par l’application
 * Les transformer en notifications transportables
 * Les publier dans Kafka pour les autres microservices

 * Cette architecture permet :
 * Un découplage fort entre les microservices
 * Une communication asynchrone et scalable
 * Une meilleure tolérance aux pannes

 * Exemple de consommateurs possibles :
 * Service de notifications (email, SMS, push)
 * Service d’audit
 * Service de monitoring
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-05-01
 */

@Component
@RequiredArgsConstructor
public class ApiEventListener {
    private final KafkaTemplate<String, Notification> kafkaTemplate;
    private static final String NOTIFICATION_TOPIC = "MEDILABO_NOTIFICATIONS";

    /**
     * Traite un événement métier et le publie dans Kafka.
     * Cette méthode est automatiquement déclenchée par Spring lorsqu’un
     * {@code Event} est publié dans le contexte applicatif.
     * Elle réalise les étapes suivantes :
     * Encapsule l’événement dans un objet {@link Notification}
     * Construit un message Kafka avec un header de topic
     * Envoie le message au broker Kafka
     * Le topic utilisé est {@code NOTIFICATION_TOPIC}, qui centralise
     * tous les événements métier de l’application.
     * Les microservices abonnés à ce topic peuvent alors :
     * Envoyer des emails
     * Déclencher des alertes
     * Historiser les actions utilisateur
     * @param event événement métier déclenché dans l’application
     */
    @EventListener
    public void onApiEvent(Event event) {
        var message = MessageBuilder.withPayload(new Notification(event)).setHeader(TOPIC, NOTIFICATION_TOPIC).build();
        kafkaTemplate.send(message);
    }
}