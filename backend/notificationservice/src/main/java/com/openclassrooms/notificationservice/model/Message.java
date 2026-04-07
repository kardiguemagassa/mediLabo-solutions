package com.openclassrooms.notificationservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "messages")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long messageId;

    @Column(name = "message_uuid", nullable = false, unique = true, length = 40)
    private String messageUuid;

    @Column(name = "conversation_id", nullable = false, length = 40)
    private String conversationId;

    @Column(nullable = false, length = 255)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "sender_uuid", nullable = false, length = 40)
    private String senderUuid;

    @Column(name = "sender_name", nullable = false, length = 100)
    private String senderName;

    @Column(name = "sender_email", nullable = false, length = 100)
    private String senderEmail;

    @Column(name = "sender_image_url", length = 255)
    private String senderImageUrl;

    @Column(name = "sender_role", length = 20)
    private String senderRole;

    @Column(name = "receiver_uuid", nullable = false, length = 40)
    private String receiverUuid;

    @Column(name = "receiver_name", nullable = false, length = 100)
    private String receiverName;

    @Column(name = "receiver_email", nullable = false, length = 100)
    private String receiverEmail;

    @Column(name = "receiver_image_url", length = 255)
    private String receiverImageUrl;

    @Column(name = "receiver_role", length = 20)
    private String receiverRole;

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MessageStatus> statuses = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper — statut pour un utilisateur donné
    @Transient
    public String getStatusForUser(String userUuid) {
        return statuses.stream()
                .filter(s -> s.getUserUuid().equals(userUuid))
                .map(MessageStatus::getMessageStatus)
                .findFirst()
                .orElse("UNREAD");
    }
}