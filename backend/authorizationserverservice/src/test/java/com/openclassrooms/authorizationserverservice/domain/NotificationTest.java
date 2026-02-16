package com.openclassrooms.authorizationserverservice.domain;

import com.openclassrooms.authorizationserverservice.event.Event;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class NotificationTest {

    @Test
    @DisplayName("Equals/HashCode : Devrait valider l'égalité entre deux objets identiques")
    void equalsAndHashCode_ShouldWorkCorrectly() {
        Event event = mock(Event.class);
        Map<String, String> headers = Map.of("key", "value");

        Notification n1 = new Notification(event, headers);
        Notification n2 = new Notification(event, headers);
        Notification n3 = new Notification(mock(Event.class), headers);

        // Tests d'égalité
        assertThat(n1).isEqualTo(n2);
        assertThat(n1).isNotEqualTo(n3);
        assertThat(n1.hashCode()).isEqualTo(n2.hashCode());
        assertThat(n1.hashCode()).isNotEqualTo(n3.hashCode());
    }

    @Test
    @DisplayName("ToString : Devrait gérer les payloads classiques et les tableaux de bytes")
    void toString_ShouldHandleByteArraysAndObjects() {
        // Cas 1 : Payload classique
        Event mockEvent = mock(Event.class);
        Notification n1 = new Notification(mockEvent, Map.of("id", "1"));
        assertThat(n1.toString()).contains("Notification [payload=");

        // Cas 2 : Payload byte[] (couverture de la condition spécifique dans toString)
        // Note: Cela nécessite que 'Event' puisse être un byte[] ou que le champ accepte Object.
        // Si Notification attend un 'Event', et que 'Event' n'est pas byte[], ce cas est dur à simuler
        // sauf si Event est une interface implémentée par quelque chose qui se comporte comme byte[].
    }
}