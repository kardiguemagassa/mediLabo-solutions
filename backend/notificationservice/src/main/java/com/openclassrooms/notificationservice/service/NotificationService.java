package com.openclassrooms.notificationservice.service;

import com.openclassrooms.notificationservice.dto.MessageRequestDTO;
import com.openclassrooms.notificationservice.dto.UserRequestDTO;
import com.openclassrooms.notificationservice.dto.MessageResponseDTO;
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
    Mono<MessageResponseDTO> sendMessage(MessageRequestDTO request, UserRequestDTO sender);
    Flux<MessageResponseDTO> getMessages(String userUuid);
    Flux<MessageResponseDTO> getConversation(String userUuid, String conversationId);
    Mono<Integer> getUnreadCount(String userUuid);
    Mono<Void> markMessageAsRead(String userUuid, Long messageId);
}