package com.openclassrooms.notificationservice.service.implementation;

import com.openclassrooms.notificationservice.dtorequest.MessageRequest;
import com.openclassrooms.notificationservice.dtorequest.UserRequest;
import com.openclassrooms.notificationservice.dtoresponse.MessageResponse;
import com.openclassrooms.notificationservice.exception.ApiException;
import com.openclassrooms.notificationservice.mapper.MessageMapper;
import com.openclassrooms.notificationservice.model.Message;
import com.openclassrooms.notificationservice.repository.NotificationRepository;
import com.openclassrooms.notificationservice.service.UserServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationServiceImpl Tests - Reactive")
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserServiceClient userService;

    @Mock
    private MessageMapper messageMapper;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private UserRequest senderInfo;
    private UserRequest receiverInfo;
    private Message expectedMessage;
    private MessageResponse expectedResponse;
    private MessageRequest messageRequest;

    @BeforeEach
    void setUp() {
        senderInfo = UserRequest.builder()
                .userUuid("sender-uuid-123")
                .firstName("Jean")
                .lastName("Dupont")
                .email("jean.dupont@medilabo.fr")
                .build();

        receiverInfo = UserRequest.builder()
                .userUuid("receiver-uuid-456")
                .firstName("Marie")
                .lastName("Martin")
                .email("marie.martin@patient.fr")
                .build();

        messageRequest = MessageRequest.builder()
                .receiverEmail(receiverInfo.getEmail())
                .subject("Résultats")
                .message("Contenu")
                .build();

        expectedMessage = Message.builder()
                .messageId(1L)
                .messageUuid("msg-uuid-789")
                .conversationId("conv-uuid-101")
                .subject("Résultats")
                .build();

        expectedResponse = MessageResponse.builder()
                .messageUuid("msg-uuid-789")
                .conversationId("conv-uuid-101")
                .subject("Résultats")
                .build();
    }

    @Nested
    @DisplayName("sendMessage Tests")
    class SendMessageTests {

        @Test
        void shouldSendMessageSuccessfully_NewConversation() {
            when(userService.getUserByEmail(anyString())).thenReturn(Mono.just(receiverInfo));
            when(notificationRepository.conversationExists(anyString(), anyString())).thenReturn(false);
            when(messageMapper.toEntity(any(), any(), any())).thenReturn(expectedMessage);
            when(notificationRepository.saveMessage(any(Message.class))).thenReturn(expectedMessage);
            when(messageMapper.toResponseWithUserInfo(any())).thenReturn(expectedResponse);

            StepVerifier.create(notificationService.sendMessage(messageRequest, senderInfo))
                    .expectNextMatches(response ->
                            response.getMessageUuid().equals("msg-uuid-789") &&
                                    response.getSubject().equals("Résultats")
                    )
                    .verifyComplete();

            verify(notificationRepository).saveMessage(any(Message.class));
        }

        @Test
        void shouldSendMessageSuccessfully_ExistingConversation() {
            String existingConvId = "existing-conv-123";
            when(userService.getUserByEmail(anyString())).thenReturn(Mono.just(receiverInfo));
            when(notificationRepository.conversationExists(anyString(), anyString())).thenReturn(true);
            when(notificationRepository.getConversationId(anyString(), anyString())).thenReturn(existingConvId);
            when(messageMapper.toEntity(any(), any(), any())).thenReturn(expectedMessage);
            when(notificationRepository.saveMessage(any(Message.class))).thenReturn(expectedMessage);
            when(messageMapper.toResponseWithUserInfo(any())).thenReturn(expectedResponse);

            StepVerifier.create(notificationService.sendMessage(messageRequest, senderInfo))
                    .expectNext(expectedResponse)
                    .verifyComplete();

            verify(notificationRepository).getConversationId(senderInfo.getUserUuid(), receiverInfo.getEmail());
        }

        @Test
        void shouldFailWhenReceiverNotFound() {
            when(userService.getUserByEmail(anyString())).thenReturn(Mono.empty());

            StepVerifier.create(notificationService.sendMessage(messageRequest, senderInfo))
                    .expectErrorMatches(e -> e instanceof ApiException &&
                            e.getMessage().contains("Destinataire introuvable"))
                    .verify();
        }
    }

    @Nested
    @DisplayName("getMessages Tests")
    class GetMessagesTests {
        @Test
        void shouldGetAllMessagesForUser() {
            when(notificationRepository.getMessages(anyString())).thenReturn(List.of(expectedMessage));
            when(messageMapper.toResponseWithUserInfo(any(Message.class))).thenReturn(expectedResponse);

            StepVerifier.create(notificationService.getMessages(senderInfo.getUserUuid()))
                    .expectNext(expectedResponse)
                    .verifyComplete();
        }

        @Test
        void shouldReturnEmptyListWhenNoMessages() {
            when(notificationRepository.getMessages(anyString())).thenReturn(List.of());

            StepVerifier.create(notificationService.getMessages("unknown"))
                    .expectNextCount(0)
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("getConversation Tests")
    class GetConversationTests {
        @Test
        void shouldGetConversationAndMarkAsRead() {
            Message unread = Message.builder().messageId(2L).status("UNREAD").build();
            when(notificationRepository.getConversations(anyString(), anyString())).thenReturn(List.of(unread));
            when(notificationRepository.updateMessageStatus(anyString(), anyLong(), anyString())).thenReturn(String.valueOf(1));
            when(messageMapper.toResponseWithUserInfo(any(Message.class))).thenReturn(expectedResponse);

            StepVerifier.create(notificationService.getConversation(senderInfo.getUserUuid(), "conv-123"))
                    .expectNext(expectedResponse)
                    .verifyComplete();

            verify(notificationRepository).updateMessageStatus(senderInfo.getUserUuid(), 2L, "READ");
        }
    }

    @Nested
    @DisplayName("getUnreadCount Tests")
    class GetUnreadCountTests {
        @Test
        void shouldReturnUnreadCount() {
            when(notificationRepository.getUnreadCount(anyString())).thenReturn(5);

            StepVerifier.create(notificationService.getUnreadCount(senderInfo.getUserUuid()))
                    .expectNext(5)
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("markMessageAsRead Tests")
    class MarkMessageAsReadTests {
        @Test
        void shouldMarkMessageAsRead() {
            when(notificationRepository.updateMessageStatus(anyString(), anyLong(), anyString())).thenReturn(String.valueOf(1));

            StepVerifier.create(notificationService.markMessageAsRead(senderInfo.getUserUuid(), 2L))
                    .verifyComplete();

            verify(notificationRepository).updateMessageStatus(senderInfo.getUserUuid(), 2L, "READ");
        }
    }
}