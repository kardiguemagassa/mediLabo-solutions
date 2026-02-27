package com.openclassrooms.notificationservice.mapper;

import com.openclassrooms.notificationservice.dtorequest.MessageRequest;
import com.openclassrooms.notificationservice.dtorequest.UserRequest;
import com.openclassrooms.notificationservice.dtoresponse.MessageResponse;
import com.openclassrooms.notificationservice.model.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MessageMapperTest {

    private MessageMapper messageMapper;

    private MessageRequest messageRequest;
    private UserRequest senderRequest;
    private UserRequest receiverRequest;
    private Message message;

    @BeforeEach
    void setUp() {
        messageMapper = new MessageMapper();

        // Initialisation des requêtes
        messageRequest = MessageRequest.builder()
                .conversationId("conv-123")
                .subject("Test Subject")
                .message("Test Message Content")
                .build();

        senderRequest = UserRequest.builder()
                .userUuid("user-111")
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@test.com")
                .imageUrl("http://test.com/john.jpg")
                .role("USER")
                .build();

        receiverRequest = UserRequest.builder()
                .userUuid("user-222")
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@test.com")
                .imageUrl("http://test.com/jane.jpg")
                .role("ADMIN")
                .build();

        // Initialisation d'un message
        message = Message.builder()
                .messageUuid("msg-123")
                .conversationId("conv-123")
                .subject("Test Subject")
                .message("Test Message Content")
                .status("UNREAD")
                .senderUuid("user-111")
                .senderName("John Doe")
                .senderEmail("john.doe@test.com")
                .senderImageUrl("http://test.com/john.jpg")
                .senderRole("USER")
                .receiverUuid("user-222")
                .receiverName("Jane Smith")
                .receiverEmail("jane.smith@test.com")
                .receiverImageUrl("http://test.com/jane.jpg")
                .receiverRole("ADMIN")
                .createdAt(LocalDateTime.now().toString())
                .updatedAt(LocalDateTime.now().toString())
                .build();
    }

    @Test
    void toEntity_ShouldConvertRequestToEntity_WhenAllParametersValid() {
        // When
        Message result = messageMapper.toEntity(messageRequest, senderRequest, receiverRequest);

        // Then
        assertNotNull(result);
        assertNotNull(result.getMessageUuid());
        assertTrue(result.getMessageUuid().matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));

        assertEquals(messageRequest.getConversationId(), result.getConversationId());
        assertEquals(messageRequest.getSubject(), result.getSubject());
        assertEquals(messageRequest.getMessage(), result.getMessage());
        assertEquals("UNREAD", result.getStatus());

        // Vérifier les informations de l'expéditeur
        assertEquals(senderRequest.getUserUuid(), result.getSenderUuid());
        assertEquals("John Doe", result.getSenderName());
        assertEquals(senderRequest.getEmail(), result.getSenderEmail());
        assertEquals(senderRequest.getImageUrl(), result.getSenderImageUrl());
        assertEquals(senderRequest.getRole(), result.getSenderRole());

        // Vérifier les informations du destinataire
        assertEquals(receiverRequest.getUserUuid(), result.getReceiverUuid());
        assertEquals("Jane Smith", result.getReceiverName());
        assertEquals(receiverRequest.getEmail(), result.getReceiverEmail());
        assertEquals(receiverRequest.getImageUrl(), result.getReceiverImageUrl());
        assertEquals(receiverRequest.getRole(), result.getReceiverRole());

        // Vérifier les timestamps
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getUpdatedAt());
    }

    @Test
    void toEntity_ShouldReturnNull_WhenRequestIsNull() {
        // When
        Message result = messageMapper.toEntity(null, senderRequest, receiverRequest);

        // Then
        assertNull(result);
    }

    @Test
    void toEntity_ShouldReturnNull_WhenSenderIsNull() {
        // When
        Message result = messageMapper.toEntity(messageRequest, null, receiverRequest);

        // Then
        assertNull(result);
    }

    @Test
    void toEntity_ShouldReturnNull_WhenReceiverIsNull() {
        // When
        Message result = messageMapper.toEntity(messageRequest, senderRequest, null);

        // Then
        assertNull(result);
    }

    @Test
    void toEntity_ShouldReturnNull_WhenAllParametersNull() {
        // When
        Message result = messageMapper.toEntity(null, null, null);

        // Then
        assertNull(result);
    }

    @Test
    void toEntity_ShouldHandleNullFirstAndLastName() {
        // Given
        UserRequest senderWithNullNames = UserRequest.builder()
                .userUuid("user-333")
                .firstName(null)
                .lastName(null)
                .email("test@test.com")
                .role("USER")
                .build();

        // When
        Message result = messageMapper.toEntity(messageRequest, senderWithNullNames, receiverRequest);

        // Then
        assertNotNull(result);
        assertEquals("", result.getSenderName()); // (null + " " + null).trim() = ""
    }

    @Test
    void toEntity_ShouldHandleNullFirstName() {
        // Given
        UserRequest senderWithNullFirstName = UserRequest.builder()
                .userUuid("user-333")
                .firstName(null)
                .lastName("Doe")
                .email("test@test.com")
                .role("USER")
                .build();

        // When
        Message result = messageMapper.toEntity(messageRequest, senderWithNullFirstName, receiverRequest);

        // Then
        assertNotNull(result);
        assertEquals("Doe", result.getSenderName());
    }

    @Test
    void toEntity_ShouldHandleNullLastName() {
        // Given
        UserRequest senderWithNullLastName = UserRequest.builder()
                .userUuid("user-333")
                .firstName("John")
                .lastName(null)
                .email("test@test.com")
                .role("USER")
                .build();

        // When
        Message result = messageMapper.toEntity(messageRequest, senderWithNullLastName, receiverRequest);

        // Then
        assertNotNull(result);
        assertEquals("John", result.getSenderName());
    }

    @Test
    void toResponse_ShouldConvertEntityToResponse() {
        // When
        MessageResponse result = messageMapper.toResponse(message);

        // Then
        assertNotNull(result);
        assertEquals(message.getMessageUuid(), result.getMessageUuid());
        assertEquals(message.getConversationId(), result.getConversationId());
        assertEquals(message.getSubject(), result.getSubject());
        assertEquals(message.getMessage(), result.getMessage());
        assertEquals(message.getStatus(), result.getStatus());
        assertEquals(message.getCreatedAt(), result.getCreatedAt());
        assertEquals(message.getUpdatedAt(), result.getUpdatedAt());

        // Vérifier que les UserInfo sont nulls (version simple)
        assertNull(result.getSender());
        assertNull(result.getReceiver());
    }

    @Test
    void toResponse_ShouldReturnNull_WhenMessageIsNull() {
        // When
        MessageResponse result = messageMapper.toResponse(null);

        // Then
        assertNull(result);
    }

    @Test
    void toResponseWithUserInfo_ShouldConvertEntityToResponseWithUserInfo() {
        // When
        MessageResponse result = messageMapper.toResponseWithUserInfo(message);

        // Then
        assertNotNull(result);
        assertEquals(message.getMessageUuid(), result.getMessageUuid());
        assertEquals(message.getConversationId(), result.getConversationId());
        assertEquals(message.getSubject(), result.getSubject());
        assertEquals(message.getMessage(), result.getMessage());
        assertEquals(message.getStatus(), result.getStatus());
        assertEquals(message.getCreatedAt(), result.getCreatedAt());
        assertEquals(message.getUpdatedAt(), result.getUpdatedAt());

        // Vérifier les informations de l'expéditeur
        assertNotNull(result.getSender());
        assertEquals(message.getSenderUuid(), result.getSender().getUserUuid());
        assertEquals(message.getSenderName(), result.getSender().getName());
        assertEquals(message.getSenderEmail(), result.getSender().getEmail());
        assertEquals(message.getSenderImageUrl(), result.getSender().getImageUrl());
        assertEquals(message.getSenderRole(), result.getSender().getRole());

        // Vérifier les informations du destinataire
        assertNotNull(result.getReceiver());
        assertEquals(message.getReceiverUuid(), result.getReceiver().getUserUuid());
        assertEquals(message.getReceiverName(), result.getReceiver().getName());
        assertEquals(message.getReceiverEmail(), result.getReceiver().getEmail());
        assertEquals(message.getReceiverImageUrl(), result.getReceiver().getImageUrl());
        assertEquals(message.getReceiverRole(), result.getReceiver().getRole());
    }

    @Test
    void toResponseWithUserInfo_ShouldReturnNull_WhenMessageIsNull() {
        // When
        MessageResponse result = messageMapper.toResponseWithUserInfo(null);

        // Then
        assertNull(result);
    }

    @Test
    void toResponseWithUserInfo_ShouldHandleNullUserFields() {
        // Given
        Message messageWithNullFields = Message.builder()
                .messageUuid("msg-456")
                .conversationId("conv-456")
                .subject("Test")
                .message("Content")
                .status("READ")
                .senderUuid(null)
                .senderName(null)
                .senderEmail(null)
                .senderImageUrl(null)
                .senderRole(null)
                .receiverUuid(null)
                .receiverName(null)
                .receiverEmail(null)
                .receiverImageUrl(null)
                .receiverRole(null)
                .createdAt("2026-01-01T00:00:00")
                .updatedAt("2026-01-01T00:00:00")
                .build();

        // When
        MessageResponse result = messageMapper.toResponseWithUserInfo(messageWithNullFields);

        // Then
        assertNotNull(result);
        assertNotNull(result.getSender());
        assertNull(result.getSender().getUserUuid());
        assertNull(result.getSender().getName());
        assertNull(result.getSender().getEmail());
        assertNull(result.getSender().getImageUrl());
        assertNull(result.getSender().getRole());

        assertNotNull(result.getReceiver());
        assertNull(result.getReceiver().getUserUuid());
        assertNull(result.getReceiver().getName());
        assertNull(result.getReceiver().getEmail());
        assertNull(result.getReceiver().getImageUrl());
        assertNull(result.getReceiver().getRole());
    }

    @Test
    void toResponseList_ShouldConvertListOfEntitiesToListOfResponses() {
        // Given
        Message message2 = Message.builder()
                .messageUuid("msg-456")
                .conversationId("conv-456")
                .subject("Subject 2")
                .message("Content 2")
                .status("READ")
                .senderUuid("user-333")
                .senderName("Bob Wilson")
                .senderEmail("bob@test.com")
                .receiverUuid("user-444")
                .receiverName("Alice Brown")
                .receiverEmail("alice@test.com")
                .build();

        List<Message> messages = List.of(message, message2);

        // When
        List<MessageResponse> results = messageMapper.toResponseList(messages);

        // Then
        assertNotNull(results);
        assertEquals(2, results.size());

        // Vérifier premier message
        MessageResponse firstResult = results.getFirst();
        assertEquals(message.getMessageUuid(), firstResult.getMessageUuid());
        assertNotNull(firstResult.getSender());
        assertEquals(message.getSenderName(), firstResult.getSender().getName());
        assertNotNull(firstResult.getReceiver());
        assertEquals(message.getReceiverName(), firstResult.getReceiver().getName());

        // Vérifier deuxième message
        MessageResponse secondResult = results.get(1);
        assertEquals(message2.getMessageUuid(), secondResult.getMessageUuid());
        assertNotNull(secondResult.getSender());
        assertEquals(message2.getSenderName(), secondResult.getSender().getName());
        assertNotNull(secondResult.getReceiver());
        assertEquals(message2.getReceiverName(), secondResult.getReceiver().getName());
    }

    @Test
    void toResponseList_ShouldReturnEmptyList_WhenMessagesListIsNull() {
        // When
        List<MessageResponse> results = messageMapper.toResponseList(null);

        // Then
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void toResponseList_ShouldReturnEmptyList_WhenMessagesListIsEmpty() {
        // When
        List<MessageResponse> results = messageMapper.toResponseList(List.of());

        // Then
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void updateStatus_ShouldUpdateStatusAndUpdatedAt() {
        // Given
        String newStatus = "READ";
        String oldUpdatedAt = message.getUpdatedAt();

        // Attendre un peu pour que le timestamp soit différent
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            // Ignorer
        }

        // When
        Message result = messageMapper.updateStatus(message, newStatus);

        // Then
        assertNotNull(result);
        assertEquals(newStatus, result.getStatus());
        assertNotEquals(oldUpdatedAt, result.getUpdatedAt());
        assertNotNull(result.getUpdatedAt());
    }

    @Test
    void updateStatus_ShouldReturnNull_WhenMessageIsNull() {
        // When
        Message result = messageMapper.updateStatus(null, "READ");

        // Then
        assertNull(result);
    }

    @Test
    void updateStatus_ShouldUpdateWithNullStatus() {
        // Given
        String oldStatus = message.getStatus();

        // When
        Message result = messageMapper.updateStatus(message, null);

        // Then
        assertNotNull(result);
        assertNull(result.getStatus());
        assertNotNull(result.getUpdatedAt());
        assertNotEquals(null, oldStatus);
    }

    @Test
    void updateStatus_ShouldPreserveOtherFields() {
        // Given
        String originalUuid = message.getMessageUuid();
        String originalConversationId = message.getConversationId();
        String originalSubject = message.getSubject();
        String originalMessage = message.getMessage();
        String originalSenderUuid = message.getSenderUuid();

        // When
        Message result = messageMapper.updateStatus(message, "READ");

        // Then
        assertNotNull(result);
        assertEquals(originalUuid, result.getMessageUuid());
        assertEquals(originalConversationId, result.getConversationId());
        assertEquals(originalSubject, result.getSubject());
        assertEquals(originalMessage, result.getMessage());
        assertEquals(originalSenderUuid, result.getSenderUuid());
        assertEquals("READ", result.getStatus());
    }

    @Test
    void buildFullName_ShouldCombineFirstAndLastName() throws Exception {
        // Utilisation de la réflexion pour tester la méthode privée
        java.lang.reflect.Method method = MessageMapper.class.getDeclaredMethod("buildFullName", UserRequest.class);
        method.setAccessible(true);

        // Test avec prénom et nom
        String result1 = (String) method.invoke(messageMapper, senderRequest);
        assertEquals("John Doe", result1);

        // Test avec prénom null
        UserRequest userNullFirst = UserRequest.builder().firstName(null).lastName("Doe").build();
        String result2 = (String) method.invoke(messageMapper, userNullFirst);
        assertEquals("Doe", result2);

        // Test avec nom null
        UserRequest userNullLast = UserRequest.builder().firstName("John").lastName(null).build();
        String result3 = (String) method.invoke(messageMapper, userNullLast);
        assertEquals("John", result3);

        // Test avec prénom et nom null
        UserRequest userBothNull = UserRequest.builder().firstName(null).lastName(null).build();
        String result4 = (String) method.invoke(messageMapper, userBothNull);
        assertEquals("", result4);
    }
}