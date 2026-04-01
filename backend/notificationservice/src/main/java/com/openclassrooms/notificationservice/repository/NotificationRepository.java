package com.openclassrooms.notificationservice.repository;

import com.openclassrooms.notificationservice.model.Message;
import java.util.List;

/**
 * @author Kardigué MAGASSA
 * @version 2.0
 * @since 2026-02-09
 */

public interface NotificationRepository {
    Message saveMessage(Message message);
    List<Message> getMessages(String userUuid);
    List<Message> getConversations(String userUuid, String conversationId);
    String getMessageStatus(String userUuid, Long messageId);
    String updateMessageStatus(String userUuid, Long messageId, String status);
    Boolean conversationExists(String userUuid, String receiverEmail);
    String getConversationId(String userUuid, String receiverEmail);
    Integer getUnreadCount(String userUuid);
}