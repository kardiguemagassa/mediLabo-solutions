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

    /**
     * Envoie un message de manière réactive.
     */
    Mono<MessageResponse> sendMessage(MessageRequest request, UserRequest sender);

    /**
     * Récupère tous les messages d'un utilisateur (Inbox).
     */
    Flux<MessageResponse> getMessages(String userUuid);

    /**
     * Récupère les messages d'une conversation.
     */
    Flux<MessageResponse> getConversation(String userUuid, String conversationId);

    /**
     * Compte les messages non lus.
     */
    Mono<Integer> getUnreadCount(String userUuid);

    /**
     * Marque un message comme lu.
     */
    Mono<Void> markMessageAsRead(String userUuid, Long messageId);
}