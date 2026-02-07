package com.openclassrooms.notificationservice.service.implementation;

import com.openclassrooms.notificationservice.client.AuthServerClient;
import com.openclassrooms.notificationservice.dto.UserInfo;
import com.openclassrooms.notificationservice.exception.ApiException;
import com.openclassrooms.notificationservice.model.Message;
import com.openclassrooms.notificationservice.repository.NotificationRepository;
import com.openclassrooms.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.openclassrooms.notificationservice.utils.NotificationUtils.randomUUID;

/**
 * Implémentation du service de notification.
 *
 * ARCHITECTURE:
 * - Async avec CompletableFuture
 * - Resilience4j pour Circuit Breaker
 * - Sender info: extraite du JWT (passée en paramètre)
 * - Receiver info: récupérée via appel API async à l'Auth Server
 *
 * @author Kardigué MAGASSA
 * @version 2.0
 * @since 2026-02-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final AuthServerClient authServerClient;

    @Override
    public CompletableFuture<Message> sendMessage(String senderUuid, String senderName, String senderEmail, String senderImageUrl, String senderRole, String receiverEmail, String subject, String messageContent) {
        log.info("Envoi message async de {} vers {}", senderEmail, receiverEmail);

        // 1. recupérer les infos du receiver depuis l'Auth Server (async)
        return authServerClient.getUserByEmail(receiverEmail)
                .thenApply(receiverOptional -> {
                    // 2. Vérifier que le receiver existe
                    UserInfo receiver = receiverOptional.orElseThrow(() -> new ApiException("Utilisateur introuvable: " + receiverEmail));

                    // 3. Vérifier si une conversation existe déjà
                    String conversationId;
                    if (notificationRepository.conversationExists(senderUuid, receiverEmail)) {
                        conversationId = notificationRepository.getConversationId(senderUuid, receiverEmail);
                        log.debug("Conversation existante trouvée: {}", conversationId);
                    } else {
                        conversationId = randomUUID.get();
                        log.debug("Nouvelle conversation créée: {}", conversationId);
                    }

                    // 4. Créer le message avec toutes les infos dénormalisées
                    return notificationRepository.sendMessage(
                            randomUUID.get(),
                            conversationId,
                            senderUuid,
                            senderName,
                            senderEmail,
                            senderImageUrl,
                            senderRole,
                            receiver.getUserUuid(),
                            receiver.getName(),
                            receiver.getEmail(),
                            receiver.getImageUrl(),
                            receiver.getRole(),
                            subject,
                            messageContent
                    );
                })
                .exceptionally(ex -> {
                    log.error("Erreur envoi message: {}", ex.getMessage(), ex);
                    throw new ApiException("Impossible d'envoyer le message: " + ex.getMessage());
                });
    }

    @Override
    public List<Message> getMessages(String userUuid) {
        log.debug("Récupération messages pour user: {}", userUuid);
        var messages = notificationRepository.getMessages(userUuid);

        messages.forEach(message -> {
            if (message.getStatus() == null) {
                var status = notificationRepository.getMessageStatus(userUuid, message.getMessageId());
                message.setStatus(status);
            }
        });

        return messages;
    }

    @Override
    public List<Message> getConversation(String userUuid, String conversationId) {
        log.debug("Récupération conversation {} pour user {}", conversationId, userUuid);
        var messages = notificationRepository.getConversations(userUuid, conversationId);

        messages.forEach(message -> {
            if ("UNREAD".equals(message.getStatus())) {
                notificationRepository.updateMessageStatus(userUuid, message.getMessageId(), "READ");
                message.setStatus("READ");
            }
        });

        return messages;
    }

    @Override
    public Integer getUnreadCount(String userUuid) {
        return notificationRepository.getUnreadCount(userUuid);
    }
}