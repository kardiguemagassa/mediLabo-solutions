package com.openclassrooms.notificationservice.repository.implementation;

import com.openclassrooms.notificationservice.exception.ApiException;
import com.openclassrooms.notificationservice.model.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour NotificationRepositoryImpl.
 * Mock de JdbcClient pour isoler les tests de la base de données.
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-27
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationRepositoryImpl Unit Tests")
class NotificationRepositoryImplTest {

    @Mock
    private JdbcClient jdbcClient;

    @Mock
    private JdbcClient.StatementSpec statementSpec;

    @Mock
    private JdbcClient.MappedQuerySpec<Message> messageMappedQuerySpec;

    @Mock
    private JdbcClient.MappedQuerySpec<String> stringMappedQuerySpec;

    @Mock
    private JdbcClient.MappedQuerySpec<Integer> integerMappedQuerySpec;

    @InjectMocks
    private NotificationRepositoryImpl notificationRepository;

    private Message testMessage;

    @BeforeEach
    void setUp() {
        testMessage = Message.builder()
                .messageId(1L)
                .messageUuid("msg-uuid-123")
                .conversationId("conv-uuid-456")
                .senderUuid("sender-uuid")
                .senderName("Dr. Jean Dupont")
                .senderEmail("jean.dupont@medilabo.fr")
                .senderRole("DOCTOR")
                .receiverUuid("receiver-uuid")
                .receiverName("Marie Martin")
                .receiverEmail("marie.martin@patient.fr")
                .receiverRole("PATIENT")
                .subject("Résultats d'analyses")
                .message("Vos résultats sont disponibles.")
                .build();
    }

    // SAVE MESSAGE

    @Nested
    @DisplayName("saveMessage Tests")
    class SaveMessageTests {

        @Test
        @DisplayName("Devrait sauvegarder un message avec succès")
        void saveMessage_validMessage_returnsMessage() {
            // Given
            when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
            when(statementSpec.paramSource(any(Message.class))).thenReturn(statementSpec);
            when(statementSpec.query(Message.class)).thenReturn(messageMappedQuerySpec);
            when(messageMappedQuerySpec.single()).thenReturn(testMessage);

            // When
            Message result = notificationRepository.saveMessage(testMessage);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getMessageUuid()).isEqualTo("msg-uuid-123");
            assertThat(result.getSenderName()).isEqualTo("Dr. Jean Dupont");
            verify(jdbcClient).sql(anyString());
        }

        @Test
        @DisplayName("Devrait lancer ApiException si aucun résultat retourné")
        void saveMessage_emptyResult_throwsApiException() {
            // Given
            when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
            when(statementSpec.paramSource(any(Message.class))).thenReturn(statementSpec);
            when(statementSpec.query(Message.class)).thenReturn(messageMappedQuerySpec);
            when(messageMappedQuerySpec.single()).thenThrow(new EmptyResultDataAccessException(1));

            // When & Then
            assertThatThrownBy(() -> notificationRepository.saveMessage(testMessage))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("Erreur lors de la création du message");
        }

