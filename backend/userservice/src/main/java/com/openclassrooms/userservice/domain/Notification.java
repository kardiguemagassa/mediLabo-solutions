package com.openclassrooms.userservice.domain;

import com.openclassrooms.userservice.event.Event;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

import java.io.Serializable;
import java.util.Map;

import static com.openclassrooms.userservice.util.UserUtils.randomUUUID;
import static java.time.LocalTime.now;
import static org.springframework.messaging.MessageHeaders.ID;
import static org.springframework.messaging.MessageHeaders.TIMESTAMP;

/**
 * Représente une notification envoyée dans l'application
 * Chaque notification contient :
 * un payload : l'événement réel à transmettre (de type Event)
 * des headers : informations supplémentaires (ID, timestamp, etc.)

 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-05-01
 */

@Builder
@Getter
@Setter
public class Notification implements Serializable {
    /** Contenu réel de la notification*/
    private Event payload;
    /** En-têtes associés à la notification (ID, timestamp, etc.)*/
    private Map<String, String> headers;

    /**
     * Constructeur simplifié : crée une notification avec des headers par défaut :
     * ID : un UUID aléatoire
     *  TIMESTAMP : l'heure actuelle
     * @param payload l'événement à transmettre (non null)
     */
    public Notification(Event payload) {
        this(payload, Map.of(ID, randomUUUID.get(), TIMESTAMP, now().toString()));
    }

    /**
     * Constructeur complet permettant de définir explicitement le payload et les headers
     * @param payload l'événement (non null)
     * @param headers les en-têtes (non null)
     */
    public Notification(@NotNull Event payload, @NotNull Map<String, String> headers) {
        this.payload = payload;
        this.headers = headers;
    }

    /**
     * Vérifie l'égalité entre cette notification et un autre objet.
     * Deux notifications sont considérées égales si :
     * Leur payload est égal
     * Leurs headers sont égaux
     * @param other l'objet à comparer
     * @return true si les objets sont égaux, false sinon
     */
    public boolean equals(@Nullable Object other) {
        boolean var10000;
        if (this != other) {
            label28: {
                if (other instanceof Notification) {
                    var that = (Notification)other;
                    if (ObjectUtils.nullSafeEquals(this.payload, that.payload) && this.headers.equals(that.headers)) {
                        break label28;
                    }
                }
                var10000 = false;
                return var10000;
            }
        }
        var10000 = true;
        return var10000;
    }

    /**
     * Calcule le hashcode basé sur le payload et les headers.
     * Utile pour l'utilisation dans des collections comme {@link java.util.HashSet} ou {@link java.util.HashMap}.
     * @return le hashcode de la notification
     */
    public int hashCode() {
        return ObjectUtils.nullSafeHash(this.payload, this.headers);
    }

    /**
     * Génère une représentation en chaîne de caractères de la notification
     * Si le payload est un tableau de bytes, indique uniquement sa taille</li>
     * Sinon, affiche le contenu du payload et les headers</li>
     * @return représentation textuelle de la notification
     */
    public String toString() {
        StringBuilder sb = new StringBuilder(this.getClass().getSimpleName());
        sb.append(" [payload=");
        Object var3 = this.payload;
        if (var3 instanceof byte[] bytes) {
            sb.append("byte[").append(bytes.length).append(']');
        } else {
            sb.append(this.payload);
        }
        sb.append(", headers=").append(this.headers).append(']');
        return sb.toString();
    }
}
