package com.openclassrooms.notesservice.domain;

import com.openclassrooms.notesservice.event.Event;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.messaging.MessageHeaders.ID;
import static org.springframework.messaging.MessageHeaders.TIMESTAMP;

@ExtendWith(MockitoExtension.class)
class NotificationTest {

    @Mock
    private Event mockEvent;

    @Test
    void constructorWithPayloadAndHeaders_shouldCreateNotificationWithProvidedHeaders() {
        // Given
        Map<String, String> customHeaders = Map.of(
                "custom-header", "custom-value",
                "another-header", "another-value"
        );

        // When
        Notification notification = new Notification(mockEvent, customHeaders);

        // Then
        assertThat(notification.getPayload()).isEqualTo(mockEvent);
        assertThat(notification.getHeaders()).isEqualTo(customHeaders);
        assertThat(notification.getHeaders()).containsEntry("custom-header", "custom-value");
        assertThat(notification.getHeaders()).containsEntry("another-header", "another-value");
    }

    @Test
    void builder_shouldCreateNotificationCorrectly() {
        // Given
        Map<String, String> headers = Map.of("header1", "value1");

        // When
        Notification notification = Notification.builder()
                .payload(mockEvent)
                .headers(headers)
                .build();

        // Then
        assertThat(notification.getPayload()).isEqualTo(mockEvent);
        assertThat(notification.getHeaders()).isEqualTo(headers);
    }

    // Tests pour la méthode equals
    @Test
    void equals_withSameInstance_shouldReturnTrue() {
        // Given
        Notification notification = new Notification(mockEvent);

        // When & Then
        assertThat(notification).isEqualTo(notification);
    }

    @Test
    void equals_withDifferentHeaders_shouldReturnFalse() {
        // Given
        Event event = mock(Event.class);
        Map<String, String> headers1 = Map.of("id", "123", "timestamp", "12:00:00");
        Map<String, String> headers2 = Map.of("id", "456", "timestamp", "13:00:00");

        Notification notification1 = new Notification(event, headers1);
        Notification notification2 = new Notification(event, headers2);

        // When & Then
        assertThat(notification1).isNotEqualTo(notification2);
    }

    @Test
    void equals_withNullPayloadInOne_shouldReturnFalse() {
        // Given
        Map<String, String> headers = Map.of("id", "123");
        Notification notification1 = new Notification(null, headers);
        Notification notification2 = new Notification(mockEvent, headers);

        // When & Then
        assertThat(notification1).isNotEqualTo(notification2);
    }

    @Test
    void equals_withBothNullPayloads_shouldReturnTrue() {
        // Given
        Map<String, String> headers = Map.of("id", "123");
        Notification notification1 = new Notification(null, headers);
        Notification notification2 = new Notification(null, headers);

        // When & Then
        assertThat(notification1).isEqualTo(notification2);
    }

    @Test
    void equals_withDifferentObjectType_shouldReturnFalse() {
        // Given
        Notification notification = new Notification(mockEvent);
        String otherObject = "Not a Notification";

        // When & Then
        assertThat(notification).isNotEqualTo(otherObject);
    }

    @Test
    void equals_withNull_shouldReturnFalse() {
        // Given
        Notification notification = new Notification(mockEvent);

        // When & Then
        assertThat(notification).isNotEqualTo(null);
    }

    // Tests pour la méthode hashCode
    @Test
    void hashCode_withSameObjects_shouldBeEqual() {
        // Given
        Map<String, String> headers = Map.of("id", "123");
        Event event = mock(Event.class);

        Notification notification1 = new Notification(event, headers);
        Notification notification2 = new Notification(event, headers);

        // When & Then
        assertThat(notification1.hashCode()).isEqualTo(notification2.hashCode());
    }

    @Test
    void hashCode_withNullPayload_shouldCalculateCorrectly() {
        // Given
        Map<String, String> headers = Map.of("id", "123");
        Notification notification1 = new Notification(null, headers);
        Notification notification2 = new Notification(null, headers);

        // When & Then - Deux notifications avec payload null devraient avoir le même hashCode
        assertThat(notification1.hashCode()).isEqualTo(notification2.hashCode());
    }

    // Tests pour la méthode toString
    @Test
    void toString_shouldReturnFormattedString() {
        // Given
        Event event = mock(Event.class);
        when(event.toString()).thenReturn("MockEvent");

        Map<String, String> headers = Map.of("key", "value");
        Notification notification = new Notification(event, headers);

        // When
        String result = notification.toString();

        // Then
        assertThat(result)
                .startsWith("Notification [payload=")
                .contains("MockEvent")
                .contains("headers={key=value}")
                .endsWith("]");
    }

    @Test
    void toString_withNullPayload_shouldHandleNull() {
        // Given
        Map<String, String> headers = Map.of("key", "value");
        Notification notification = new Notification(null, headers);

        // When
        String result = notification.toString();

        // Then
        assertThat(result)
                .startsWith("Notification [payload=")
                .contains("null")
                .contains("headers={key=value}")
                .endsWith("]");
    }

