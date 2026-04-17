package com.openclassrooms.notificationservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "message_statuses",
        uniqueConstraints = @UniqueConstraint(columnNames = {"message_id", "user_uuid"}))
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MessageStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_status_id")
    private Long messageStatusId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @Column(name = "user_uuid", nullable = false, length = 40)
    private String userUuid;

    @Column(name = "message_status", nullable = false, length = 10)
    @Builder.Default
    private String messageStatus = "UNREAD";

    @Column(name = "read_at")
    private LocalDateTime readAt;
}