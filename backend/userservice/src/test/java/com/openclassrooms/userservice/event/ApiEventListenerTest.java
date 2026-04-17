package com.openclassrooms.userservice.event;

import com.openclassrooms.userservice.domain.Notification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;
import org.springframework.kafka.support.KafkaHeaders;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ApiEventListenerTest {

    @Mock
    private KafkaTemplate<String, Notification> kafkaTemplate;

    @InjectMocks
    private ApiEventListener apiEventListener;

    @Captor
    private ArgumentCaptor<Message<Notification>> messageCaptor;

    @Test
    @DisplayName("Kafka : Devrait envoyer une notification lors de la réception d'un événement")
    void onApiEvent_ShouldSendNotificationToKafka() {
        // 1. GIVEN
        Event mockEvent = mock(Event.class);

        // 2. WHEN
        apiEventListener.onApiEvent(mockEvent);

        // 3. THEN
        // Utilisation du verify de Mockito
        verify(kafkaTemplate, times(1)).send(messageCaptor.capture());

        Message<Notification> sentMessage = messageCaptor.getValue();

        // Vérification du Header en utilisant la constante Spring Kafka
        // Dans le code on importes TOPIC de KafkaHeaders
        assertThat(sentMessage.getHeaders().get(KafkaHeaders.TOPIC)).isEqualTo("MEDILABO_NOTIFICATIONS");

        // Vérification du Payload
        assertThat(sentMessage.getPayload()).isNotNull();
    }
}