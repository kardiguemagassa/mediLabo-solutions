package com.openclassrooms.notificationservice.service.implementation;

import com.openclassrooms.notificationservice.dtorequest.MessageRequest;
import com.openclassrooms.notificationservice.dtorequest.UserRequest;
import com.openclassrooms.notificationservice.dtoresponse.MessageResponse;
import com.openclassrooms.notificationservice.exception.ApiException;
import com.openclassrooms.notificationservice.mapper.MessageMapper;
import com.openclassrooms.notificationservice.model.Message;
import com.openclassrooms.notificationservice.repository.NotificationRepository;
import com.openclassrooms.notificationservice.service.NotificationService;
import com.openclassrooms.notificationservice.service.UserServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import static com.openclassrooms.notificationservice.utils.NotificationUtils.randomUUID;

/**
 * Implémentation réactive du service de notification.
 * Mono<T> : Représente 0 ou 1 élément (équivalent à Optional réactif)
 * Flux<T> : Représente 0 à N éléments (équivalent à Stream réactif)
 * Schedulers.boundedElastic() : Thread-pool pour opérations bloquantes (JDBC)
 * Mono.fromCallable() : Encapsule les appels JDBC synchrones dans un contexte réactif
 * subscribeOn() : Déplace l'exécution vers un thread-pool dédié
 * flatMap() : Chaîne les opérations asynchrones séquentiellement
 * flatMapMany() : Convertit un Mono<List> en Flux<Element>
 *
 * @author Kardigué MAGASSA
 * @version 2.1
 * @since 2026-02-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserServiceClient userService;
    private final MessageMapper messageMapper;

    /**
     * Envoie un message de manière réactive.
     * Appel réactif à Auth Server pour récupérer le destinataire (UserService)
     * Si destinataire introuvable → erreur
     * Vérification/création de conversation (JDBC sur boundedElastic)
     * Mapping et sauvegarde du message (JDBC sur boundedElastic)
     * Retour du MessageResponse
     *
     * @param request Les données du message (sujet, contenu, email destinataire)
     * @param sender L'expéditeur (extrait du JWT dans le controller)
     * @return Mono<MessageResponse> Le message créé
     */
    @Override
    public Mono<MessageResponse> sendMessage(MessageRequest request, UserRequest sender) {
        log.info("Envoi de message de {} vers {}", sender.getEmail(), request.getReceiverEmail());

        return Mono.zip(userService.getUserByEmail(sender.getEmail()).defaultIfEmpty(sender), userService.getUserByEmail(request.getReceiverEmail())
                        .switchIfEmpty(Mono.error(new ApiException("Destinataire introuvable : " + request.getReceiverEmail()))))
                .flatMap(tuple -> {UserRequest enrichedSender = tuple.getT1();UserRequest receiver = tuple.getT2();

            UserRequest finalSender = sender.toBuilder().imageUrl(enrichedSender.getImageUrl()).build();

            return Mono.fromCallable(() -> {
                String conversationId = notificationRepository.conversationExists(finalSender.getUserUuid(), receiver.getEmail())
                        ? notificationRepository.getConversationId(finalSender.getUserUuid(), receiver.getEmail())
                        : randomUUID.get();

                Message messageToSave = messageMapper.toEntity(request, finalSender, receiver);
                messageToSave.setConversationId(conversationId);

                Message savedMessage = notificationRepository.saveMessage(messageToSave);
                log.info("Message créé avec succès: {}", savedMessage.getMessageUuid());
                return messageMapper.toResponseWithUserInfo(savedMessage);
            }).subscribeOn(Schedulers.boundedElastic());
        });
    }

    /**
     * Récupère tous les messages d'un utilisateur (Inbox).
     * Appel JDBC pour récupérer la liste des messages
     * Conversion List<Message> → Flux<Message>
     * Mapping de chaque message vers MessageResponse
     *
     * @param userUuid L'UUID de l'utilisateur
     * @return Flux<MessageResponse> Stream réactif des messages
     */
    @Override
    public Flux<MessageResponse> getMessages(String userUuid) {
        log.debug("Récupération messages pour user: {}", userUuid);
        return Mono.fromCallable(() -> notificationRepository.getMessages(userUuid))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable)
                .map(messageMapper::toResponseWithUserInfo);
    }

    /**
     * Récupère les messages d'une conversation et marque les non-lus comme lus.
     * Appel JDBC pour récupérer les messages de la conversation
     * Pour chaque message UNREAD → mise à jour du statut en READ
     * Mapping vers MessageResponse
     * @param userUuid L'UUID de l'utilisateur
     * @param conversationId L'ID de la conversation
     * @return Flux<MessageResponse> Stream réactif des messages de la conversation
     */
    @Override
    public Flux<MessageResponse> getConversation(String userUuid, String conversationId) {
        log.debug("Récupération conversation {} pour user {}", conversationId, userUuid);

        return Mono.fromCallable(() -> notificationRepository.getConversations(userUuid, conversationId))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable)
                .flatMap(message -> {
                    if ("UNREAD".equals(message.getStatus())) {

                        return Mono.fromCallable(() -> {
                                    notificationRepository.updateMessageStatus(userUuid, message.getMessageId(), "READ");
                                    message.setStatus("READ");
                                    return message;
                                })
                                .subscribeOn(Schedulers.boundedElastic());
                    }
                    return Mono.just(message);
                })
                .map(messageMapper::toResponseWithUserInfo);
    }

    /**
     * Compte le nombre de messages non lus pour un utilisateur.
     *
     * @param userUuid L'UUID de l'utilisateur
     * @return Mono<Integer> Le nombre de messages non lus
     */
    @Override
    public Mono<Integer> getUnreadCount(String userUuid) {
        log.debug("Comptage messages non lus pour user: {}", userUuid);
        return Mono.fromCallable(() -> notificationRepository.getUnreadCount(userUuid)).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Marque un message spécifique comme lu.
     *
     * @param userUuid L'UUID de l'utilisateur
     * @param messageId L'ID du message à marquer
     * @return Mono<Void> Complète quand l'opération est terminée
     */
    @Override
    public Mono<Void> markMessageAsRead(String userUuid, Long messageId) {
        log.debug("Marquage message {} comme lu pour user {}", messageId, userUuid);

        return Mono.fromCallable(() -> notificationRepository.updateMessageStatus(userUuid, messageId, "READ"))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(status -> log.info("Message {} marqué comme lu", messageId))
                .then();
    }
}