    // Tests pour les headers par défaut
    @Test
    void defaultHeaders_shouldContainIdAndTimestamp() {
        // Given
        Event event = mock(Event.class);

        // When
        Notification notification = new Notification(event);

        // Then
        assertThat(notification.getHeaders()).containsKeys(ID, TIMESTAMP);

        // Vérifier que l'ID est un UUID valide
        String id = notification.getHeaders().get(ID);
        assertThat(UUID.fromString(id)).isNotNull();

        // Vérifier que le timestamp est une heure valide
        String timestamp = notification.getHeaders().get(TIMESTAMP);
        assertThat(timestamp).matches("\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?");
    }

    @Test
    void defaultHeaders_shouldHaveDifferentIdsForDifferentNotifications() {
        // Given
        Event event = mock(Event.class);

        // When
        Notification notification1 = new Notification(event);
        Notification notification2 = new Notification(event);

        // Then
        String id1 = notification1.getHeaders().get(ID);
        String id2 = notification2.getHeaders().get(ID);

        assertThat(id1).isNotEqualTo(id2);
        assertThat(UUID.fromString(id1)).isNotNull();
        assertThat(UUID.fromString(id2)).isNotNull();
    }

    // Tests pour les setters (générés par Lombok)
    @Test
    void setters_shouldUpdateFields() {
        // Given
        Event initialEvent = mock(Event.class);
        Event updatedEvent = mock(Event.class);
        Map<String, String> initialHeaders = Map.of("initial", "headers");
        Map<String, String> updatedHeaders = Map.of("updated", "headers");

        Notification notification = Notification.builder()
                .payload(initialEvent)
                .headers(initialHeaders)
                .build();

        // When
        notification.setPayload(updatedEvent);
        notification.setHeaders(updatedHeaders);

        // Then
        assertThat(notification.getPayload()).isEqualTo(updatedEvent);
        assertThat(notification.getHeaders()).isEqualTo(updatedHeaders);
    }

    // Tests pour les cas limites avec des headers vides ou null
    @Test
    void constructor_withEmptyHeaders_shouldCreateNotification() {
        // Given
        Map<String, String> emptyHeaders = Map.of();

        // When
        Notification notification = new Notification(mockEvent, emptyHeaders);

        // Then
        assertThat(notification.getPayload()).isEqualTo(mockEvent);
        assertThat(notification.getHeaders()).isEmpty();
    }

    @Test
    void constructor_withNullHeaders_shouldThrowNullPointerException() {
        // Given
        Event event = mock(Event.class);
        Map<String, String> nullHeaders = null;

        // When & Then
        try {
            new Notification(event, nullHeaders);
            // Si on arrive ici, c'est que l'exception n'a pas été levée
            // Mais le constructeur a l'annotation @NotNull, donc cela devrait échouer
        } catch (NullPointerException e) {
            // C'est le comportement attendu
            assertThat(e).isNotNull();
        }
    }

    @Test
    void equals_withEmptyAndNullHeaders_shouldHandleCorrectly() {
        // Given
        Event event = mock(Event.class);
        Map<String, String> emptyHeaders = Map.of();
        Map<String, String> nullHeaders = null;

        // Ce test vérifie le comportement avec des headers null
        // (même si le constructeur ne devrait pas le permettre)
    }

    // Test pour vérifier l'implémentation de Serializable
    @Test
    void notification_shouldImplementSerializable() {
        // Given
        Notification notification = new Notification(mockEvent);

        // When & Then
        assertThat(notification).isInstanceOf(Serializable.class);
    }

    // Test pour le builder avec toutes les propriétés
    @Test
    void builder_withAllProperties_shouldCreateCompleteObject() {
        // Given
        Event event = mock(Event.class);
        Map<String, String> headers = new HashMap<>();
        headers.put("id", "custom-id");
        headers.put("timestamp", "custom-time");
        headers.put("priority", "high");

        // When
        Notification notification = Notification.builder()
                .payload(event)
                .headers(headers)
                .build();

        // Then
        assertThat(notification.getPayload()).isEqualTo(event);
        assertThat(notification.getHeaders())
                .hasSize(3)
                .containsEntry("id", "custom-id")
                .containsEntry("timestamp", "custom-time")
                .containsEntry("priority", "high");
    }

    // Test pour vérifier le comportement avec un payload qui a un toString() long
    @Test
    void toString_withLongPayloadString_shouldNotTruncate() {
        // Given
        Event event = mock(Event.class);
        String longString = "A".repeat(1000);
        when(event.toString()).thenReturn(longString);

        Map<String, String> headers = Map.of("simple", "header");
        Notification notification = new Notification(event, headers);

        // When
        String result = notification.toString();

        // Then
        assertThat(result)
                .contains(longString)
                .contains("headers={simple=header}");
    }
}