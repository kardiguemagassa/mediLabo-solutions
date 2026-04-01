package com.openclassrooms.userservice.event;

import com.openclassrooms.userservice.enumeration.EventType;
import lombok.*;

import java.util.Map;

/**
 * Représente un événement métier généré dans l'application.
 * Cette classe sert à encapsuler toutes les informations nécessaires
 * pour notifier d'autres composants ou microservices via un bus d'événements (comme Kafka).

 * Un type d'événement ({@link EventType}) qui permet d'identifier la nature de l'événement (ex. USER_CREATED, PASSWORD_RESET)
 *  Une carte de données ({@link Map}) contenant les informations pertinentes associées à l'événement (ex. UUID utilisateur, email, timestamp)
 * Exemple d'utilisation :
 * Création d'un utilisateur → génération d'un événement USER_CREATED
 * Réinitialisation de mot de passe → génération d'un événement PASSWORD_RESET
 * Suivi d'activité → génération d'événements d'audit ou de logs
 * Cette classe est conçue pour être immutable via {@link Builder} ou mutable
 * via les setters si nécessaire.
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-05-01
 */

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Event {
    /**
     * Type de l'événement pour catégoriser l'action effectuée.
     * Par exemple :
     * USER_CREATED : un utilisateur a été créé
     * PASSWORD_RESET : un utilisateur a réinitialisé son mot de passe
     *
     */
    private EventType eventType;

    /**
     * Données associées à l'événement.
     * Cette carte peut contenir n'importe quelle information pertinente
     * au traitement ou à la notification des autres composants.
     * UUID de l'utilisateur
     * Email
     * Horodatage
     */
    private Map<String, ?> data;
}