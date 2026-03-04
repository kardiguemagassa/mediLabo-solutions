package com.openclassrooms.authorizationserverservice.service.impl;


import com.openclassrooms.authorizationserverservice.event.Event;
import com.openclassrooms.authorizationserverservice.exception.ApiException;
import com.openclassrooms.authorizationserverservice.model.Credential;
import com.openclassrooms.authorizationserverservice.model.Device;
import com.openclassrooms.authorizationserverservice.model.User;
import com.openclassrooms.authorizationserverservice.repository.UserRepository;
import com.openclassrooms.authorizationserverservice.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.openclassrooms.authorizationserverservice.enumeration.EventType.RESETPASSWORD;
import static com.openclassrooms.authorizationserverservice.enumeration.EventType.USER_CREATED;
import static com.openclassrooms.authorizationserverservice.util.UserUtils.randomUUUID;
import static com.openclassrooms.authorizationserverservice.util.UserUtils.verifyCode;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Map.of;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang.WordUtils.capitalizeFully;

/**
 * <p>
 * Implémentation du service métier responsable de la gestion des utilisateurs,
 * de l’authentification et de la sécurité dans l’application.
 * </p>
 *
 * <p>
 * Cette classe agit comme une couche d’orchestration entre :
 * </p>
 * <ul>
 *     <li>la couche <b>Repository</b> pour l’accès aux données</li>
 *     <li>la couche <b>Security</b> pour la gestion des mots de passe et du MFA</li>
 *     <li>le système d’<b>événements</b> pour la communication asynchrone (Kafka, emails, etc.)</li>
 * </ul>
 *
 * <p>
 * Les responsabilités principales de ce service incluent :
 * </p>
 * <ul>
 *     <li>la création et la gestion des comptes utilisateurs</li>
 *     <li>la vérification des comptes et des mots de passe</li>
 *     <li>l'activation et la désactivation du MFA</li>
 *     <li>la gestion des appareils de connexion</li>
 *     <li>le téléversement des photos de profil</li>
 *     <li>la publication d'événements métiers</li>
 * </ul>
 *
 * <p>
 * Cette classe est annotée avec {@link Service} afin d'être gérée par le
 * conteneur Spring et injectée dans les contrôleurs.
 * </p>
 *
 * <p>
 * L'annotation {@link lombok.RequiredArgsConstructor} permet l’injection
 * automatique des dépendances finales.
 * </p>
 *
 * @author FirstName LastName
 * @version 1.0
 * @since 2026-05-01
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder;
    /**
     * Éditeur d'événements Spring permettant de publier des événements métiers.
     * Utilisé pour notifier d'autres composants de l'application lors de la création de compte,
     * réinitialisation de mot de passe, ou autres actions utilisateur importantes.
     */
    private final ApplicationEventPublisher publisher;
    @Value("${ui.app.url}")
    private String uiAppUrl;

    @Value("${app.photo.directory}")
    private String photoDirectory;

    // USER MANAGEMENT TOKEN SERVICE

    /**
     * @param email adresse e-mail de l'utilisateur
     * @return utilisateur correspondant à l'adresse e-mail
     */
    @Override
    public User getUserByEmail(String email) {
        return userRepository.getUserByEmail(email);
    }

    /**
     * <p>
     * Réinitialise le compteur de tentatives de connexion d’un utilisateur.
     * </p>
     *
     * <p>
     * Cette opération est déclenchée après :
     * </p>
     * <ul>
     *     <li>une authentification réussie</li>
     *     <li>un déverrouillage de compte</li>
     * </ul>
     *
     * @param userId identifiant unique de l’utilisateur
     */
    @Override
    public void resetLoginAttempts(String userId) {
        userRepository.resetLoginAttempts(userId);
    }

    /**
     * <p>
     * Incrémente le nombre de tentatives de connexion échouées.
     * </p>
     *
     * <p>
     * Cette information est utilisée pour :
     * </p>
     * <ul>
     *     <li>détecter les attaques par force brute</li>
     *     <li>verrouiller automatiquement un compte</li>
     * </ul>
     *
     * @param email adresse e-mail utilisée pour la tentative
     */
    @Override
    public void updateLoginAttempts(String email) {
        userRepository.updateLoginAttempts(email);
    }

    /**
     * <p>
     * Met à jour la date de dernière connexion de l’utilisateur.
     * </p>
     *
     * <p>
     * Cette information est utilisée pour :
     * </p>
     * <ul>
     *     <li>le suivi d’activité</li>
     *     <li>les audits de sécurité</li>
     * </ul>
     *
     * @param userId identifiant unique de l’utilisateur
     */
    @Override
    public void setLastLogin(Long userId) {
        userRepository.setLastLogin(userId);
    }

    /**
     * <p>
     * Enregistre un appareil utilisé lors d’une connexion.
     * </p>
     *
     * <p>
     * Ces informations permettent :
     * </p>
     * <ul>
     *     <li>de détecter les connexions suspectes</li>
     *     <li>d’afficher l’historique des appareils</li>
     * </ul>
     *
     * @param userId identifiant de l’utilisateur
     * @param deviceName nom de l’appareil
     * @param client navigateur ou application
     * @param ipAddress adresse IP utilisée
     */
    @Override
    public void addLoginDevice(Long userId, String deviceName, String client, String ipAddress) {
        userRepository.addLoginDevice(userId, deviceName, client, ipAddress);
    }

    /**
     * <p>
     * Valide un code MFA généré par une application d’authentification.
     * </p>
     *
     * <p>
     * Le code est comparé au secret MFA stocké pour l’utilisateur.
     * </p>
     *
     * @param userId identifiant unique de l’utilisateur
     * @param code code à usage unique fourni par l’utilisateur
     * @return {@code true} si le code est valide, sinon {@code false}
     */
    @Override
    public boolean verifyQrCode(String userId, String code) {
        var user = userRepository.getUserByUuid(userId);
        return verifyCode(user.getQrCodeSecret(), code);
    }

    // USER MANAGEMENT SERVICE

    /**
     * <p>
     * Récupère un utilisateur à partir de son UUID.
     * </p>
     *
     * <p>
     * L’UUID est l’identifiant fonctionnel utilisé dans l’API.
     * </p>
     *
     * @param userUuid identifiant unique de l’utilisateur
     * @return utilisateur correspondant
     */
    @Override
    public User getUserByUuid(String userUuid) {
        return userRepository.getUserByUuid(userUuid);
    }


    /**
     * Met à jour les informations d'un utilisateur.
     *
     * @param userUuid UUID de l'utilisateur
     * @param firstName prénom
     * @param lastName nom
     * @param email e-mail
     * @param phone téléphone
     * @param bio biographie
     * @param address adresse
     * @return {@link User} mis à jour
     */
    @Override
    public User updateUser(String userUuid, String firstName, String lastName, String email, String phone, String bio, String address) {
        return userRepository.updateUser(userUuid, firstName, lastName, email, phone, bio, address);
    }

    /**
     * Crée un nouvel utilisateur avec un mot de passe encodé et déclenche un événement de type USER_CREATED.
     *
     * @param firstName prénom
     * @param lastName nom
     * @param email e-mail
     * @param username nom d'utilisateur
     * @param password mot de passe en clair
     */
    @Override
    public void createUser(String firstName, String lastName, String email, String username, String password) {
        var token = userRepository.createUser(firstName, lastName, email, username, encoder.encode(password));
        publisher.publishEvent(new Event(USER_CREATED, of("token", token, "name", capitalizeFully(firstName), "email", email)));
    }

    /**
     * <p>
     * Valide un compte utilisateur à partir d’un token d’activation.
     * </p>
     *
     * <p>
     * Cette opération :
     * </p>
     * <ul>
     *     <li>active le compte</li>
     *     <li>supprime le token</li>
     * </ul>
     *
     * @param token token de validation reçu par e-mail
     */
    @Override
    public void verifyAccount(String token) {
        var accountToken = userRepository.getAccountToken(token);
        if(!nonNull(accountToken)) {
            throw new ApiException("Lien invalide. Veuillez réessayer.");
        }
        if(accountToken.isExpired()) {
            userRepository.deleteAccountToken(token);
            throw new ApiException("Ce lien a expiré. Veuillez recréer votre compte.");
        }
        userRepository.updateAccountSettings(accountToken.getUserId());
        userRepository.deleteAccountToken(token);
    }

    /**
     * <p>
     * Vérifie la validité d’un token de réinitialisation de mot de passe.
     * </p>
     *
     * <p>
     * Si le token est valide, l’utilisateur correspondant est retourné.
     * </p>
     *
     * @param token token de réinitialisation
     * @return utilisateur associé au token
     */
    @Override
    public User verifyPasswordToken(String token) {
        var passwordToken = userRepository.getPasswordToken(token);
        if(!nonNull(passwordToken)) {
            throw new ApiException("Lien invalide. Veuillez réessayer.");
        }
        if(passwordToken.isExpired()) {
            userRepository.deletePasswordToken(token);
            throw new ApiException("Le lien a expiré. Veuillez réinitialiser votre mot de passe.");
        }
        return userRepository.getUserById(passwordToken.getUserId());
    }

    /**
     * <p>
     * Active l'authentification multi-facteurs pour un utilisateur.
     * </p>
     *
     * @param userUuid identifiant de l’utilisateur
     * @return utilisateur après activation du MFA
     */
    @Override
    public User enableMfa(String userUuid) {
        return userRepository.enableMfa(userUuid);
    }

    /**
     * <p>
     * Désactive l’authentification multi-facteurs pour un utilisateur.
     * </p>
     *
     * @param userUuid identifiant de l’utilisateur
     * @return utilisateur après désactivation
     */
    @Override
    public User disableMfa(String userUuid) {
        return userRepository.disableMfa(userUuid);
    }

    /**
     * <p>
     * Téléverse et associe une photo de profil à un utilisateur.
     * </p>
     *
     * <p>
     * L'image est stockée sur le serveur et une URL est générée.
     * </p>
     *
     * @param userUuid identifiant utilisateur
     * @param file image à téléverser
     * @return utilisateur avec la nouvelle image
     */
    @Override
    public User uploadPhoto(String userUuid, MultipartFile file) {
        var user = userRepository.getUserByUuid(userUuid);
        var imageUrl = savePhoto(user.getImageUrl(), file);
        userRepository.updateImageUrl(userUuid, imageUrl);
        user.setImageUrl(imageUrl + "?timestamp=" + System.currentTimeMillis());
        return user;
    }

    /**
     * <p>
     * Bascule l'état du compte utilisateur.
     * </p>
     *
     * <p>
     * Cette méthode inverse l'état actuel :
     * </p>
     * <ul>
     *     <li>expiré ↔ valide</li>
     *     <li>verrouillé ↔ déverrouillé</li>
     *     <li>activé ↔ désactivé</li>
     * </ul>
     *
     * @param userUuid identifiant utilisateur
     * @return utilisateur mis à jour
     */
    @Override
    public User toggleAccountExpired(String userUuid) {
        return userRepository.toggleAccountExpired(userUuid);
    }

    @Override
    public User toggleAccountLocked(String userUuid) {
        return userRepository.toggleAccountLocked(userUuid);
    }

    @Override
    public User toggleAccountEnabled(String userUuid) {
        return userRepository.toggleAccountEnabled(userUuid);
    }

    @Override
    public User toggleCredentialsExpired(String userUuid) {
        return null;
    }

    /**
     * <p>
     * Modifie le mot de passe d’un utilisateur.
     * </p>
     *
     * <p>
     * Le mot de passe actuel est vérifié avant la mise à jour.
     * </p>
     *
     * @param userUuid identifiant utilisateur
     * @param currentPassword mot de passe actuel
     * @param newPassword nouveau mot de passe
     * @param confirmNewPassword confirmation
     */
    @Override
    public void updatePassword(String userUuid, String currentPassword, String newPassword, String confirmNewPassword) {
        if(!Objects.equals(confirmNewPassword, newPassword)) {
            throw new ApiException("Les mots de passe ne correspondent pas. Veuillez réessayer.");
        }
        if(!encoder.matches(currentPassword, userRepository.getPassword(userUuid))) {
            throw new ApiException("Le mot de passe actuel est incorrect. Veuillez réessayer.");
        }
        userRepository.updatePassword(userUuid, encoder.encode(newPassword));
    }

    @Override
    public User updateRole(String userUuid, String role) {
        return userRepository.updateRole(userUuid, role);
    }

    /**
     * <p>
     * Lance le processus de réinitialisation du mot de passe.
     * </p>
     *
     * <p>
     * Un token est généré et envoyé par e-mail à l’utilisateur.
     * </p>
     *
     * @param email adresse e-mail de l'utilisateur
     */
    @Override
    public void resetPassword(String email) {
        var user = userRepository.getUserByEmail(email);
        var passwordToken = userRepository.getPasswordToken(user.getUserId());
        if(!nonNull(passwordToken)) {
            var newToken = userRepository.createPasswordToken(user.getUserId());
            publisher.publishEvent(new Event(RESETPASSWORD, of("token", newToken, "email", email, "name", Objects.requireNonNull(capitalizeFully(user.getFirstName())))));
        } else if (passwordToken.isExpired()) {
            userRepository.deletePasswordToken(user.getUserId());
            var newToken = userRepository.createPasswordToken(user.getUserId());
            publisher.publishEvent(new Event(RESETPASSWORD, of("token", newToken, "email", email, "name", capitalizeFully(user.getFirstName()))));
        } else {
            publisher.publishEvent(new Event(RESETPASSWORD, of("token", passwordToken.getToken(), "email", email, "name", capitalizeFully(user.getFirstName()))));
        }
    }

    /**
     * <p>
     * Applique la réinitialisation du mot de passe.
     * </p>
     *
     * <p>
     * Le token est validé avant la mise à jour du mot de passe.
     * </p>
     *
     * @param userUuid identifiant utilisateur
     * @param token token de réinitialisation
     * @param password nouveau mot de passe
     * @param confirmPassword confirmation
     */
    @Override
    public void doResetPassword(String userUuid, String token, String password, String confirmPassword) {
        if(!Objects.equals(confirmPassword, password)) {
            throw new ApiException("Les mots de passe ne correspondent pas. Veuillez réessayer.");
        }
        var user = userRepository.getUserByUuid(userUuid);
        var passwordToken = userRepository.getPasswordToken(token);
        if(!Objects.equals(user.getUserId(), passwordToken.getUserId())) {
            throw new ApiException("Lien invalide. Veuillez réessayer.");
        }
        userRepository.updatePassword(userUuid, encoder.encode(password));
        userRepository.deletePasswordToken(user.getUserId());
    }

    @Override
    public List<User> getUsers() {
        return userRepository.getUsers();
    }

    @Override
    public User getAssignee(String ticketUuid) {
        return userRepository.getAssignee(ticketUuid);
    }

    @Override
    public Credential getCredential(String userUuid) {
        return userRepository.getCredential(userUuid);
    }

    @Override
    public List<Device> getDevices(String userUuid) {
        return userRepository.getDevices(userUuid);
    }

    @Override
    public User getPatientUser(String patientUuid) {
        return userRepository.getPatientUser(patientUuid);
    }

    /**
     * Récupère la liste des utilisateurs support technique de MediLabo.
     *
     * @return Liste des utilisateurs support technique.
     */
    @Override
    public List<User> getMediLaboSupports() {
        return userRepository.getMediLaboSupports();
    }

    /**
     * Fonction utilitaire pour récupérer l'extension d'un fichier.
     * Si le fichier n'a pas d'extension, ".png" est utilisée par défaut.
     */
    private final Function<String, String> fileExtension = filename -> Optional.of(filename).filter(name -> name.contains("."))
            .map(name -> "." + name.substring(filename.lastIndexOf(".") + 1)).orElse(".png");

    /**
     * Fonction utilitaire pour sauvegarder une photo utilisateur.
     * Supprime l'ancienne image si elle existe et copie la nouvelle dans le répertoire PHOTO_DIRECTORY.
     * Retourne l'URL publique de l'image sauvegardée.
     */
    private String savePhoto(String imageUrl, MultipartFile image) {
        try {
            var existingImage = Paths.get(photoDirectory + imageUrl.split("/")[imageUrl.split("/").length - 1]);
            var fileStorageLocation = Paths.get(photoDirectory).toAbsolutePath().normalize();
            if(!Files.exists(fileStorageLocation)) { Files.createDirectories(fileStorageLocation); }
            if(Files.exists(existingImage)) { Files.deleteIfExists(existingImage); }
            var filename = randomUUUID.get() + fileExtension.apply(image.getOriginalFilename());
            Files.copy(image.getInputStream(), fileStorageLocation.resolve(filename), REPLACE_EXISTING);
            return ServletUriComponentsBuilder.fromCurrentContextPath().path("/user/image/" + filename).toUriString();
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("Impossible de sauvegardé l'image");
        }
    }

    // USER PATIENT MANAGEMENT

    /**
     * Vérifie si un utilisateur existe dans la base de données à partir de son UUID.
     *
     * @param userUuid UUID de l'utilisateur.
     * @return true si l'utilisateur existe, false sinon.
     */
    @Override
    public boolean userExistsByUuid(String userUuid) {
        log.debug("Checking if user exists with UUID: {}", userUuid);

        try {
            userRepository.getUserByUuid(userUuid);
            return true;
        } catch (ApiException e) {
            return false;
        }
    }

}