        @Test
        @DisplayName("Devrait lancer ApiException en cas d'erreur technique")
        void saveMessage_technicalError_throwsApiException() {
            // Given
            when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
            when(statementSpec.paramSource(any(Message.class))).thenReturn(statementSpec);
            when(statementSpec.query(Message.class)).thenReturn(messageMappedQuerySpec);
            when(messageMappedQuerySpec.single()).thenThrow(new RuntimeException("Database connection failed"));

            // When & Then
            assertThatThrownBy(() -> notificationRepository.saveMessage(testMessage))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("Une erreur inattendue s'est produite");
        }
    }

    // GET MESSAGES

    @Nested
    @DisplayName("getMessages Tests")
    class GetMessagesTests {

        @Test
        @DisplayName("Devrait retourner la liste des messages pour un utilisateur")
        void getMessages_messagesExist_returnsList() {
            // Given
            List<Message> expectedMessages = List.of(testMessage);
            when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
            when(statementSpec.params(anyMap())).thenReturn(statementSpec);
            when(statementSpec.query(Message.class)).thenReturn(messageMappedQuerySpec);
            when(messageMappedQuerySpec.list()).thenReturn(expectedMessages);

            // When
            List<Message> result = notificationRepository.getMessages("receiver-uuid");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getMessageUuid()).isEqualTo("msg-uuid-123");
        }

        @Test
        @DisplayName("Devrait retourner une liste vide si aucun message")
        void getMessages_noMessages_returnsEmptyList() {
            // Given
            when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
            when(statementSpec.params(anyMap())).thenReturn(statementSpec);
            when(statementSpec.query(Message.class)).thenReturn(messageMappedQuerySpec);
            when(messageMappedQuerySpec.list()).thenReturn(List.of());

            // When
            List<Message> result = notificationRepository.getMessages("unknown-user");

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Devrait lancer ApiException en cas d'erreur")
        void getMessages_error_throwsApiException() {
            // Given
            when(jdbcClient.sql(anyString())).thenThrow(new RuntimeException("Database error"));

            // When & Then
            assertThatThrownBy(() -> notificationRepository.getMessages("user-uuid"))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("Une erreur s'est produite");
        }
    }

    // GET CONVERSATIONS

    @Nested
    @DisplayName("getConversations Tests")
    class GetConversationsTests {

        @Test
        @DisplayName("Devrait retourner les messages d'une conversation")
        void getConversations_conversationExists_returnsList() {
            // Given
            List<Message> expectedMessages = List.of(testMessage);
            when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
            when(statementSpec.params(anyMap())).thenReturn(statementSpec);
            when(statementSpec.query(Message.class)).thenReturn(messageMappedQuerySpec);
            when(messageMappedQuerySpec.list()).thenReturn(expectedMessages);

            // When
            List<Message> result = notificationRepository.getConversations("user-uuid", "conv-uuid-456");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getConversationId()).isEqualTo("conv-uuid-456");
        }

        @Test
        @DisplayName("Devrait retourner une liste vide si conversation inexistante")
        void getConversations_noConversation_returnsEmptyList() {
            // Given
            when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
            when(statementSpec.params(anyMap())).thenReturn(statementSpec);
            when(statementSpec.query(Message.class)).thenReturn(messageMappedQuerySpec);
            when(messageMappedQuerySpec.list()).thenReturn(List.of());

            // When
            List<Message> result = notificationRepository.getConversations("user", "non-existent");

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Devrait lancer ApiException en cas d'erreur")
        void getConversations_error_throwsApiException() {
            // Given
            when(jdbcClient.sql(anyString())).thenThrow(new RuntimeException("Database error"));

            // When & Then
            assertThatThrownBy(() -> notificationRepository.getConversations("user", "conv"))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("Une erreur s'est produite");
        }
    }

    // GET MESSAGE STATUS

    @Nested
    @DisplayName("getMessageStatus Tests")
    class GetMessageStatusTests {

        @Test
        @DisplayName("Devrait retourner le statut d'un message")
        void getMessageStatus_messageExists_returnsStatus() {
            // Given
            when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
            when(statementSpec.params(anyMap())).thenReturn(statementSpec);
            when(statementSpec.query(String.class)).thenReturn(stringMappedQuerySpec);
            when(stringMappedQuerySpec.single()).thenReturn("UNREAD");

            // When
            String result = notificationRepository.getMessageStatus("user-uuid", 1L);

            // Then
            assertThat(result).isEqualTo("UNREAD");
        }

        @Test
        @DisplayName("Devrait retourner UNREAD si message introuvable")
        void getMessageStatus_messageNotFound_returnsUnread() {
            // Given
            when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
            when(statementSpec.params(anyMap())).thenReturn(statementSpec);
            when(statementSpec.query(String.class)).thenReturn(stringMappedQuerySpec);
            when(stringMappedQuerySpec.single()).thenThrow(new EmptyResultDataAccessException(1));

            // When
            String result = notificationRepository.getMessageStatus("user-uuid", 99999L);

            // Then
            assertThat(result).isEqualTo("UNREAD");
        }

        @Test
        @DisplayName("Devrait lancer ApiException en cas d'erreur technique")
        void getMessageStatus_error_throwsApiException() {
            // Given
            when(jdbcClient.sql(anyString())).thenThrow(new RuntimeException("Database error"));

            // When & Then
            assertThatThrownBy(() -> notificationRepository.getMessageStatus("user", 1L))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("Une erreur s'est produite");
        }
    }

    // UPDATE MESSAGE STATUS

    @Nested
    @DisplayName("updateMessageStatus Tests")
    class UpdateMessageStatusTests {

        @Test
        @DisplayName("Devrait mettre à jour le statut d'un message")
        void updateMessageStatus_validData_returnsNewStatus() {
            // Given
            when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
            when(statementSpec.params(anyMap())).thenReturn(statementSpec);
            when(statementSpec.update()).thenReturn(1);

            // When
            String result = notificationRepository.updateMessageStatus("user-uuid", 1L, "READ");

            // Then
            assertThat(result).isEqualTo("READ");
            verify(statementSpec).update();
        }

        @Test
        @DisplayName("Devrait lancer ApiException en cas d'erreur")
        void updateMessageStatus_error_throwsApiException() {
            // Given
            when(jdbcClient.sql(anyString())).thenThrow(new RuntimeException("Database error"));

            // When & Then
            assertThatThrownBy(() -> notificationRepository.updateMessageStatus("user", 1L, "READ"))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("Une erreur s'est produite");
        }
    }

    // CONVERSATION EXISTS

    @Nested
    @DisplayName("conversationExists Tests")
    class ConversationExistsTests {

        @Test
        @DisplayName("Devrait retourner true si conversation existe")
        void conversationExists_exists_returnsTrue() {
            // Given
            when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
            when(statementSpec.params(anyMap())).thenReturn(statementSpec);
            when(statementSpec.query(Integer.class)).thenReturn(integerMappedQuerySpec);
            when(integerMappedQuerySpec.single()).thenReturn(1);

            // When
            Boolean result = notificationRepository.conversationExists("user-uuid", "receiver@email.com");

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Devrait retourner false si conversation n'existe pas")
        void conversationExists_notExists_returnsFalse() {
            // Given
            when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
            when(statementSpec.params(anyMap())).thenReturn(statementSpec);
            when(statementSpec.query(Integer.class)).thenReturn(integerMappedQuerySpec);
            when(integerMappedQuerySpec.single()).thenReturn(0);

            // When
            Boolean result = notificationRepository.conversationExists("user", "unknown@email.com");

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Devrait retourner false en cas d'erreur")
        void conversationExists_error_returnsFalse() {
            // Given
            when(jdbcClient.sql(anyString())).thenThrow(new RuntimeException("Database error"));

            // When
            Boolean result = notificationRepository.conversationExists("user", "email@test.com");

            // Then
            assertThat(result).isFalse();
        }
    }

    // GET CONVERSATION ID

    @Nested
    @DisplayName("getConversationId Tests")
    class GetConversationIdTests {

        @Test
        @DisplayName("Devrait retourner l'ID de conversation")
        void getConversationId_exists_returnsId() {
            // Given
            when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
            when(statementSpec.params(anyMap())).thenReturn(statementSpec);
            when(statementSpec.query(String.class)).thenReturn(stringMappedQuerySpec);
            when(stringMappedQuerySpec.single()).thenReturn("conv-uuid-456");

            // When
            String result = notificationRepository.getConversationId("user-uuid", "receiver@email.com");

            // Then
            assertThat(result).isEqualTo("conv-uuid-456");
        }

        @Test
        @DisplayName("Devrait retourner null si conversation inexistante")
        void getConversationId_notExists_returnsNull() {
            // Given
            when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
            when(statementSpec.params(anyMap())).thenReturn(statementSpec);
            when(statementSpec.query(String.class)).thenReturn(stringMappedQuerySpec);
            when(stringMappedQuerySpec.single()).thenThrow(new EmptyResultDataAccessException(1));

            // When
            String result = notificationRepository.getConversationId("user", "unknown@email.com");

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Devrait lancer ApiException en cas d'erreur technique")
        void getConversationId_error_throwsApiException() {
            // Given
            when(jdbcClient.sql(anyString())).thenThrow(new RuntimeException("Database error"));

            // When & Then
            assertThatThrownBy(() -> notificationRepository.getConversationId("user", "email@test.com"))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("Une erreur s'est produite");
        }
    }

    // GET UNREAD COUNT

    @Nested
    @DisplayName("getUnreadCount Tests")
    class GetUnreadCountTests {

        @Test
        @DisplayName("Devrait retourner le nombre de messages non lus")
        void getUnreadCount_unreadExist_returnsCount() {
            // Given
            when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
            when(statementSpec.params(anyMap())).thenReturn(statementSpec);
            when(statementSpec.query(Integer.class)).thenReturn(integerMappedQuerySpec);
            when(integerMappedQuerySpec.single()).thenReturn(5);

            // When
            Integer result = notificationRepository.getUnreadCount("user-uuid");

            // Then
            assertThat(result).isEqualTo(5);
        }

        @Test
        @DisplayName("Devrait retourner zéro si aucun message non lu")
        void getUnreadCount_noUnread_returnsZero() {
            // Given
            when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
            when(statementSpec.params(anyMap())).thenReturn(statementSpec);
            when(statementSpec.query(Integer.class)).thenReturn(integerMappedQuerySpec);
            when(integerMappedQuerySpec.single()).thenReturn(0);

            // When
            Integer result = notificationRepository.getUnreadCount("user-uuid");

            // Then
            assertThat(result).isZero();
        }

        @Test
        @DisplayName("Devrait retourner zéro en cas d'erreur")
        void getUnreadCount_error_returnsZero() {
            // Given
            when(jdbcClient.sql(anyString())).thenThrow(new RuntimeException("Database error"));

            // When
            Integer result = notificationRepository.getUnreadCount("user-uuid");

            // Then
            assertThat(result).isZero();
        }
    }
}