package com.openclassrooms.authorizationserverservice.controller;

import com.openclassrooms.authorizationserverservice.domain.Response;
import com.openclassrooms.authorizationserverservice.dtorequest.PasswordRequest;
import com.openclassrooms.authorizationserverservice.dtorequest.ResetPasswordRequest;
import com.openclassrooms.authorizationserverservice.dtorequest.RoleRequest;
import com.openclassrooms.authorizationserverservice.dtorequest.UserRequest;
import com.openclassrooms.authorizationserverservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;

import static com.openclassrooms.authorizationserverservice.util.RequestUtils.getResponse;
import static java.util.Collections.emptyMap;
import static java.util.Map.of;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.IMAGE_JPEG_VALUE;
import static org.springframework.http.MediaType.IMAGE_PNG_VALUE;
import static org.springframework.http.ResponseEntity.created;
import static org.springframework.http.ResponseEntity.ok;

/**
 * Contrôleur REST pour la gestion des utilisateurs de l'application.
 *
 * <p>
 * Cette classe expose les endpoints liés aux utilisateurs, tels que :
 * <ul>
 *     <li>Inscription et activation de compte</li>
 *     <li>Authentification multi-facteurs (MFA)</li>
 *     <li>Gestion du profil utilisateur (récupération et mise à jour)</li>
 *     <li>Gestion des rôles et des statuts de compte (verrouillé, activé, expiré)</li>
 *     <li>Gestion des mots de passe (mise à jour, réinitialisation)</li>
 *     <li>Gestion des photos et avatars des utilisateurs</li>
 *     <li>Récupération de listes d'utilisateurs et utilisateurs assignés à des patients</li>
 * </ul>
 * </p>
 *
 * <p>
 * La documentation Swagger/OpenAPI est générée automatiquement grâce aux annotations
 * {@link io.swagger.v3.oas.annotations.Operation}, {@link io.swagger.v3.oas.annotations.responses.ApiResponse}
 * et {@link io.swagger.v3.oas.annotations.tags.Tag}.
 * </p>
 *
 * <p>
 * Sécurité :
 * <ul>
 *     <li>Les méthodes sensibles (mise à jour des rôles, verrouillage de compte, activation/désactivation)
 *         sont protégées par {@link org.springframework.security.access.prepost.PreAuthorize}</li>
 *     <li>Les endpoints nécessitant une authentification récupèrent l'utilisateur courant via {@link org.springframework.security.core.Authentication}</li>
 * </ul>
 * </p>
 *
 * <p>
 * Tous les endpoints renvoient des réponses uniformes encapsulées dans la classe
 * {@link com.openclassrooms.authorizationserverservice.domain.Response}, permettant :
 * <ul>
 *     <li>Une structure JSON standardisée pour le frontend</li>
 *     <li>Le suivi des messages, codes HTTP et exceptions</li>
 * </ul>
 * </p>
 *
 * <h2>Exemples de fonctionnalités</h2>
 * <ul>
 *     <li>POST /user/register : création d'un compte utilisateur avec envoi de mail de confirmation</li>
 *     <li>GET /user/verify/account : vérification d'un compte via token</li>
 *     <li>PATCH /user/mfa/enable : activation de la MFA pour l'utilisateur connecté</li>
 *     <li>PATCH /user/updatepassword : mise à jour du mot de passe pour l'utilisateur connecté</li>
 *     <li>PATCH /user/photo : upload de la photo de profil de l'utilisateur</li>
 *     <li>GET /user/list : récupération de tous les utilisateurs (ADMIN/SUPER_ADMIN)</li>
 * </ul>
 *
 * <p>
 * Notes techniques :
 * <ul>
 *     <li>Les endpoints manipulant des fichiers utilisent {@link org.springframework.web.multipart.MultipartFile}</li>
 *     <li>Les réponses contiennent toujours l'objet {@link com.openclassrooms.authorizationserverservice.domain.Response}</li>
 *     <li>Les requêtes non authentifiées ou non autorisées déclenchent des exceptions gérées par
 *         {@link com.openclassrooms.authorizationserverservice.exception.HandleException}</li>
 * </ul>
 * </p>
 *
 * <p>
 * Cette approche garantit une API REST sécurisée, cohérente et facile à maintenir,
 * avec une documentation automatique grâce à Springdoc/OpenAPI.
 * </p>
 *
 * http://localhost:9001/swagger-ui/index.html#/
 *
 * @author FirstName LastName
 * @version 1.0
 * @since 2026-05-01
 */

