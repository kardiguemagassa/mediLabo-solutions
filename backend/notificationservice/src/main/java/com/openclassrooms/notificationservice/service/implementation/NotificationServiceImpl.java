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
 *
 * ARCHITECTURE RÉACTIVE:
 * - Mono<T> : Représente 0 ou 1 élément (équivalent à Optional réactif)
 * - Flux<T> : Représente 0 à N éléments (équivalent à Stream réactif)
 * - Schedulers.boundedElastic() : Thread-pool pour opérations bloquantes (JDBC)
 *
 * PATTERN UTILISÉ:
 * - Mono.fromCallable() : Encapsule les appels JDBC synchrones dans un contexte réactif
 * - subscribeOn() : Déplace l'exécution vers un thread-pool dédié
 * - flatMap() : Chaîne les opérations asynchrones séquentiellement
 * - flatMapMany() : Convertit un Mono<List> en Flux<Element>
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

    // ==================== ENVOI DE MESSAGE ====================

    /**
     * Envoie un message de manière réactive.
     *
     * FLUX D'EXÉCUTION:
     * 1. Appel réactif à Auth Server pour récupérer le destinataire (UserService)
     * 2. Si destinataire introuvable → erreur
     * 3. Vérification/création de conversation (JDBC sur boundedElastic)
     * 4. Mapping et sauvegarde du message (JDBC sur boundedElastic)
     * 5. Retour du MessageResponse
     *
     * @param request Les données du message (sujet, contenu, email destinataire)
     * @param sender L'expéditeur (extrait du JWT dans le controller)
     * @return Mono<MessageResponse> Le message créé
     */
    @Override
    public Mono<MessageResponse> sendMessage(MessageRequest request, UserRequest sender) {
        log.info("Envoi de message de {} vers {}", sender.getEmail(), request.getReceiverEmail());

        // Étape 1: Récupérer le destinataire via Auth Server (appel HTTP réactif)
        return userService.getUserByEmail(request.getReceiverEmail())
                // Étape 2: Si Mono vide (user non trouvé) → erreur
                .switchIfEmpty(Mono.error(new ApiException("Destinataire introuvable : " + request.getReceiverEmail())))
                // Étape 3: Traitement JDBC (bloquant) → on le déplace sur boundedElastic
                .flatMap(receiver -> Mono.fromCallable(() -> {
                            /*
                             * fromCallable() encapsule le code bloquant dans un Callable
                             * Ce code sera exécuté sur le Scheduler spécifié (boundedElastic)
                             * et non sur le thread principal (event loop)
                             */

                            // Vérifier si une conversation existe déjà entre les deux utilisateurs
                            String conversationId = notificationRepository.conversationExists(sender.getUserUuid(), receiver.getEmail())
                                    ? notificationRepository.getConversationId(sender.getUserUuid(), receiver.getEmail())
                                    : randomUUID.get();

                            // Mapper la requête + infos users → Entity Message
                            Message messageToSave = messageMapper.toEntity(request, sender, receiver);
                            messageToSave.setConversationId(conversationId);

                            // Sauvegarder en base (appel JDBC bloquant)
                            Message savedMessage = notificationRepository.saveMessage(messageToSave);
                            log.info("Message créé avec succès: {}", savedMessage.getMessageUuid());

                            // Mapper Entity → Response DTO
                            return messageMapper.toResponseWithUserInfo(savedMessage);
                        })
                        // subscribeOn() déplace TOUTE la chaîne fromCallable sur boundedElastic
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    // RÉCUPÉRATION DES MESSAGES

    /**
     * Récupère tous les messages d'un utilisateur (Inbox).
     *
     * FLUX D'EXÉCUTION:
     * 1. Appel JDBC pour récupérer la liste des messages
     * 2. Conversion List<Message> → Flux<Message>
     * 3. Mapping de chaque message vers MessageResponse
     *
     * @param userUuid L'UUID de l'utilisateur
     * @return Flux<MessageResponse> Stream réactif des messages
     */
    @Override
    public Flux<MessageResponse> getMessages(String userUuid) {
        log.debug("Récupération messages pour user: {}", userUuid);

        // Mono.fromCallable : encapsule l'appel JDBC bloquant
        return Mono.fromCallable(() -> notificationRepository.getMessages(userUuid))
                // Exécuter sur thread-pool élastique (pour ne pas bloquer l'event loop)
                .subscribeOn(Schedulers.boundedElastic())
                // flatMapMany : Convertit Mono<List<Message>> → Flux<Message>
                // Chaque élément de la liste devient un élément du Flux
                .flatMapMany(Flux::fromIterable)
                // map : Transformer chaque Message → MessageResponse
                .map(messageMapper::toResponseWithUserInfo);
    }

    //  RÉCUPÉRATION D'UNE CONVERSATION

    /**
     * Récupère les messages d'une conversation et marque les non-lus comme lus.
     *
     * FLUX D'EXÉCUTION:
     * 1. Appel JDBC pour récupérer les messages de la conversation
     * 2. Pour chaque message UNREAD → mise à jour du statut en READ
     * 3. Mapping vers MessageResponse
     *
     * NOTE: Le marquage comme lu est fait de manière réactive pour chaque message
     * individuellement. Une alternative serait de faire un UPDATE en batch.
     *
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
                // flatMap (au lieu de map) car l'opération interne retourne un Mono
                .flatMap(message -> {
                    // Si le message est non lu, le marquer comme lu
                    if ("UNREAD".equals(message.getStatus())) {
                        /*
                         * On retourne un Mono qui:
                         * 1. Met à jour le statut en base (JDBC)
                         * 2. Met à jour l'objet en mémoire
                         * 3. Retourne l'objet mis à jour
                         */
                        return Mono.fromCallable(() -> {
                                    notificationRepository.updateMessageStatus(userUuid, message.getMessageId(), "READ");
                                    message.setStatus("READ");
                                    return message;
                                })
                                .subscribeOn(Schedulers.boundedElastic());
                    }
                    // Si déjà lu, retourner directement (pas d'appel JDBC)
                    return Mono.just(message);
                })
                // Transformer chaque Message → MessageResponse
                .map(messageMapper::toResponseWithUserInfo);
    }

    // ==================== COMPTAGE DES NON-LUS ====================

    /**
     * Compte le nombre de messages non lus pour un utilisateur.
     *
     * @param userUuid L'UUID de l'utilisateur
     * @return Mono<Integer> Le nombre de messages non lus
     */
    @Override
    public Mono<Integer> getUnreadCount(String userUuid) {
        log.debug("Comptage messages non lus pour user: {}", userUuid);

        // Simple encapsulation d'un appel JDBC
        return Mono.fromCallable(() -> notificationRepository.getUnreadCount(userUuid))
                .subscribeOn(Schedulers.boundedElastic());
    }

    // ==================== MARQUAGE COMME LU ====================

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
                // doOnSuccess : Side effect pour logging (n'affecte pas le flux)
                .doOnSuccess(status -> log.info("Message {} marqué comme lu", messageId))
                // then() : Ignore la valeur retournée et convertit en Mono<Void>
                // Utile pour les opérations "fire and forget" où seul le succès compte
                .then();
    }
}