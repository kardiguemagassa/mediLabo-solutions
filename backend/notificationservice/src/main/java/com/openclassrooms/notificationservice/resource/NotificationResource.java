package com.openclassrooms.notificationservice.resource;

import com.openclassrooms.notificationservice.domain.Response;
import com.openclassrooms.notificationservice.dtorequest.MessageRequest;
import com.openclassrooms.notificationservice.dtorequest.UserRequest;
import com.openclassrooms.notificationservice.service.NotificationService;
import com.openclassrooms.notificationservice.service.UserServiceClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.net.URI;

import static com.openclassrooms.notificationservice.utils.RequestUtils.getResponse;
import static java.util.Map.of;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

/**
 * Controller REST réactif pour la gestion des messages.
 *
 * ARCHITECTURE RÉACTIVE:
 * - Toutes les méthodes retournent Mono<ResponseEntity<Response>>
 * - Les Flux sont collectés en List via .collectList() pour la réponse JSON
 * - Le JWT est extrait automatiquement via @AuthenticationPrincipal
 *
 * @author Kardigué MAGASSA
 * @version 2.1
 * @since 2026-02-09
 */
@Slf4j
@Tag(name = "Messages", description = "Gestion des messages entre utilisateurs (patient ↔ médecin)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationResource {
    private final NotificationService notificationService;

    @Operation(summary = "Envoyer un nouveau message")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Message créé avec succès"),
            @ApiResponse(responseCode = "400", description = "Paramètres invalides"),
            @ApiResponse(responseCode = "401", description = "Utilisateur non authentifié"),
            @ApiResponse(responseCode = "404", description = "Destinataire introuvable"),
            @ApiResponse(responseCode = "503", description = "Service Auth Server indisponible")
    })
    @PostMapping("/messages")
    public Mono<ResponseEntity<Response>> sendMessage(@AuthenticationPrincipal Jwt jwt, HttpServletRequest request, @Valid @RequestBody MessageRequest messageRequest) {

        log.info("Envoi message de {} vers {}", jwt.getClaim("email"), messageRequest.getReceiverEmail());

        UserRequest sender = buildSenderFromJwt(jwt);

        return notificationService.sendMessage(messageRequest, sender)
                .map(messageResponse -> ResponseEntity
                        .created(URI.create("/api/notifications/messages/" + messageResponse.getConversationId()))
                        .body(getResponse(request, of("message", messageResponse), "Message envoyé avec succès", CREATED)));
    }

    @Operation(summary = "Répondre à un message")
    @PostMapping("/reply")
    public Mono<ResponseEntity<Response>> replyMessage(@AuthenticationPrincipal Jwt jwt, HttpServletRequest request, @Valid @RequestBody MessageRequest messageRequest) {

        log.info("Réponse de {} dans conversation {}", jwt.getClaim("email"), messageRequest.getConversationId());

        UserRequest sender = buildSenderFromJwt(jwt);

        return notificationService.sendMessage(messageRequest, sender)
                .map(messageResponse -> ResponseEntity
                        .created(URI.create("/api/notifications/messages/" + messageResponse.getConversationId()))
                        .body(getResponse(request, of("message", messageResponse), "Message envoyé avec succès", CREATED)));
    }

    @Operation(summary = "Récupérer tous les messages de l'utilisateur connecté")
    @GetMapping("/messages")
    public Mono<ResponseEntity<Response>> getMessages(@AuthenticationPrincipal Jwt jwt, HttpServletRequest request) {

        String userUuid = extractUserUuid(jwt);
        log.debug("Récupération messages pour user: {}", userUuid);

        return Mono.zip(notificationService.getMessages(userUuid).collectList(), notificationService.getUnreadCount(userUuid)).map(tuple -> {
            var messages = tuple.getT1();
            var unreadCount = tuple.getT2();
            return ResponseEntity.ok(getResponse(request, of("messages", messages, "count", messages.size(), "unreadCount", unreadCount), "Messages récupérés avec succès", OK));
        });
    }

    /**
     * Récupère une conversation par son ID.
     *
     * NOTE: Les messages UNREAD sont automatiquement marqués comme READ
     * par le service lors de la récupération.
     */
    @Operation(summary = "Récupérer une conversation par ID")
    @GetMapping("/messages/{conversationId}")
    public Mono<ResponseEntity<Response>> getConversation(@AuthenticationPrincipal Jwt jwt, HttpServletRequest request, @Parameter(description = "UUID de la conversation") @PathVariable String conversationId) {

        String userUuid = extractUserUuid(jwt);
        log.debug("Récupération conversation {} pour user {}", conversationId, userUuid);

        return notificationService.getConversation(userUuid, conversationId).collectList()
                .map(messages -> ResponseEntity.ok(getResponse(request, of("conversation", messages, "count", messages.size()), "Conversation récupérée avec succès", OK)));
    }

    /**
     * Récupère le nombre de messages non lus.
     */
    @Operation(summary = "Récupérer le nombre de messages non lus")
    @GetMapping("/messages/unread/count")
    public Mono<ResponseEntity<Response>> getUnreadCount(@AuthenticationPrincipal Jwt jwt, HttpServletRequest request) {

        String userUuid = extractUserUuid(jwt);
        log.debug("Comptage non-lus pour user: {}", userUuid);

        return notificationService.getUnreadCount(userUuid)
                .map(unreadCount -> ResponseEntity.ok(getResponse(request, of("unreadCount", unreadCount), "Compteur récupéré avec succès", OK)));
    }

    @Operation(summary = "Marquer un message comme lu")
    @PatchMapping("/messages/{messageId}/read")
    public Mono<ResponseEntity<Response>> markAsRead(@AuthenticationPrincipal Jwt jwt, HttpServletRequest request, @Parameter(description = "ID du message") @PathVariable Long messageId) {

        String userUuid = extractUserUuid(jwt);
        log.debug("Marquage message {} comme lu pour user {}", messageId, userUuid);

        return notificationService.markMessageAsRead(userUuid, messageId)
                .then(Mono.just(ResponseEntity.ok(getResponse(request, of("messageId", messageId, "status", "READ"), "Message marqué comme lu", OK))));
    }

    /**
     * Construit un UserRequest à partir des claims JWT.
     * Centralise l'extraction des informations utilisateur.
     */
    private UserRequest buildSenderFromJwt(Jwt jwt) {
        return UserRequest.builder()
                .userUuid(extractUserUuid(jwt))
                .firstName(extractFirstName(jwt))
                .lastName(extractLastName(jwt))
                .email(jwt.getClaim("email"))
                .imageUrl(jwt.getClaim("image_url"))
                .role(extractUserRole(jwt))
                .build();
    }

    /**
     * Extrait l'UUID utilisateur du JWT.
     * Vérifie plusieurs claims possibles pour compatibilité.
     */
    private String extractUserUuid(Jwt jwt) {
        String uuid = jwt.getClaim("user_uuid");
        if (uuid == null) uuid = jwt.getClaim("userId");
        if (uuid == null) uuid = jwt.getSubject();
        return uuid;
    }

    /**
     * Extrait le prénom du JWT.
     */
    private String extractFirstName(Jwt jwt) {
        String firstName = jwt.getClaim("first_name");
        if (firstName != null) return firstName;

        /** Fallback : essayer de parser le claim "name"*/
        String name = jwt.getClaim("name");
        if (name != null && name.contains(" ")) {
            return name.split(" ")[0];
        }

        return name;
    }

    /**
     * Extrait le nom de famille du JWT.
     */
    private String extractLastName(Jwt jwt) {
        String lastName = jwt.getClaim("last_name");
        if (lastName != null) return lastName;

        /** Fallback : essayer de parser le claim "name"*/
        String name = jwt.getClaim("name");
        if (name != null && name.contains(" ")) {
            String[] parts = name.split(" ");
            return parts[parts.length - 1];
        }

        return "";
    }

    /**
     * Extrait le rôle utilisateur du JWT.
     */
    private String extractUserRole(Jwt jwt) {
        String role = jwt.getClaim("role");
        if (role != null) return role;

        String authorities = jwt.getClaim("authorities");
        if (authorities != null) return authorities.split(",")[0].replace("ROLE_", "");

        return "USER";
    }
}