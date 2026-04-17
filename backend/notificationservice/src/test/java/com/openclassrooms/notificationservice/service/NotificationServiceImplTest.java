package com.openclassrooms.notificationservice.service;

import com.openclassrooms.notificationservice.dto.MessageRequestDTO;
import com.openclassrooms.notificationservice.dto.UserRequestDTO;
import com.openclassrooms.notificationservice.dto.MessageResponseDTO;
import com.openclassrooms.notificationservice.exception.ApiException;
import com.openclassrooms.notificationservice.mapper.MessageMapper;
import com.openclassrooms.notificationservice.model.Conversation;
import com.openclassrooms.notificationservice.model.Message;
import com.openclassrooms.notificationservice.model.MessageStatus;
import com.openclassrooms.notificationservice.repository.ConversationRepository;
import com.openclassrooms.notificationservice.repository.MessageRepository;
import com.openclassrooms.notificationservice.repository.MessageStatusRepository;
import com.openclassrooms.notificationservice.service.implementation.NotificationServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationServiceImpl Tests")
class NotificationServiceImplTest {

    @Mock private MessageRepository messageRepository;
    @Mock private MessageStatusRepository messageStatusRepository;
    @Mock private ConversationRepository conversationRepository;
    @Mock private UserServiceClient userService;
    @Mock private MessageMapper messageMapper;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private UserRequestDTO sender;
    private UserRequestDTO receiver;
    private MessageRequestDTO messageRequest;
    private Message savedMessage;
    private MessageResponseDTO expectedResponse;

    @BeforeEach
    void setUp() {
        sender = UserRequestDTO.builder()
                .userUuid("sender-uuid-123")
                .firstName("Jean").lastName("Dupont")
                .email("jean.dupont@medilabo.fr")
                .role("DOCTOR")
                .build();

        receiver = UserRequestDTO.builder()
                .userUuid("receiver-uuid-456")
                .firstName("Marie").lastName("Martin")
                .email("marie.martin@patient.fr")
                .role("PATIENT")
                .build();

        messageRequest = MessageRequestDTO.builder()
                .receiverEmail(receiver.getEmail())
                .subject("Résultats")
                .message("Contenu du message")
                .build();

        savedMessage = Message.builder()
                .messageId(1L)
                .messageUuid("msg-uuid-789")
                .conversationId("conv-uuid-101")
                .subject("Résultats")
                .message("Contenu du message")
                .senderUuid(sender.getUserUuid())
                .receiverUuid(receiver.getUserUuid())
                .build();

        expectedResponse = MessageResponseDTO.builder()
                .messageUuid("msg-uuid-789")
                .conversationId("conv-uuid-101")
                .subject("Résultats")
                .build();
    }

    @Nested
    @DisplayName("sendMessage Tests")
    class SendMessageTests {

        @Test
        @DisplayName("Should send message — new conversation")
        void shouldSendMessage_newConversation() {
            when(userService.getUserByUuid(anyString())).thenReturn(Mono.just(sender));
            when(userService.getUserByEmail(receiver.getEmail())).thenReturn(Mono.just(receiver));
            when(messageRepository.conversationExists(anyString(), anyString())).thenReturn(false);
            when(messageMapper.toEntity(any(), any(), any())).thenReturn(savedMessage);
            when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);
            when(messageStatusRepository.save(any(MessageStatus.class))).thenReturn(MessageStatus.builder().build());
            when(conversationRepository.findByConversationUuid(anyString())).thenReturn(Optional.empty());
            when(conversationRepository.save(any(Conversation.class))).thenReturn(Conversation.builder().build());
            when(messageMapper.toResponse(any(Message.class))).thenReturn(expectedResponse);
            when(messageMapper.buildFullName(any())).thenReturn("Jean Dupont");

            StepVerifier.create(notificationService.sendMessage(messageRequest, sender))
                    .expectNextMatches(r -> r.getMessageUuid().equals("msg-uuid-789"))
                    .verifyComplete();

            verify(messageRepository).save(any(Message.class));
            verify(messageStatusRepository, atLeast(1)).save(any(MessageStatus.class));
        }