@Tag(
        name = "User Management",
        description = "Public REST API for user registration, authentication, profile management and security (MFA, password, roles, photos)"
)
@RestController
@RequestMapping("/user")
public class UserResourceController {
    private final UserService userService;
    private final String photoDirectory;

    public UserResourceController(UserService userService, @Value("${app.photo.directory}") String photoDirectory) {
        this.userService = userService;
        this.photoDirectory = photoDirectory;
    }

    /** REGISTRATION AND ACCOUNT VERIFICATION */

    @Operation(summary = "Register a new user", description = "Creates a new user and sends an email confirmation")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    @PostMapping("/register")
    public ResponseEntity<Response> register(@RequestBody UserRequest user, HttpServletRequest request) {
        userService.createUser(user.getFirstName(), user.getLastName(), user.getEmail(), user.getUsername(), user.getPassword());
        return created(getUri()).body(getResponse(request, emptyMap(), "Compte créé avec succès. Veuillez consulter votre boîte email pour activer votre compte.", CREATED));
    }

    @Operation(summary = "Verify account via token", description = "Verifies a user account using the email token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account verified successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired token")
    })
    @GetMapping("/verify/account")
    public ResponseEntity<Response> verifyAccount(@RequestParam("token") String token, HttpServletRequest request) {
        userService.verifyAccount(token);
        return ok(getResponse(request, emptyMap(), "Compte vérifié. Vous pouvez vous connecter maintenant.", OK));
    }

    /** MFA (MULTI-FACTOR AUTHENTICATION) */

    @Operation(summary = "Enable MFA", description = "Activates two-factor authentication for the logged-in user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "2FA enabled successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    @PatchMapping("/mfa/enable")
    public ResponseEntity<Response> enableMfa(@NotNull Authentication authentication, HttpServletRequest request) {
        var user = userService.enableMfa(authentication.getName());
        return ok(getResponse(request, of("user", user), "L'authentification 2FA a été activée avec succès", OK));
    }

    @Operation(summary = "Disable MFA", description = "Deactivates two-factor authentication for the logged-in user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "2FA disabled successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    @PatchMapping("/mfa/disable")
    public ResponseEntity<Response> disableMfa(@NotNull Authentication authentication, HttpServletRequest request) {
        var user = userService.disableMfa(authentication.getName());
        return ok(getResponse(request, of("user", user), "L'authentification 2FA a été désactivée avec succès", OK));
    }

    /** USER PROFILE */

    @Operation(summary = "Get profile", description = "Retrieve profile and devices for the logged-in user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    @GetMapping("/profile")
    public ResponseEntity<Response> profile(@NotNull Authentication authentication, HttpServletRequest request) {
        var user = userService.getUserByUuid(authentication.getName());
        var devices = userService.getDevices(authentication.getName());
        return ok(getResponse(request, of("user", user, "devices", devices), "Profil récupéré", OK));
    }

    @Operation(summary = "Get user by UUID", description = "Retrieve a user by their UUID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{userUuid}")
    public ResponseEntity<Response> getUserByUuid(@NotNull Authentication authentication, @PathVariable("userUuid") String userUuid, HttpServletRequest request) {
        var user = userService.getUserByUuid(userUuid);
        return ok(getResponse(request, of("user", user), "Profil récupéré", OK));
    }

    @Operation(summary = "Get assignee by patient UUID", description = "Retrieve the user assigned to a patient")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Patient not found")
    })
    @GetMapping("/assignee/{patientUuid}")
    public ResponseEntity<Response> getAssigneeByUuid(@NotNull Authentication authentication, @PathVariable("patientUuid") String patientUuid, HttpServletRequest request) {
        var user = userService.getAssignee(patientUuid);
        return ok(getResponse(request, of("user", user), "Profil récupéré", OK));
    }

    @Operation(
            summary = "Get patient user",
            description = "Retrieve the user associated with a specific patient by its UUID"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Patient or user not found"),
            @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    @GetMapping("/patient/{patientUuid}")
    public ResponseEntity<Response> getPatientUser(@NotNull Authentication authentication, @PathVariable("patientUuid") String patientUuid, HttpServletRequest request) {
        var user = userService.getPatientUser(patientUuid);
        return ok(getResponse(request, of("user", user), "Profil récupéré", OK));
    }

    @Operation(summary = "Get mediLabo support users", description = "Retrieve all users with mediLado support role")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "LediLabo support users retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    @GetMapping("/medilabosupports")
    public ResponseEntity<Response> getMediLaboSupports(@NotNull Authentication authentication, HttpServletRequest request) {
        var medilaboSupports = userService.getMediLaboSupports();
        return ok(getResponse(request, of("medilaboSupports", medilaboSupports), "Profil récupéré", OK));
    }

    @Operation(summary = "Get user by email", description = "Retrieve a user by their email address")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    @GetMapping("/user/{email}")
    public ResponseEntity<Response> getUserByEmail(@NotNull Authentication authentication, @PathVariable("email") String email, HttpServletRequest request) {
        var user = userService.getUserByEmail(email);
        return ok(getResponse(request, of("user", user), "Profil récupéré", OK));
    }

    @Operation(summary = "Get user credentials", description = "Retrieve credentials of a user by their UUID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Credentials retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User or credentials not found"),
            @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    @GetMapping("/credential/{userUuid}")
    public ResponseEntity<Response> getCredential(@NotNull Authentication authentication, @PathVariable("userUuid") String userUuid, HttpServletRequest request) {
        var credential = userService.getCredential(userUuid);
        return ok(getResponse(request, of("credential", credential), "Profil récupéré", OK));
    }

    @Operation(summary = "Update user profile", description = "Update details of the logged-in user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    @PatchMapping("/update")
    public ResponseEntity<Response> updateUser(@NotNull Authentication authentication, @RequestBody UserRequest user, HttpServletRequest request) {
        var updatedUser = userService.updateUser(authentication.getName(), user.getFirstName(), user.getLastName(), user.getEmail(), user.getPhone(), user.getBio(), user.getAddress());
        return ok(getResponse(request, of("user", updatedUser), "Utilisateur mis à jour avec succès", OK));
    }

    @Operation(summary = "Update user by UUID (admin)", description = "Update any user's contact info (ADMIN, SUPER_ADMIN, PRACTITIONER)")
    @PatchMapping("/update/{userUuid}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN', 'PRACTITIONER', 'HEAD_PRACTITIONER')")
    public ResponseEntity<Response> updateUserByUuid(@NotNull Authentication authentication, @PathVariable("userUuid") String userUuid, @RequestBody UserRequest user, HttpServletRequest request) {
        var updatedUser = userService.updateUser(userUuid, user.getFirstName(), user.getLastName(), user.getEmail(), user.getPhone(), user.getBio(), user.getAddress());
        return ok(getResponse(request, of("user", updatedUser), "Utilisateur mis à jour avec succès", OK));
    }

    @Operation(summary = "Update user role", description = "Update the role of a user (ADMIN, SUPER_ADMIN, MANAGER required)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User role updated successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    @PatchMapping("/updaterole")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN', 'MANAGER')")
    public ResponseEntity<Response> updateRole(@NotNull Authentication authentication, @RequestBody RoleRequest roleRequest, HttpServletRequest request) {
        var updatedUser = userService.updateRole(authentication.getName(), roleRequest.getRole());
        return ok(getResponse(request, of("user", updatedUser), "Utilisateur mis à jour avec succès", OK));
    }

    @Operation(summary = "Toggle account expired status", description = "Activate or deactivate account expiration for a user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account expiration status toggled successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    @PatchMapping("/toggleaccountexpired")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN', 'MANAGER')")
    public ResponseEntity<Response> toggleAccountExpired(@NotNull Authentication authentication, HttpServletRequest request) {
        var user = userService.toggleAccountExpired(authentication.getName());
        return ok(getResponse(request, of("user", user), "Utilisateur mis à jour avec succès", OK));
    }


    @Operation(summary = "Toggle account locked status", description = "Lock or unlock a user's account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account lock status toggled successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    @PatchMapping("/toggleaccountlocked")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN', 'MANAGER')")
    public ResponseEntity<Response> toggleAccountLocked(@NotNull Authentication authentication, HttpServletRequest request) {
        var user = userService.toggleAccountLocked(authentication.getName());
        return ok(getResponse(request, of("user", user), "Utilisateur mis à jour avec succès", OK));
    }

    @Operation(summary = "Toggle account enabled status", description = "Enable or disable a user's account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account status toggled successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    @PatchMapping("/toggleaccountenabled")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN', 'MANAGER')")
    public ResponseEntity<Response> toggleAccountEnabled(@NotNull Authentication authentication, HttpServletRequest request) {
        var user = userService.toggleAccountEnabled(authentication.getName());
        return ok(getResponse(request, of("user", user), "Utilisateur mis à jour avec succès", OK));
    }

    /** PASSWORD MANAGEMENT WHEN USER IS LOGGED IN */

    @Operation(summary = "Update password", description = "Update the logged-in user's password")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid password or confirmation mismatch")
    })
    @PatchMapping("/updatepassword")
    public ResponseEntity<Response> updatePassword(@NotNull Authentication authentication, @RequestBody PasswordRequest passwordRequest, HttpServletRequest request) {
        userService.updatePassword(authentication.getName(), passwordRequest.getCurrentPassword(), passwordRequest.getNewPassword(), passwordRequest.getConfirmNewPassword());
        return ok(getResponse(request, emptyMap(), "Mot de passe mis à jour avec succès", OK));
    }

    /** WHEN USER IS NOT LOGGED */
    @Operation(summary = "Request password reset", description = "Request password reset email (user not logged in)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset email sent"),
            @ApiResponse(responseCode = "404", description = "Email not found")
    })
    @PostMapping("/resetpassword")
    public ResponseEntity<Response> resetPassword(@RequestParam("email") String email, HttpServletRequest request) {
        userService.resetPassword(email);
        return ok(getResponse(request, emptyMap(), "Nous vous avons envoyé un email pour réinitialiser votre mot de passe.", OK));
    }

    /** WHEN USER IS NOT LOGGED */
    @Operation(summary = "Verify password reset token", description = "Verifies the password reset token sent by email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token verified successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired token")
    })
    @GetMapping("/verify/password")
    public ResponseEntity<Response> verifyPassword(@RequestParam("token") String token, HttpServletRequest request) {
        var user = userService.verifyPasswordToken(token);
        return ok(getResponse(request, of("user", user), "Saisissez votre nouveau mot de passe", OK));
    }

    /** WHEN USER IS NOT LOGGED IN */
    @Operation(summary = "Reset password", description = "Reset password using token sent by email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid token or mismatch")
    })
    @PostMapping("/resetpassword/reset")
    public ResponseEntity<Response> doResetPassword(@RequestBody ResetPasswordRequest passwordRequest, HttpServletRequest request) {
        userService.doResetPassword(passwordRequest.getUserUuid(), passwordRequest.getToken(), passwordRequest.getPassword(), passwordRequest.getConfirmPassword());
        return ok(getResponse(request, emptyMap(), "Mot de passe réinitialisé avec succès. Vous pouvez vous connecter maintenant.", OK));
    }

    @Operation(summary = "Get all users", description = "Retrieve a list of all users (requires ADMIN or SUPER_ADMIN role)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    @GetMapping("/list")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Response> getUsers(@NotNull Authentication authentication, HttpServletRequest request) {
        return ok(getResponse(request, of("users", userService.getUsers()), "Utilisateurs récupérés", OK));
    }

    /** PHOTOS AND AVATARS */

    @Operation(summary = "Upload user photo", description = "Uploads a photo for the logged-in user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Photo uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid file")
    })
    @PatchMapping("/photo")
    public ResponseEntity<Response> uploadPhoto(@NotNull Authentication authentication, @RequestParam("file") MultipartFile file, HttpServletRequest request) {
        var user = userService.uploadPhoto(authentication.getName(), file);
        return ok(getResponse(request, of("user", user), "Photo mise à jour avec succès", OK));
    }

    @Operation(summary = "Get user photo", description = "Retrieves a user's photo by filename")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Photo retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Photo not found")
    })
    @GetMapping(path = "/image/{filename}", produces = { IMAGE_PNG_VALUE, IMAGE_JPEG_VALUE })
    public byte [] getPhoto(@PathVariable("filename") String filename) throws IOException {
        return Files.readAllBytes(Paths.get(photoDirectory + filename));
    }

    /** PRIVATE UTIL */

    private URI getUri() {
        return URI.create("/user/profile/userId");
    }

}