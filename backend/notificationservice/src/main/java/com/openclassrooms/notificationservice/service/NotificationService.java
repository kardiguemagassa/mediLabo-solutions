package com.openclassrooms.notificationservice.service;

import com.openclassrooms.notificationservice.model.Message;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service pour la gestion des messages et notifications.
 *
 * @author Kardigué MAGASSA
 * @version 2.0
 * @since 2026-02-09
 */
public interface NotificationService {

    /**
     * Envoie un message de manière asynchrone.
     */
    CompletableFuture<Message> sendMessage(String senderUuid, String senderName, String senderEmail, String senderImageUrl, String senderRole, String receiverEmail, String subject, String messageContent);
    List<Message> getMessages(String userUuid);
    List<Message> getConversation(String userUuid, String conversationId);
    Integer getUnreadCount(String userUuid);
}