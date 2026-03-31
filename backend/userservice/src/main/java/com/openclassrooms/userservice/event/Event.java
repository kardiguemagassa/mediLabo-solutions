package com.openclassrooms.userservice.event;


import com.openclassrooms.userservice.enumeration.EventType;
import lombok.*;

import java.util.Map;

/**
 * <p>
 * Représente un événement métier généré dans l'application.
 * </p>
 *
 * <p>
 * Cette classe sert à encapsuler toutes les informations nécessaires
 * pour notifier d'autres composants ou microservices via un bus d'événements
 * (comme Kafka).
 * </p>
 *
 * <p>
 * Un événement contient :
 * </p>
 * <ul>
 *   <li>Un type d'événement ({@link EventType}) qui permet d'identifier
 *       la nature de l'événement (ex. USER_CREATED, PASSWORD_RESET)</li>
 *   <li>Une carte de données ({@link Map}) contenant les informations
 *       pertinentes associées à l'événement (ex. UUID utilisateur, email, timestamp)</li>
 * </ul>
 *
 * <p>
 * Exemple d'utilisation :
 * </p>
 * <ul>
 *   <li>Création d'un utilisateur → génération d'un événement USER_CREATED</li>
 *   <li>Réinitialisation de mot de passe → génération d'un événement PASSWORD_RESET</li>
 *   <li>Suivi d'activité → génération d'événements d'audit ou de logs</li>
 * </ul>
 *
 * <p>
 * Cette classe est conçue pour être immutable via {@link Builder} ou mutable
 * via les setters si nécessaire.
 * </p>
 *
 * @author FirstName LastName
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
     * <p>
     * Par exemple :
     * <ul>
     *   <li>USER_CREATED : un utilisateur a été créé</li>
     *   <li>PASSWORD_RESET : un utilisateur a réinitialisé son mot de passe</li>
     * </ul>
     * </p>
     */
    private EventType eventType;

    /**
     * Données associées à l'événement.
     * <p>
     * Cette carte peut contenir n'importe quelle information pertinente
     * au traitement ou à la notification des autres composants.
     * <ul>
     *   <li>UUID de l'utilisateur</li>
     *   <li>Email</li>
     *   <li>Horodatage</li>
     * </ul>
     * </p>
     */
    private Map<String, ?> data;
}