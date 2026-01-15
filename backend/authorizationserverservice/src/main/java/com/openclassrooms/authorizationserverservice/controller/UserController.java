package com.openclassrooms.authorizationserverservice.controller;

import com.openclassrooms.authorizationserverservice.dto.UserInfoDTO;
import com.openclassrooms.authorizationserverservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur REST pour exposer les informations utilisateur à d'autres microservices.
 *
 * <p>
 * Ce contrôleur fournit des endpoints utilisés principalement par le "Patient Service"
 * pour récupérer et valider les données des utilisateurs (user) afin d’enrichir les informations
 * patient dans un contexte de microservices.
 * </p>
 *
 * <p>
 * Endpoints exposés :
 * <ul>
 *     <li>GET /api/users/{userUuid} : récupère les informations d'un utilisateur par UUID.</li>
 *     <li>GET /api/users/{userUuid}/exists : vérifie l’existence d’un utilisateur par UUID.</li>
 * </ul>
 * </p>
 *
 * <p>
 * Sécurité :
 * <ul>
 *     <li>Les endpoints sont sécurisés avec {@link PreAuthorize} pour restreindre l’accès aux rôles et permissions spécifiques :
 *         'user:read', 'ADMIN', 'ORGANIZER', 'PRACTITIONER'.</li>
 *     <li>Les logs détaillent les actions pour faciliter le suivi des appels depuis d’autres services.</li>
 * </ul>
 * </p>
 *
 * <p>
 * Notes pédagogiques :
 * <ul>
 *     <li>Utilisation de DTO (UserInfoDTO) pour exposer uniquement les données nécessaires et éviter de divulguer des informations sensibles.</li>
 *     <li>Structure adaptée aux architectures microservices : ce contrôleur n’expose pas la gestion complète des utilisateurs,
 *         mais uniquement les informations nécessaires aux autres services.</li>
 *     <li>Les exceptions telles que "user not found" ou "access denied" doivent être gérées par un
 *         {@link com.openclassrooms.authorizationserverservice.exception.HandleException} global pour standardiser les réponses.</li>
 * </ul>
 * </p>
 *
 * <p>
 * Documentation OpenAPI/Swagger est automatiquement générée via les annotations {@link Operation} et {@link ApiResponses}.
 * </p>
 *
 * Auteur : FirstName LastName
 * Version : 1.0
 * Date : 2026-01-09
 */

@Tag(name = "Users", description = "User information exposed to other microservices")
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // USER PATIENT MANAGEMENT

    @Operation(
            summary = "Get user information",
            description = "Returns user information used by Patient Service to enrich patient data"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/{userUuid}")
    @PreAuthorize("hasAnyAuthority('user:read', 'ADMIN', 'ORGANIZER', 'PRACTITIONER')")
    public ResponseEntity<UserInfoDTO> getUserInfo(@PathVariable String userUuid) {
        log.debug("Fetching user info for UUID: {}", userUuid);

        UserInfoDTO userInfo = userService.getUserInfoByUuid(userUuid);

        return ResponseEntity.ok(userInfo);
    }

    @Operation(
            summary = "Check if user exists",
            description = "Used by other services to validate that a user UUID is valid"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Check performed"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/{userUuid}/exists")
    @PreAuthorize("hasAnyAuthority('user:read', 'ADMIN', 'ORGANIZER', 'PRACTITIONER')")
    public ResponseEntity<Boolean> userExists(@PathVariable String userUuid) {
        log.debug("Checking if user exists: {}", userUuid);

        boolean exists = userService.userExistsByUuid(userUuid);

        return ResponseEntity.ok(exists);
    }
}