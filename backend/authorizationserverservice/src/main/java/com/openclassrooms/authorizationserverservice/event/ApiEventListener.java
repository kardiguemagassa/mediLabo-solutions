package com.openclassrooms.authorizationserverservice.event;


import com.openclassrooms.authorizationserverservice.domain.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import static org.springframework.kafka.support.KafkaHeaders.TOPIC;

/**
 * <p>
 * Écouteur central des événements métier de l’application.
 * </p>
 *
 * <p>
 * Cette classe est responsable de la publication des événements applicatifs
 * vers l’infrastructure de messagerie Kafka.
 * </p>
 *
 * <p>
 * Son rôle est de :
 * </p>
 * <ul>
 *   <li>Recevoir les événements métiers générés par l’application</li>
 *   <li>Les transformer en notifications transportables</li>
 *   <li>Les publier dans Kafka pour les autres microservices</li>
 * </ul>
 *
 * <p>
 * Cette architecture permet :
 * </p>
 * <ul>
 *   <li>Un découplage fort entre les microservices</li>
 *   <li>Une communication asynchrone et scalable</li>
 *   <li>Une meilleure tolérance aux pannes</li>
 * </ul>
 *
 * <p>
 * Exemple de consommateurs possibles :
 * </p>
 * <ul>
 *   <li>Service de notifications (email, SMS, push)</li>
 *   <li>Service d’audit</li>
 *   <li>Service de monitoring</li>
 * </ul>
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-05-01
 */

@Component
@RequiredArgsConstructor
public class ApiEventListener {
    private final KafkaTemplate<String, Notification> kafkaTemplate;
    private static final String NOTIFICATION_TOPIC = "NOTIFICATION_TOPIC";

    /**
     * <p>
     * Traite un événement métier et le publie dans Kafka.
     * </p>
     *
     * <p>
     * Cette méthode est automatiquement déclenchée par Spring lorsqu’un
     * {@code Event} est publié dans le contexte applicatif.
     * </p>
     *
     * <p>
     * Elle réalise les étapes suivantes :
     * </p>
     * <ul>
     *   <li>Encapsule l’événement dans un objet {@link Notification}</li>
     *   <li>Construit un message Kafka avec un header de topic</li>
     *   <li>Envoie le message au broker Kafka</li>
     * </ul>
     *
     * <p>
     * Le topic utilisé est {@code NOTIFICATION_TOPIC}, qui centralise
     * tous les événements métier de l’application.
     * </p>
     *
     * <p>
     * Les microservices abonnés à ce topic peuvent alors :
     * </p>
     * <ul>
     *   <li>Envoyer des emails</li>
     *   <li>Déclencher des alertes</li>
     *   <li>Historiser les actions utilisateur</li>
     * </ul>
     *
     * @param event événement métier déclenché dans l’application
     */
    @EventListener
    public void onApiEvent(Event event) {
        var message = MessageBuilder.withPayload(new Notification(event)).setHeader(TOPIC, NOTIFICATION_TOPIC).build();
        kafkaTemplate.send(message);
    }
}