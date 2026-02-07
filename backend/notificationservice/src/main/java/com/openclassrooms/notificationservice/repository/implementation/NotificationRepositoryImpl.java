package com.openclassrooms.notificationservice.repository.implementation;

import com.openclassrooms.notificationservice.exception.ApiException;
import com.openclassrooms.notificationservice.model.Message;
import com.openclassrooms.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;

import static com.openclassrooms.notificationservice.query.MessageQuery.*;
import static java.util.Map.of;

/**
 * Implémentation du repository pour les messages.
 *
 * @author Kardigué MAGASSA
 * @version 2.0
 * @since 2026-02-09
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class NotificationRepositoryImpl implements NotificationRepository {

    private final JdbcClient jdbc;

    @Override
    public Message sendMessage(String messageUuid, String conversationId, String senderUuid, String senderName, String senderEmail, String senderImageUrl, String senderRole, String receiverUuid, String receiverName, String receiverEmail, String receiverImageUrl, String receiverRole, String subject, String message) {
        try {
            var params = new HashMap<String, Object>();
            params.put("messageUuid", messageUuid);
            params.put("conversationId", conversationId);
            params.put("senderUuid", senderUuid);
            params.put("senderName", senderName);
            params.put("senderEmail", senderEmail);
            params.put("senderImageUrl", senderImageUrl);
            params.put("senderRole", senderRole);
            params.put("receiverUuid", receiverUuid);
            params.put("receiverName", receiverName);
            params.put("receiverEmail", receiverEmail);
            params.put("receiverImageUrl", receiverImageUrl);
            params.put("receiverRole", receiverRole);
            params.put("subject", subject);
            params.put("message", message);

            return jdbc.sql(CREATE_MESSAGE_FUNCTION).params(params).query(Message.class).single();
        } catch (EmptyResultDataAccessException exception) {
            log.error("Erreur création message: {}", exception.getMessage());
            throw new ApiException("Impossible de créer le message. Veuillez réessayer.");
        } catch (Exception exception) {
            log.error("Erreur inattendue: {}", exception.getMessage(), exception);
            throw new ApiException("Une erreur s'est produite. Veuillez réessayer.");
        }
    }

    @Override
    public List<Message> getMessages(String userUuid) {
        try {
            return jdbc.sql(SELECT_MESSAGES_QUERY).params(of("userUuid", userUuid)).query(Message.class).list();
        } catch (Exception exception) {
            log.error("Erreur récupération messages: {}", exception.getMessage());
            throw new ApiException("Une erreur s'est produite. Veuillez réessayer.");
        }
    }

    @Override
    public List<Message> getConversations(String userUuid, String conversationId) {
        try {
            return jdbc.sql(SELECT_MESSAGES_BY_CONVERSATION_ID_QUERY).params(of("userUuid", userUuid, "conversationId", conversationId)).query(Message.class).list();
        } catch (Exception exception) {
            log.error("Erreur récupération conversation: {}", exception.getMessage());
            throw new ApiException("Une erreur s'est produite. Veuillez réessayer.");
        }
    }

    @Override
    public String getMessageStatus(String userUuid, Long messageId) {
        try {
            return jdbc.sql(SELECT_MESSAGE_STATUS_QUERY).params(of("userUuid", userUuid, "messageId", messageId)).query(String.class).single();
        } catch (EmptyResultDataAccessException exception) {
            log.error("Statut message introuvable: userUuid={}, messageId={}", userUuid, messageId);
            return "UNREAD";
        } catch (Exception exception) {
            log.error("Erreur récupération statut: {}", exception.getMessage());
            throw new ApiException("Une erreur s'est produite. Veuillez réessayer.");
        }
    }

    @Override
    public String updateMessageStatus(String userUuid, Long messageId, String status) {
        try {
            jdbc.sql(UPDATE_MESSAGE_STATUS_QUERY).params(of("userUuid", userUuid, "messageId", messageId, "status", status)).update();
            return status;
        } catch (Exception exception) {
            log.error("Erreur mise à jour statut: {}", exception.getMessage());
            throw new ApiException("Une erreur s'est produite. Veuillez réessayer.");
        }
    }

    @Override
    public Boolean conversationExists(String userUuid, String receiverEmail) {
        try {
            var count = jdbc.sql(SELECT_MESSAGE_COUNT_QUERY).params(of("userUuid", userUuid, "receiverEmail", receiverEmail)).query(Integer.class).single();
            return count > 0;
        } catch (Exception exception) {
            log.error("Erreur vérification conversation: {}", exception.getMessage());
            return false;
        }
    }

    @Override
    public String getConversationId(String userUuid, String receiverEmail) {
        try {
            return jdbc.sql(SELECT_CONVERSATION_ID_QUERY).params(of("userUuid", userUuid, "receiverEmail", receiverEmail)).query(String.class).single();
        } catch (EmptyResultDataAccessException exception) {
            log.debug("Aucune conversation existante entre {} et {}", userUuid, receiverEmail);
            return null;
        } catch (Exception exception) {
            log.error("Erreur récupération conversationId: {}", exception.getMessage());
            throw new ApiException("Une erreur s'est produite. Veuillez réessayer.");
        }
    }

    @Override
    public Integer getUnreadCount(String userUuid) {
        try {
            return jdbc.sql(SELECT_UNREAD_COUNT_QUERY).params(of("userUuid", userUuid)).query(Integer.class).single();
        } catch (Exception exception) {
            log.error("Erreur comptage non lus: {}", exception.getMessage());
            return 0;
        }
    }
}