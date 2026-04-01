package com.openclassrooms.userservice.service.impl;


import com.openclassrooms.userservice.event.Event;
import com.openclassrooms.userservice.exception.ApiException;
import com.openclassrooms.userservice.model.Credential;
import com.openclassrooms.userservice.model.Device;
import com.openclassrooms.userservice.model.User;
import com.openclassrooms.userservice.repository.UserRepository;
import com.openclassrooms.userservice.service.UserService;

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
import java.util.function.Function;

import static com.openclassrooms.userservice.enumeration.EventType.PASSWORD_RESET;
import static com.openclassrooms.userservice.enumeration.EventType.USER_CREATED;
import static com.openclassrooms.userservice.util.UserUtils.randomUUUID;
//import static com.openclassrooms.userservice.util.UserUtils.verifyCode;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Map.of;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang.WordUtils.capitalizeFully;

/**
 * Implémentation du service métier responsable de la gestion des utilisateurs,
 * de l’authentification et de la sécurité dans l’application.
 * Cette classe agit comme une couche d’orchestration entre :
 * la couche <b>Repository</b> pour l’accès aux données</li>
 * la couche <b>Security</b> pour la gestion des mots de passe et du MFA
 * le système d’<b>événements</b> pour la communication asynchrone (Kafka, emails, etc.)
 * Les responsabilités principales de ce service incluent :
 * la création et la gestion des comptes utilisateurs
 * la vérification des comptes et des mots de passe
 * l'activation et la désactivation du MFA
 * la gestion des appareils de connexion
 * le téléversement des photos de profil
 * la publication d'événements métiers
 * Cette classe est annotée avec {@link Service} afin d'être gérée par le
 * conteneur Spring et injectée dans les contrôleurs.
 * L'annotation {@link lombok.RequiredArgsConstructor} permet l’injection automatique des dépendances finales.

 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-05-01
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder;
    private final ApplicationEventPublisher publisher;
    @Value("${app.photo.directory}")
    private String photoDirectory;


    /**
     * @param email adresse e-mail de l'utilisateur
     * @return utilisateur correspondant à l'adresse e-mail
     */
    @Override
    public User getUserByEmail(String email) {
        return userRepository.getUserByEmail(email);
    }


    /**
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
     * Valide un compte utilisateur à partir d’un token d’activation.
     * Cette opération :
     * active le compte</li>
     * supprime le token</li>>
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
     * Vérifie la validité d’un token de réinitialisation de mot de passe.
     * Si le token est valide, l’utilisateur correspondant est retourné.
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
     * Active l'authentification multi-facteurs pour un utilisateur.
     * @param userUuid identifiant de l’utilisateur
     * @return utilisateur après activation du MFA
     */
    @Override
    public User enableMfa(String userUuid) {
        return userRepository.enableMfa(userUuid);
    }

    /**
     * Désactive l’authentification multi-facteurs pour un utilisateur.
     * @param userUuid identifiant de l’utilisateur
     * @return utilisateur après désactivation
     */
    @Override
    public User disableMfa(String userUuid) {
        return userRepository.disableMfa(userUuid);
    }

    /**
     * Téléverse et associe une photo de profil à un utilisateur.
     * L'image est stockée sur le serveur et une URL est générée.
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
     * Bascule l'état du compte utilisateur.
     * Cette méthode inverse l'état actuel :
     * expiré ↔ valide
     * verrouillé ↔ déverrouillé
     * activé ↔ désactivé
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
     * Modifie le mot de passe d’un utilisateur.
     * Le mot de passe actuel est vérifié avant la mise à jour.
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
     * Lance le processus de réinitialisation du mot de passe.
     * Un token est généré et envoyé par e-mail à l’utilisateur.
     * @param email adresse e-mail de l'utilisateur
     */
    @Override
    public void resetPassword(String email) {
        var user = userRepository.getUserByEmail(email);
        var passwordToken = userRepository.getPasswordToken(user.getUserId());
        if(!nonNull(passwordToken)) {
            var newToken = userRepository.createPasswordToken(user.getUserId());
            publisher.publishEvent(new Event(PASSWORD_RESET, of("token", newToken, "email", email, "name", Objects.requireNonNull(capitalizeFully(user.getFirstName())))));
        } else if (passwordToken.isExpired()) {
            userRepository.deletePasswordToken(user.getUserId());
            var newToken = userRepository.createPasswordToken(user.getUserId());
            publisher.publishEvent(new Event(PASSWORD_RESET, of("token", newToken, "email", email, "name", capitalizeFully(user.getFirstName()))));
        } else {
            publisher.publishEvent(new Event(PASSWORD_RESET, of("token", passwordToken.getToken(), "email", email, "name", capitalizeFully(user.getFirstName()))));
        }
    }

    /**
     * Applique la réinitialisation du mot de passe.
     * Le token est validé avant la mise à jour du mot de passe.
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

    /**
     * USER PATIENT MANAGEMENT
     * Vérifie si un utilisateur existe dans la base de données à partir de son UUID.
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
