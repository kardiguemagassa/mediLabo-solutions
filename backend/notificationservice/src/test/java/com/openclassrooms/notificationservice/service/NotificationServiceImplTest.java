package com.openclassrooms.notificationservice.service;

import com.openclassrooms.notificationservice.client.AuthServerClient;
import com.openclassrooms.notificationservice.dto.UserInfo;
import com.openclassrooms.notificationservice.exception.ApiException;
import com.openclassrooms.notificationservice.model.Message;
import com.openclassrooms.notificationservice.repository.NotificationRepository;
import com.openclassrooms.notificationservice.service.implementation.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour NotificationServiceImpl.
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-09
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationServiceImpl Tests")
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private AuthServerClient authServerClient;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private UserInfo senderInfo;
    private UserInfo receiverInfo;
    private Message expectedMessage;

    @BeforeEach
    void setUp() {
        senderInfo = UserInfo.builder()
                .userUuid("sender-uuid-123")
                .firstName("Jean")
                .lastName("Dupont")
                .email("jean.dupont@medilabo.fr")
                .imageUrl("https://example.com/jean.jpg")
                .role("DOCTOR")
                .build();

        receiverInfo = UserInfo.builder()
                .userUuid("receiver-uuid-456")
                .firstName("Marie")
                .lastName("Martin")
                .email("marie.martin@patient.fr")
                .imageUrl("https://example.com/marie.jpg")
                .role("PATIENT")
                .build();

        expectedMessage = Message.builder()
                .messageId(1L)
                .messageUuid("msg-uuid-789")
                .conversationId("conv-uuid-101")
                .senderUuid(senderInfo.getUserUuid())
                .senderName(senderInfo.getName())
                .senderEmail(senderInfo.getEmail())
                .receiverUuid(receiverInfo.getUserUuid())
                .receiverName(receiverInfo.getName())
                .receiverEmail(receiverInfo.getEmail())
                .subject("Résultats d'analyses")
                .message("Vos résultats sont disponibles.")
                .status("READ")
                .build();
    }

    @Nested
    @DisplayName("sendMessage Tests")
    class SendMessageTests {

        @Test
        @DisplayName("Devrait envoyer un message avec succès - nouvelle conversation")
        void shouldSendMessageSuccessfully_NewConversation() throws ExecutionException, InterruptedException {
            // Given
            when(authServerClient.getUserByEmail(receiverInfo.getEmail()))
                    .thenReturn(CompletableFuture.completedFuture(Optional.of(receiverInfo)));
            when(notificationRepository.conversationExists(anyString(), anyString()))
                    .thenReturn(false);
            when(notificationRepository.sendMessage(
                    anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
                    anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(expectedMessage);

            // When
            CompletableFuture<Message> result = notificationService.sendMessage(
                    senderInfo.getUserUuid(),
                    senderInfo.getName(),
                    senderInfo.getEmail(),
                    senderInfo.getImageUrl(),
                    senderInfo.getRole(),
                    receiverInfo.getEmail(),
                    "Résultats d'analyses",
                    "Vos résultats sont disponibles."
            );

            Message message = result.get();

            // Then
            assertThat(message).isNotNull();
            assertThat(message.getSenderUuid()).isEqualTo(senderInfo.getUserUuid());
            assertThat(message.getReceiverUuid()).isEqualTo(receiverInfo.getUserUuid());
            assertThat(message.getSubject()).isEqualTo("Résultats d'analyses");

            verify(authServerClient).getUserByEmail(receiverInfo.getEmail());
            verify(notificationRepository).conversationExists(senderInfo.getUserUuid(), receiverInfo.getEmail());
            verify(notificationRepository).sendMessage(
                    anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
                    anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("Devrait envoyer un message dans une conversation existante")
        void shouldSendMessageSuccessfully_ExistingConversation() throws ExecutionException, InterruptedException {
            // Given
            String existingConversationId = "existing-conv-123";

            when(authServerClient.getUserByEmail(receiverInfo.getEmail()))
                    .thenReturn(CompletableFuture.completedFuture(Optional.of(receiverInfo)));
            when(notificationRepository.conversationExists(anyString(), anyString()))
                    .thenReturn(true);
            when(notificationRepository.getConversationId(anyString(), anyString()))
                    .thenReturn(existingConversationId);
            when(notificationRepository.sendMessage(
                    anyString(), eq(existingConversationId), anyString(), anyString(), anyString(), anyString(), anyString(),
                    anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(expectedMessage);

            // When
            CompletableFuture<Message> result = notificationService.sendMessage(
                    senderInfo.getUserUuid(),
                    senderInfo.getName(),
                    senderInfo.getEmail(),
                    senderInfo.getImageUrl(),
                    senderInfo.getRole(),
                    receiverInfo.getEmail(),
                    "Résultats d'analyses",
                    "Vos résultats sont disponibles."
            );

            Message message = result.get();

            // Then
            assertThat(message).isNotNull();
            verify(notificationRepository).getConversationId(senderInfo.getUserUuid(), receiverInfo.getEmail());
        }

        @Test
        @DisplayName("Devrait échouer si le destinataire n'existe pas")
        void shouldFailWhenReceiverNotFound() {
            // Given
            when(authServerClient.getUserByEmail(anyString()))
                    .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

            // When & Then
            CompletableFuture<Message> result = notificationService.sendMessage(
                    senderInfo.getUserUuid(),
                    senderInfo.getName(),
                    senderInfo.getEmail(),
                    senderInfo.getImageUrl(),
                    senderInfo.getRole(),
                    "unknown@email.com",
                    "Sujet",
                    "Message"
            );

            assertThatThrownBy(result::get)
                    .hasCauseInstanceOf(ApiException.class)
                    .hasMessageContaining("Utilisateur introuvable");
        }
    }

    @Nested
    @DisplayName("getMessages Tests")
    class GetMessagesTests {

        @Test
        @DisplayName("Devrait récupérer tous les messages d'un utilisateur")
        void shouldGetAllMessagesForUser() {
            // Given
            List<Message> expectedMessages = List.of(expectedMessage);
            when(notificationRepository.getMessages(senderInfo.getUserUuid()))
                    .thenReturn(expectedMessages);

            // When
            List<Message> messages = notificationService.getMessages(senderInfo.getUserUuid());

            // Then
            assertThat(messages).hasSize(1);
            assertThat(messages.get(0).getMessageUuid()).isEqualTo(expectedMessage.getMessageUuid());
            verify(notificationRepository).getMessages(senderInfo.getUserUuid());
        }

        @Test
        @DisplayName("Devrait retourner une liste vide si aucun message")
        void shouldReturnEmptyListWhenNoMessages() {
            // Given
            when(notificationRepository.getMessages(anyString()))
                    .thenReturn(List.of());

            // When
            List<Message> messages = notificationService.getMessages("user-without-messages");

            // Then
            assertThat(messages).isEmpty();
        }
    }

    @Nested
    @DisplayName("getConversation Tests")
    class GetConversationTests {

        @Test
        @DisplayName("Devrait récupérer une conversation et marquer les messages comme lus")
        void shouldGetConversationAndMarkAsRead() {
            // Given
            Message unreadMessage = Message.builder()
                    .messageId(2L)
                    .messageUuid("msg-uuid-unread")
                    .status("UNREAD")
                    .build();

            when(notificationRepository.getConversations(anyString(), anyString()))
                    .thenReturn(List.of(unreadMessage));
            when(notificationRepository.updateMessageStatus(anyString(), anyLong(), eq("READ")))
                    .thenReturn("READ");

            // When
            List<Message> conversation = notificationService.getConversation(
                    senderInfo.getUserUuid(),
                    "conv-123"
            );

            // Then
            assertThat(conversation).hasSize(1);
            assertThat(conversation.get(0).getStatus()).isEqualTo("READ");
            verify(notificationRepository).updateMessageStatus(senderInfo.getUserUuid(), 2L, "READ");
        }
    }

    @Nested
    @DisplayName("getUnreadCount Tests")
    class GetUnreadCountTests {

        @Test
        @DisplayName("Devrait retourner le nombre de messages non lus")
        void shouldReturnUnreadCount() {
            // Given
            when(notificationRepository.getUnreadCount(senderInfo.getUserUuid()))
                    .thenReturn(5);

            // When
            Integer unreadCount = notificationService.getUnreadCount(senderInfo.getUserUuid());

            // Then
            assertThat(unreadCount).isEqualTo(5);
        }

        @Test
        @DisplayName("Devrait retourner 0 si aucun message non lu")
        void shouldReturnZeroWhenNoUnreadMessages() {
            // Given
            when(notificationRepository.getUnreadCount(anyString()))
                    .thenReturn(0);

            // When
            Integer unreadCount = notificationService.getUnreadCount("user-123");

            // Then
            assertThat(unreadCount).isZero();
        }
    }
}