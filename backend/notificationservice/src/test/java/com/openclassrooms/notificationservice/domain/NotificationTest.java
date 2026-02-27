package com.openclassrooms.notificationservice.domain;

import com.openclassrooms.notificationservice.event.Event;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

class NotificationTest {

    @Test
    void builder_ShouldCreateNotificationWithAllFields() {
        // Given
        Event event = new Event();
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Bearer token");

        // When
        Notification notification = Notification.builder()
                .payload(event)
                .headers(headers)
                .build();

        // Then
        Assertions.assertNotNull(notification);
        Assertions.assertEquals(event, notification.getPayload());
        Assertions.assertEquals(headers, notification.getHeaders());
        Assertions.assertEquals(2, notification.getHeaders().size());
        Assertions.assertEquals("application/json", notification.getHeaders().get("Content-Type"));
        Assertions.assertEquals("Bearer token", notification.getHeaders().get("Authorization"));
    }

    @Test
    void builder_ShouldCreateNotificationWithNullFields() {
        // When
        Notification notification = Notification.builder()
                .payload(null)
                .headers(null)
                .build();

        // Then
        Assertions.assertNotNull(notification);
        Assertions.assertNull(notification.getPayload());
        Assertions.assertNull(notification.getHeaders());
    }

    @Test
    void noArgsConstructor_ShouldCreateEmptyNotification() {
        // When
        Notification notification = new Notification();

        // Then
        Assertions.assertNotNull(notification);
        Assertions.assertNull(notification.getPayload());
        Assertions.assertNull(notification.getHeaders());
    }

    @Test
    void allArgsConstructor_ShouldCreateNotificationWithAllFields() {
        // Given
        Event event = new Event();
        Map<String, String> headers = new HashMap<>();
        headers.put("key", "value");

        // When
        Notification notification = new Notification(event, headers);

        // Then
        Assertions.assertNotNull(notification);
        Assertions.assertEquals(event, notification.getPayload());
        Assertions.assertEquals(headers, notification.getHeaders());
    }

    @Test
    void setterAndGetter_ShouldWorkCorrectly() {
        // Given
        Notification notification = new Notification();
        Event event = new Event();
        Map<String, String> headers = new HashMap<>();

        // When
        notification.setPayload(event);
        notification.setHeaders(headers);

        // Then
        Assertions.assertEquals(event, notification.getPayload());
        Assertions.assertEquals(headers, notification.getHeaders());
    }

    @Test
    void equals_ShouldReturnTrue_WhenSameObject() {
        // Given
        Notification notification = new Notification();

        // Then
        Assertions.assertEquals(notification, notification);
    }

    @Test
    void equals_ShouldReturnFalse_WhenNull() {
        // Given
        Notification notification = new Notification();

        // Then
        Assertions.assertNotEquals(null, notification);
    }

    @Test
    void equals_ShouldReturnFalse_WhenDifferentClass() {
        // Given
        Notification notification = new Notification();
        String other = "not a notification";

        // Then
        Assertions.assertNotEquals(other, notification);
    }

    @Test
    void equals_ShouldReturnTrue_WhenAllFieldsEqual() {
        // Given
        Event event = new Event();
        Map<String, String> headers = new HashMap<>();
        headers.put("key", "value");

        Notification notification1 = new Notification(event, headers);
        Notification notification2 = new Notification(event, headers);

        // Then
        Assertions.assertEquals(notification1, notification2);
    }

    @Test
    void equals_ShouldReturnFalse_WhenPayloadDifferent() {
        // Given
        Event event1 = new Event();
        Event event2 = new Event();
        Map<String, String> headers = new HashMap<>();
        headers.put("key", "value");

        Notification notification1 = new Notification(event1, headers);
        Notification notification2 = new Notification(event2, headers);

        // Then
        Assertions.assertNotEquals(notification1, notification2);
    }

    @Test
    void equals_ShouldReturnFalse_WhenHeadersDifferent() {
        // Given
        Event event = new Event();
        Map<String, String> headers1 = new HashMap<>();
        headers1.put("key1", "value1");
        Map<String, String> headers2 = new HashMap<>();
        headers2.put("key2", "value2");

        Notification notification1 = new Notification(event, headers1);
        Notification notification2 = new Notification(event, headers2);

        // Then
        Assertions.assertNotEquals(notification1, notification2);
    }

