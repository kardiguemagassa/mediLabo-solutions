package com.openclassrooms.notificationservice.query;

/**
 * SQL Queries for Notification Service
 * ARCHITECTURE: Microservices avec données dénormalisées
 *
 * @author Kardigué MAGASSA
 * @version 2.0
 * @since 2026-02-09
 */
public class MessageQuery {

    public static final String CREATE_MESSAGE_FUNCTION =
            """
            SELECT * FROM create_message(
                :messageUuid,
                :conversationId,
                :senderUuid,
                :senderName,
                :senderEmail,
                :senderImageUrl,
                :senderRole,
                :receiverUuid,
                :receiverName,
                :receiverEmail,
                :receiverImageUrl,
                :receiverRole,
                :subject,
                :message
            )
            """;

    public static final String SELECT_MESSAGES_QUERY =
            """
            SELECT 
                m.message_id,
                m.message_uuid,
                m.conversation_id,
                m.sender_uuid,
                m.sender_name,
                m.sender_email,
                m.sender_image_url,
                m.sender_role,
                m.receiver_uuid,
                m.receiver_name,
                m.receiver_email,
                m.receiver_image_url,
                m.receiver_role,
                m.subject,
                m.message,
                ms.message_status AS status,
                m.created_at,
                m.updated_at
            FROM messages m
            JOIN message_statuses ms ON ms.message_id = m.message_id AND ms.user_uuid = :userUuid
            WHERE m.sender_uuid = :userUuid OR m.receiver_uuid = :userUuid
            ORDER BY m.created_at DESC
            """;

    public static final String SELECT_MESSAGES_BY_CONVERSATION_ID_QUERY =
            """
            SELECT 
                m.message_id,
                m.message_uuid,
                m.conversation_id,
                m.sender_uuid,
                m.sender_name,
                m.sender_email,
                m.sender_image_url,
                m.sender_role,
                m.receiver_uuid,
                m.receiver_name,
                m.receiver_email,
                m.receiver_image_url,
                m.receiver_role,
                m.subject,
                m.message,
                ms.message_status AS status,
                m.created_at,
                m.updated_at
            FROM messages m
            JOIN message_statuses ms ON ms.message_id = m.message_id AND ms.user_uuid = :userUuid
            WHERE m.conversation_id = :conversationId
            ORDER BY m.created_at ASC
            """;

    public static final String SELECT_MESSAGE_STATUS_QUERY =
            """
            SELECT ms.message_status 
            FROM message_statuses ms 
            WHERE ms.user_uuid = :userUuid AND ms.message_id = :messageId
            """;

    public static final String UPDATE_MESSAGE_STATUS_QUERY =
            """
            UPDATE message_statuses 
            SET message_status = :status, read_at = CASE WHEN :status = 'READ' THEN NOW() ELSE read_at END
            WHERE user_uuid = :userUuid AND message_id = :messageId
            """;
    public static final String SELECT_MESSAGE_COUNT_QUERY =
            """
            SELECT COUNT(m.message_id) 
            FROM messages m 
            WHERE (m.sender_uuid = :userUuid AND m.receiver_email = :receiverEmail)
               OR (m.sender_email = :receiverEmail AND m.receiver_uuid = :userUuid)
            """;

    public static final String SELECT_CONVERSATION_ID_QUERY =
            """
            SELECT m.conversation_id 
            FROM messages m 
            WHERE (m.sender_uuid = :userUuid AND m.receiver_email = :receiverEmail)
               OR (m.sender_email = :receiverEmail AND m.receiver_uuid = :userUuid)
            LIMIT 1
            """;

    public static final String SELECT_USER_CONVERSATIONS_QUERY =
            """
            SELECT 
                c.conversation_uuid,
                c.subject,
                c.last_message_at,
                c.message_count,
                CASE 
                    WHEN c.participant_1_uuid = :userUuid THEN c.participant_2_uuid
                    ELSE c.participant_1_uuid
                END AS other_participant_uuid,
                CASE 
                    WHEN c.participant_1_uuid = :userUuid THEN c.participant_2_name
                    ELSE c.participant_1_name
                END AS other_participant_name,
                CASE 
                    WHEN c.participant_1_uuid = :userUuid THEN c.participant_2_role
                    ELSE c.participant_1_role
                END AS other_participant_role,
                (
                    SELECT COUNT(*) 
                    FROM messages m 
                    JOIN message_statuses ms ON ms.message_id = m.message_id
                    WHERE m.conversation_id = c.conversation_uuid 
                    AND ms.user_uuid = :userUuid 
                    AND ms.message_status = 'UNREAD'
                ) AS unread_count,
                c.created_at,
                c.updated_at
            FROM conversations c
            WHERE c.participant_1_uuid = :userUuid OR c.participant_2_uuid = :userUuid
            ORDER BY c.last_message_at DESC
            """;

    public static final String SELECT_UNREAD_COUNT_QUERY =
            """
            SELECT COUNT(*) 
            FROM message_statuses 
            WHERE user_uuid = :userUuid AND message_status = 'UNREAD'
            """;
}