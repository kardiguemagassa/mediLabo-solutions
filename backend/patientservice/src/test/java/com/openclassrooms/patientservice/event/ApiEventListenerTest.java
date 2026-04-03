package com.openclassrooms.patientservice.event;

import com.openclassrooms.patientservice.domain.Notification;
import com.openclassrooms.patientservice.enumeration.EventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApiEventListener - Tests unitaires")
class ApiEventListenerTest {

    @Mock
    private KafkaTemplate<String, Notification> kafkaTemplate;

    @InjectMocks
    private ApiEventListener apiEventListener;

    @Nested
    @DisplayName("onApiEvent() - Tests")
    class OnApiEventTests {

        @Test
        @DisplayName("Devrait publier l'événement sur Kafka avec succès")
        void onApiEvent_shouldPublishToKafkaSuccessfully() {
            // Given
            Event event = Event.builder()
                    .eventType(EventType.PATIENT_CREATED)
                    .data(Map.of("email", "test@test.com", "name", "John Doe"))
                    .build();

            when(kafkaTemplate.send(any(Message.class)))
                    .thenReturn(CompletableFuture.completedFuture(null));

            // When
            apiEventListener.onApiEvent(event);

            // Then
            ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
            verify(kafkaTemplate, timeout(1000)).send(messageCaptor.capture());

            Message<Notification> sentMessage = messageCaptor.getValue();
            assertThat(sentMessage).isNotNull();
            assertThat(sentMessage.getHeaders().get("kafka_topic")).isEqualTo("MEDILABO_NOTIFICATIONS");
            assertThat(sentMessage.getPayload()).isNotNull();
            assertThat(sentMessage.getPayload().getPayload()).isEqualTo(event);
        }

        @Test
        @DisplayName("Devrait gérer les exceptions lors de la création du message")
        void onApiEvent_shouldHandleMessageCreationException() {
            // Given
            Event event = null;

            // When
            apiEventListener.onApiEvent(event);

            // Then - Ne devrait pas planter, juste logger l'erreur
            verify(kafkaTemplate, never()).send(any(Message.class));
        }

        @Test
        @DisplayName("Devrait créer une notification avec les bons headers")
        void onApiEvent_shouldCreateNotificationWithCorrectHeaders() {
            // Given
            Event event = Event.builder()
                    .eventType(EventType.PATIENT_DELETED)
                    .data(Map.of("patientId", "123"))
                    .build();

            when(kafkaTemplate.send(any(Message.class)))
                    .thenReturn(CompletableFuture.completedFuture(null));

            // When
            apiEventListener.onApiEvent(event);

            // Then
            ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
            verify(kafkaTemplate, timeout(1000)).send(messageCaptor.capture());

            Message<Notification> sentMessage = messageCaptor.getValue();
            Notification notification = sentMessage.getPayload();

            assertThat(notification).isNotNull();
            assertThat(notification.getPayload()).isEqualTo(event);
            assertThat(notification.getHeaders()).containsKeys("id", "timestamp");
        }

        @Test
        @DisplayName("Devrait traiter différents types d'événements")
        void onApiEvent_shouldHandleDifferentEventTypes() {
            // Given
            EventType[] eventTypes = {
                    EventType.PATIENT_CREATED,
                    EventType.PATIENT_UPDATED,
                    EventType.PATIENT_DELETED
            };

            when(kafkaTemplate.send(any(Message.class)))
                    .thenReturn(CompletableFuture.completedFuture(null));

            // When & Then
            for (EventType eventType : eventTypes) {
                Event event = Event.builder()
                        .eventType(eventType)
                        .data(Map.of("test", "data"))
                        .build();

                apiEventListener.onApiEvent(event);

                verify(kafkaTemplate, timeout(1000).atLeastOnce()).send(any(Message.class));
            }
        }
    }
}
