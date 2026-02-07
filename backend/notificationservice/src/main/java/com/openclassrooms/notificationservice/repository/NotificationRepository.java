package com.openclassrooms.notificationservice.repository;

import com.openclassrooms.notificationservice.model.Message;

import java.util.List;

/**
 * Repository pour la gestion des messages et notifications.
 *
 * @author Kardigué MAGASSA
 * @version 2.0
 * @since 2026-02-09
 */
public interface NotificationRepository {

    /**
     * Envoie un message avec toutes les infos dénormalisées.
     *
     * @param messageUuid UUID unique du message
     * @param conversationId UUID de la conversation (existante ou nouvelle)
     * @param senderUuid UUID de l'expéditeur
     * @param senderName Nom complet de l'expéditeur
     * @param senderEmail Email de l'expéditeur
     * @param senderImageUrl URL de l'avatar de l'expéditeur
     * @param senderRole Rôle de l'expéditeur (PATIENT, DOCTOR, ADMIN)
     * @param receiverUuid UUID du destinataire
     * @param receiverName Nom complet du destinataire
     * @param receiverEmail Email du destinataire
     * @param receiverImageUrl URL de l'avatar du destinataire
     * @param receiverRole Rôle du destinataire
     * @param subject Sujet du message
     * @param message Contenu du message
     * @return Le message créé
     */
    Message sendMessage(String messageUuid, String conversationId, String senderUuid, String senderName, String senderEmail, String senderImageUrl, String senderRole, String receiverUuid, String receiverName, String receiverEmail, String receiverImageUrl, String receiverRole, String subject, String message);

    /**
     * Récupère tous les messages d'un utilisateur.
     */
    List<Message> getMessages(String userUuid);

    /**
     * Récupère les messages d'une conversation.
     */
    List<Message> getConversations(String userUuid, String conversationId);

    /**
     * Récupère le statut d'un message pour un utilisateur.
     */
    String getMessageStatus(String userUuid, Long messageId);

    /**
     * Met à jour le statut d'un message (READ/UNREAD).
     */
    String updateMessageStatus(String userUuid, Long messageId, String status);

    /**
     * Vérifie si une conversation existe entre deux utilisateurs.
     */
    Boolean conversationExists(String userUuid, String receiverEmail);

    /**
     * Récupère l'ID d'une conversation existante.
     */
    String getConversationId(String userUuid, String receiverEmail);

    /**
     * Compte le nombre de messages non lus pour un utilisateur.
     */
    Integer getUnreadCount(String userUuid);
}