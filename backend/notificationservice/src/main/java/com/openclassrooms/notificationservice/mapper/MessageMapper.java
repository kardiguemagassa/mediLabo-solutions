package com.openclassrooms.notificationservice.mapper;

import com.openclassrooms.notificationservice.dtorequest.MessageRequest;
import com.openclassrooms.notificationservice.dtorequest.UserRequest;
import com.openclassrooms.notificationservice.dtoresponse.MessageResponse;
import com.openclassrooms.notificationservice.model.Message;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Mapper pour les conversions Message Entity ↔ DTOs.
 *
 * @author Kardigué MAGASSA
 * @version 2.0
 * @since 2026-02-09
 */
@Component
public class MessageMapper {

    /**
     * Convertit une requête en Entité Message.
     */
    public Message toEntity(MessageRequest request, UserRequest sender, UserRequest receiver) {
        if (ObjectUtils.anyNull(request, sender, receiver)) {
            return null;
        }

        return Message.builder()
                .messageUuid(UUID.randomUUID().toString())
                .conversationId(request.getConversationId())
                .subject(request.getSubject())
                .message(request.getMessage())
                .status("UNREAD")
                .senderUuid(sender.getUserUuid())
                .senderName(buildFullName(sender))
                .senderEmail(sender.getEmail())
                .senderImageUrl(sender.getImageUrl())
                .senderRole(sender.getRole())
                .receiverUuid(receiver.getUserUuid())
                .receiverName(buildFullName(receiver))
                .receiverEmail(receiver.getEmail())
                .receiverImageUrl(receiver.getImageUrl())
                .receiverRole(receiver.getRole())
                .createdAt(LocalDateTime.now().toString())
                .updatedAt(LocalDateTime.now().toString())
                .build();
    }

    /**
     * Convertit l'entité Message vers la réponse API (version simple).
     */
    public MessageResponse toResponse(Message message) {
        if (message == null) return null;

        return MessageResponse.builder()
                .messageUuid(message.getMessageUuid())
                .conversationId(message.getConversationId())
                .subject(message.getSubject())
                .message(message.getMessage())
                .status(message.getStatus())
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .build();
    }

    /**
     * Convertit l'entité Message vers la réponse API avec UserInfo.
     * Utilise les données dénormalisées stockées dans le message.
     */
    public MessageResponse toResponseWithUserInfo(Message message) {
        if (message == null) return null;

        return MessageResponse.builder()
                .messageUuid(message.getMessageUuid())
                .conversationId(message.getConversationId())
                .subject(message.getSubject())
                .message(message.getMessage())
                .status(message.getStatus())
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .sender(MessageResponse.UserInfo.builder()
                        .userUuid(message.getSenderUuid())
                        .name(message.getSenderName())
                        .email(message.getSenderEmail())
                        .imageUrl(message.getSenderImageUrl())
                        .role(message.getSenderRole())
                        .build())
                .receiver(MessageResponse.UserInfo.builder()
                        .userUuid(message.getReceiverUuid())
                        .name(message.getReceiverName())
                        .email(message.getReceiverEmail())
                        .imageUrl(message.getReceiverImageUrl())
                        .role(message.getReceiverRole())
                        .build())
                .build();
    }

    /**
     * Convertit une liste d'entités en liste de DTOs avec UserInfo.
     */
    public List<MessageResponse> toResponseList(List<Message> messages) {
        if (messages == null) return List.of();
        return messages.stream()
                .map(this::toResponseWithUserInfo)
                .toList();
    }

    /**
     * Met à jour le statut du message.
     */
    public Message updateStatus(Message existing, String newStatus) {
        if (existing != null) {
            existing.setStatus(newStatus);
            existing.setUpdatedAt(LocalDateTime.now().toString());
        }
        return existing;
    }

    // PRIVATE METHODS

    private String buildFullName(UserRequest user) {
        String firstName = user.getFirstName() != null ? user.getFirstName() : "";
        String lastName = user.getLastName() != null ? user.getLastName() : "";
        return (firstName + " " + lastName).trim();
    }
}