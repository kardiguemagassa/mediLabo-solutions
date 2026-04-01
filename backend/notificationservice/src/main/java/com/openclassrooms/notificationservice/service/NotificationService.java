package com.openclassrooms.notificationservice.service;

import com.openclassrooms.notificationservice.dtorequest.MessageRequest;
import com.openclassrooms.notificationservice.dtorequest.UserRequest;
import com.openclassrooms.notificationservice.dtoresponse.MessageResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service de gestion des notifications et messages.
 *
 * @author Kardigué MAGASSA
 * @version 2.1
 * @since 2026-02-09
 */
public interface NotificationService {
    Mono<MessageResponse> sendMessage(MessageRequest request, UserRequest sender);
    Flux<MessageResponse> getMessages(String userUuid);
    Flux<MessageResponse> getConversation(String userUuid, String conversationId);
    Mono<Integer> getUnreadCount(String userUuid);
    Mono<Void> markMessageAsRead(String userUuid, Long messageId);
}