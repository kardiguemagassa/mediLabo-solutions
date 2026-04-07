package com.openclassrooms.notificationservice.repository;

import com.openclassrooms.notificationservice.model.Conversation;
import com.openclassrooms.notificationservice.model.Message;
import com.openclassrooms.notificationservice.model.MessageStatus;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@ActiveProfiles("test")
class MessageRepositoryIT {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private MessageStatusRepository messageStatusRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private TestEntityManager entityManager;

    private String senderUuid;
    private String receiverUuid;
    private String receiverEmail;
    private String conversationId;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @BeforeEach
    void setUp() {
        messageStatusRepository.deleteAll();
        messageRepository.deleteAll();
        conversationRepository.deleteAll();

        senderUuid = UUID.randomUUID().toString();        // 36 chars
        receiverUuid = UUID.randomUUID().toString();
        receiverEmail = "r" + System.currentTimeMillis() + "@email.com";
        conversationId = UUID.randomUUID().toString();
    }

    private Message createAndSaveMessage(String convId, String sUuid, String rUuid, String rEmail) {
        Message message = Message.builder()
                .messageUuid(UUID.randomUUID().toString())
                .conversationId(convId)
                .subject("Test Subject")
                .message("Test content")
                .senderUuid(sUuid)
                .senderName("Dr. Jean Dupont")
                .senderEmail("jean.dupont@medilabo.fr")
                .senderRole("DOCTOR")
                .receiverUuid(rUuid)
                .receiverName("Marie Martin")
                .receiverEmail(rEmail)
                .receiverRole("PATIENT")
                .build();

        Message saved = messageRepository.save(message);

        // Créer les statuts
        messageStatusRepository.save(MessageStatus.builder()
                .message(saved).userUuid(sUuid).messageStatus("READ").readAt(LocalDateTime.now()).build());
        messageStatusRepository.save(MessageStatus.builder()
                .message(saved).userUuid(rUuid).messageStatus("UNREAD").build());

        entityManager.flush();
        entityManager.clear();

        return saved;
    }

    @Test
    @Order(1)
    @DisplayName("Should find messages for user as sender or receiver")
    void findAllByUser_messagesExist_returnsList() {
        createAndSaveMessage(conversationId, senderUuid, receiverUuid, receiverEmail);

        List<Message> messages = messageRepository.findAllByUser(receiverUuid);

        assertThat(messages).hasSize(1);
        assertThat(messages.getFirst().getReceiverUuid()).isEqualTo(receiverUuid);
        assertThat(messages.getFirst().getStatuses()).isNotEmpty();
    }

    @Test
    @Order(2)
    @DisplayName("Should return empty list when no messages")
    void findAllByUser_noMessages_returnsEmpty() {
        List<Message> messages = messageRepository.findAllByUser("non-existent-user");
        assertThat(messages).isEmpty();
    }

    @Test
    @Order(3)
    @DisplayName("Should find messages by conversation ID")
    void findByConversationId_messagesExist_returnsList() {
        createAndSaveMessage(conversationId, senderUuid, receiverUuid, receiverEmail);
        createAndSaveMessage(conversationId, receiverUuid, senderUuid, "jean.dupont@medilabo.fr");

        List<Message> conversation = messageRepository.findByConversationId(conversationId);

        assertThat(conversation).hasSize(2);
    }

    @Test
    @Order(4)
    @DisplayName("Should return empty for non-existent conversation")
    void findByConversationId_noConversation_returnsEmpty() {
        List<Message> conversation = messageRepository.findByConversationId("non-existent");
        assertThat(conversation).isEmpty();
    }

    @Test
    @Order(5)
    @DisplayName("Should return true when conversation exists")
    void conversationExists_exists_returnsTrue() {
        createAndSaveMessage(conversationId, senderUuid, receiverUuid, receiverEmail);

        boolean exists = messageRepository.conversationExists(senderUuid, receiverEmail);

        assertThat(exists).isTrue();
    }

    @Test
    @Order(6)
    @DisplayName("Should return false when conversation does not exist")
    void conversationExists_notExists_returnsFalse() {
        boolean exists = messageRepository.conversationExists("unknown", "unknown@email.com");
        assertThat(exists).isFalse();
    }

