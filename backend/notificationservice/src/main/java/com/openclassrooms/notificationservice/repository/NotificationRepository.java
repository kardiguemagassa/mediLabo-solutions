package com.openclassrooms.notificationservice.repository;

import com.openclassrooms.notificationservice.model.Message;
import java.util.List;

/**
 * Repository pour la gestion des messages et notifications.
 * Optimisé pour utiliser l'objet domaine Message.
 */
public interface NotificationRepository {

    /**
     * Sauvegarde un message complet dans la base de données.
     * Utilise les données de l'objet Message pour remplir les colonnes dénormalisées.
     *
     * @param message L'entité message complète préparée par le mapper
     * @return Le message créé avec son ID généré par la DB
     */
    Message saveMessage(Message message);

    /**
     * Récupère tous les messages d'un utilisateur (Inbox).
     */
    List<Message> getMessages(String userUuid);

    /**
     * Récupère les messages d'une conversation spécifique.
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