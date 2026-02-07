package com.openclassrooms.notificationservice.model;

import lombok.*;

/**
 * Model représentant un message entre utilisateurs.
 *
 * ARCHITECTURE: Données dénormalisées
 * Les infos sender/receiver sont stockées directement
 *
 * @author Kardigué MAGASSA
 * @version 2.0
 * @since 2026-02-09
 */
@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Message {
    private Long messageId;
    private String messageUuid;
    private String conversationId;
    // Message content
    private String subject;
    private String message;
    private String status;
    // Sender info Auth Server
    private String senderUuid;
    private String senderName;
    private String senderEmail;
    private String senderImageUrl;
    private String senderRole;
    // Receiver info Auth Server
    private String receiverUuid;
    private String receiverName;
    private String receiverEmail;
    private String receiverImageUrl;
    private String receiverRole;
    private String createdAt;
    private String updatedAt;
}