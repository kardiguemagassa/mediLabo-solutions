package com.openclassrooms.notificationservice.repository;

import com.openclassrooms.notificationservice.model.Message;
import com.openclassrooms.notificationservice.repository.implementation.NotificationRepositoryImpl;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests d'intégration pour NotificationRepository avec PostgreSQL (TestContainers).
 * Utilise @DataJdbcTest pour charger uniquement les composants JDBC (pas Email, Kafka, etc.)
 *
 * @author Kardigué MAGASSA
 */
@DataJdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Import({NotificationRepositoryImpl.class})
@ActiveProfiles("test")
@DisplayName("NotificationRepository Integration Tests")
class NotificationRepositoryIT {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private JdbcClient jdbcClient;

    private String messageUuid;
    private String conversationId;
    private String senderUuid;
    private String receiverUuid;
    private String receiverEmail;

    @BeforeEach
    void setUp() {
        // Clean up before each test (order matters due to FK)
        jdbcClient.sql("DELETE FROM message_statuses").update();
        jdbcClient.sql("DELETE FROM messages").update();
        jdbcClient.sql("DELETE FROM conversations").update();

        messageUuid = UUID.randomUUID().toString();
        conversationId = UUID.randomUUID().toString();
        senderUuid = "sender-uuid-" + System.currentTimeMillis();
        receiverUuid = "receiver-uuid-" + System.currentTimeMillis();
        receiverEmail = "receiver-" + System.currentTimeMillis() + "@email.com";
    }

    //SEND MESSAGE TESTS

    @Test
    @Order(1)
    @DisplayName("Should send message successfully")
    void sendMessage_validData_savesAndReturnsMessage() {
        // When
        Message savedMessage = notificationRepository.sendMessage(
                messageUuid,
                conversationId,
                senderUuid,
                "Dr. Jean Dupont",
                "jean.dupont@medilabo.fr",
                "https://example.com/avatar.jpg",
                "DOCTOR",
                receiverUuid,
                "Marie Martin",
                receiverEmail,
                null,
                "PATIENT",
                "Test Subject",
                "Test message content"
        );

        // Then
        assertThat(savedMessage).isNotNull();
        assertThat(savedMessage.getMessageUuid()).isEqualTo(messageUuid);
        assertThat(savedMessage.getConversationId()).isEqualTo(conversationId);
        assertThat(savedMessage.getSenderName()).isEqualTo("Dr. Jean Dupont");
        assertThat(savedMessage.getReceiverEmail()).isEqualTo(receiverEmail);
        assertThat(savedMessage.getSubject()).isEqualTo("Test Subject");
        assertThat(savedMessage.getStatus()).isEqualTo("READ"); // Sender sees as READ
    }

    @Test
    @Order(2)
    @DisplayName("Should create message statuses for sender and receiver")
    void sendMessage_validData_createsStatusesForBothUsers() {
        // When
        notificationRepository.sendMessage(
                messageUuid, conversationId, senderUuid, "Sender", "sender@email.com",
                null, "DOCTOR", receiverUuid, "Receiver", receiverEmail, null, "PATIENT",
                "Subject", "Message"
        );

        // Then - Verify statuses created
        Integer senderUnread = notificationRepository.getUnreadCount(senderUuid);
        Integer receiverUnread = notificationRepository.getUnreadCount(receiverUuid);

        assertThat(senderUnread).isZero(); // Sender has READ status
        assertThat(receiverUnread).isEqualTo(1); // Receiver has UNREAD status
    }

    // GET MESSAGES TESTS

    @Test
    @Order(3)
    @DisplayName("Should get messages for user")
    void getMessages_messagesExist_returnsList() {
        // Given
        notificationRepository.sendMessage(
                messageUuid, conversationId, senderUuid, "Sender", "sender@email.com",
                null, "DOCTOR", receiverUuid, "Receiver", receiverEmail, null, "PATIENT",
                "Subject 1", "Message 1"
        );

        // When - Get messages where user is receiver
        List<Message> messages = notificationRepository.getMessages(receiverUuid);

        // Then
        assertThat(messages).isNotEmpty();
        assertThat(messages.get(0).getReceiverUuid()).isEqualTo(receiverUuid);
    }

    @Test
    @Order(4)
    @DisplayName("Should return empty list when no messages")
    void getMessages_noMessages_returnsEmptyList() {
        // When
        List<Message> messages = notificationRepository.getMessages("non-existent-user");

        // Then
        assertThat(messages).isEmpty();
    }

