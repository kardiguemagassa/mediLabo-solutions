package com.openclassrooms.notificationservice.mapper;

import com.openclassrooms.notificationservice.dto.MessageRequestDTO;
import com.openclassrooms.notificationservice.dto.UserRequestDTO;
import com.openclassrooms.notificationservice.dto.MessageResponseDTO;
import com.openclassrooms.notificationservice.model.Message;
import com.openclassrooms.notificationservice.model.MessageStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MessageMapper Tests (MapStruct)")
class MessageMapperTest {

    private MessageMapper mapper;

    private UserRequestDTO sender;
    private UserRequestDTO receiver;
    private MessageRequestDTO request;
    private Message message;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(MessageMapper.class);

        sender = UserRequestDTO.builder()
                .userUuid("user-111").firstName("John").lastName("Doe")
                .email("john@test.com").imageUrl("http://test.com/john.jpg").role("DOCTOR")
                .build();

        receiver = UserRequestDTO.builder()
                .userUuid("user-222").firstName("Jane").lastName("Smith")
                .email("jane@test.com").imageUrl("http://test.com/jane.jpg").role("PATIENT")
                .build();

        request = MessageRequestDTO.builder()
                .subject("Test Subject")
                .message("Test Content")
                .build();

        message = Message.builder()
                .messageUuid("msg-123").conversationId("conv-123")
                .subject("Test Subject").message("Test Content")
                .senderUuid("user-111").senderName("John Doe").senderEmail("john@test.com")
                .senderImageUrl("http://test.com/john.jpg").senderRole("DOCTOR")
                .receiverUuid("user-222").receiverName("Jane Smith").receiverEmail("jane@test.com")
                .receiverImageUrl("http://test.com/jane.jpg").receiverRole("PATIENT")
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .statuses(List.of(
                        MessageStatus.builder().userUuid("user-111").messageStatus("READ").build(),
                        MessageStatus.builder().userUuid("user-222").messageStatus("UNREAD").build()
                ))
                .build();
    }

    @Test
    @DisplayName("Should convert request + sender + receiver to entity")
    void toEntity_validParams_returnsEntity() {
        Message result = mapper.toEntity(request, sender, receiver);

        assertThat(result).isNotNull();
        assertThat(result.getMessageUuid()).isNotNull().matches("[0-9a-f-]{36}");
        assertThat(result.getSubject()).isEqualTo("Test Subject");
        assertThat(result.getMessage()).isEqualTo("Test Content");
        assertThat(result.getSenderUuid()).isEqualTo("user-111");
        assertThat(result.getSenderName()).isEqualTo("John Doe");
        assertThat(result.getSenderEmail()).isEqualTo("john@test.com");
        assertThat(result.getReceiverUuid()).isEqualTo("user-222");
        assertThat(result.getReceiverName()).isEqualTo("Jane Smith");
        assertThat(result.getReceiverEmail()).isEqualTo("jane@test.com");
    }

    @Test
    @DisplayName("Should handle null names in sender")
    void toEntity_nullNames_handlesGracefully() {
        UserRequestDTO senderNoName = UserRequestDTO.builder()
                .userUuid("user-333").email("test@test.com").build();

        Message result = mapper.toEntity(request, senderNoName, receiver);

        assertThat(result).isNotNull();
        assertThat(result.getSenderName()).isEmpty();
    }

    @Test
    @DisplayName("Should convert entity to response with UserInfo")
    void toResponse_validMessage_returnsResponse() {
        MessageResponseDTO result = mapper.toResponse(message);

        assertThat(result).isNotNull();
        assertThat(result.getMessageUuid()).isEqualTo("msg-123");
        assertThat(result.getSubject()).isEqualTo("Test Subject");
        assertThat(result.getSender()).isNotNull();
        assertThat(result.getSender().getName()).isEqualTo("John Doe");
        assertThat(result.getReceiver()).isNotNull();
        assertThat(result.getReceiver().getName()).isEqualTo("Jane Smith");
    }

    @Test
    @DisplayName("Should return null when message is null")
    void toResponse_nullMessage_returnsNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }

    @Test
    @DisplayName("Should set correct status for sender (READ)")
    void toResponseForUser_sender_statusRead() {
        MessageResponseDTO result = mapper.toResponseForUser(message, "user-111");

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("READ");
    }

    @Test
    @DisplayName("Should set correct status for receiver (UNREAD)")
    void toResponseForUser_receiver_statusUnread() {
        MessageResponseDTO result = mapper.toResponseForUser(message, "user-222");

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("UNREAD");
    }

    @Test
    @DisplayName("Should convert list with correct statuses")
    void toResponseListForUser_validList_returnsResponses() {
        List<MessageResponseDTO> results = mapper.toResponseListForUser(List.of(message), "user-222");

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().getStatus()).isEqualTo("UNREAD");
    }

    @Test
    @DisplayName("Should return empty list for null input")
    void toResponseListForUser_nullList_returnsEmpty() {
        assertThat(mapper.toResponseListForUser(null, "user")).isEmpty();
    }

    @Test
    @DisplayName("Should return empty list for empty input")
    void toResponseListForUser_emptyList_returnsEmpty() {
        assertThat(mapper.toResponseListForUser(List.of(), "user")).isEmpty();
    }

    @Test
    @DisplayName("Should build full name from first + last")
    void buildFullName_bothNames_returnsCombined() {
        assertThat(mapper.buildFullName(sender)).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("Should handle null first name")
    void buildFullName_nullFirst_returnsLast() {
        UserRequestDTO user = UserRequestDTO.builder().lastName("Doe").build();
        assertThat(mapper.buildFullName(user)).isEqualTo("Doe");
    }

    @Test
    @DisplayName("Should handle null last name")
    void buildFullName_nullLast_returnsFirst() {
        UserRequestDTO user = UserRequestDTO.builder().firstName("John").build();
        assertThat(mapper.buildFullName(user)).isEqualTo("John");
    }

    @Test
    @DisplayName("Should handle both names null")
    void buildFullName_bothNull_returnsEmpty() {
        UserRequestDTO user = UserRequestDTO.builder().build();
        assertThat(mapper.buildFullName(user)).isEmpty();
    }

    @Test
    @DisplayName("Should handle null user")
    void buildFullName_nullUser_returnsEmpty() {
        assertThat(mapper.buildFullName(null)).isEmpty();
    }
}