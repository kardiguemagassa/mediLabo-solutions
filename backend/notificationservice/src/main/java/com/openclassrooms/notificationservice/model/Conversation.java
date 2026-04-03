package com.openclassrooms.notificationservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "conversations")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "conversation_id")
    private Long conversationId;

    @Column(name = "conversation_uuid", nullable = false, unique = true, length = 40)
    private String conversationUuid;

    @Column(name = "participant_1_uuid", nullable = false, length = 40)
    private String participant1Uuid;

    @Column(name = "participant_1_name", length = 100)
    private String participant1Name;

    @Column(name = "participant_1_role", length = 20)
    private String participant1Role;

    @Column(name = "participant_2_uuid", nullable = false, length = 40)
    private String participant2Uuid;

    @Column(name = "participant_2_name", length = 100)
    private String participant2Name;

    @Column(name = "participant_2_role", length = 20)
    private String participant2Role;

    @Column(length = 255)
    private String subject;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Column(name = "message_count")
    @Builder.Default
    private Integer messageCount = 0;

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
}