    @Test
    @Order(7)
    @DisplayName("Should find conversation IDs")
    void findConversationIds_exists_returnsIds() {
        createAndSaveMessage(conversationId, senderUuid, receiverUuid, receiverEmail);

        List<String> ids = messageRepository.findConversationIds(senderUuid, receiverEmail);

        assertThat(ids).contains(conversationId);
    }

    @Test
    @Order(8)
    @DisplayName("Should return empty when no conversation IDs")
    void findConversationIds_notExists_returnsEmpty() {
        List<String> ids = messageRepository.findConversationIds("unknown", "unknown@email.com");
        assertThat(ids).isEmpty();
    }

    @Test
    @Order(9)
    @DisplayName("Should count unread messages")
    void countUnread_unreadExist_returnsCount() {
        createAndSaveMessage(conversationId, senderUuid, receiverUuid, receiverEmail);
        createAndSaveMessage(UUID.randomUUID().toString(), senderUuid, receiverUuid, receiverEmail);

        int unread = messageStatusRepository.countUnread(receiverUuid);

        assertThat(unread).isEqualTo(2);
    }

    @Test
    @Order(10)
    @DisplayName("Should return zero when no unread messages")
    void countUnread_noUnread_returnsZero() {
        int unread = messageStatusRepository.countUnread("non-existent");
        assertThat(unread).isZero();
    }

    @Test
    @Order(11)
    @DisplayName("Should update message status to READ")
    void updateStatus_validData_updatesStatus() {
        Message saved = createAndSaveMessage(conversationId, senderUuid, receiverUuid, receiverEmail);

        int updated = messageStatusRepository.updateStatus(receiverUuid, saved.getMessageId(), "READ");

        assertThat(updated).isEqualTo(1);
        assertThat(messageStatusRepository.countUnread(receiverUuid)).isZero();
    }

    @Test
    @Order(12)
    @DisplayName("Should decrease unread count after marking as read")
    void updateStatus_afterMarkRead_decreasesUnread() {
        Message saved = createAndSaveMessage(conversationId, senderUuid, receiverUuid, receiverEmail);
        assertThat(messageStatusRepository.countUnread(receiverUuid)).isEqualTo(1);

        messageStatusRepository.updateStatus(receiverUuid, saved.getMessageId(), "READ");

        assertThat(messageStatusRepository.countUnread(receiverUuid)).isZero();
    }

    @Test
    @Order(13)
    @DisplayName("Should find conversation by UUID")
    void findByConversationUuid_exists_returnsConversation() {
        conversationRepository.save(Conversation.builder()
                .conversationUuid(conversationId)
                .participant1Uuid(senderUuid)
                .participant1Name("Dr. Dupont")
                .participant2Uuid(receiverUuid)
                .participant2Name("Marie Martin")
                .subject("Test")
                .lastMessageAt(LocalDateTime.now())
                .messageCount(1)
                .build());

        var conv = conversationRepository.findByConversationUuid(conversationId);

        assertThat(conv).isPresent();
        assertThat(conv.get().getParticipant1Uuid()).isEqualTo(senderUuid);
    }

    @Test
    @Order(14)
    @DisplayName("Should return empty for non-existent conversation UUID")
    void findByConversationUuid_notExists_returnsEmpty() {
        var conv = conversationRepository.findByConversationUuid("non-existent");
        assertThat(conv).isEmpty();
    }

    @Test
    @Order(15)
    @DisplayName("Should get status for specific user via entity method")
    void getStatusForUser_loaded_returnsCorrectStatus() {
        Message saved = createAndSaveMessage(conversationId, senderUuid, receiverUuid, receiverEmail);

        // Recharger avec statuses (JOIN FETCH)
        List<Message> messages = messageRepository.findAllByUser(receiverUuid);
        Message loaded = messages.getFirst();

        assertThat(loaded.getStatusForUser(senderUuid)).isEqualTo("READ");
        assertThat(loaded.getStatusForUser(receiverUuid)).isEqualTo("UNREAD");
    }
}