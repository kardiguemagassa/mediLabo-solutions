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

    /**
     * Helper pour créer un objet Message conforme à la nouvelle signature
     */
    private Message createTestMessage(String mUuid, String cId, String sUuid, String rUuid, String rEmail) {
        return Message.builder()
                .messageUuid(mUuid)
                .conversationId(cId)
                .senderUuid(sUuid)
                .senderName("Dr. Jean Dupont")
                .senderEmail("jean.dupont@medilabo.fr")
                .senderImageUrl("https://example.com/avatar.jpg")
                .senderRole("DOCTOR")
                .receiverUuid(rUuid)
                .receiverName("Marie Martin")
                .receiverEmail(rEmail)
                .receiverRole("PATIENT")
                .subject("Test Subject")
                .message("Test message content")
                .build();
    }

    @Test
    @Order(1)
    @DisplayName("Should send message successfully")
    void sendMessage_validData_savesAndReturnsMessage() {
        // Given
        Message messageToSave = createTestMessage(messageUuid, conversationId, senderUuid, receiverUuid, receiverEmail);

        // When - Appel avec 1 seul argument comme dans ton RepositoryImpl
        Message savedMessage = notificationRepository.saveMessage(messageToSave);

        // Then
        assertThat(savedMessage).isNotNull();
        assertThat(savedMessage.getMessageUuid()).isEqualTo(messageUuid);
        assertThat(savedMessage.getSenderName()).isEqualTo("Dr. Jean Dupont");
        assertThat(savedMessage.getSubject()).isEqualTo("Test Subject");
    }

    @Test
    @Order(2)
    @DisplayName("Should create message statuses for sender and receiver")
    void sendMessage_validData_createsStatusesForBothUsers() {
        // Given
        Message messageToSave = createTestMessage(messageUuid, conversationId, senderUuid, receiverUuid, receiverEmail);

        // When
        notificationRepository.saveMessage(messageToSave);

        // Then
        Integer senderUnread = notificationRepository.getUnreadCount(senderUuid);
        Integer receiverUnread = notificationRepository.getUnreadCount(receiverUuid);

        assertThat(senderUnread).isZero();
        assertThat(receiverUnread).isEqualTo(1);
    }

    // GET MESSAGES TESTS

    @Test
    @Order(3)
    @DisplayName("Should get messages for user")
    void getMessages_messagesExist_returnsList() {
        // Given
        notificationRepository.saveMessage(createTestMessage(messageUuid, conversationId, senderUuid, receiverUuid, receiverEmail));

        // When
        List<Message> messages = notificationRepository.getMessages(receiverUuid);

        // Then
        assertThat(messages).isNotEmpty();
        assertThat(messages.getFirst().getReceiverUuid()).isEqualTo(receiverUuid);
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
        notificationRepository.saveMessage(createTestMessage(messageUuid, conversationId, senderUuid, receiverUuid, receiverEmail));

        // Second message (réponse)
        Message reply = createTestMessage(UUID.randomUUID().toString(), conversationId, receiverUuid, senderUuid, "jean.dupont@medilabo.fr");
        notificationRepository.saveMessage(reply);

        // When
        List<Message> conversation = notificationRepository.getConversations(senderUuid, conversationId);

        // Then
        assertThat(conversation).hasSize(2);
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
        Message savedMessage = notificationRepository.saveMessage(createTestMessage(messageUuid, conversationId, senderUuid, receiverUuid, receiverEmail));

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
        Message savedMessage = notificationRepository.saveMessage(createTestMessage(messageUuid, conversationId, senderUuid, receiverUuid, receiverEmail));

        // When
        String newStatus = notificationRepository.updateMessageStatus(receiverUuid, savedMessage.getMessageId(), "READ");

        // Then
        assertThat(newStatus).isEqualTo("READ");
        assertThat(notificationRepository.getMessageStatus(receiverUuid, savedMessage.getMessageId())).isEqualTo("READ");
    }

    // CONVERSATION EXISTS TESTS

    @Test
    @Order(10)
    @DisplayName("Should return true when conversation exists")
    void conversationExists_exists_returnsTrue() {
        // Given
        Message message = createTestMessage(messageUuid, conversationId, senderUuid, receiverUuid, receiverEmail);
        notificationRepository.saveMessage(message);

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
        Message message = createTestMessage(messageUuid, conversationId, senderUuid, receiverUuid, receiverEmail);
        notificationRepository.saveMessage(message);

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
        notificationRepository.saveMessage(createTestMessage(messageUuid, conversationId, senderUuid, receiverUuid, receiverEmail));

        notificationRepository.saveMessage(createTestMessage(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                senderUuid,
                receiverUuid,
                receiverEmail));

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
        Message savedMessage = notificationRepository.saveMessage(
                createTestMessage(messageUuid, conversationId, senderUuid, receiverUuid, receiverEmail)
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
        notificationRepository.saveMessage(createTestMessage(messageUuid, conversationId, senderUuid, receiverUuid, receiverEmail));

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
        notificationRepository.saveMessage(createTestMessage(messageUuid, conversationId, senderUuid, receiverUuid, receiverEmail));

        // When - Second message in same conversation (Inversion sender/receiver pour simuler une réponse)
        Message reply = createTestMessage(UUID.randomUUID().toString(), conversationId, receiverUuid, senderUuid, "sender@email.com");
        notificationRepository.saveMessage(reply);

        // Then
        Integer messageCount = jdbcClient.sql("SELECT message_count FROM conversations WHERE conversation_uuid = :convId")
                .param("convId", conversationId)
                .query(Integer.class)
                .single();

        assertThat(messageCount).isEqualTo(2);
    }
}