    // GET CONVERSATIONS TESTS

    @Test
    @Order(5)
    @DisplayName("Should get conversation messages")
    void getConversations_conversationExists_returnsList() {
        // Given
        notificationRepository.sendMessage(
                messageUuid, conversationId, senderUuid, "Sender", "sender@email.com",
                null, "DOCTOR", receiverUuid, "Receiver", receiverEmail, null, "PATIENT",
                "Subject", "First message"
        );

        // Add reply in same conversation
        notificationRepository.sendMessage(
                UUID.randomUUID().toString(), conversationId, receiverUuid, "Receiver", receiverEmail,
                null, "PATIENT", senderUuid, "Sender", "sender@email.com", null, "DOCTOR",
                "Re: Subject", "Reply message"
        );

        // When
        List<Message> conversation = notificationRepository.getConversations(senderUuid, conversationId);

        // Then
        assertThat(conversation).hasSize(2);
        assertThat(conversation).allMatch(m -> m.getConversationId().equals(conversationId));
    }

    @Test
    @Order(6)
    @DisplayName("Should return empty list for non-existent conversation")
    void getConversations_noConversation_returnsEmptyList() {
        // When
        List<Message> conversation = notificationRepository.getConversations("user", "non-existent-conv");

        // Then
        assertThat(conversation).isEmpty();
    }

    // MESSAGE STATUS TESTS

    @Test
    @Order(7)
    @DisplayName("Should get message status for receiver (UNREAD)")
    void getMessageStatus_receiverMessage_returnsUnread() {
        // Given
        Message savedMessage = notificationRepository.sendMessage(
                messageUuid, conversationId, senderUuid, "Sender", "sender@email.com",
                null, "DOCTOR", receiverUuid, "Receiver", receiverEmail, null, "PATIENT",
                "Subject", "Message"
        );

        // When
        String status = notificationRepository.getMessageStatus(receiverUuid, savedMessage.getMessageId());

        // Then
        assertThat(status).isEqualTo("UNREAD");
    }

    @Test
    @Order(8)
    @DisplayName("Should return UNREAD when message not found")
    void getMessageStatus_messageNotFound_returnsUnread() {
        // When
        String status = notificationRepository.getMessageStatus("user", 99999L);

        // Then
        assertThat(status).isEqualTo("UNREAD");
    }

    @Test
    @Order(9)
    @DisplayName("Should update message status to READ")
    void updateMessageStatus_validData_updatesAndReturnsStatus() {
        // Given
        Message savedMessage = notificationRepository.sendMessage(
                messageUuid, conversationId, senderUuid, "Sender", "sender@email.com",
                null, "DOCTOR", receiverUuid, "Receiver", receiverEmail, null, "PATIENT",
                "Subject", "Message"
        );

        // Verify initial status is UNREAD
        String initialStatus = notificationRepository.getMessageStatus(receiverUuid, savedMessage.getMessageId());
        assertThat(initialStatus).isEqualTo("UNREAD");

        // When
        String newStatus = notificationRepository.updateMessageStatus(receiverUuid, savedMessage.getMessageId(), "READ");

        // Then
        assertThat(newStatus).isEqualTo("READ");

        // Verify status changed
        String currentStatus = notificationRepository.getMessageStatus(receiverUuid, savedMessage.getMessageId());
        assertThat(currentStatus).isEqualTo("READ");
    }

    // CONVERSATION EXISTS TESTS

