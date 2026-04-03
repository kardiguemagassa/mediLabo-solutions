package com.openclassrooms.notificationservice.mapper;

import com.openclassrooms.notificationservice.dto.MessageRequestDTO;
import com.openclassrooms.notificationservice.dto.UserRequestDTO;
import com.openclassrooms.notificationservice.dto.MessageResponseDTO;
import com.openclassrooms.notificationservice.model.Message;
import org.mapstruct.*;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE, imports = UUID.class)
public interface MessageMapper {

    /**MessageRequest + sender + receiver → Entity*/
    @Mapping(target = "messageId", ignore = true)
    @Mapping(target = "messageUuid", expression = "java(UUID.randomUUID().toString())")
    @Mapping(target = "conversationId", ignore = true) // set dans le service
    @Mapping(target = "subject", source = "request.subject")
    @Mapping(target = "message", source = "request.message")
    @Mapping(target = "senderUuid", source = "sender.userUuid")
    @Mapping(target = "senderName", expression = "java(buildFullName(sender))")
    @Mapping(target = "senderEmail", source = "sender.email")
    @Mapping(target = "senderImageUrl", source = "sender.imageUrl")
    @Mapping(target = "senderRole", source = "sender.role")
    @Mapping(target = "receiverUuid", source = "receiver.userUuid")
    @Mapping(target = "receiverName", expression = "java(buildFullName(receiver))")
    @Mapping(target = "receiverEmail", source = "receiver.email")
    @Mapping(target = "receiverImageUrl", source = "receiver.imageUrl")
    @Mapping(target = "receiverRole", source = "receiver.role")
    @Mapping(target = "statuses", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Message toEntity(MessageRequestDTO request, UserRequestDTO sender, UserRequestDTO receiver);

    /**Entity → Response avec UserInfo (sender + receiver)*/
    default MessageResponseDTO toResponse(Message message) {
        if (message == null) return null;
        return MessageResponseDTO.builder()
                .messageUuid(message.getMessageUuid())
                .conversationId(message.getConversationId())
                .subject(message.getSubject())
                .message(message.getMessage())
                .status(null) // sera set par le service avec le contexte utilisateur
                .createdAt(message.getCreatedAt() != null ? message.getCreatedAt().toString() : null)
                .updatedAt(message.getUpdatedAt() != null ? message.getUpdatedAt().toString() : null)
                .sender(MessageResponseDTO.UserInfo.builder()
                        .userUuid(message.getSenderUuid())
                        .name(message.getSenderName())
                        .email(message.getSenderEmail())
                        .imageUrl(message.getSenderImageUrl())
                        .role(message.getSenderRole())
                        .build())
                .receiver(MessageResponseDTO.UserInfo.builder()
                        .userUuid(message.getReceiverUuid())
                        .name(message.getReceiverName())
                        .email(message.getReceiverEmail())
                        .imageUrl(message.getReceiverImageUrl())
                        .role(message.getReceiverRole())
                        .build())
                .build();
    }

    /**Entity → Response avec statut pour un utilisateur donné*/
    default MessageResponseDTO toResponseForUser(Message message, String userUuid) {
        MessageResponseDTO response = toResponse(message);
        if (response != null) {
            response.setStatus(message.getStatusForUser(userUuid));
        }
        return response;
    }

    default List<MessageResponseDTO> toResponseListForUser(List<Message> messages, String userUuid) {
        if (messages == null) return List.of();
        return messages.stream()
                .map(m -> toResponseForUser(m, userUuid))
                .toList();
    }

    default String buildFullName(UserRequestDTO user) {
        if (user == null) return "";
        String first = user.getFirstName() != null ? user.getFirstName() : "";
        String last = user.getLastName() != null ? user.getLastName() : "";
        return (first + " " + last).trim();
    }
}