        @Test
        @DisplayName("Should send message — existing conversation")
        void shouldSendMessage_existingConversation() {
            when(userService.getUserByUuid(anyString())).thenReturn(Mono.just(sender));
            when(userService.getUserByEmail(receiver.getEmail())).thenReturn(Mono.just(receiver));
            when(messageRepository.conversationExists(anyString(), anyString())).thenReturn(true);
            when(messageRepository.findConversationIds(anyString(), anyString())).thenReturn(List.of("existing-conv"));
            when(messageMapper.toEntity(any(), any(), any())).thenReturn(savedMessage);
            when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);
            when(messageStatusRepository.save(any(MessageStatus.class))).thenReturn(MessageStatus.builder().build());
            when(conversationRepository.findByConversationUuid(anyString()))
                    .thenReturn(Optional.of(Conversation.builder().messageCount(1).build()));
            when(conversationRepository.save(any(Conversation.class))).thenReturn(Conversation.builder().build());
            when(messageMapper.toResponse(any(Message.class))).thenReturn(expectedResponse);

            StepVerifier.create(notificationService.sendMessage(messageRequest, sender))
                    .expectNext(expectedResponse)
                    .verifyComplete();

            verify(messageRepository).findConversationIds(anyString(), anyString());
        }

        @Test
        @DisplayName("Should fail when receiver not found")
        void shouldFail_receiverNotFound() {
            when(userService.getUserByUuid(anyString())).thenReturn(Mono.just(sender));
            when(userService.getUserByEmail(receiver.getEmail())).thenReturn(Mono.empty());

            StepVerifier.create(notificationService.sendMessage(messageRequest, sender))
                    .expectErrorMatches(e -> e instanceof ApiException && e.getMessage().contains("Destinataire introuvable"))
                    .verify();
        }
    }

    @Nested
    @DisplayName("getMessages Tests")
    class GetMessagesTests {

        @Test
        @DisplayName("Should get all messages for user")
        void shouldGetMessages() {
            when(messageRepository.findAllByUser(anyString())).thenReturn(List.of(savedMessage));
            when(messageMapper.toResponseListForUser(anyList(), anyString())).thenReturn(List.of(expectedResponse));

            StepVerifier.create(notificationService.getMessages(sender.getUserUuid()))
                    .expectNext(expectedResponse)
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should return empty when no messages")
        void shouldReturnEmpty_noMessages() {
            when(messageRepository.findAllByUser(anyString())).thenReturn(List.of());
            when(messageMapper.toResponseListForUser(anyList(), anyString())).thenReturn(List.of());

            StepVerifier.create(notificationService.getMessages("unknown"))
                    .expectNextCount(0)
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("getConversation Tests")
    class GetConversationTests {

        @Test
        @DisplayName("Should get conversation and mark unread as read")
        void shouldGetConversation_markAsRead() {
            Message unread = Message.builder()
                    .messageId(2L)
                    .conversationId("conv-123")
                    .statuses(List.of(MessageStatus.builder()
                            .userUuid(sender.getUserUuid())
                            .messageStatus("UNREAD")
                            .build()))
                    .build();

            when(messageRepository.findByConversationId("conv-123")).thenReturn(List.of(unread));
            when(messageStatusRepository.updateStatus(sender.getUserUuid(), 2L, "READ")).thenReturn(1);
            when(messageMapper.toResponseListForUser(anyList(), anyString())).thenReturn(List.of(expectedResponse));

            StepVerifier.create(notificationService.getConversation(sender.getUserUuid(), "conv-123"))
                    .expectNext(expectedResponse)
                    .verifyComplete();

            verify(messageStatusRepository).updateStatus(sender.getUserUuid(), 2L, "READ");
        }
    }

    @Nested
    @DisplayName("getUnreadCount Tests")
    class GetUnreadCountTests {

        @Test
        @DisplayName("Should return unread count")
        void shouldReturnUnreadCount() {
            when(messageStatusRepository.countUnread(anyString())).thenReturn(5);

            StepVerifier.create(notificationService.getUnreadCount(sender.getUserUuid()))
                    .expectNext(5)
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("markMessageAsRead Tests")
    class MarkMessageAsReadTests {

        @Test
        @DisplayName("Should mark message as read")
        void shouldMarkAsRead() {
            when(messageStatusRepository.updateStatus(anyString(), anyLong(), anyString())).thenReturn(1);

            StepVerifier.create(notificationService.markMessageAsRead(sender.getUserUuid(), 2L))
                    .verifyComplete();

            verify(messageStatusRepository).updateStatus(sender.getUserUuid(), 2L, "READ");
        }
    }
}