    @Test
    @Order(10)
    @DisplayName("Should return true when conversation exists")
    void conversationExists_exists_returnsTrue() {
        // Given
        notificationRepository.sendMessage(
                messageUuid, conversationId, senderUuid, "Sender", "sender@email.com",
                null, "DOCTOR", receiverUuid, "Receiver", receiverEmail, null, "PATIENT",
                "Subject", "Message"
        );

        // When
        Boolean exists = notificationRepository.conversationExists(senderUuid, receiverEmail);

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @Order(11)
    @DisplayName("Should return false when conversation does not exist")
    void conversationExists_notExists_returnsFalse() {
        // When
        Boolean exists = notificationRepository.conversationExists("unknown-user", "unknown@email.com");

        // Then
        assertThat(exists).isFalse();
    }

    // GET CONVERSATION ID TESTS

    @Test
    @Order(12)
    @DisplayName("Should get conversation ID")
    void getConversationId_conversationExists_returnsId() {
        // Given
        notificationRepository.sendMessage(
                messageUuid, conversationId, senderUuid, "Sender", "sender@email.com",
                null, "DOCTOR", receiverUuid, "Receiver", receiverEmail, null, "PATIENT",
                "Subject", "Message"
        );

        // When
        String foundConversationId = notificationRepository.getConversationId(senderUuid, receiverEmail);

        // Then
        assertThat(foundConversationId).isEqualTo(conversationId);
    }

    @Test
    @Order(13)
    @DisplayName("Should return null when conversation does not exist")
    void getConversationId_conversationNotExists_returnsNull() {
        // When
        String foundConversationId = notificationRepository.getConversationId("unknown-user", "unknown@email.com");

        // Then
        assertThat(foundConversationId).isNull();
    }

    // UNREAD COUNT TESTS

    @Test
    @Order(14)
    @DisplayName("Should count unread messages")
    void getUnreadCount_unreadMessagesExist_returnsCount() {
        // Given - Send 2 messages to same receiver
        notificationRepository.sendMessage(
                messageUuid, conversationId, senderUuid, "Sender", "sender@email.com",
                null, "DOCTOR", receiverUuid, "Receiver", receiverEmail, null, "PATIENT",
                "Subject 1", "Message 1"
        );

        notificationRepository.sendMessage(
                UUID.randomUUID().toString(), UUID.randomUUID().toString(), senderUuid, "Sender", "sender@email.com",
                null, "DOCTOR", receiverUuid, "Receiver", receiverEmail, null, "PATIENT",
                "Subject 2", "Message 2"
        );

        // When
        Integer unreadCount = notificationRepository.getUnreadCount(receiverUuid);

        // Then
        assertThat(unreadCount).isEqualTo(2);
    }

    @Test
    @Order(15)
    @DisplayName("Should return zero when no unread messages")
    void getUnreadCount_noUnreadMessages_returnsZero() {
        // When
        Integer unreadCount = notificationRepository.getUnreadCount("non-existent-user");

        // Then
        assertThat(unreadCount).isZero();
    }

    @Test
    @Order(16)
    @DisplayName("Should decrease unread count after marking as read")
    void getUnreadCount_afterMarkingAsRead_decreases() {
        // Given
        Message savedMessage = notificationRepository.sendMessage(
                messageUuid, conversationId, senderUuid, "Sender", "sender@email.com",
                null, "DOCTOR", receiverUuid, "Receiver", receiverEmail, null, "PATIENT",
                "Subject", "Message"
        );

        Integer initialCount = notificationRepository.getUnreadCount(receiverUuid);
        assertThat(initialCount).isEqualTo(1);

        // When
        notificationRepository.updateMessageStatus(receiverUuid, savedMessage.getMessageId(), "READ");

        // Then
        Integer newCount = notificationRepository.getUnreadCount(receiverUuid);
        assertThat(newCount).isZero();
    }

    // CONVERSATION TABLE TESTS

    @Test
    @Order(17)
    @DisplayName("Should create conversation entry when sending first message")
    void sendMessage_firstMessage_createsConversation() {
        // When
        notificationRepository.sendMessage(
                messageUuid, conversationId, senderUuid, "Sender", "sender@email.com",
                null, "DOCTOR", receiverUuid, "Receiver", receiverEmail, null, "PATIENT",
                "Subject", "Message"
        );

        // Then - Verify conversation was created
        Integer count = jdbcClient.sql("SELECT COUNT(*) FROM conversations WHERE conversation_uuid = :convId")
                .param("convId", conversationId)
                .query(Integer.class)
                .single();

        assertThat(count).isEqualTo(1);
    }

    @Test
    @Order(18)
    @DisplayName("Should increment message count on subsequent messages")
    void sendMessage_subsequentMessages_incrementsCount() {
        // Given - First message
        notificationRepository.sendMessage(
                messageUuid, conversationId, senderUuid, "Sender", "sender@email.com",
                null, "DOCTOR", receiverUuid, "Receiver", receiverEmail, null, "PATIENT",
                "Subject", "First message"
        );

        // When - Second message in same conversation
        notificationRepository.sendMessage(
                UUID.randomUUID().toString(), conversationId, receiverUuid, "Receiver", receiverEmail,
                null, "PATIENT", senderUuid, "Sender", "sender@email.com", null, "DOCTOR",
                "Re: Subject", "Reply"
        );

        // Then
        Integer messageCount = jdbcClient.sql("SELECT message_count FROM conversations WHERE conversation_uuid = :convId")
                .param("convId", conversationId)
                .query(Integer.class)
                .single();

        assertThat(messageCount).isEqualTo(2);
    }
}