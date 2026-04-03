package com.openclassrooms.notificationservice.service.implementation;

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
import com.openclassrooms.notificationservice.service.NotificationService;
import com.openclassrooms.notificationservice.service.UserServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private final MessageRepository messageRepository;
    private final MessageStatusRepository messageStatusRepository;
    private final ConversationRepository conversationRepository;
    private final UserServiceClient userService;
    private final MessageMapper messageMapper;

    @Override
    @Transactional
    public Mono<MessageResponseDTO> sendMessage(MessageRequestDTO request, UserRequestDTO sender) {
        log.info("Envoi de message de {} vers {}", sender.getEmail(), request.getReceiverEmail());


        Mono<UserRequestDTO> senderMono = userService.getUserByUuid(sender.getUserUuid()).defaultIfEmpty(sender);

        Mono<UserRequestDTO> receiverMono = userService.getUserByEmail(request.getReceiverEmail())
                .switchIfEmpty(Mono.error(new ApiException("Destinataire introuvable : " + request.getReceiverEmail())));

        return Mono.zip(senderMono, receiverMono)
                .flatMap(tuple -> {
                    UserRequestDTO enrichedSender = tuple.getT1();
                    UserRequestDTO receiver = tuple.getT2();

                    UserRequestDTO finalSender = sender.toBuilder()
                            .email(enrichedSender.getEmail())
                            .firstName(enrichedSender.getFirstName() != null ? enrichedSender.getFirstName() : sender.getFirstName())
                            .lastName(enrichedSender.getLastName() != null ? enrichedSender.getLastName() : sender.getLastName())
                            .imageUrl(enrichedSender.getImageUrl())
                            .build();

                    return Mono.fromCallable(() -> createMessageWithConversation(request, finalSender, receiver))
                            .subscribeOn(Schedulers.boundedElastic());
                });
    }

    @Override
    public Flux<MessageResponseDTO> getMessages(String userUuid) {
        log.debug("Récupération messages pour user: {}", userUuid);

        return Mono.fromCallable(() -> {
                    List<Message> messages = messageRepository.findAllByUser(userUuid);
                    return messageMapper.toResponseListForUser(messages, userUuid);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable);
    }

    @Override
    @Transactional
    public Flux<MessageResponseDTO> getConversation(String userUuid, String conversationId) {
        log.debug("Récupération conversation {} pour user {}", conversationId, userUuid);

        return Mono.fromCallable(() -> {
                    List<Message> messages = messageRepository.findByConversationId(conversationId);
                    // Marquer les messages non lus comme lus
                    messages.forEach(msg -> {
                        if ("UNREAD".equals(msg.getStatusForUser(userUuid))) {
                            messageStatusRepository.updateStatus(userUuid, msg.getMessageId(), "READ");
                        }
                    });
                    return messageMapper.toResponseListForUser(messages, userUuid);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable);
    }

    @Override
    public Mono<Integer> getUnreadCount(String userUuid) {
        log.debug("Comptage messages non lus pour user: {}", userUuid);
        return Mono.fromCallable(() -> messageStatusRepository.countUnread(userUuid))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    @Transactional
    public Mono<Void> markMessageAsRead(String userUuid, Long messageId) {
        log.debug("Marquage message {} comme lu pour user {}", messageId, userUuid);
        return Mono.fromCallable(() -> messageStatusRepository.updateStatus(userUuid, messageId, "READ"))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(count -> log.info("Message {} marqué comme lu", messageId))
                .then();
    }

    private MessageResponseDTO createMessageWithConversation(MessageRequestDTO request, UserRequestDTO sender, UserRequestDTO receiver) {
        // Résoudre ou créer la conversation
        String conversationId = resolveConversationId(sender, receiver, request.getSubject());

        // Créer le message
        Message message = messageMapper.toEntity(request, sender, receiver);
        message.setConversationId(conversationId);
        Message saved = messageRepository.save(message);

        // Créer les statuts (READ pour sender, UNREAD pour receiver)
        createStatuses(saved, sender.getUserUuid(), receiver.getUserUuid());

        // Mettre à jour la conversation
        updateConversation(conversationId, sender, receiver, request.getSubject());

        log.info("Message créé avec succès: {}", saved.getMessageUuid());

        MessageResponseDTO response = messageMapper.toResponse(saved);
        response.setStatus("READ"); // l'expéditeur voit son message comme lu
        return response;
    }

    private String resolveConversationId(UserRequestDTO sender, UserRequestDTO receiver, String subject) {
        if (messageRepository.conversationExists(sender.getUserUuid(), receiver.getEmail())) {
            List<String> ids = messageRepository.findConversationIds(sender.getUserUuid(), receiver.getEmail());
            if (!ids.isEmpty()) return ids.getFirst();
        }
        return UUID.randomUUID().toString();
    }

    private void createStatuses(Message message, String senderUuid, String receiverUuid) {
        MessageStatus senderStatus = MessageStatus.builder()
                .message(message)
                .userUuid(senderUuid)
                .messageStatus("READ")
                .readAt(LocalDateTime.now())
                .build();

        MessageStatus receiverStatus = MessageStatus.builder()
                .message(message)
                .userUuid(receiverUuid)
                .messageStatus("UNREAD")
                .build();

        messageStatusRepository.save(senderStatus);
        if (!senderUuid.equals(receiverUuid)) {
            messageStatusRepository.save(receiverStatus);
        }
    }

    private void updateConversation(String conversationId, UserRequestDTO sender, UserRequestDTO receiver, String subject) {
        conversationRepository.findByConversationUuid(conversationId)
                .ifPresentOrElse(
                        conv -> {
                            conv.setLastMessageAt(LocalDateTime.now());
                            conv.setMessageCount(conv.getMessageCount() + 1);
                            conversationRepository.save(conv);
                        },
                        () -> conversationRepository.save(Conversation.builder()
                                .conversationUuid(conversationId)
                                .participant1Uuid(sender.getUserUuid())
                                .participant1Name(messageMapper.buildFullName(sender))
                                .participant1Role(sender.getRole())
                                .participant2Uuid(receiver.getUserUuid())
                                .participant2Name(messageMapper.buildFullName(receiver))
                                .participant2Role(receiver.getRole())
                                .subject(subject)
                                .lastMessageAt(LocalDateTime.now())
                                .messageCount(1)
                                .build())
                );
    }
}