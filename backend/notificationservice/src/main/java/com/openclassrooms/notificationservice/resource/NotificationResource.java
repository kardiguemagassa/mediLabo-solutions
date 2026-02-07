package com.openclassrooms.notificationservice.resource;

import com.openclassrooms.notificationservice.domain.Response;
import com.openclassrooms.notificationservice.dto.SendMessageRequest;
import com.openclassrooms.notificationservice.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

import static com.openclassrooms.notificationservice.utils.RequestUtils.getResponse;
import static java.util.Map.of;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

/**
 * Controller REST pour la gestion des messages.
 * Version async avec CompletableFuture.
 *
 * @author Kardigué MAGASSA
 * @version 2.0
 * @since 2026-02-09
 */

@Slf4j
@Tag(name = "Messages", description = "Gestion des messages entre utilisateurs (patient ↔ médecin)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/messages")
public class NotificationResource {

    private final NotificationService notificationService;

    @Operation(summary = "Envoyer un nouveau message (async)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Message créé avec succès"),
            @ApiResponse(responseCode = "400", description = "Paramètres invalides"),
            @ApiResponse(responseCode = "401", description = "Utilisateur non authentifié"),
            @ApiResponse(responseCode = "404", description = "Destinataire introuvable"),
            @ApiResponse(responseCode = "503", description = "Service Auth Server indisponible")
    })
    @PostMapping
    public CompletableFuture<ResponseEntity<Response>> sendMessage(@AuthenticationPrincipal Jwt jwt, HttpServletRequest request, @Valid @RequestBody SendMessageRequest messageRequest) {
        log.info("Envoi message async de {} vers {}", jwt.getClaim("email"), messageRequest.getReceiverEmail());
        String senderUuid = extractUserUuid(jwt);
        String senderName = extractUserName(jwt);
        String senderEmail = jwt.getClaim("email");
        String senderImageUrl = jwt.getClaim("image_url");
        String senderRole = extractUserRole(jwt);

        return notificationService.sendMessage(senderUuid, senderName, senderEmail, senderImageUrl, senderRole,
                messageRequest.getReceiverEmail(),
                messageRequest.getSubject(),
                messageRequest.getMessage()
        ).thenApply(message -> ResponseEntity
                .created(URI.create("/api/messages/" + message.getConversationId()))
                .body(getResponse(request, of("message", message), "Message envoyé avec succès", CREATED)));
    }

    @Operation(summary = "Récupérer tous les messages de l'utilisateur connecté")
    @GetMapping
    public ResponseEntity<Response> getMessages(@AuthenticationPrincipal Jwt jwt, HttpServletRequest request) {
        String userUuid = extractUserUuid(jwt);
        log.debug("Récupération messages pour user: {}", userUuid);
        var messages = notificationService.getMessages(userUuid);
        var unreadCount = notificationService.getUnreadCount(userUuid);
        return ResponseEntity.ok(getResponse(request, of("messages", messages, "unreadCount", unreadCount), "Messages récupérés avec succès", OK));
    }

    @Operation(summary = "Récupérer une conversation par ID")
    @GetMapping("/{conversationId}")
    public ResponseEntity<Response> getConversation(@AuthenticationPrincipal Jwt jwt, HttpServletRequest request, @Parameter(description = "UUID de la conversation") @PathVariable String conversationId) {
        String userUuid = extractUserUuid(jwt);
        log.debug("Récupération conversation {} pour user {}", conversationId, userUuid);
        var conversation = notificationService.getConversation(userUuid, conversationId);
        return ResponseEntity.ok(getResponse(request, of("conversation", conversation), "Conversation récupérée avec succès", OK));
    }

    @Operation(summary = "Récupérer le nombre de messages non lus")
    @GetMapping("/unread/count")
    public ResponseEntity<Response> getUnreadCount(@AuthenticationPrincipal Jwt jwt, HttpServletRequest request) {
        String userUuid = extractUserUuid(jwt);
        var unreadCount = notificationService.getUnreadCount(userUuid);
        return ResponseEntity.ok(getResponse(request, of("unreadCount", unreadCount), "Compteur récupéré avec succès", OK));
    }

    // HELPER METHODS
    private String extractUserUuid(Jwt jwt) {
        String uuid = jwt.getClaim("user_uuid");
        if (uuid == null) uuid = jwt.getClaim("userId");
        if (uuid == null) uuid = jwt.getSubject();
        return uuid;
    }

    private String extractUserName(Jwt jwt) {
        String name = jwt.getClaim("name");
        if (name != null) return name;

        String firstName = jwt.getClaim("first_name");
        String lastName = jwt.getClaim("last_name");
        if (firstName != null && lastName != null) return firstName + " " + lastName;

        return jwt.getClaim("email");
    }

    private String extractUserRole(Jwt jwt) {
        String role = jwt.getClaim("role");
        if (role != null) return role;

        String authorities = jwt.getClaim("authorities");
        if (authorities != null) return authorities.split(",")[0].replace("ROLE_", "");

        return "USER";
    }
}