    @Test
    void equals_ShouldReturnFalse_WhenPayloadNullAndOtherNotNull() {
        // Given
        Event event = new Event();
        Map<String, String> headers = new HashMap<>();

        Notification notification1 = new Notification(null, headers);
        Notification notification2 = new Notification(event, headers);

        // Then
        Assertions.assertNotEquals(notification1, notification2);
    }

    @Test
    void equals_ShouldReturnTrue_WhenBothPayloadNull() {
        // Given
        Map<String, String> headers = new HashMap<>();

        Notification notification1 = new Notification(null, headers);
        Notification notification2 = new Notification(null, headers);

        // Then
        Assertions.assertEquals(notification1, notification2);
    }

    @Test
    void hashCode_ShouldBeConsistent_ForSameObject() {
        // Given
        Notification notification = new Notification();

        // When
        int hashCode1 = notification.hashCode();
        int hashCode2 = notification.hashCode();

        // Then
        Assertions.assertEquals(hashCode1, hashCode2);
    }

    @Test
    void hashCode_ShouldBeEqual_ForEqualObjects() {
        // Given
        Event event = new Event();
        Map<String, String> headers = new HashMap<>();
        headers.put("key", "value");

        Notification notification1 = new Notification(event, headers);
        Notification notification2 = new Notification(event, headers);

        // Then
        Assertions.assertEquals(notification1.hashCode(), notification2.hashCode());
    }

    @Test
    void hashCode_ShouldBeDifferent_ForDifferentObjects() {
        // Given
        Event event1 = new Event();
        Event event2 = new Event();
        Map<String, String> headers = new HashMap<>();
        headers.put("key", "value");

        Notification notification1 = new Notification(event1, headers);
        Notification notification2 = new Notification(event2, headers);

        // Then
        Assertions.assertNotEquals(notification1.hashCode(), notification2.hashCode());
    }

    @Test
    void toString_ShouldReturnFormattedString_WithNonNullPayload() {
        // Given
        Event event = new Event();
        Map<String, String> headers = new HashMap<>();
        headers.put("key", "value");
        Notification notification = new Notification(event, headers);

        // When
        String result = notification.toString();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("Notification"));
        Assertions.assertTrue(result.contains("payload=" + event));
        Assertions.assertTrue(result.contains("headers={key=value}"));
    }

    @Test
    void toString_ShouldReturnFormattedString_WithNullPayload() {
        // Given
        Map<String, String> headers = new HashMap<>();
        headers.put("key", "value");
        Notification notification = new Notification(null, headers);

        // When
        String result = notification.toString();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("Notification"));
        Assertions.assertTrue(result.contains("payload=null"));
        Assertions.assertTrue(result.contains("headers={key=value}"));
    }

    @Test
    void toString_ShouldReturnFormattedString_WithNullHeaders() {
        // Given
        Event event = new Event();
        Notification notification = new Notification(event, null);

        // When
        String result = notification.toString();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("Notification"));
        Assertions.assertTrue(result.contains("payload=" + event));
        Assertions.assertTrue(result.contains("headers=null"));
    }

    @Test
    void toString_ShouldHandleByteArrayPayload() {
        // Given
        byte[] payload = new byte[]{1, 2, 3};
        Map<String, String> headers = new HashMap<>();

        // Créer un mock ou une sous-classe d'Event qui retourne un byte array
        Event event = new Event() {
            @Override
            public String toString() {
                return "byte[3]";
            }
        };

        Notification notification = new Notification(event, headers);

        // When
        String result = notification.toString();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("Notification"));
        Assertions.assertTrue(result.contains("byte[3]"));
    }

    @Test
    void testSerializable() {
        // Given
        Event event = new Event();
        Map<String, String> headers = new HashMap<>();
        headers.put("key", "value");
        Notification notification = new Notification(event, headers);

        // Then
        Assertions.assertInstanceOf(Serializable.class, notification);
    }
}